package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupManager
import com.example.data.content.JsonContentRepository
import com.example.data.database.JourneyDatabase
import com.example.data.repository.JourneyRepositoryImpl
import com.example.data.storage.InternalFileStorage
import com.example.data.storage.SettingsPreferencesStore
import com.example.domain.engine.SkillProgressEngine
import com.example.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JourneyDatabaseAndBackupTest {

    private lateinit var context: Context
    private lateinit var db: JourneyDatabase
    private lateinit var contentRepo: JsonContentRepository
    private lateinit var fileStorage: InternalFileStorage
    private lateinit var settingsStore: SettingsPreferencesStore
    private lateinit var repository: JourneyRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, JourneyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        contentRepo = JsonContentRepository(context)
        fileStorage = InternalFileStorage(context)
        settingsStore = SettingsPreferencesStore(context)
        repository = JourneyRepositoryImpl(db, contentRepo, SkillProgressEngine(), fileStorage)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `json content assets load all 30 missions, 4 territories, 5 skills, 14 interests, 12 badges, 6 companion modules`() = runTest {
        val territories = contentRepo.getTerritories()
        val missions = contentRepo.getMissions()
        val skills = contentRepo.getSkills()
        val interests = contentRepo.getDefaultInterests()
        val achievements = contentRepo.getAchievements()
        val companion = contentRepo.getCompanionModules()

        assertEquals(4, territories.size)
        assertEquals(30, missions.size)
        assertEquals((1..30).toList(), missions.map { it.dayNumber })
        assertEquals(5, skills.size)
        assertEquals(14, interests.size)
        assertEquals(12, achievements.size)
        assertEquals(6, companion.size)
    }

    @Test
    fun `room repository prevents duplicate xp transactions and isolates private projects`() = runTest {
        repository.ensureSeededBaseData()
        val tx = XPTransaction("mission_01_start_activity", "mission_01", XpEventType.START_ACTIVITY, 2, 20000L)
        repository.upsertMissionProgressAndAwardXp(
            MissionProgress("mission_01", MissionStatus.IN_PROGRESS),
            tx
        )
        // Attempt duplicate insertion
        repository.upsertMissionProgressAndAwardXp(
            MissionProgress("mission_01", MissionStatus.IN_PROGRESS, completedSteps = setOf(0)),
            tx
        )
        val allTxs = repository.xpTransactionsFlow.first()
        assertEquals(1, allTxs.size)

        // Private vs shared project isolation
        repository.createOrUpdateProject(
            Project(title = "Secret Sketchbook", description = "", goal = "", sharedWithFamily = false)
        )
        repository.createOrUpdateProject(
            Project(title = "Solar Car Model", description = "", goal = "", sharedWithFamily = true)
        )
        val allProjects = repository.projectsFlow.first()
        val sharedProjects = repository.sharedProjectsFlow.first()
        assertEquals(2, allProjects.size)
        assertEquals(1, sharedProjects.size)
        assertEquals("Solar Car Model", sharedProjects.first().title)
    }

    @Test
    fun `encrypted zip backup restores safely and rejects wrong password without wiping data`() = runTest {
        repository.ensureSeededBaseData()
        repository.saveUserProfile(
            UserProfile(
                displayName = "آرتین",
                avatarId = "astronaut",
                avatarColorHex = 0xFF00B4D8,
                onboardingCompleted = true
            )
        )
        repository.saveJournalEntry(JournalEntry(title = "راز من", content = "یک ایده جدید"))

        val backupManager = BackupManager(context, db, settingsStore, fileStorage)
        val outFile = File(context.cacheDir, "test_backup.zip")
        val outUri = Uri.fromFile(outFile)

        val exportResult = backupManager.createBackupToUri(outUri, includeJournal = true, password = "secret123")
        assertTrue(exportResult.isSuccess)
        assertTrue(exportResult.getOrThrow().isEncrypted)

        // Wrong password should fail without destroying existing profile
        val wrongRestore = backupManager.restoreBackup(outUri, password = "wrong")
        assertTrue(wrongRestore.isFailure)
        assertEquals("آرتین", repository.userProfileFlow.first()?.displayName)

        // Wipe and restore with correct password
        repository.resetAllUserData()
        assertNull(repository.userProfileFlow.first())

        val validRestore = backupManager.restoreBackup(outUri, password = "secret123")
        assertTrue(validRestore.isSuccess)
        assertEquals("آرتین", repository.userProfileFlow.first()?.displayName)
        assertEquals(1, repository.journalEntriesFlow.first().size)
        outFile.delete()
    }
}
