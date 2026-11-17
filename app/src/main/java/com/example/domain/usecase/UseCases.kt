package com.example.domain.usecase

import com.example.domain.engine.*
import com.example.domain.model.*
import com.example.domain.repository.ContentRepository
import com.example.domain.repository.JourneyRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun currentDayEpoch(): Long {
    return LocalDate.now(ZoneId.systemDefault()).toEpochDay()
}

class StartMissionUseCase(
    private val repository: JourneyRepository,
    private val xpEngine: XpEngine
) {
    suspend operator fun invoke(
        mission: Mission,
        existingProgress: MissionProgress?,
        existingTransactions: List<XPTransaction>,
        isShortVersion: Boolean
    ): XPTransaction? {
        val now = System.currentTimeMillis()
        val updated = (existingProgress ?: MissionProgress(missionId = mission.id)).copy(
            status = MissionStatus.IN_PROGRESS,
            isShortVersion = isShortVersion,
            startedAt = existingProgress?.startedAt ?: now,
            updatedAt = now
        )
        val xpTx = xpEngine.computeAwardablePoints(
            existingTransactions = existingTransactions,
            missionId = mission.id,
            eventType = XpEventType.START_ACTIVITY,
            dayEpoch = currentDayEpoch()
        )
        repository.upsertMissionProgressAndAwardXp(updated, xpTx)
        return xpTx
    }
}

