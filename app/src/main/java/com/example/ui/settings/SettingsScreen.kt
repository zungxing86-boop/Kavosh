package com.example.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.common.formatTimestamp
import com.example.core.common.t
import com.example.core.design.*
import com.example.domain.model.*
import com.example.ui.MainUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: MainUiState,
    settingsState: SettingsUiState,
    viewModel: SettingsBackupViewModel,
    modifier: Modifier = Modifier
) {
    val lang = uiState.settings.language
    val isFa = lang == AppLanguage.PERSIAN
    val profile = uiState.userProfile

    var showAvatarSheet by remember { mutableStateOf(false) }
    var showInterestsDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    var exportIncludeJournal by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var restorePassword by remember { mutableStateOf("") }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(
                uri = uri,
                includeJournal = exportIncludeJournal,
                password = exportPassword.ifBlank { null },
                isPersian = isFa
            )
            exportPassword = ""
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.inspectBackupForRestore(uri, isFa)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setNotificationsEnabled(granted, lang, uiState.settings)
    }

    val avatarPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && profile != null) {
            viewModel.updateHeroAvatar(profile.avatarId, profile.avatarColorHex, profile.avatarAccessory, uri)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = t(lang, "تنظیمات و پشتیبان‌گیری", "Settings & Backup"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (settingsState.statusMessage != null) {
            item {
                AssistChip(
                    onClick = { viewModel.clearStatus() },
                    label = { Text(settingsState.statusMessage) },
                    leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) }
                )
            }
        }

        // Language & Theme
        item {
            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(t(lang, "زبان و نمایش", "Language & Theme"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = lang == AppLanguage.PERSIAN,
                            onClick = { viewModel.setLanguage(AppLanguage.PERSIAN) },
                            label = { Text("فارسی (RTL)") },
                            modifier = Modifier.testTag("settings_lang_fa")
                        )
                        FilterChip(
                            selected = lang == AppLanguage.ENGLISH,
                            onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                            label = { Text("English (LTR)") },
                            modifier = Modifier.testTag("settings_lang_en")
                        )
                    }

                    Text(t(lang, "پوسته رنگی", "Theme Mode"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            ThemeMode.LIGHT to t(lang, "روشن", "Light"),
                            ThemeMode.DARK to t(lang, "تیره", "Dark"),
                            ThemeMode.SYSTEM to t(lang, "هماهنگ با سیستم", "System")
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = uiState.settings.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("settings_theme_${mode.key}")
                            )
                        }
                    }
                }
            }
        }

        // Profile, Avatar & Interests
        item {
            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(t(lang, "قهرمان و علایق", "Hero & Interests"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showAvatarSheet = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Face, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(t(lang, "تغییر آواتار", "Edit Avatar"))
                        }
                        OutlinedButton(onClick = { showInterestsDialog = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Interests, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(t(lang, "ویرایش علایق", "Edit Interests"))
                        }
                    }
                }
            }
        }

        // Gentle Notifications & Privacy
        item {
            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t(lang, "اعلان‌های ملایم (اختیاری)", "Gentle Reminders (Optional)"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                t(lang, "بدون مزاحمت شبانه و بدون پیام سرزنش", "Quiet-hours aware, never nagging"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.settings.notificationsEnabled,
                            onCheckedChange = { enable ->
                                if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.setNotificationsEnabled(enable, lang, uiState.settings)
                                }
                            }
                        )
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = SuccessEmerald)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            t(
                                lang,
                                "حریم خصوصی: همه اطلاعات فقط روی دستگاه ذخیره می‌شوند و به هیچ سروری ارسال نمی‌شوند.",
                                "Privacy Guarantee: 100% on-device storage, zero tracking, zero server uploads."
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Encrypted ZIP Backup & Restore (SAF)
        item {
            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(t(lang, "پشتیبان‌گیری و بازیابی (ZIP)", "Backup & Restore (ZIP)"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        t(
                            lang,
                            "می‌توانی با رمز عبور دلخواه (AES-256) از کل پیشرفت، پروژه‌ها و به انتخاب خودت دفترچه پشتیبان بگیری.",
                            "Export your journey, projects, and optionally your journal into a standard or AES-encrypted ZIP."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { showExportDialog = true },
                            enabled = !settingsState.isBusy,
                            modifier = Modifier.weight(1f).testTag("settings_backup_export_button")
                        ) {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(t(lang, "پشتیبان‌گیری", "Export ZIP"))
                        }
                        OutlinedButton(
                            onClick = { openBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                            enabled = !settingsState.isBusy,
                            modifier = Modifier.weight(1f).testTag("settings_backup_restore_button")
                        ) {
                            Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(t(lang, "بازیابی", "Restore ZIP"))
                        }
                    }
                }
            }
        }

        // Reset & Version Info
        item {
            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = t(lang, "مسیر من | My Journey • نسخه ۱.۰ (آفلاین)", "My Journey | مسیر من • Version 1.0 (Offline)"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().testTag("settings_reset_app_button")
                    ) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(t(lang, "بازنشانی کامل برنامه", "Reset All App Data"))
                    }
                }
            }
        }
    }

    if (showAvatarSheet && profile != null) {
        var avatarId by remember { mutableStateOf(profile.avatarId) }
        var colorHex by remember { mutableStateOf(profile.avatarColorHex) }
        var accessory by remember { mutableStateOf(profile.avatarAccessory) }

        AlertDialog(
            onDismissRequest = { showAvatarSheet = false },
            title = { Text(t(lang, "شخصی‌سازی آواتار", "Customize Avatar")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HeroAvatarPresets.forEach { p ->
                            FilterChip(
                                selected = avatarId == p.id,
                                onClick = { avatarId = p.id },
                                label = { Text(t(lang, p.labelFa, p.labelEn)) }
                            )
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HeroAccessoryOptions.forEach { acc ->
                            FilterChip(
                                selected = accessory == acc.id,
                                onClick = { accessory = acc.id },
                                label = { Text(t(lang, acc.labelFa, acc.labelEn)) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            avatarPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Text(t(lang, "تصویر گالری", "Pick Photo"))
                        }
                        if (profile.customAvatarPath != null) {
                            TextButton(onClick = { viewModel.clearCustomAvatarPhoto() }) {
                                Text(t(lang, "حذف عکس", "Clear Photo"))
                            }
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeroColorOptions.forEach { c ->
                            val hex = c.toArgb().toLong() and 0xFFFFFFFFL
                            FilterChip(
                                selected = colorHex == hex,
                                onClick = { colorHex = hex },
                                label = { Text("●", color = c) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateHeroAvatar(avatarId, colorHex, accessory, null)
                    showAvatarSheet = false
                }) {
                    Text(t(lang, "ذخیره", "Save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAvatarSheet = false }) { Text(t(lang, "بستن", "Close")) }
            }
        )
    }

    if (showInterestsDialog) {
        var currentInterests by remember(uiState.interests) { mutableStateOf(uiState.interests) }
        AlertDialog(
            onDismissRequest = { showInterestsDialog = false },
            title = { Text(t(lang, "ویرایش علایق", "Edit Interests")) },
            text = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    currentInterests.forEachIndexed { idx, interest ->
                        FilterChip(
                            selected = interest.isSelected,
                            onClick = {
                                currentInterests = currentInterests.toMutableList().also {
                                    it[idx] = interest.copy(isSelected = !interest.isSelected)
                                }
                            },
                            label = { Text(t(lang, interest.nameFa, interest.nameEn)) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateInterests(currentInterests)
                    showInterestsDialog = false
                }) {
                    Text(t(lang, "ذخیره", "Save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showInterestsDialog = false }) { Text(t(lang, "انصراف", "Cancel")) }
            }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(t(lang, "تنظیمات پشتیبان ZIP", "Backup Export Options")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = exportIncludeJournal, onCheckedChange = { exportIncludeJournal = it })
                        Text(t(lang, "شامل کردن دفترچه خصوصی (با تأیید من)", "Include Private Journal (explicitly opted-in)"))
                    }
                    OutlinedTextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it },
                        label = { Text(t(lang, "رمزگذاری پشتیبان (اختیاری)", "Encryption password (optional)")) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showExportDialog = false
                    createBackupLauncher.launch("MyJourney_Backup_${System.currentTimeMillis()}.zip")
                }) {
                    Text(t(lang, "انتخاب محل ذخیره", "Choose Destination"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text(t(lang, "انصراف", "Cancel")) }
            }
        )
    }

    settingsState.pendingRestoreSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissPendingRestore() },
            title = { Text(t(lang, "تأیید بازیابی پشتیبان", "Confirm Backup Restore")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t(lang, "قهرمان: ${summary.heroName}", "Hero: ${summary.heroName}"))
                    Text(t(lang, "تاریخ: ${formatTimestamp(summary.createdAt, lang)}", "Date: ${formatTimestamp(summary.createdAt, lang)}"))
                    Text(t(lang, "مأموریت‌های تکمیل‌شده: ${summary.missionsCompleted} • پروژه‌ها: ${summary.projectsCount}", "Missions completed: ${summary.missionsCompleted} • Projects: ${summary.projectsCount}"))
                    if (summary.isEncrypted) {
                        OutlinedTextField(
                            value = restorePassword,
                            onValueChange = { restorePassword = it },
                            label = { Text(t(lang, "رمز فایل پشتیبان", "Backup Password")) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.confirmRestore(restorePassword.ifBlank { null }, isFa)
                    restorePassword = ""
                }) {
                    Text(t(lang, "تأیید و بازیابی ایمن", "Confirm & Restore"))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPendingRestore() }) {
                    Text(t(lang, "انصراف", "Cancel"))
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(t(lang, "بازنشانی کامل برنامه؟", "Reset All Data?")) },
            text = {
                Text(t(lang, "تمام اطلاعات محلی شامل مأموریت‌ها، پروژه‌ها و دفترچه پاک می‌شوند. پیشنهاد می‌شود ابتدا پشتیبان بگیرید.", "All local data (missions, projects, journal) will be erased. Consider exporting a backup first."))
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetApp(isFa)
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(t(lang, "بله، بازنشانی شود", "Yes, Reset"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text(t(lang, "انصراف", "Cancel")) }
            }
        )
    }
}
