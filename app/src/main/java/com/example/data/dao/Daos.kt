package com.example.data.dao

import androidx.room.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE id = 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE id = 1")
    suspend fun getProfileOnce(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)

    @Query("DELETE FROM user_profiles")
    suspend fun clear()
}

@Dao
interface InterestDao {
    @Query("SELECT * FROM interests")
    fun observeAll(): Flow<List<InterestEntity>>

    @Query("SELECT COUNT(*) FROM interests")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<InterestEntity>)

    @Query("DELETE FROM interests")
    suspend fun clear()
}

@Dao
interface TerritoryDao {
    @Query("SELECT * FROM territories ORDER BY orderNum ASC")
    fun observeAll(): Flow<List<TerritoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TerritoryEntity>)
}

@Dao
interface MissionProgressDao {
    @Query("SELECT * FROM mission_progress")
    fun observeAll(): Flow<List<MissionProgressEntity>>

    @Query("SELECT * FROM mission_progress WHERE missionId = :missionId")
    suspend fun getByMissionId(missionId: String): MissionProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MissionProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MissionProgressEntity>)

    @Query("DELETE FROM mission_progress")
    suspend fun clear()
}

@Dao
interface XPTransactionDao {
    @Query("SELECT * FROM xp_transactions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<XPTransactionEntity>>

    @Query("SELECT * FROM xp_transactions")
    suspend fun getAllOnce(): List<XPTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(tx: XPTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<XPTransactionEntity>)

    @Query("DELETE FROM xp_transactions")
    suspend fun clear()
}

@Dao
interface SkillProgressDao {
    @Query("SELECT * FROM skill_progress")
    fun observeAll(): Flow<List<SkillProgressEntity>>

    @Query("SELECT * FROM skill_progress WHERE skillId = :skillId")
    suspend fun getById(skillId: String): SkillProgressEntity?

    @Query("SELECT COUNT(*) FROM skill_progress")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SkillProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SkillProgressEntity>)

    @Query("DELETE FROM skill_progress")
    suspend fun clear()
}

@Dao
interface UserAchievementDao {
    @Query("SELECT * FROM user_achievements ORDER BY unlockedAt DESC")
    fun observeAll(): Flow<List<UserAchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<UserAchievementEntity>)

    @Query("DELETE FROM user_achievements")
    suspend fun clear()
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE sharedWithFamily = 1 ORDER BY updatedAt DESC")
    fun observeSharedWithFamily(): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: ProjectEntity): Long

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteById(projectId: Long)

    @Query("DELETE FROM projects")
    suspend fun clear()
}

@Dao
interface ProjectAttachmentDao {
    @Query("SELECT * FROM project_attachments WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeForProject(projectId: Long): Flow<List<ProjectAttachmentEntity>>

    @Query("SELECT * FROM project_attachments")
    suspend fun getAllOnce(): List<ProjectAttachmentEntity>

    @Query("SELECT * FROM project_attachments WHERE id = :id")
    suspend fun getById(id: Long): ProjectAttachmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ProjectAttachmentEntity): Long

    @Query("DELETE FROM project_attachments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM project_attachments")
    suspend fun clear()
}

@Dao
interface JournalEntryDao {
    @Query("SELECT * FROM journal_entries ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<JournalEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: JournalEntryEntity): Long

    @Query("DELETE FROM journal_entries WHERE id = :entryId")
    suspend fun deleteById(entryId: Long)

    @Query("DELETE FROM journal_entries")
    suspend fun clear()
}

@Dao
interface ParentSettingsDao {
    @Query("SELECT * FROM parent_settings WHERE id = 1")
    fun observe(): Flow<ParentSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: ParentSettingsEntity)

    @Query("DELETE FROM parent_settings")
    suspend fun clear()
}

@Dao
interface SupportMessageDao {
    @Query("SELECT * FROM support_messages ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SupportMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: SupportMessageEntity): Long

    @Query("UPDATE support_messages SET isRead = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM support_messages")
    suspend fun clear()
}