class ToggleMissionStepUseCase(
    private val repository: JourneyRepository,
    private val missionEngine: MissionEngine,
    private val xpEngine: XpEngine
) {
    suspend operator fun invoke(
        mission: Mission,
        existingProgress: MissionProgress?,
        stepIndex: Int,
        existingTransactions: List<XPTransaction>
    ): XPTransaction? {
        val base = existingProgress ?: MissionProgress(
            missionId = mission.id,
            status = MissionStatus.IN_PROGRESS,
            startedAt = System.currentTimeMillis()
        )
        val nextSteps = base.completedSteps.toMutableSet().apply {
            if (stepIndex in this) remove(stepIndex) else add(stepIndex)
        }
        val nextStatus = if (base.status == MissionStatus.SUGGESTED || base.status == MissionStatus.SAVED_FOR_LATER || base.status == MissionStatus.PAUSED) {
            MissionStatus.IN_PROGRESS
        } else {
            base.status
        }
        val updated = base.copy(
            status = nextStatus,
            completedSteps = nextSteps,
            startedAt = base.startedAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val xpTx = if (missionEngine.isMissionStepTriggeringContinuationXp(nextSteps.size, mission.stepsFa.size)) {
            xpEngine.computeAwardablePoints(
                existingTransactions = existingTransactions,
                missionId = mission.id,
                eventType = XpEventType.CONTINUE_STEPS,
                dayEpoch = currentDayEpoch()
            )
        } else null

        repository.upsertMissionProgressAndAwardXp(updated, xpTx)
        return xpTx
    }
}

class CompleteMissionReflectionUseCase(
    private val repository: JourneyRepository,
    private val contentRepository: ContentRepository,
    private val xpEngine: XpEngine,
    private val achievementEngine: AchievementEngine
) {
    suspend operator fun invoke(
        mission: Mission,
        existingProgress: MissionProgress?,
        resultNote: String,
        reflectionAnswers: List<String>,
        feedback: MissionFeedback?,
        existingTransactions: List<XPTransaction>,
        allProgress: List<MissionProgress>,
        projects: List<Project>,
        skillProgressList: List<SkillProgress>,
        unlockedAchievementIds: Set<String>,
        journalCount: Int,
        isPersian: Boolean
    ): Pair<XPTransaction?, List<UserAchievement>> {
        val now = System.currentTimeMillis()
        val base = existingProgress ?: MissionProgress(missionId = mission.id, startedAt = now)
        val wasAlreadyCompleted = base.status == MissionStatus.COMPLETED

        val updated = base.copy(
            status = MissionStatus.COMPLETED,
            resultNote = resultNote,
            reflectionAnswers = reflectionAnswers,
            feedback = feedback ?: base.feedback,
            completedAt = base.completedAt ?: now,
            updatedAt = now
        )

        val xpTx = xpEngine.computeAwardablePoints(
            existingTransactions = existingTransactions,
            missionId = mission.id,
            eventType = XpEventType.SUBMIT_RESULT,
            dayEpoch = currentDayEpoch()
        )
        repository.upsertMissionProgressAndAwardXp(updated, xpTx)

        if (!wasAlreadyCompleted) {
            val expLabel = if (isPersian) mission.titleFa else mission.titleEn
            repository.recordSkillExperience(
                skillIds = mission.relatedSkills,
                pointsPerSkill = 3,
                experienceLabel = expLabel
            )
        }

        val updatedProgressList = allProgress.filterNot { it.missionId == mission.id } + updated
        val allMissions = contentRepository.getMissions()
        val allAchievements = contentRepository.getAchievements()
        val newAchievements = achievementEngine.evaluateNewUnlocks(
            allAchievements = allAchievements,
            unlockedIds = unlockedAchievementIds,
            missions = allMissions,
            progressList = updatedProgressList,
            projects = projects,
            skillProgressList = skillProgressList,
            journalCount = journalCount
        )
        if (newAchievements.isNotEmpty()) {
            repository.unlockAchievements(newAchievements)
        }
        return xpTx to newAchievements
    }
}

class PauseOrSaveMissionUseCase(
    private val repository: JourneyRepository
) {
    suspend operator fun invoke(
        missionId: String,
        existingProgress: MissionProgress?,
        targetStatus: MissionStatus,
        skipReason: SkipReason? = null,
        skipNote: String = ""
    ) {
        val base = existingProgress ?: MissionProgress(missionId = missionId)
        val updated = base.copy(
            status = targetStatus,
            skipReason = skipReason ?: base.skipReason,
            skipNote = if (skipNote.isNotBlank()) skipNote else base.skipNote,
            updatedAt = System.currentTimeMillis()
        )
        repository.upsertMissionProgressAndAwardXp(updated, null)
    }
}

class GenerateProgressReportUseCase(
    private val xpEngine: XpEngine,
    private val missionEngine: MissionEngine
) {
    operator fun invoke(
        territories: List<Territory>,
        missions: List<Mission>,
        progressList: List<MissionProgress>,
        transactions: List<XPTransaction>,
        projects: List<Project>,
        skillProgressList: List<SkillProgress>,
        achievements: List<Achievement>,
        unlockedAchievements: List<UserAchievement>
    ): ProgressReport {
        val totalXp = xpEngine.calculateTotalXp(transactions)
        val todayXp = xpEngine.calculateTodayXp(transactions, currentDayEpoch())
        val (level, nextLevel, progress) = xpEngine.calculateLevelProgress(totalXp)

        val triedCount = progressList.count {
            it.status == MissionStatus.IN_PROGRESS ||
                it.status == MissionStatus.PAUSED ||
                it.status == MissionStatus.COMPLETED ||
                it.status == MissionStatus.SELECTED
        }
        val completedMissions = progressList.filter { it.status == MissionStatus.COMPLETED }
        val completedIds = completedMissions.map { it.missionId }.toSet()
        val exploredTerritories = territories.count { t ->
            missions.any { m -> m.territoryId == t.id && (m.id in completedIds || progressList.any { it.missionId == m.id && it.status == MissionStatus.IN_PROGRESS }) }
        }
        val currentTerritory = missionEngine.determineCurrentTerritory(territories, missions, progressList)

        return ProgressReport(
            totalXp = totalXp,
            currentLevel = level,
            nextLevel = nextLevel,
            xpProgressToNext = progress,
            todayXp = todayXp,
            dailyXpCap = XpEngine.DAILY_XP_CAP,
            missionsTriedCount = triedCount,
            missionsCompletedCount = completedMissions.size,
            totalMissionsCount = missions.size,
            activeProjectsCount = projects.count { it.status == ProjectStatus.IN_PROGRESS || it.status == ProjectStatus.READY_TO_START },
            completedProjectsCount = projects.count { it.status == ProjectStatus.COMPLETED },
            practicedSkillsCount = skillProgressList.count { it.practicedMissionsCount > 0 || it.experiencePoints > 0 },
            unlockedAchievementsCount = unlockedAchievements.size,
            totalAchievementsCount = achievements.size,
            territoriesExploredCount = exploredTerritories.coerceAtLeast(if (missions.isNotEmpty()) 1 else 0),
            currentTerritoryId = currentTerritory?.id ?: territories.firstOrNull()?.id ?: "territory_discovery"
        )
    }
}
