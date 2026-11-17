package com.example.domain.model

enum class AppLanguage(val code: String) {
    PERSIAN("fa"),
    ENGLISH("en");

    companion object {
        fun fromCode(code: String): AppLanguage = entries.firstOrNull { it.code == code } ?: PERSIAN
    }
}

enum class ThemeMode(val key: String) {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system");

    companion object {
        fun fromKey(key: String): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

enum class DurationPreference { SHORT, MEDIUM, LONG, ANY }
enum class ActivityStylePreference { PRACTICAL, REFLECTIVE, BALANCED }
enum class SocialPreference { SOLO, GROUP, FLEXIBLE }
enum class DifficultyLevel(val rank: Int) { EASY(1), MEDIUM(2), CHALLENGING(3) }

data class UserPreferences(
    val duration: DurationPreference = DurationPreference.ANY,
    val activityStyle: ActivityStylePreference = ActivityStylePreference.BALANCED,
    val social: SocialPreference = SocialPreference.FLEXIBLE,
    val difficulty: DifficultyLevel = DifficultyLevel.EASY
)

data class UserProfile(
    val id: Long = 1L,
    val displayName: String,
    val avatarId: String,
    val avatarColorHex: Long,
    val avatarAccessory: String = "compass",
    val customAvatarPath: String? = null,
    val preferences: UserPreferences = UserPreferences(),
    val onboardingCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class Interest(
    val id: String,
    val nameFa: String,
    val nameEn: String,
    val iconKey: String,
    val isSelected: Boolean = false
)

data class Territory(
    val id: String,
    val order: Int,
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
)

enum class MissionStatus(val key: String) {
    SUGGESTED("suggested"),
    SELECTED("selected"),
    IN_PROGRESS("in_progress"),
    PAUSED("paused"),
    COMPLETED("completed"),
    SAVED_FOR_LATER("saved_for_later");

    companion object {
        fun fromKey(key: String): MissionStatus = entries.firstOrNull { it.key == key } ?: SUGGESTED
    }
}

enum class MissionFeedback(val key: String, val score: Int) {
    LOVED_IT("loved_it", 4),
    INTERESTING("interesting", 3),
    OKAY("okay", 2),
    NOT_FOR_ME("not_for_me", 1);

    companion object {
        fun fromKey(key: String?): MissionFeedback? = entries.firstOrNull { it.key == key }
    }
}

enum class SkipReason(val key: String) {
    NO_TIME("no_time"),
    TOO_HARD("too_hard"),
    TIRED("tired"),
    DIDNT_KNOW_WHERE_TO_START("didnt_know_where_to_start"),
    NOT_APPEALING("not_appealing"),
    PREFERRED_REST("preferred_rest"),
    OTHER("other");

    companion object {
        fun fromKey(key: String?): SkipReason? = entries.firstOrNull { it.key == key }
    }
}

data class Mission(
    val id: String,
    val dayNumber: Int,
    val titleFa: String,
    val titleEn: String,
    val shortDescFa: String,
    val shortDescEn: String,
    val storyFa: String,
    val storyEn: String,
    val goalFa: String,
    val goalEn: String,
    val durationMinutes: Int,
    val durationCategory: DurationPreference,
    val difficulty: DifficultyLevel,
    val activityStyle: ActivityStylePreference,
    val socialStyle: SocialPreference,
    val relatedSkills: List<String>,
    val relatedInterests: List<String>,
    val toolsFa: List<String>,
    val toolsEn: List<String>,
    val stepsFa: List<String>,
    val stepsEn: List<String>,
    val expectedOutputFa: String,
    val expectedOutputEn: String,
    val shortVersionFa: String,
    val shortVersionEn: String,
    val reflectionQuestionsFa: List<String>,
    val reflectionQuestionsEn: List<String>,
    val startGuideFa: String,
    val startGuideEn: String,
    val territoryId: String,
    val imageKey: String
)

data class MissionProgress(
    val missionId: String,
    val status: MissionStatus = MissionStatus.SUGGESTED,
    val completedSteps: Set<Int> = emptySet(),
    val isShortVersion: Boolean = false,
    val resultNote: String = "",
    val reflectionAnswers: List<String> = emptyList(),
    val feedback: MissionFeedback? = null,
    val skipReason: SkipReason? = null,
    val skipNote: String = "",
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

enum class XpEventType(val key: String, val points: Int) {
    START_ACTIVITY("start_activity", 2),
    CONTINUE_STEPS("continue_steps", 2),
    SUBMIT_RESULT("submit_result", 1);

    companion object {
        fun fromKey(key: String): XpEventType? = entries.firstOrNull { it.key == key }
    }
}

data class XPTransaction(
    val eventId: String,
    val missionId: String,
    val eventType: XpEventType,
    val points: Int,
    val dayEpoch: Long,
    val createdAt: Long = System.currentTimeMillis()
)

enum class HeroLevel(val levelNumber: Int, val minXp: Int, val nameFa: String, val nameEn: String) {
    NEWCOMER(1, 0, "تازه‌وارد", "Newcomer"),
    SEEKER(2, 15, "جست‌وجوگر", "Seeker"),
    EXPLORER(3, 40, "کاوشگر", "Explorer"),
    BUILDER(4, 75, "سازنده", "Builder"),
    PATH_MASTER(5, 115, "استاد مسیر", "Path Master");

    companion object {
        fun forXp(xp: Int): HeroLevel {
            return entries.lastOrNull { xp >= it.minXp } ?: NEWCOMER
        }

        fun nextLevel(current: HeroLevel): HeroLevel? {
            return entries.firstOrNull { it.levelNumber == current.levelNumber + 1 }
        }
    }
}

data class Skill(
    val id: String,
    val nameFa: String,
    val nameEn: String,
    val descriptionFa: String,
    val descriptionEn: String,
    val iconKey: String,
    val colorHex: Long
)

data class SkillProgress(
    val skillId: String,
    val experiencePoints: Int = 0,
    val level: Int = 1,
    val practicedMissionsCount: Int = 0,
    val recordedExperiences: List<String> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Achievement(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val descriptionFa: String,
    val descriptionEn: String,
    val criteriaFa: String,
    val criteriaEn: String,
    val territoryId: String?,
    val iconKey: String
)

data class UserAchievement(
    val achievementId: String,
    val unlockedAt: Long
)

enum class ProjectStatus(val key: String) {
    INITIAL_IDEA("initial_idea"),
    READY_TO_START("ready_to_start"),
    IN_PROGRESS("in_progress"),
    PAUSED("paused"),
    COMPLETED("completed"),
    ARCHIVED("archived");

    companion object {
        fun fromKey(key: String): ProjectStatus = entries.firstOrNull { it.key == key } ?: INITIAL_IDEA
    }
}

data class ProjectStep(
    val id: String,
    val title: String,
    val isDone: Boolean = false
)

data class Project(
    val id: Long = 0L,
    val title: String,
    val description: String,
    val goal: String,
    val steps: List<ProjectStep> = emptyList(),
    val notes: String = "",
    val status: ProjectStatus = ProjectStatus.INITIAL_IDEA,
    val finalOutcome: String = "",
    val sharedWithFamily: Boolean = false,
    val startedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ProjectAttachment(
    val id: Long = 0L,
    val projectId: Long,
    val fileName: String,
    val mimeType: String,
    val internalPath: String,
    val fileSizeBytes: Long,
    val createdAt: Long = System.currentTimeMillis()
)

enum class JournalCategory(val key: String) {
    THOUGHTS("thoughts"),
    FEELINGS("feelings"),
    IDEAS("ideas"),
    EXPERIENCES("experiences"),
    PLANS("plans"),
    QUESTIONS("questions"),
    DAILY_NOTES("daily_notes");

    companion object {
        fun fromKey(key: String): JournalCategory = entries.firstOrNull { it.key == key } ?: DAILY_NOTES
    }
}

data class JournalEntry(
    val id: Long = 0L,
    val title: String,
    val content: String,
    val category: JournalCategory = JournalCategory.DAILY_NOTES,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class AppSettings(
    val language: AppLanguage = AppLanguage.PERSIAN,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = false,
    val quietHoursStartHour: Int = 21,
    val quietHoursEndHour: Int = 8,
    val privacyShowProjectsDefault: Boolean = false
)

data class ParentSettings(
    val id: Long = 1L,
    val pinHash: String = "",
    val pinSalt: String = "",
    val recoveryQuestion: String = "",
    val recoveryAnswerHash: String = "",
    val recoveryAnswerSalt: String = "",
    val failedAttempts: Int = 0,
    val lockedUntilMillis: Long = 0L,
    val isConfigured: Boolean = false
)

data class SupportMessage(
    val id: Long = 0L,
    val senderLabel: String,
    val message: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class CompanionStep(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val promptFa: String,
    val promptEn: String,
    val tipFa: String,
    val tipEn: String
)

data class CompanionModule(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val subtitleFa: String,
    val subtitleEn: String,
    val iconKey: String,
    val steps: List<CompanionStep>
)

data class DailyRecommendations(
    val primaryMission: Mission,
    val interestMatchMission: Mission?,
    val skillGrowthMission: Mission?,
    val discoveryMission: Mission?
)

data class ProgressReport(
    val totalXp: Int,
    val currentLevel: HeroLevel,
    val nextLevel: HeroLevel?,
    val xpProgressToNext: Float,
    val todayXp: Int,
    val dailyXpCap: Int,
    val missionsTriedCount: Int,
    val missionsCompletedCount: Int,
    val totalMissionsCount: Int,
    val activeProjectsCount: Int,
    val completedProjectsCount: Int,
    val practicedSkillsCount: Int,
    val unlockedAchievementsCount: Int,
    val totalAchievementsCount: Int,
    val territoriesExploredCount: Int,
    val currentTerritoryId: String
)
