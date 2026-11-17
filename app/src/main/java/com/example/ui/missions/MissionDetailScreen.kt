package com.example.ui.missions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.common.label
import com.example.core.common.t
import com.example.core.design.CompassGold
import com.example.core.design.SuccessEmerald
import com.example.core.design.TerritoryBannerImage
import com.example.domain.model.*
import com.example.ui.MainUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MissionDetailScreen(
    missionId: String,
    uiState: MainUiState,
    onBack: () -> Unit,
    onStartMission: (Mission, Boolean) -> Unit,
    onToggleStep: (Mission, Int) -> Unit,
    onToggleShortVersion: (String, Boolean) -> Unit,
    onSaveDraft: (String, String, List<String>) -> Unit,
    onCompleteMission: (Mission, String, List<String>, MissionFeedback?) -> Unit,
    onPauseOrSave: (String, MissionStatus, SkipReason?, String) -> Unit
) {
    val lang = uiState.settings.language
    val mission = uiState.missions.firstOrNull { it.id == missionId }

    if (mission == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(t(lang, "مأموریت", "Mission")) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t(lang, "بازگشت", "Back"))
                        }
                    }
                )
            }
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text(t(lang, "مأموریت پیدا نشد.", "Mission not found."))
            }
        }
        return
    }

    val progress = uiState.missionProgressMap[mission.id]
    val isShort = progress?.isShortVersion ?: false
    var resultNote by remember(mission.id, progress?.resultNote) {
        mutableStateOf(progress?.resultNote ?: "")
    }
    val questions = if (lang == AppLanguage.PERSIAN) mission.reflectionQuestionsFa else mission.reflectionQuestionsEn
    val answers = remember(mission.id, progress?.reflectionAnswers) {
        mutableStateListOf<String>().apply {
            questions.forEachIndexed { idx, _ ->
                add(progress?.reflectionAnswers?.getOrNull(idx) ?: "")
            }
        }
    }
    var selectedFeedback by remember(mission.id, progress?.feedback) {
        mutableStateOf(progress?.feedback)
    }
    var showPauseReasonDialog by remember { mutableStateOf(false) }

    val steps = if (lang == AppLanguage.PERSIAN) mission.stepsFa else mission.stepsEn
    val tools = if (lang == AppLanguage.PERSIAN) mission.toolsFa else mission.toolsEn
    val skillNames = mission.relatedSkills.mapNotNull { sid ->
        uiState.skills.firstOrNull { it.id == sid }?.let { t(lang, it.nameFa, it.nameEn) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = t(lang, "روز ${mission.dayNumber} • ${mission.titleFa}", "Day ${mission.dayNumber} • ${mission.titleEn}"),
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("mission_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t(lang, "بازگشت", "Back"))
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                TerritoryBannerImage(
                    visualKey = mission.imageKey,
                    title = t(lang, mission.titleFa, mission.titleEn),
                    subtitle = progress?.status?.label(lang)
                        ?: t(lang, "آماده شروع • ${mission.durationMinutes} دقیقه", "Ready • ${mission.durationMinutes} min"),
                    accentColor = CompassGold,
                    height = 164.dp
                )
            }

            // Metadata Chips
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(t(lang, "${mission.durationMinutes} دقیقه", "${mission.durationMinutes} min")) },
                        leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text(mission.difficulty.label(lang)) },
                        leadingIcon = { Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    skillNames.forEach { sName ->
                        SuggestionChip(onClick = {}, label = { Text(sName) })
                    }
                }
            }

            // Story & Goal
            item {
                ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = t(lang, "داستان مأموریت", "Mission Story"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(t(lang, mission.storyFa, mission.storyEn), style = MaterialTheme.typography.bodyLarge)
                        HorizontalDivider()
                        Text(
                            text = t(lang, "هدف آموزشی", "Learning Goal"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(t(lang, mission.goalFa, mission.goalEn), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = t(lang, "راهنمای شروع: ${mission.startGuideFa}", "Quick Start Tip: ${mission.startGuideEn}"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Short Version Switch (for low energy / busy days)
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = t(lang, "نسخه کوتاه (روزهای پرمشغله)", "Short Version (Low-Energy Mode)"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = t(lang, mission.shortVersionFa, mission.shortVersionEn),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Switch(
                                checked = isShort,
                                onCheckedChange = { onToggleShortVersion(mission.id, it) },
                                modifier = Modifier.testTag("mission_short_version_switch")
                            )
                        }
                    }
                }
            }

            // Action Buttons (Start, Save for Later, Gentle Pause)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onStartMission(mission, isShort) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("mission_start_button")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (progress?.startedAt != null) t(lang, "در حال انجام", "Active (+XP)")
                            else t(lang, "شروع (+۲ XP)", "Start (+2 XP)")
                        )
                    }
                    OutlinedButton(
                        onClick = { onPauseOrSave(mission.id, MissionStatus.SAVED_FOR_LATER, null, "") },
                        modifier = Modifier.height(50.dp).testTag("mission_save_later_button")
                    ) {
                        Icon(Icons.Filled.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(t(lang, "برای بعد", "Later"))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(
                    onClick = { showPauseReasonDialog = true },
                    modifier = Modifier.testTag("mission_pause_reason_button")
                ) {
                    Icon(Icons.Filled.PauseCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(t(lang, "الان نمی‌خواهم ادامه دهم (توقف بدون جریمه)", "Not today / Pause gently (Zero penalty)"))
                }
            }

            // Tools Needed & Expected Output
            item {
                ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(t(lang, "وسایل مورد نیاز", "Tools Needed"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        tools.forEach { tool ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.BuildCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(tool, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        HorizontalDivider()
                        Text(t(lang, "خروجی مورد انتظار", "Expected Output"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(t(lang, mission.expectedOutputFa, mission.expectedOutputEn), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Checkable Steps (Auto-saved + Awards Continuation XP)
            item {
                Text(
                    text = t(lang, "مراحل انجام (ذخیره خودکار • ادامه مراحل +۲ XP)", "Steps (Auto-Saved • +2 XP on progress)"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(steps.size) { index ->
                val done = index in (progress?.completedSteps ?: emptySet())
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleStep(mission, index) }
                        .testTag("mission_step_checkbox_$index"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (done) SuccessEmerald.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = done,
                            onCheckedChange = { onToggleStep(mission, index) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${index + 1}. ${steps[index]}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Result, Reflection Questions, Feedback & Complete
            item {
                ElevatedCard(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = t(lang, "ثبت نتیجه و بازتاب (+۱ XP)", "Log Result & Reflection (+1 XP)"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = resultNote,
                            onValueChange = {
                                resultNote = it
                                onSaveDraft(mission.id, it, answers.toList())
                            },
                            label = { Text(t(lang, "چه نتیجه‌ای گرفتی یا چه چیزی ساختی؟", "What did you create, notice, or learn?")) },
                            minLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("mission_result_input"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        questions.forEachIndexed { idx, q ->
                            OutlinedTextField(
                                value = answers.getOrElse(idx) { "" },
                                onValueChange = { text ->
                                    if (idx < answers.size) {
                                        answers[idx] = text
                                        onSaveDraft(mission.id, resultNote, answers.toList())
                                    }
                                },
                                label = { Text(q) },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }

                        Text(
                            text = t(lang, "نظرت درباره این مأموریت چه بود؟", "How did this mission feel?"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MissionFeedback.entries.forEach { fb ->
                                FilterChip(
                                    selected = selectedFeedback == fb,
                                    onClick = { selectedFeedback = fb },
                                    label = { Text(fb.label(lang)) }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onCompleteMission(mission, resultNote, answers.toList(), selectedFeedback)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("mission_complete_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (progress?.status == MissionStatus.COMPLETED) {
                                    t(lang, "به‌روزرسانی نتیجه", "Update Reflection")
                                } else {
                                    t(lang, "ثبت تجربه و تکمیل مأموریت", "Save Reflection & Complete")
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPauseReasonDialog) {
        PauseMissionDialog(
            language = lang,
            onDismiss = { showPauseReasonDialog = false },
            onConfirm = { reason, note ->
                onPauseOrSave(mission.id, MissionStatus.PAUSED, reason, note)
                showPauseReasonDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PauseMissionDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (SkipReason, String) -> Unit
) {
    var selectedReason by remember { mutableStateOf(SkipReason.NO_TIME) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(t(language, "استراحت و تغییر برنامه کاملاً مجاز است", "Rest or Reschedule Anytime"))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = t(
                        language,
                        "هیچ امتیازی کم نمی‌شود! گفتن دلیل فقط کمک می‌کند پیشنهادهای بهتری برایت بسازیم.",
                        "Zero XP is ever lost! Sharing why simply helps tune better future suggestions."
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkipReason.entries.forEach { reason ->
                        FilterChip(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            label = { Text(reason.label(language)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(t(language, "یادداشت اختیاری", "Optional note")) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedReason, note) }) {
                Text(t(language, "ثبت و توقف آرام", "Pause Gently"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(t(language, "انصراف", "Cancel"))
            }
        }
    )
}
