package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.*
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: Long = 1L,
    val displayName: String,
    val avatarId: String,
    val avatarColorHex: Long,
    val avatarAccessory: String,
    val customAvatarPath: String?,
    val durationPref: String,
    val activityStylePref: String,
    val socialPref: String,
    val difficultyPref: String,
    val onboardingCompleted: Boolean,
    val createdAt: Long
) {
    fun toDomain(): UserProfile = UserProfile(
        id = id,
        displayName = displayName,
        avatarId = avatarId,
        avatarColorHex = avatarColorHex,
        avatarAccessory = avatarAccessory,
        customAvatarPath = customAvatarPath,
        preferences = UserPreferences(
            duration = runCatching { DurationPreference.valueOf(durationPref) }.getOrDefault(DurationPreference.ANY),
            activityStyle = runCatching { ActivityStylePreference.valueOf(activityStylePref) }.getOrDefault(ActivityStylePreference.BALANCED),
            social = runCatching { SocialPreference.valueOf(socialPref) }.getOrDefault(SocialPreference.FLEXIBLE),
            difficulty = runCatching { DifficultyLevel.valueOf(difficultyPref) }.getOrDefault(DifficultyLevel.EASY)
        ),
        onboardingCompleted = onboardingCompleted,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(profile: UserProfile): UserProfileEntity = UserProfileEntity(
            id = profile.id,
            displayName = profile.displayName,
            avatarId = profile.avatarId,
            avatarColorHex = profile.avatarColorHex,
            avatarAccessory = profile.avatarAccessory,
            customAvatarPath = profile.customAvatarPath,
            durationPref = profile.preferences.duration.name,
            activityStylePref = profile.preferences.activityStyle.name,
            socialPref = profile.preferences.social.name,
            difficultyPref = profile.preferences.difficulty.name,
            onboardingCompleted = profile.onboardingCompleted,
            createdAt = profile.createdAt
        )
    }
}

@Entity(tableName = "interests")
data class InterestEntity(
    @PrimaryKey val id: String,
    val nameFa: String,
    val nameEn: String,
    val iconKey: String,
    val isSelected: Boolean
) {
    fun toDomain(): Interest = Interest(id, nameFa, nameEn, iconKey, isSelected)

    companion object {
        fun fromDomain(interest: Interest): InterestEntity =
            InterestEntity(interest.id, interest.nameFa, interest.nameEn, interest.iconKey, interest.isSelected)
    }
}

@Entity(tableName = "territories")
data class TerritoryEntity(
    @PrimaryKey val id: String,
    val orderNum: Int,
    val nameFa: String,
    val nameEn: String,
    val subtitleFa: String,
    val subtitleEn: String,
    val descriptionFa: String,
    val descriptionEn: String,
    val dayStart: Int,
    val dayEnd: Int,
    val themeColorHex: Long,
    val visualKey: String
) {
    companion object {
        fun fromDomain(t: Territory): TerritoryEntity = TerritoryEntity(
            id = t.id,
            orderNum = t.order,
            nameFa = t.nameFa,
            nameEn = t.nameEn,
            subtitleFa = t.subtitleFa,
            subtitleEn = t.subtitleEn,
            descriptionFa = t.descriptionFa,
            descriptionEn = t.descriptionEn,
            dayStart = t.dayStart,
            dayEnd = t.dayEnd,
            themeColorHex = t.themeColorHex,
            visualKey = t.visualKey
        )
    }
}

@Entity(
    tableName = "mission_progress",
    indices = [Index(value = ["status"]), Index(value = ["updatedAt"])]
)
data class MissionProgressEntity(
    @PrimaryKey val missionId: String,
    val status: String,
    val completedStepsJson: String,
    val isShortVersion: Boolean,
    val resultNote: String,
    val reflectionAnswersJson: String,
    val feedback: String?,
    val skipReason: String?,
    val skipNote: String,
    val startedAt: Long?,
    val completedAt: Long?,
    val updatedAt: Long
) {
    fun toDomain(): MissionProgress = MissionProgress(
        missionId = missionId,
        status = MissionStatus.fromKey(status),
        completedSteps = decodeIntSet(completedStepsJson),
        isShortVersion = isShortVersion,
        resultNote = resultNote,
        reflectionAnswers = decodeStringList(reflectionAnswersJson),
        feedback = MissionFeedback.fromKey(feedback),
        skipReason = SkipReason.fromKey(skipReason),
        skipNote = skipNote,
        startedAt = startedAt,
        completedAt = completedAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(p: MissionProgress): MissionProgressEntity = MissionProgressEntity(
            missionId = p.missionId,
            status = p.status.key,
            completedStepsJson = encodeIntSet(p.completedSteps),
            isShortVersion = p.isShortVersion,
            resultNote = p.resultNote,
            reflectionAnswersJson = encodeStringList(p.reflectionAnswers),
            feedback = p.feedback?.key,
            skipReason = p.skipReason?.key,
            skipNote = p.skipNote,
            startedAt = p.startedAt,
            completedAt = p.completedAt,
            updatedAt = p.updatedAt
        )
    }
}

