package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.entity.*

@Database(
    entities = [
        UserProfileEntity::class,
        InterestEntity::class,
        TerritoryEntity::class,
        MissionProgressEntity::class,
        XPTransactionEntity::class,
        SkillProgressEntity::class,
        UserAchievementEntity::class,
        ProjectEntity::class,
        ProjectAttachmentEntity::class,
        JournalEntryEntity::class,
        ParentSettingsEntity::class,
        SupportMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class JourneyDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun interestDao(): InterestDao
    abstract fun territoryDao(): TerritoryDao
    abstract fun missionProgressDao(): MissionProgressDao
    abstract fun xpTransactionDao(): XPTransactionDao
    abstract fun skillProgressDao(): SkillProgressDao
    abstract fun userAchievementDao(): UserAchievementDao
    abstract fun projectDao(): ProjectDao
    abstract fun projectAttachmentDao(): ProjectAttachmentDao
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun parentSettingsDao(): ParentSettingsDao
    abstract fun supportMessageDao(): SupportMessageDao

    companion object {
        const val DB_NAME = "my_journey.db"

        // Example future migration scaffold to preserve user data across schema updates
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Reserved for additive non-destructive future migrations
            }
        }

        fun create(context: Context): JourneyDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                JourneyDatabase::class.java,
                DB_NAME
            ).build()
        }
    }
}
