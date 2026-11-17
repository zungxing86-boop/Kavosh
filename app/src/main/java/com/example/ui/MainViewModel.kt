package com.example.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.core.common.GentleReminderType
import com.example.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MainUiState(
    val isLoading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val userProfile: UserProfile? = null,
    val interests: List<Interest> = emptyList(),
    val territories: List<Territory> = emptyList(),
    val missions: List<Mission> = emptyList(),
    val missionProgressMap: Map<String, MissionProgress> = emptyMap(),
    val xpTransactions: List<XPTransaction> = emptyList(),
    val skills: List<Skill> = emptyList(),
    val skillProgressMap: Map<String, SkillProgress> = emptyMap(),
    val achievements: List<Achievement> = emptyList(),
    val unlockedAchievements: List<UserAchievement> = emptyList(),
    val projects: List<Project> = emptyList(),
    val journalEntries: List<JournalEntry> = emptyList(),
    val supportMessages: List<SupportMessage> = emptyList(),
    val companionModules: List<CompanionModule> = emptyList(),
    val dailyRecommendations: DailyRecommendations? = null,
    val progressReport: ProgressReport? = null,
    val snackbarEvent: String? = null
)

class MainViewModel(private val container: AppContainer) : ViewModel() {

    private val staticContentFlow = flow {
        container.journeyRepository.ensureSeededBaseData()
        val territories = container.contentRepository.getTerritories()
        val missions = container.contentRepository.getMissions()
        val skills = container.contentRepository.getSkills()
        val achievements = container.contentRepository.getAchievements()
        val companion = container.contentRepository.getCompanionModules()
        emit(StaticContentBundle(territories, missions, skills, achievements, companion))
    }

    private val _snackbar = MutableStateFlow<String?>(null)

    private data class StaticContentBundle(
        val territories: List<Territory>,
        val missions: List<Mission>,
        val skills: List<Skill>,
        val achievements: List<Achievement>,
        val companionModules: List<CompanionModule>
    )

    private val userProgressBundle = combine(
        container.journeyRepository.userProfileFlow,
        container.journeyRepository.interestsFlow,
        container.journeyRepository.missionProgressFlow,
        container.journeyRepository.xpTransactionsFlow,
        container.journeyRepository.skillProgressFlow
    ) { profile, interests, missionProg, xpTxs, skillProg ->
        UserProgressBundle(profile, interests, missionProg, xpTxs, skillProg)
    }

    private val creativeBundle = combine(
        container.journeyRepository.userAchievementsFlow,
        container.journeyRepository.projectsFlow,
        container.journeyRepository.journalEntriesFlow,
        container.journeyRepository.supportMessagesFlow,
        container.settingsRepository.settingsFlow
    ) { unlockedAch, projects, journal, supportMsgs, settings ->
        CreativeBundle(unlockedAch, projects, journal, supportMsgs, settings)
    }

    private data class UserProgressBundle(
        val profile: UserProfile?,
        val interests: List<Interest>,
        val missionProgress: List<MissionProgress>,
        val xpTransactions: List<XPTransaction>,
        val skillProgress: List<SkillProgress>
    )

    private data class CreativeBundle(
        val unlockedAchievements: List<UserAchievement>,
        val projects: List<Project>,
        val journalEntries: List<JournalEntry>,
        val supportMessages: List<SupportMessage>,
        val settings: AppSettings
    )

    val uiState: StateFlow<MainUiState> = combine(
        staticContentFlow,
        userProgressBundle,
        creativeBundle,
        _snackbar
    ) { content, userProg, creative, snack ->
        val progressMap = userProg.missionProgress.associateBy { it.missionId }
        val skillMap = userProg.skillProgress.associateBy { it.skillId }
        val prefs = userProg.profile?.preferences ?: UserPreferences()

        val recommendations = container.recommendationEngine.generateDailyRecommendations(
            missions = content.missions,
            progressMap = progressMap,
            selectedInterests = userProg.interests,
            skillProgressMap = skillMap,
            preferences = prefs
        )

        val report = container.generateProgressReportUseCase(
            territories = content.territories,
            missions = content.missions,
            progressList = userProg.missionProgress,
            transactions = userProg.xpTransactions,
            projects = creative.projects,
            skillProgressList = userProg.skillProgress,
            achievements = content.achievements,
            unlockedAchievements = creative.unlockedAchievements
        )

        MainUiState(
            isLoading = false,
            settings = creative.settings,
            userProfile = userProg.profile,
            interests = userProg.interests,
            territories = content.territories,
            missions = content.missions,
            missionProgressMap = progressMap,
            xpTransactions = userProg.xpTransactions,
            skills = content.skills,
            skillProgressMap = skillMap,
            achievements = content.achievements,
            unlockedAchievements = creative.unlockedAchievements,
            projects = creative.projects,
            journalEntries = creative.journalEntries,
            supportMessages = creative.supportMessages,
            companionModules = content.companionModules,
            dailyRecommendations = recommendations,
            progressReport = report,
            snackbarEvent = snack
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(isLoading = true)
    )

    fun clearSnackbar() {
        _snackbar.value = null
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val current = uiState.value.settings.language
            val next = if (current == AppLanguage.PERSIAN) AppLanguage.ENGLISH else AppLanguage.PERSIAN
            container.settingsRepository.setLanguage(next)
        }
    }