@Entity(
    tableName = "xp_transactions",
    indices = [Index(value = ["dayEpoch"]), Index(value = ["missionId"])]
)
data class XPTransactionEntity(
    @PrimaryKey val eventId: String,
    val missionId: String,
    val eventType: String,
    val points: Int,
    val dayEpoch: Long,
    val createdAt: Long
) {
    fun toDomain(): XPTransaction = XPTransaction(
        eventId = eventId,
        missionId = missionId,
        eventType = XpEventType.fromKey(eventType) ?: XpEventType.START_ACTIVITY,
        points = points,
        dayEpoch = dayEpoch,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(tx: XPTransaction): XPTransactionEntity = XPTransactionEntity(
            eventId = tx.eventId,
            missionId = tx.missionId,
            eventType = tx.eventType.key,
            points = tx.points,
            dayEpoch = tx.dayEpoch,
            createdAt = tx.createdAt
        )
    }
}

@Entity(tableName = "skill_progress")
data class SkillProgressEntity(
    @PrimaryKey val skillId: String,
    val experiencePoints: Int,
    val level: Int,
    val practicedMissionsCount: Int,
    val recordedExperiencesJson: String,
    val updatedAt: Long
) {
    fun toDomain(): SkillProgress = SkillProgress(
        skillId = skillId,
        experiencePoints = experiencePoints,
        level = level,
        practicedMissionsCount = practicedMissionsCount,
        recordedExperiences = decodeStringList(recordedExperiencesJson),
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(sp: SkillProgress): SkillProgressEntity = SkillProgressEntity(
            skillId = sp.skillId,
            experiencePoints = sp.experiencePoints,
            level = sp.level,
            practicedMissionsCount = sp.practicedMissionsCount,
            recordedExperiencesJson = encodeStringList(sp.recordedExperiences),
            updatedAt = sp.updatedAt
        )
    }
}

@Entity(tableName = "user_achievements")
data class UserAchievementEntity(
    @PrimaryKey val achievementId: String,
    val unlockedAt: Long
) {
    fun toDomain(): UserAchievement = UserAchievement(achievementId, unlockedAt)

    companion object {
        fun fromDomain(ua: UserAchievement): UserAchievementEntity =
            UserAchievementEntity(ua.achievementId, ua.unlockedAt)
    }
}

@Entity(
    tableName = "projects",
    indices = [Index(value = ["status"]), Index(value = ["sharedWithFamily"])]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String,
    val goal: String,
    val stepsJson: String,
    val notes: String,
    val status: String,
    val finalOutcome: String,
    val sharedWithFamily: Boolean,
    val startedAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): Project = Project(
        id = id,
        title = title,
        description = description,
        goal = goal,
        steps = decodeProjectSteps(stepsJson),
        notes = notes,
        status = ProjectStatus.fromKey(status),
        finalOutcome = finalOutcome,
        sharedWithFamily = sharedWithFamily,
        startedAt = startedAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(p: Project): ProjectEntity = ProjectEntity(
            id = p.id,
            title = p.title,
            description = p.description,
            goal = p.goal,
            stepsJson = encodeProjectSteps(p.steps),
            notes = p.notes,
            status = p.status.key,
            finalOutcome = p.finalOutcome,
            sharedWithFamily = p.sharedWithFamily,
            startedAt = p.startedAt,
            updatedAt = p.updatedAt
        )
    }
}

