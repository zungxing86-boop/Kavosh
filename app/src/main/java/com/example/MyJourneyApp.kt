package com.example

import android.app.Application
import com.example.core.common.NotificationHelper
import com.example.data.backup.BackupManager
import com.example.data.content.JsonContentRepository
import com.example.data.database.JourneyDatabase
import com.example.data.repository.JourneyRepositoryImpl
import com.example.data.repository.SettingsRepositoryImpl
import com.example.data.storage.InternalFileStorage
import com.example.data.storage.SettingsPreferencesStore
import com.example.domain.engine.*
import com.example.domain.repository.ContentRepository
import com.example.domain.repository.JourneyRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.usecase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(application: Application) {
    val database: JourneyDatabase by lazy { JourneyDatabase.create(application) }
    val settingsStore: SettingsPreferencesStore by lazy { SettingsPreferencesStore(application) }
    val fileStorage: InternalFileStorage by lazy { InternalFileStorage(application) }

    val xpEngine = XpEngine()
    val missionEngine = MissionEngine()
    val recommendationEngine = RecommendationEngine()
    val skillProgressEngine = SkillProgressEngine()
    val achievementEngine = AchievementEngine()
    val parentSecurityEngine = ParentSecurityEngine()

    val contentRepository: ContentRepository by lazy { JsonContentRepository(application) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(settingsStore) }
    val journeyRepository: JourneyRepository by lazy {
        JourneyRepositoryImpl(database, contentRepository, skillProgressEngine, fileStorage)
    }

    val backupManager: BackupManager by lazy {
        BackupManager(application, database, settingsStore, fileStorage)
    }

    val startMissionUseCase by lazy { StartMissionUseCase(journeyRepository, xpEngine) }
    val toggleMissionStepUseCase by lazy { ToggleMissionStepUseCase(journeyRepository, missionEngine, xpEngine) }
    val completeMissionReflectionUseCase by lazy {
        CompleteMissionReflectionUseCase(journeyRepository, contentRepository, xpEngine, achievementEngine)
    }
    val pauseOrSaveMissionUseCase by lazy { PauseOrSaveMissionUseCase(journeyRepository) }
    val generateProgressReportUseCase by lazy { GenerateProgressReportUseCase(xpEngine, missionEngine) }

    val notificationHelper by lazy { NotificationHelper(application) }
}

class MyJourneyApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notificationHelper.createNotificationChannel()
        appScope.launch {
            container.journeyRepository.ensureSeededBaseData()
        }
    }
}
