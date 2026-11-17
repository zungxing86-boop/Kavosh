package com.example.ui.parent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.common.formatTimestamp
import com.example.core.common.label
import com.example.core.common.t
import com.example.core.design.EmptyStateCard
import com.example.core.design.SuccessEmerald
import com.example.domain.model.AppLanguage
import com.example.domain.model.MissionStatus
import com.example.ui.MainUiState

@Composable
fun FamilyRoomScreen(
    uiState: MainUiState,
    parentState: ParentRoomUiState,
    onSetupPin: (String, String, String) -> Unit,
    onVerifyPin: (String) -> Unit,
    onRecoverPin: (String, String) -> Unit,
    onLockRoom: () -> Unit,
    onSendEncouragement: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = uiState.settings.language

    if (!parentState.parentSettings.isConfigured) {
        ParentSetupPinView(
            language = lang,
            errorText = parentState.errorText,
            onSetup = onSetupPin,
            modifier = modifier
        )
    } else if (!parentState.isUnlocked) {
        ParentLoginView(
            language = lang,
            parentState = parentState,
            onVerify = onVerifyPin,
            onRecover = onRecoverPin,
            modifier = modifier
        )
    } else {
        ParentDashboardView(
            uiState = uiState,
            parentState = parentState,
            onLockRoom = onLockRoom,
            onSendEncouragement = onSendEncouragement,
            modifier = modifier
        )
    }
}

