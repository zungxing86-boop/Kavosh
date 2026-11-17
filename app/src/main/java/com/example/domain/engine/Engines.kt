package com.example.domain.engine

import com.example.domain.model.*
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class XpEngine {
    companion object {
        const val DAILY_XP_CAP = 5
    }

    fun calculateTotalXp(transactions: List<XPTransaction>): Int {
        return transactions.sumOf { it.points }.coerceAtLeast(0)
    }

    fun calculateTodayXp(transactions: List<XPTransaction>, dayEpoch: Long): Int {
        return transactions
            .filter { it.dayEpoch == dayEpoch }
            .sumOf { it.points }
            .coerceAtLeast(0)
    }

    fun buildEventId(missionId: String, eventType: XpEventType): String {
        return "${missionId}_${eventType.key}"
    }

    fun computeAwardablePoints(
        existingTransactions: List<XPTransaction>,
        missionId: String,
        eventType: XpEventType,
        dayEpoch: Long
    ): XPTransaction? {
        val eventId = buildEventId(missionId, eventType)
        if (existingTransactions.any { it.eventId == eventId }) {
            return null
        }
        val todayTotal = calculateTodayXp(existingTransactions, dayEpoch)
        val remainingToday = (DAILY_XP_CAP - todayTotal).coerceAtLeast(0)
        if (remainingToday <= 0) {
            return null
        }
        val awardPoints = minOf(eventType.points, remainingToday)
        return XPTransaction(
            eventId = eventId,
            missionId = missionId,
            eventType = eventType,
            points = awardPoints,
            dayEpoch = dayEpoch
        )
    }

    fun calculateLevelProgress(totalXp: Int): Triple<HeroLevel, HeroLevel?, Float> {
        val current = HeroLevel.forXp(totalXp)
        val next = HeroLevel.nextLevel(current) ?: return Triple(current, null, 1f)
        val span = (next.minXp - current.minXp).coerceAtLeast(1)
        val progress = ((totalXp - current.minXp).toFloat() / span.toFloat()).coerceIn(0f, 1f)
        return Triple(current, next, progress)
    }
}

class MissionEngine {
    fun determineCurrentTerritory(
        territories: List<Territory>,
        missions: List<Mission>,
        progressList: List<MissionProgress>
    ): Territory? {
        if (territories.isEmpty()) return null
        val completedIds = progressList.filter { it.status == MissionStatus.COMPLETED }.map { it.missionId }.toSet()
        val activeMission = progressList
            .filter { it.status == MissionStatus.IN_PROGRESS || it.status == MissionStatus.SELECTED }
            .maxByOrNull { it.updatedAt }
            ?.let { p -> missions.firstOrNull { it.id == p.missionId } }

        if (activeMission != null) {
            territories.firstOrNull { it.id == activeMission.territoryId }?.let { return it }
        }

        val nextUncompleted = missions.sortedBy { it.dayNumber }.firstOrNull { it.id !in completedIds }
        return if (nextUncompleted != null) {
            territories.firstOrNull { it.id == nextUncompleted.territoryId } ?: territories.first()
        } else {
            territories.last()
        }
    }

    fun calculateTerritoryProgress(
        territory: Territory,
        missions: List<Mission>,
        progressList: List<MissionProgress>
    ): Float {
        val territoryMissions = missions.filter { it.territoryId == territory.id }
        if (territoryMissions.isEmpty()) return 0f
        val completed = progressList.count { p ->
            p.status == MissionStatus.COMPLETED && territoryMissions.any { it.id == p.missionId }
        }
        return (completed.toFloat() / territoryMissions.size.toFloat()).coerceIn(0f, 1f)
    }

    fun isMissionStepTriggeringContinuationXp(
        completedStepsCount: Int,
        totalStepsCount: Int
    ): Boolean {
        if (totalStepsCount <= 0) return false
        return completedStepsCount >= 1
    }
}