    fun completeOnboarding(
        displayName: String,
        avatarId: String,
        avatarColorHex: Long,
        avatarAccessory: String,
        customAvatarUri: Uri?,
        selectedInterestIds: Set<String>,
        preferences: UserPreferences
    ) {
        viewModelScope.launch {
            val customPath = customAvatarUri?.let { uri ->
                container.fileStorage.copyAvatarImage(uri).getOrNull()
            }
            val cleanName = displayName.trim().ifBlank {
                if (uiState.value.settings.language == AppLanguage.PERSIAN) "کاوشگر جوان" else "Young Explorer"
            }
            container.journeyRepository.saveUserProfile(
                UserProfile(
                    id = 1L,
                    displayName = cleanName,
                    avatarId = avatarId,
                    avatarColorHex = avatarColorHex,
                    avatarAccessory = avatarAccessory,
                    customAvatarPath = customPath,
                    preferences = preferences,
                    onboardingCompleted = true
                )
            )
            val updatedInterests = uiState.value.interests.map {
                it.copy(isSelected = it.id in selectedInterestIds)
            }
            if (updatedInterests.isNotEmpty()) {
                container.journeyRepository.saveInterests(updatedInterests)
            }
        }
    }

    fun selectMission(missionId: String) {
        viewModelScope.launch {
            val existing = container.journeyRepository.getMissionProgress(missionId)
            if (existing == null || existing.status == MissionStatus.SUGGESTED) {
                container.pauseOrSaveMissionUseCase(
                    missionId = missionId,
                    existingProgress = existing,
                    targetStatus = MissionStatus.SELECTED
                )
            }
        }
    }

    fun startMission(mission: Mission, isShortVersion: Boolean) {
        viewModelScope.launch {
            val state = uiState.value
            val existing = state.missionProgressMap[mission.id]
            val xpTx = container.startMissionUseCase(
                mission = mission,
                existingProgress = existing,
                existingTransactions = state.xpTransactions,
                isShortVersion = isShortVersion
            )
            if (xpTx != null) {
                val msg = if (state.settings.language == AppLanguage.PERSIAN) {
                    "+${xpTx.points} XP برای شروع مأموریت ثبت شد!"
                } else {
                    "+${xpTx.points} XP earned for starting!"
                }
                _snackbar.value = msg
            }
        }
    }

    fun toggleMissionStep(mission: Mission, stepIndex: Int) {
        viewModelScope.launch {
            val state = uiState.value
            val existing = state.missionProgressMap[mission.id]
            val xpTx = container.toggleMissionStepUseCase(
                mission = mission,
                existingProgress = existing,
                stepIndex = stepIndex,
                existingTransactions = state.xpTransactions
            )
            if (xpTx != null) {
                val msg = if (state.settings.language == AppLanguage.PERSIAN) {
                    "+${xpTx.points} XP برای پیشرفت مراحل ثبت شد!"
                } else {
                    "+${xpTx.points} XP earned for step progress!"
                }
                _snackbar.value = msg
            }
        }
    }

    fun toggleMissionShortVersion(missionId: String, isShort: Boolean) {
        viewModelScope.launch {
            val existing = container.journeyRepository.getMissionProgress(missionId)
                ?: MissionProgress(missionId = missionId)
            container.journeyRepository.upsertMissionProgressAndAwardXp(
                existing.copy(isShortVersion = isShort, updatedAt = System.currentTimeMillis()),
                null
            )
        }
    }

    fun saveMissionDraft(missionId: String, resultNote: String, reflectionAnswers: List<String>) {
        viewModelScope.launch {
            val existing = container.journeyRepository.getMissionProgress(missionId)
                ?: MissionProgress(missionId = missionId)
            container.journeyRepository.upsertMissionProgressAndAwardXp(
                existing.copy(
                    resultNote = resultNote,
                    reflectionAnswers = reflectionAnswers,
                    updatedAt = System.currentTimeMillis()
                ),
                null
            )
        }
    }

