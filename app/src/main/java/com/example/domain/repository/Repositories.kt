package com.example.domain.repository

import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ContentRepository {
    suspend fun getTerritories(): List<Territory>
    suspend fun getMissions(): List<Mission>
    suspend fun getMissionById(id: String): Mission?
    suspend fun getSkills(): List<Skill>
    suspend fun getAchievements(): List<Achievement>
    suspend fun getDefaultInterests(): List<Interest>
    suspend fun getCompanionModules(): List<CompanionModule>
}

interface SettingsRepository {
    val settingsFlow: Flow<AppSettings>
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setQuietHours(startHour: Int, endHour: Int)
    suspend fun resetSettings()
}

interface JourneyRepository {
    val userProfileFlow: Flow<UserProfile?>
    val interestsFlow: Flow<List<Interest>>
    val missionProgressFlow: Flow<List<MissionProgress>>
    val xpTransactionsFlow: Flow<List<XPTransaction>>
    val skillProgressFlow: Flow<List<SkillProgress>>
    val userAchievementsFlow: Flow<List<UserAchievement>>
    val projectsFlow: Flow<List<Project>>
    val sharedProjectsFlow: Flow<List<Project>>
    val journalEntriesFlow: Flow<List<JournalEntry>>
    val parentSettingsFlow: Flow<ParentSettings?>
    val supportMessagesFlow: Flow<List<SupportMessage>>

    suspend fun ensureSeededBaseData()
    suspend fun saveUserProfile(profile: UserProfile)
    suspend fun saveInterests(interests: List<Interest>)
    suspend fun updateAvatar(avatarId: String, colorHex: Long, accessory: String, customAvatarPath: String?)
    suspend fun updatePreferences(preferences: UserPreferences)

    suspend fun getMissionProgress(missionId: String): MissionProgress?
    suspend fun upsertMissionProgressAndAwardXp(
        progress: MissionProgress,
        xpTransaction: XPTransaction?
    )

    suspend fun recordSkillExperience(
        skillIds: List<String>,
        pointsPerSkill: Int,
        experienceLabel: String
    )

    suspend fun unlockAchievements(achievements: List<UserAchievement>)

    // Projects & Attachments
    fun observeProjectAttachments(projectId: Long): Flow<List<ProjectAttachment>>
    suspend fun getAllAttachments(): List<ProjectAttachment>
    suspend fun createOrUpdateProject(project: Project): Long
    suspend fun deleteProject(projectId: Long)
    suspend fun addProjectAttachment(attachment: ProjectAttachment): Long
    suspend fun deleteProjectAttachment(attachmentId: Long)

    // Private Journal (NEVER exposed to Parent Room)
    suspend fun saveJournalEntry(entry: JournalEntry): Long
    suspend fun deleteJournalEntry(entryId: Long)

    // Family Room / Parent Settings
    suspend fun saveParentSettings(settings: ParentSettings)
    suspend fun addSupportMessage(message: SupportMessage)
    suspend fun markSupportMessagesRead()

    // Full Reset & Atomic Backup Snapshot Helpers
    suspend fun resetAllUserData()
}
