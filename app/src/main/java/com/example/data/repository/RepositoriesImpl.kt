package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.database.JourneyDatabase
import com.example.data.entity.*
import com.example.data.storage.InternalFileStorage
import com.example.data.storage.SettingsPreferencesStore
import com.example.domain.engine.SkillProgressEngine
import com.example.domain.model.*
import com.example.domain.repository.ContentRepository
import com.example.domain.repository.JourneyRepository
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val store: SettingsPreferencesStore
) : SettingsRepository {
    override val settingsFlow: Flow<AppSettings> = store.settingsFlow

    override suspend fun setLanguage(language: AppLanguage) = store.setLanguage(language)
    override suspend fun setThemeMode(themeMode: ThemeMode) = store.setThemeMode(themeMode)
    override suspend fun setNotificationsEnabled(enabled: Boolean) = store.setNotificationsEnabled(enabled)
    override suspend fun setQuietHours(startHour: Int, endHour: Int) = store.setQuietHours(startHour, endHour)
    override suspend fun resetSettings() = store.reset()
}

class JourneyRepositoryImpl(
    private val db: JourneyDatabase,
    private val contentRepository: ContentRepository,
    private val skillEngine: SkillProgressEngine,
    private val fileStorage: InternalFileStorage
) : JourneyRepository {

    override val userProfileFlow: Flow<UserProfile?> =
        db.userProfileDao().observeProfile().map { it?.toDomain() }

    override val interestsFlow: Flow<List<Interest>> =
        db.interestDao().observeAll().map { list -> list.map { it.toDomain() } }

    override val missionProgressFlow: Flow<List<MissionProgress>> =
        db.missionProgressDao().observeAll().map { list -> list.map { it.toDomain() } }

    override val xpTransactionsFlow: Flow<List<XPTransaction>> =
        db.xpTransactionDao().observeAll().map { list -> list.map { it.toDomain() } }

    override val skillProgressFlow: Flow<List<SkillProgress>> =
        db.skillProgressDao().observeAll().map { list -> list.map { it.toDomain() } }

    override val userAchievementsFlow: Flow<List<UserAchievement>> =
        db.userAchievementDao().observeAll().map { list -> list.map { it.toDomain() } }

    override val projectsFlow: Flow<List<Project>> =
        db.projectDao().observeAll().map { list -> list.map { it.toDomain() } }

    override val sharedProjectsFlow: Flow<List<Project>> =
        db.projectDao().observeSharedWithFamily().map { list -> list.map { it.toDomain() } }

    override val journalEntriesFlow: Flow<List<JournalEntry>> =
        db.journalEntryDao().observeAll().map { list -> list.map { it.toDomain() } }

    override val parentSettingsFlow: Flow<ParentSettings?> =
        db.parentSettingsDao().observe().map { it?.toDomain() }

    override val supportMessagesFlow: Flow<List<SupportMessage>> =
        db.supportMessageDao().observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun ensureSeededBaseData() {
        val territories = contentRepository.getTerritories()
        db.territoryDao().insertAll(territories.map { TerritoryEntity.fromDomain(it) })

        if (db.interestDao().count() == 0) {
            val defaultInterests = contentRepository.getDefaultInterests()
            db.interestDao().insertAll(defaultInterests.map { InterestEntity.fromDomain(it) })
        }

        if (db.skillProgressDao().count() == 0) {
            val skills = contentRepository.getSkills()
            db.skillProgressDao().insertAll(
                skills.map { skill ->
                    SkillProgressEntity.fromDomain(SkillProgress(skillId = skill.id))
                }
            )
        }
    }

    override suspend fun saveUserProfile(profile: UserProfile) {
        db.userProfileDao().upsert(UserProfileEntity.fromDomain(profile))
    }

    override suspend fun saveInterests(interests: List<Interest>) {
        db.interestDao().insertAll(interests.map { InterestEntity.fromDomain(it) })
    }

    override suspend fun updateAvatar(
        avatarId: String,
        colorHex: Long,
        accessory: String,
        customAvatarPath: String?
    ) {
        val current = db.userProfileDao().getProfileOnce()?.toDomain() ?: return
        db.userProfileDao().upsert(
            UserProfileEntity.fromDomain(
                current.copy(
                    avatarId = avatarId,
                    avatarColorHex = colorHex,
                    avatarAccessory = accessory,
                    customAvatarPath = customAvatarPath
                )
            )
        )
    }

    override suspend fun updatePreferences(preferences: UserPreferences) {
        val current = db.userProfileDao().getProfileOnce()?.toDomain() ?: return
        db.userProfileDao().upsert(UserProfileEntity.fromDomain(current.copy(preferences = preferences)))
    }

    override suspend fun getMissionProgress(missionId: String): MissionProgress? {
        return db.missionProgressDao().getByMissionId(missionId)?.toDomain()
    }

    override suspend fun upsertMissionProgressAndAwardXp(
        progress: MissionProgress,
        xpTransaction: XPTransaction?
    ) {
        db.withTransaction {
            db.missionProgressDao().upsert(MissionProgressEntity.fromDomain(progress))
            if (xpTransaction != null) {
                db.xpTransactionDao().insertIfAbsent(XPTransactionEntity.fromDomain(xpTransaction))
            }
        }
    }

    override suspend fun recordSkillExperience(
        skillIds: List<String>,
        pointsPerSkill: Int,
        experienceLabel: String
    ) {
        if (skillIds.isEmpty()) return
        db.withTransaction {
            for (skillId in skillIds) {
                val current = db.skillProgressDao().getById(skillId)?.toDomain()
                    ?: SkillProgress(skillId = skillId)
                val updated = skillEngine.computeUpdatedSkill(current, pointsPerSkill, experienceLabel)
                db.skillProgressDao().upsert(SkillProgressEntity.fromDomain(updated))
            }
        }
    }

    override suspend fun unlockAchievements(achievements: List<UserAchievement>) {
        if (achievements.isEmpty()) return
        db.userAchievementDao().insertAll(achievements.map { UserAchievementEntity.fromDomain(it) })
    }

    override fun observeProjectAttachments(projectId: Long): Flow<List<ProjectAttachment>> {
        return db.projectAttachmentDao().observeForProject(projectId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAllAttachments(): List<ProjectAttachment> {
        return db.projectAttachmentDao().getAllOnce().map { it.toDomain() }
    }

    override suspend fun createOrUpdateProject(project: Project): Long {
        return db.projectDao().upsert(ProjectEntity.fromDomain(project.copy(updatedAt = System.currentTimeMillis())))
    }

    override suspend fun deleteProject(projectId: Long) {
        val attachments = db.projectAttachmentDao().getAllOnce().filter { it.projectId == projectId }
        attachments.forEach { fileStorage.deleteFileIfExists(it.internalPath) }
        db.projectDao().deleteById(projectId)
    }

    override suspend fun addProjectAttachment(attachment: ProjectAttachment): Long {
        return db.projectAttachmentDao().insert(ProjectAttachmentEntity.fromDomain(attachment))
    }

    override suspend fun deleteProjectAttachment(attachmentId: Long) {
        val existing = db.projectAttachmentDao().getById(attachmentId)
        fileStorage.deleteFileIfExists(existing?.internalPath)
        db.projectAttachmentDao().deleteById(attachmentId)
    }

    override suspend fun saveJournalEntry(entry: JournalEntry): Long {
        return db.journalEntryDao().upsert(JournalEntryEntity.fromDomain(entry.copy(updatedAt = System.currentTimeMillis())))
    }

    override suspend fun deleteJournalEntry(entryId: Long) {
        db.journalEntryDao().deleteById(entryId)
    }

    override suspend fun saveParentSettings(settings: ParentSettings) {
        db.parentSettingsDao().upsert(ParentSettingsEntity.fromDomain(settings))
    }

    override suspend fun addSupportMessage(message: SupportMessage) {
        db.supportMessageDao().insert(SupportMessageEntity.fromDomain(message))
    }

    override suspend fun markSupportMessagesRead() {
        db.supportMessageDao().markAllRead()
    }

    override suspend fun resetAllUserData() {
        db.withTransaction {
            db.userProfileDao().clear()
            db.missionProgressDao().clear()
            db.xpTransactionDao().clear()
            db.skillProgressDao().clear()
            db.userAchievementDao().clear()
            db.projectAttachmentDao().clear()
            db.projectDao().clear()
            db.journalEntryDao().clear()
            db.parentSettingsDao().clear()
            db.supportMessageDao().clear()
            db.interestDao().clear()
        }
        fileStorage.clearAllUserFiles()
        ensureSeededBaseData()
    }
}