    fun completeMission(
        mission: Mission,
        resultNote: String,
        reflectionAnswers: List<String>,
        feedback: MissionFeedback?
    ) {
        viewModelScope.launch {
            val state = uiState.value
            val existing = state.missionProgressMap[mission.id]
            val isFa = state.settings.language == AppLanguage.PERSIAN
            val (xpTx, newAchievements) = container.completeMissionReflectionUseCase(
                mission = mission,
                existingProgress = existing,
                resultNote = resultNote,
                reflectionAnswers = reflectionAnswers,
                feedback = feedback,
                existingTransactions = state.xpTransactions,
                allProgress = state.missionProgressMap.values.toList(),
                projects = state.projects,
                skillProgressList = state.skillProgressMap.values.toList(),
                unlockedAchievementIds = state.unlockedAchievements.map { it.achievementId }.toSet(),
                journalCount = state.journalEntries.size,
                isPersian = isFa
            )
            val achMsg = if (newAchievements.isNotEmpty()) {
                val firstAch = state.achievements.firstOrNull { it.id == newAchievements.first().achievementId }
                val title = firstAch?.let { if (isFa) it.titleFa else it.titleEn } ?: ""
                container.notificationHelper.sendGentleReminder(
                    type = GentleReminderType.ACHIEVEMENT_CELEBRATION,
                    language = state.settings.language,
                    notificationsEnabled = state.settings.notificationsEnabled,
                    quietStartHour = state.settings.quietHoursStartHour,
                    quietEndHour = state.settings.quietHoursEndHour,
                    detailText = title
                )
                if (isFa) " • دستاورد جدید: $title" else " • Unlocked: $title"
            } else ""

            val xpText = if (xpTx != null) "+${xpTx.points} XP" else if (isFa) "ثبت شد" else "Saved"
            _snackbar.value = (if (isFa) "تجربه مأموریت ثبت شد ($xpText)$achMsg" else "Mission logged ($xpText)$achMsg")
        }
    }

    fun pauseOrSaveMission(
        missionId: String,
        targetStatus: MissionStatus,
        skipReason: SkipReason? = null,
        skipNote: String = ""
    ) {
        viewModelScope.launch {
            val existing = container.journeyRepository.getMissionProgress(missionId)
            container.pauseOrSaveMissionUseCase(
                missionId = missionId,
                existingProgress = existing,
                targetStatus = targetStatus,
                skipReason = skipReason,
                skipNote = skipNote
            )
            val isFa = uiState.value.settings.language == AppLanguage.PERSIAN
            _snackbar.value = when (targetStatus) {
                MissionStatus.SAVED_FOR_LATER -> if (isFa) "برای بعد ذخیره شد — هر وقت آماده بودی برگرد." else "Saved for later — come back anytime."
                MissionStatus.PAUSED -> if (isFa) "با آرامش متوقف شد؛ هیچ امتیازی کم نمی‌شود." else "Paused peacefully — zero penalties."
                else -> null
            }
        }
    }

    fun saveJournalEntry(entry: JournalEntry) {
        viewModelScope.launch {
            container.journeyRepository.saveJournalEntry(entry)
            evaluateAdditionalAchievements()
        }
    }

    fun deleteJournalEntry(entryId: Long) {
        viewModelScope.launch {
            container.journeyRepository.deleteJournalEntry(entryId)
        }
    }

    fun markSupportMessagesRead() {
        viewModelScope.launch {
            container.journeyRepository.markSupportMessagesRead()
        }
    }

    fun saveCompanionNotesToJournal(title: String, body: String) {
        viewModelScope.launch {
            container.journeyRepository.saveJournalEntry(
                JournalEntry(
                    title = title,
                    content = body,
                    category = JournalCategory.PLANS
                )
            )
            evaluateAdditionalAchievements()
            val isFa = uiState.value.settings.language == AppLanguage.PERSIAN
            _snackbar.value = if (isFa) "در دفترچه خصوصی ذخیره شد" else "Saved to Private Journal"
        }
    }

    fun createProjectFromCompanion(title: String, goal: String, steps: List<String>) {
        viewModelScope.launch {
            val project = Project(
                title = title,
                description = goal,
                goal = goal,
                steps = steps.filter { it.isNotBlank() }.mapIndexed { idx, s ->
                    ProjectStep(id = "step_${System.currentTimeMillis()}_$idx", title = s)
                },
                status = ProjectStatus.READY_TO_START
            )
            container.journeyRepository.createOrUpdateProject(project)
            evaluateAdditionalAchievements()
            val isFa = uiState.value.settings.language == AppLanguage.PERSIAN
            _snackbar.value = if (isFa) "پروژه جدید در آزمایشگاه ساخته شد!" else "New project created in Project Lab!"
        }
    }

    private suspend fun evaluateAdditionalAchievements() {
        val state = uiState.value
        val newUnlocks = container.achievementEngine.evaluateNewUnlocks(
            allAchievements = state.achievements,
            unlockedIds = state.unlockedAchievements.map { it.achievementId }.toSet(),
            missions = state.missions,
            progressList = state.missionProgressMap.values.toList(),
            projects = state.projects,
            skillProgressList = state.skillProgressMap.values.toList(),
            journalCount = state.journalEntries.size + 1
        )
        if (newUnlocks.isNotEmpty()) {
            container.journeyRepository.unlockAchievements(newUnlocks)
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(container) as T
        }
    }
}