class RecommendationEngine {
    fun generateDailyRecommendations(
        missions: List<Mission>,
        progressMap: Map<String, MissionProgress>,
        selectedInterests: List<Interest>,
        skillProgressMap: Map<String, SkillProgress>,
        preferences: UserPreferences
    ): DailyRecommendations? {
        if (missions.isEmpty()) return null

        val completedIds = progressMap.values
            .filter { it.status == MissionStatus.COMPLETED }
            .map { it.missionId }
            .toSet()

        val recentlySkippedOrDisliked = progressMap.values
            .filter {
                it.feedback == MissionFeedback.NOT_FOR_ME ||
                    it.skipReason == SkipReason.NOT_APPEALING
            }
            .map { it.missionId }
            .toSet()

        val activeInProgress = missions
            .sortedBy { it.dayNumber }
            .firstOrNull { m ->
                val st = progressMap[m.id]?.status
                st == MissionStatus.IN_PROGRESS || st == MissionStatus.SELECTED
            }

        val candidates = missions
            .filter { it.id !in completedIds }
            .sortedWith(compareBy<Mission> { it.id in recentlySkippedOrDisliked }.thenBy { it.dayNumber })

        val pool = candidates.ifEmpty { missions.sortedBy { it.dayNumber } }
        val interestIds = selectedInterests.filter { it.isSelected }.map { it.id }.toSet()

        // 1. Primary / Interest-aligned mission (prefer active in-progress or lowest day matching interest & preferences)
        val interestMission = activeInProgress ?: pool.maxByOrNull { mission ->
            scoreMission(mission, interestIds, preferences, progressMap[mission.id])
        } ?: pool.first()

        // 2. Least-practiced skill mission
        val leastPracticedSkillId = skillProgressMap.values
            .minByOrNull { it.experiencePoints }
            ?.skillId

        val skillPool = pool.filter { it.id != interestMission.id }
        val skillMission = skillPool
            .filter { leastPracticedSkillId != null && leastPracticedSkillId in it.relatedSkills }
            .maxByOrNull { scorePreferences(it, preferences) }
            ?: skillPool.firstOrNull()

        // 3. Discovery mission (outside existing selected interests / fresh territory experience)
        val discoveryPool = pool.filter { it.id != interestMission.id && it.id != skillMission?.id }
        val discoveryMission = discoveryPool
            .firstOrNull { m -> m.relatedInterests.none { it in interestIds } }
            ?: discoveryPool.firstOrNull()

        return DailyRecommendations(
            primaryMission = interestMission,
            interestMatchMission = interestMission,
            skillGrowthMission = skillMission,
            discoveryMission = discoveryMission
        )
    }

    private fun scoreMission(
        mission: Mission,
        interestIds: Set<String>,
        preferences: UserPreferences,
        progress: MissionProgress?
    ): Int {
        var score = 0
        if (mission.relatedInterests.any { it in interestIds }) score += 30
        if (progress?.status == MissionStatus.SAVED_FOR_LATER) score += 15
        if (progress?.status == MissionStatus.PAUSED) score += 12
        if (progress?.feedback == MissionFeedback.NOT_FOR_ME) score -= 25
        if (progress?.skipReason == SkipReason.TOO_HARD && mission.difficulty == DifficultyLevel.CHALLENGING) {
            score -= 20
        }
        score += scorePreferences(mission, preferences)
        // Slight preference for earlier uncompleted days for smooth progression
        score += (35 - mission.dayNumber).coerceAtLeast(0)
        return score
    }

    private fun scorePreferences(mission: Mission, preferences: UserPreferences): Int {
        var score = 0
        if (preferences.duration == DurationPreference.ANY || mission.durationCategory == preferences.duration) {
            score += 10
        }
        if (preferences.activityStyle == ActivityStylePreference.BALANCED || mission.activityStyle == preferences.activityStyle) {
            score += 8
        }
        if (preferences.social == SocialPreference.FLEXIBLE || mission.socialStyle == preferences.social) {
            score += 6
        }
        if (mission.difficulty.rank <= preferences.difficulty.rank) {
            score += 8
        }
        return score
    }
}

class SkillProgressEngine {
    companion object {
        const val XP_PER_SKILL_LEVEL = 10
    }

    fun calculateSkillLevel(experiencePoints: Int): Pair<Int, Float> {
        val safeXp = experiencePoints.coerceAtLeast(0)
        val level = (safeXp / XP_PER_SKILL_LEVEL) + 1
        val remainder = safeXp % XP_PER_SKILL_LEVEL
        val progress = remainder.toFloat() / XP_PER_SKILL_LEVEL.toFloat()
        return level to progress
    }