@Entity(
    tableName = "project_attachments",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class ProjectAttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val projectId: Long,
    val fileName: String,
    val mimeType: String,
    val internalPath: String,
    val fileSizeBytes: Long,
    val createdAt: Long
) {
    fun toDomain(): ProjectAttachment = ProjectAttachment(
        id = id,
        projectId = projectId,
        fileName = fileName,
        mimeType = mimeType,
        internalPath = internalPath,
        fileSizeBytes = fileSizeBytes,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(a: ProjectAttachment): ProjectAttachmentEntity = ProjectAttachmentEntity(
            id = a.id,
            projectId = a.projectId,
            fileName = a.fileName,
            mimeType = a.mimeType,
            internalPath = a.internalPath,
            fileSizeBytes = a.fileSizeBytes,
            createdAt = a.createdAt
        )
    }
}

@Entity(
    tableName = "journal_entries",
    indices = [Index(value = ["category"]), Index(value = ["createdAt"])]
)
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val content: String,
    val category: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): JournalEntry = JournalEntry(
        id = id,
        title = title,
        content = content,
        category = JournalCategory.fromKey(category),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(e: JournalEntry): JournalEntryEntity = JournalEntryEntity(
            id = e.id,
            title = e.title,
            content = e.content,
            category = e.category.key,
            createdAt = e.createdAt,
            updatedAt = e.updatedAt
        )
    }
}

@Entity(tableName = "parent_settings")
data class ParentSettingsEntity(
    @PrimaryKey val id: Long = 1L,
    val pinHash: String,
    val pinSalt: String,
    val recoveryQuestion: String,
    val recoveryAnswerHash: String,
    val recoveryAnswerSalt: String,
    val failedAttempts: Int,
    val lockedUntilMillis: Long,
    val isConfigured: Boolean
) {
    fun toDomain(): ParentSettings = ParentSettings(
        id = id,
        pinHash = pinHash,
        pinSalt = pinSalt,
        recoveryQuestion = recoveryQuestion,
        recoveryAnswerHash = recoveryAnswerHash,
        recoveryAnswerSalt = recoveryAnswerSalt,
        failedAttempts = failedAttempts,
        lockedUntilMillis = lockedUntilMillis,
        isConfigured = isConfigured
    )

    companion object {
        fun fromDomain(s: ParentSettings): ParentSettingsEntity = ParentSettingsEntity(
            id = s.id,
            pinHash = s.pinHash,
            pinSalt = s.pinSalt,
            recoveryQuestion = s.recoveryQuestion,
            recoveryAnswerHash = s.recoveryAnswerHash,
            recoveryAnswerSalt = s.recoveryAnswerSalt,
            failedAttempts = s.failedAttempts,
            lockedUntilMillis = s.lockedUntilMillis,
            isConfigured = s.isConfigured
        )
    }
}

@Entity(tableName = "support_messages")
data class SupportMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val senderLabel: String,
    val message: String,
    val createdAt: Long,
    val isRead: Boolean
) {
    fun toDomain(): SupportMessage = SupportMessage(id, senderLabel, message, createdAt, isRead)

    companion object {
        fun fromDomain(m: SupportMessage): SupportMessageEntity =
            SupportMessageEntity(m.id, m.senderLabel, m.message, m.createdAt, m.isRead)
    }
}

private fun encodeIntSet(set: Set<Int>): String = JSONArray(set.toList()).toString()

private fun decodeIntSet(raw: String): Set<Int> {
    if (raw.isBlank()) return emptySet()
    return runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { arr.getInt(it) }.toSet()
    }.getOrDefault(emptySet())
}

private fun encodeStringList(list: List<String>): String = JSONArray(list).toString()

private fun decodeStringList(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    return runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(emptyList())
}

private fun encodeProjectSteps(steps: List<ProjectStep>): String {
    val arr = JSONArray()
    steps.forEach { s ->
        arr.put(JSONObject().apply {
            put("id", s.id)
            put("title", s.title)
            put("isDone", s.isDone)
        })
    }
    return arr.toString()
}

private fun decodeProjectSteps(raw: String): List<ProjectStep> {
    if (raw.isBlank()) return emptyList()
    return runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { idx ->
            val obj = arr.getJSONObject(idx)
            ProjectStep(
                id = obj.optString("id", "step_$idx"),
                title = obj.optString("title", ""),
                isDone = obj.optBoolean("isDone", false)
            )
        }
    }.getOrDefault(emptyList())
}
