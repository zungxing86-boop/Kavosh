package com.example.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.domain.engine.ParentSecurityEngine
import com.example.domain.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ParentRoomUiState(
    val parentSettings: ParentSettings = ParentSettings(),
    val isUnlocked: Boolean = false,
    val errorText: String? = null,
    val lockoutRemainingSeconds: Long = 0L,
    val sharedProjects: List<Project> = emptyList(),
    val supportMessages: List<SupportMessage> = emptyList(),
    val infoBanner: String? = null
)

class ParentRoomViewModel(private val container: AppContainer) : ViewModel() {
    private val _isUnlocked = MutableStateFlow(false)
    private val _errorText = MutableStateFlow<String?>(null)
    private val _infoBanner = MutableStateFlow<String?>(null)
    private val _ticker = MutableStateFlow(System.currentTimeMillis())

    init {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                _ticker.value = System.currentTimeMillis()
            }
        }
    }

    val uiState: StateFlow<ParentRoomUiState> = combine(
        container.journeyRepository.parentSettingsFlow,
        container.journeyRepository.sharedProjectsFlow,
        container.journeyRepository.supportMessagesFlow,
        _isUnlocked,
        combine(_errorText, _infoBanner, _ticker) { e, i, t -> Triple(e, i, t) }
    ) { settingsOpt, shared, messages, unlocked, extras ->
        val settings = settingsOpt ?: ParentSettings()
        val (err, info, now) = extras
        val remaining = container.parentSecurityEngine.remainingLockoutSeconds(settings, now)
        ParentRoomUiState(
            parentSettings = settings,
            isUnlocked = unlocked,
            errorText = err,
            lockoutRemainingSeconds = remaining,
            sharedProjects = shared,
            supportMessages = messages,
            infoBanner = info
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ParentRoomUiState())

    fun lockRoom() {
        _isUnlocked.value = false
        _errorText.value = null
    }

    fun clearFeedback() {
        _errorText.value = null
        _infoBanner.value = null
    }

    fun setupPin(pin: String, recoveryQuestion: String, recoveryAnswer: String, isPersian: Boolean) {
        if (pin.trim().length < 4) {
            _errorText.value = if (isPersian) "رمز باید حداقل ۴ کاراکتر باشد." else "PIN must be at least 4 characters."
            return
        }
        if (recoveryQuestion.isBlank() || recoveryAnswer.isBlank()) {
            _errorText.value = if (isPersian) "پرسش و پاسخ بازیابی را وارد کنید." else "Please enter a recovery question and answer."
            return
        }
        viewModelScope.launch {
            val sec = container.parentSecurityEngine
            val pinSalt = sec.generateSaltHex()
            val pinHash = sec.hashSecret(pin.trim(), pinSalt)
            val recSalt = sec.generateSaltHex()
            val recHash = sec.hashSecret(recoveryAnswer.trim().lowercase(), recSalt)
            container.journeyRepository.saveParentSettings(
                ParentSettings(
                    id = 1L,
                    pinHash = pinHash,
                    pinSalt = pinSalt,
                    recoveryQuestion = recoveryQuestion.trim(),
                    recoveryAnswerHash = recHash,
                    recoveryAnswerSalt = recSalt,
                    failedAttempts = 0,
                    lockedUntilMillis = 0L,
                    isConfigured = true
                )
            )
            _isUnlocked.value = true
            _errorText.value = null
        }
    }

    fun verifyPin(pin: String, isPersian: Boolean) {
        val current = uiState.value.parentSettings
        val now = System.currentTimeMillis()
        val sec = container.parentSecurityEngine
        if (sec.isLockedOut(current, now)) {
            _errorText.value = if (isPersian) {
                "تعداد تلاش‌های ناموفق زیاد بود. لطفاً چند دقیقه صبر کنید."
            } else {
                "Too many failed attempts. Please wait until cooldown ends."
            }
            return
        }

        val ok = sec.verifySecret(pin.trim(), current.pinHash, current.pinSalt)
        viewModelScope.launch {
            if (ok) {
                container.journeyRepository.saveParentSettings(
                    current.copy(failedAttempts = 0, lockedUntilMillis = 0L)
                )
                _isUnlocked.value = true
                _errorText.value = null
            } else {
                val nextAttempts = current.failedAttempts + 1
                val shouldLock = nextAttempts >= ParentSecurityEngine.MAX_FAILED_ATTEMPTS
                val lockedUntil = if (shouldLock) now + ParentSecurityEngine.LOCKOUT_DURATION_MS else 0L
                container.journeyRepository.saveParentSettings(
                    current.copy(
                        failedAttempts = if (shouldLock) 0 else nextAttempts,
                        lockedUntilMillis = lockedUntil
                    )
                )
                _errorText.value = if (shouldLock) {
                    if (isPersian) "ورود موقتاً به مدت ۵ دقیقه قفل شد." else "Temporarily locked for 5 minutes."
                } else {
                    val left = ParentSecurityEngine.MAX_FAILED_ATTEMPTS - nextAttempts
                    if (isPersian) "رمز نادرست است ($left تلاش باقی‌مانده)." else "Incorrect PIN ($left attempts left)."
                }
            }
        }
    }

    fun recoverAndResetPin(recoveryAnswer: String, newPin: String, isPersian: Boolean) {
        val current = uiState.value.parentSettings
        val sec = container.parentSecurityEngine
        if (sec.isLockedOut(current)) {
            _errorText.value = if (isPersian) "در حال حاضر قفل امنیتی فعال است." else "Lockout is currently active."
            return
        }
        if (newPin.trim().length < 4) {
            _errorText.value = if (isPersian) "رمز جدید حداقل ۴ کاراکتر باشد." else "New PIN must be at least 4 characters."
            return
        }
        val validAnswer = sec.verifySecret(
            recoveryAnswer.trim().lowercase(),
            current.recoveryAnswerHash,
            current.recoveryAnswerSalt
        )
        if (!validAnswer) {
            _errorText.value = if (isPersian) "پاسخ بازیابی مطابقت ندارد." else "Recovery answer does not match."
            return
        }
        viewModelScope.launch {
            val newSalt = sec.generateSaltHex()
            val newHash = sec.hashSecret(newPin.trim(), newSalt)
            container.journeyRepository.saveParentSettings(
                current.copy(
                    pinHash = newHash,
                    pinSalt = newSalt,
                    failedAttempts = 0,
                    lockedUntilMillis = 0L
                )
            )
            _isUnlocked.value = true
            _errorText.value = null
            _infoBanner.value = if (isPersian) "رمز با موفقیت تغییر کرد." else "PIN updated successfully."
        }
    }

    fun sendEncouragement(senderLabel: String, messageText: String, isPersian: Boolean) {
        if (messageText.isBlank()) return
        viewModelScope.launch {
            container.journeyRepository.addSupportMessage(
                SupportMessage(
                    senderLabel = senderLabel.trim().ifBlank { if (isPersian) "خانواده" else "Family" },
                    message = messageText.trim()
                )
            )
            _infoBanner.value = if (isPersian) "پیام حمایتی ثبت شد!" else "Encouragement message saved!"
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ParentRoomViewModel(container) as T
        }
    }
}