    fun computeUpdatedSkill(
        current: SkillProgress,
        addedPoints: Int,
        experienceLabel: String
    ): SkillProgress {
        val newXp = (current.experiencePoints + addedPoints).coerceAtLeast(0)
        val (newLevel, _) = calculateSkillLevel(newXp)
        val updatedExperiences = if (experienceLabel.isNotBlank() && experienceLabel !in current.recordedExperiences) {
            (listOf(experienceLabel) + current.recordedExperiences).take(15)
        } else {
            current.recordedExperiences
        }
        return current.copy(
            experiencePoints = newXp,
            level = newLevel,
            practicedMissionsCount = current.practicedMissionsCount + 1,
            recordedExperiences = updatedExperiences,
            updatedAt = System.currentTimeMillis()
        )
    }
}

class AchievementEngine {
    fun evaluateNewUnlocks(
        allAchievements: List<Achievement>,
        unlockedIds: Set<String>,
        missions: List<Mission>,
        progressList: List<MissionProgress>,
        projects: List<Project>,
        skillProgressList: List<SkillProgress>,
        journalCount: Int
    ): List<UserAchievement> {
        val now = System.currentTimeMillis()
        val completedMissions = progressList.filter { it.status == MissionStatus.COMPLETED }
        val completedIds = completedMissions.map { it.missionId }.toSet()
        val missionById = missions.associateBy { it.id }

        val newlyUnlocked = mutableListOf<UserAchievement>()

        for (ach in allAchievements) {
            if (ach.id in unlockedIds) continue
            val conditionMet = when (ach.id) {
                "ach_first_mission" -> completedMissions.isNotEmpty()
                "ach_curious_mind" -> completedMissions.size >= 3
                "ach_discovery_explorer" -> {
                    val t1 = missions.filter { it.territoryId == "territory_discovery" }.map { it.id }
                    t1.isNotEmpty() && t1.count { it in completedIds } >= 5
                }
                "ach_independent_learner" -> {
                    val t2 = missions.filter { it.territoryId == "territory_learning" }.map { it.id }
                    t2.isNotEmpty() && t2.count { it in completedIds } >= 3
                }
                "ach_little_teacher" -> "mission_09" in completedIds
                "ach_creative_builder" -> {
                    projects.isNotEmpty() || "mission_10" in completedIds
                }
                "ach_problem_solver" -> {
                    "mission_12" in completedIds || "mission_28" in completedIds
                }
                "ach_focused_mind" -> {
                    val t3 = missions.filter { it.territoryId == "territory_mind" }.map { it.id }
                    t3.isNotEmpty() && t3.count { it in completedIds } >= 3
                }
                "ach_all_skills" -> skillProgressList.count { it.practicedMissionsCount > 0 } >= 5
                "ach_journal_keeper" -> journalCount >= 3
                "ach_future_maker" -> {
                    projects.any { it.status == ProjectStatus.COMPLETED } ||
                        missions.filter { it.territoryId == "territory_future" }.count { it.id in completedIds } >= 4
                }
                "ach_path_master" -> completedMissions.size >= 20
                else -> false
            }
            if (conditionMet) {
                newlyUnlocked.add(UserAchievement(achievementId = ach.id, unlockedAt = now))
            }
        }
        return newlyUnlocked
    }
}

class ParentSecurityEngine {
    companion object {
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 5 * 60 * 1000L // 5 minutes
        private const val PBKDF2_ITERATIONS = 12000
        private const val KEY_LENGTH_BITS = 256
    }

    fun generateSaltHex(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashSecret(secret: String, saltHex: String): String {
        val saltBytes = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val spec = PBEKeySpec(secret.trim().toCharArray(), saltBytes, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun verifySecret(input: String, expectedHash: String, saltHex: String): Boolean {
        if (expectedHash.isBlank() || saltHex.isBlank()) return false
        val actualHash = hashSecret(input, saltHex)
        return MessageDigest.isEqual(actualHash.toByteArray(), expectedHash.toByteArray())
    }

    fun isLockedOut(settings: ParentSettings, nowMillis: Long = System.currentTimeMillis()): Boolean {
        return settings.lockedUntilMillis > nowMillis
    }

    fun remainingLockoutSeconds(settings: ParentSettings, nowMillis: Long = System.currentTimeMillis()): Long {
        val diff = settings.lockedUntilMillis - nowMillis
        return if (diff > 0) (diff / 1000L) + 1 else 0L
    }
}
