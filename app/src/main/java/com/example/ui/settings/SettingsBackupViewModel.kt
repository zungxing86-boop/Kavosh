package com.example.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.core.common.GentleReminderType
import com.example.data.backup.BackupSummary
import com.example.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val pendingRestoreUri: Uri? = null,
    val pendingRestoreSummary: BackupSummary? = null
)

class SettingsBackupViewModel(private val container: AppContainer) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun dismissPendingRestore() {
        _uiState.update { it.copy(pendingRestoreUri = null, pendingRestoreSummary = null) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { container.settingsRepository.setLanguage(language) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { container.settingsRepository.setThemeMode(mode) }
    }

    fun setNotificationsEnabled(enabled: Boolean, language: AppLanguage, settings: AppSettings) {
        viewModelScope.launch {
            container.settingsRepository.setNotificationsEnabled(enabled)
            if (enabled) {
                container.notificationHelper.sendGentleReminder(
                    type = GentleReminderType.MISSION_REMINDER,
                    language = language,
                    notificationsEnabled = true,
                    quietStartHour = settings.quietHoursStartHour,
                    quietEndHour = settings.quietHoursEndHour
                )
            }
        }
    }

    fun updateHeroAvatar(avatarId: String, colorHex: Long, accessory: String, customUri: Uri?) {
        viewModelScope.launch {
            val currentPath = container.journeyRepository.userProfileFlow.first()?.customAvatarPath
            val nextPath = if (customUri != null) {
                container.fileStorage.copyAvatarImage(customUri).getOrNull() ?: currentPath
            } else {
                currentPath
            }
            container.journeyRepository.updateAvatar(avatarId, colorHex, accessory, nextPath)
        }
    }

    fun clearCustomAvatarPhoto() {
        viewModelScope.launch {
            val profile = container.journeyRepository.userProfileFlow.first() ?: return@launch
            container.fileStorage.deleteFileIfExists(profile.customAvatarPath)
            container.journeyRepository.updateAvatar(
                profile.avatarId,
                profile.avatarColorHex,
                profile.avatarAccessory,
                null
            )
        }
    }

    fun updateInterests(interests: List<Interest>) {
        viewModelScope.launch {
            container.journeyRepository.saveInterests(interests)
        }
    }

    fun updatePreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            container.journeyRepository.updatePreferences(preferences)
        }
    }

    fun exportBackup(uri: Uri, includeJournal: Boolean, password: String?, isPersian: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusMessage = null) }
            val result = container.backupManager.createBackupToUri(uri, includeJournal, password)
            _uiState.update { state ->
                val msg = result.fold(
                    onSuccess = { summary ->
                        if (isPersian) {
                            "پشتیبان با موفقیت ذخیره شد (${summary.missionsCompleted} مأموریت، ${summary.projectsCount} پروژه)."
                        } else {
                            "Backup exported (${summary.missionsCompleted} missions, ${summary.projectsCount} projects)."
                        }
                    },
                    onFailure = { err ->
                        if (err.message == "LOW_STORAGE") {
                            if (isPersian) "فضای ذخیره‌سازی کافی نیست." else "Insufficient storage space."
                        } else {
                            if (isPersian) "خطا در ایجاد فایل پشتیبان." else "Failed to create backup ZIP."
                        }
                    }
                )
                state.copy(isBusy = false, statusMessage = msg)
            }
        }
    }

    fun inspectBackupForRestore(uri: Uri, isPersian: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusMessage = null) }
            val res = container.backupManager.inspectBackup(uri)
            res.onSuccess { summary ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        pendingRestoreUri = uri,
                        pendingRestoreSummary = summary
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        statusMessage = if (isPersian) "فایل پشتیبان معتبر نیست یا آسیب دیده است." else "Backup file is invalid or corrupted."
                    )
                }
            }
        }
    }

    fun confirmRestore(password: String?, isPersian: Boolean) {
        val uri = _uiState.value.pendingRestoreUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusMessage = null) }
            val res = container.backupManager.restoreBackup(uri, password)
            res.onSuccess { summary ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        pendingRestoreUri = null,
                        pendingRestoreSummary = null,
                        statusMessage = if (isPersian) {
                            "بازیابی برای ${summary.heroName} با موفقیت انجام شد!"
                        } else {
                            "Restored backup for ${summary.heroName}!"
                        }
                    )
                }
            }.onFailure { err ->
                val msg = when {
                    err.message == "PASSWORD_REQUIRED" -> if (isPersian) "رمز فایل پشتیبان را وارد کنید." else "Password required."
                    err.message == "LOW_STORAGE" -> if (isPersian) "فضای ذخیره‌سازی کافی نیست." else "Insufficient storage space."
                    else -> if (isPersian) "رمز اشتباه است یا فایل آسیب دیده؛ داده‌های فعلی دست‌نخورده ماندند." else "Wrong password or corrupt archive; current data preserved."
                }
                _uiState.update { it.copy(isBusy = false, statusMessage = msg) }
            }
        }
    }

    fun resetApp(isPersian: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            container.journeyRepository.resetAllUserData()
            container.settingsRepository.resetSettings()
            _uiState.update {
                it.copy(
                    isBusy = false,
                    statusMessage = if (isPersian) "برنامه بازنشانی شد." else "App reset completed."
                )
            }
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsBackupViewModel(container) as T
        }
    }
}