@Composable
private fun ParentSetupPinView(
    language: AppLanguage,
    errorText: String?,
    onSetup: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var pin by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ElevatedCard(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.FamilyRestroom, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = t(language, "راه‌اندازی اتاق خانواده", "Set Up Family Room"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = t(
                            language,
                            "اتاق خانواده برای همراهی و تشویق والدین ساخته شده است. دفترچه خصوصی و پروژه‌های غیرهم‌رسانی‌شده در این بخش نمایش داده نمی‌شوند.",
                            "Family Room is for encouragement and collaboration. Private journals and unshared projects stay strictly private."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it },
                        label = { Text(t(language, "تعیین رمز عبور (حداقل ۴ کاراکتر)", "Create PIN / Passcode (4+ chars)")) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("parent_setup_pin_input")
                    )
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        label = { Text(t(language, "پرسش بازیابی رمز (مثلاً شهر مورد علاقه)", "Recovery question")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("parent_setup_question_input")
                    )
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it },
                        label = { Text(t(language, "پاسخ بازیابی", "Recovery answer")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("parent_setup_answer_input")
                    )
                    if (errorText != null) {
                        Text(errorText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { onSetup(pin, question, answer) },
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("parent_setup_confirm_button")
                    ) {
                        Text(t(language, "ذخیره رمز امن و ورود", "Save Secure PIN & Enter"))
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentLoginView(
    language: AppLanguage,
    parentState: ParentRoomUiState,
    onVerify: (String) -> Unit,
    onRecover: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var pin by remember { mutableStateOf("") }
    var showRecovery by remember { mutableStateOf(false) }
    var recAnswer by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ElevatedCard(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = t(language, "ورود به اتاق خانواده", "Family Room Access"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (parentState.lockoutRemainingSeconds > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = t(
                                    language,
                                    "قفل امنیتی فعال است: ${parentState.lockoutRemainingSeconds} ثانیه دیگر امتحان کنید.",
                                    "Cooldown active: try again in ${parentState.lockoutRemainingSeconds}s."
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it },
                        label = { Text(t(language, "رمز اتاق خانواده", "Enter PIN")) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = parentState.lockoutRemainingSeconds == 0L,
                        modifier = Modifier.fillMaxWidth().testTag("parent_login_pin_input")
                    )
                    if (parentState.errorText != null) {
                        Text(parentState.errorText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { onVerify(pin) },
                        enabled = parentState.lockoutRemainingSeconds == 0L,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("parent_login_submit_button")
                    ) {
                        Text(t(language, "ورود", "Unlock"))
                    }
                    TextButton(onClick = { showRecovery = !showRecovery }) {
                        Text(t(language, "فراموشی یا تغییر رمز", "Forgot or Reset PIN"))
                    }
                }
            }
        }

        if (showRecovery) {
            item {
                ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = t(language, "پرسش بازیابی: ${parentState.parentSettings.recoveryQuestion}", "Question: ${parentState.parentSettings.recoveryQuestion}"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = recAnswer,
                            onValueChange = { recAnswer = it },
                            label = { Text(t(language, "پاسخ بازیابی", "Your Answer")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { newPin = it },
                            label = { Text(t(language, "رمز جدید", "New PIN (4+ chars)")) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(onClick = { onRecover(recAnswer, newPin) }, modifier = Modifier.fillMaxWidth()) {
                            Text(t(language, "بازنشانی رمز", "Reset PIN"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentDashboardView(
    uiState: MainUiState,
    parentState: ParentRoomUiState,
    onLockRoom: () -> Unit,
    onSendEncouragement: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = uiState.settings.language
    val report = uiState.progressReport
    val completedMissions = uiState.missions.filter {
        uiState.missionProgressMap[it.id]?.status == MissionStatus.COMPLETED
    }
    var senderName by remember { mutableStateOf("") }
    var encouragementText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = t(lang, "اتاق خانواده • همراهی و حمایت", "Family Room • Support & Overview"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(onClick = onLockRoom, modifier = Modifier.testTag("parent_lock_button")) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(t(lang, "قفل", "Lock"))
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SuccessEmerald.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PrivacyTip, contentDescription = null, tint = SuccessEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = t(
                            lang,
                            "حریم خصوصی نوجوان حفظ می‌شود: دفترچه خصوصی، یادداشت‌های شخصی و پروژه‌های خصوصی اینجا نمایش داده نمی‌شوند.",
                            "Teen privacy protected: private journals, personal notes, and unshared projects are never shown here."
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (parentState.infoBanner != null) {
            item {
                AssistChip(onClick = {}, label = { Text(parentState.infoBanner) })
            }
        }

        // Overall Progress Summary (Read-only, zero punitive controls)
        if (report != null) {
            item {
                ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(t(lang, "پیشرفت کلی سفر", "Overall Journey Progress"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = t(
                                lang,
                                "سطح فعلی: ${report.currentLevel.nameFa} • مأموریت‌های تکمیل‌شده: ${report.missionsCompletedCount} از ${report.totalMissionsCount}",
                                "Current Level: ${report.currentLevel.nameEn} • Completed Missions: ${report.missionsCompletedCount}/${report.totalMissionsCount}"
                            )
                        )
                        Text(
                            text = t(
                                lang,
                                "مهارت‌های تمرین‌شده: ${report.practicedSkillsCount}/5 • دستاوردها: ${report.unlockedAchievementsCount}/${report.totalAchievementsCount}",
                                "Skills practiced: ${report.practicedSkillsCount}/5 • Achievements: ${report.unlockedAchievementsCount}/${report.totalAchievementsCount}"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Send Encouragement Note
        item {
            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(t(lang, "ارسال پیام حمایتی و دلگرمی", "Send an Encouragement Note"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = senderName,
                        onValueChange = { senderName = it },
                        label = { Text(t(lang, "نام شما (مثلاً مامان، بابا، خانواده)", "Your name (e.g. Mom, Dad, Family)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = encouragementText,
                        onValueChange = { encouragementText = it },
                        label = { Text(t(lang, "پیام تشویقی", "Supportive message")) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("parent_encouragement_input")
                    )
                    Button(
                        onClick = {
                            if (encouragementText.isNotBlank()) {
                                onSendEncouragement(senderName, encouragementText)
                                encouragementText = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("parent_send_encouragement_button")
                    ) {
                        Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(t(lang, "ثبت پیام حمایتی", "Send Note"))
                    }
                }
            }
        }

        // Shared Projects ONLY (strictly excludes unshared projects)
        item {
            Text(
                text = t(lang, "پروژه‌های هم‌رسانی‌شده (${parentState.sharedProjects.size})", "Shared Projects (${parentState.sharedProjects.size})"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (parentState.sharedProjects.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.FolderShared,
                    title = t(lang, "هنوز پروژه‌ای هم‌رسانی نشده", "No shared projects yet"),
                    subtitle = t(
                        lang,
                        "نوجوان خودش انتخاب می‌کند کدام پروژه را با خانواده به اشتراک بگذارد.",
                        "Your teen chooses which projects to share with the Family Room."
                    )
                )
            }
        } else {
            items(parentState.sharedProjects, key = { it.id }) { proj ->
                ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(proj.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(proj.status.label(lang), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        if (proj.goal.isNotBlank()) Text(proj.goal, style = MaterialTheme.typography.bodySmall)
                        if (proj.finalOutcome.isNotBlank()) {
                            Text(t(lang, "نتیجه: ${proj.finalOutcome}", "Outcome: ${proj.finalOutcome}"), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // Completed Missions & Practiced Skills Overview
        if (completedMissions.isNotEmpty()) {
            item {
                Text(t(lang, "مأموریت‌های تکمیل‌شده", "Completed Missions"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(completedMissions.take(8), key = { it.id }) { m ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessEmerald, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(t(lang, "روز ${m.dayNumber}: ${m.titleFa}", "Day ${m.dayNumber}: ${m.titleEn}"), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
