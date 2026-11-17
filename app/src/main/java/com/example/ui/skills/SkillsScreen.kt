package com.example.ui.skills

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.common.t
import com.example.core.design.iconForSkillKey
import com.example.domain.engine.SkillProgressEngine
import com.example.domain.model.SkillProgress
import com.example.ui.MainUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkillsScreen(
    uiState: MainUiState,
    onOpenMission: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = uiState.settings.language
    val engine = remember { SkillProgressEngine() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = t(language = lang, fa = "پنج مهارت اصلی سفر", en = "Five Core Journey Skills"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = t(
                    lang,
                    "هر تجربه و مأموریت این مهارت‌ها را رشد می‌دهد؛ این عددها نشان‌دهنده تمرین‌های تو هستند، نه ارزش شخصیتی تو.",
                    "Every mission and project grows these skills—they reflect practice, never your worth as a person."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(uiState.skills, key = { it.id }) { skill ->
            val sp = uiState.skillProgressMap[skill.id] ?: SkillProgress(skillId = skill.id)
            val (level, progressFraction) = engine.calculateSkillLevel(sp.experiencePoints)
            val accent = Color(skill.colorHex)
            val relatedMissions = uiState.missions.filter { skill.id in it.relatedSkills }.take(4)

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = accent.copy(alpha = 0.18f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = iconForSkillKey(skill.iconKey),
                                    contentDescription = t(lang, skill.nameFa, skill.nameEn),
                                    tint = accent
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = t(lang, skill.nameFa, skill.nameEn),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = t(
                                    lang,
                                    "سطح $level • ${sp.practicedMissionsCount} تجربه ثبت‌شده",
                                    "Level $level • ${sp.practicedMissionsCount} logged experiences"
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = accent
                            )
                        }
                    }

                    Text(
                        text = t(lang, skill.descriptionFa, skill.descriptionEn),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(99.dp)),
                        color = accent
                    )

                    if (sp.recordedExperiences.isNotEmpty()) {
                        Text(
                            text = t(lang, "تجربه‌های اخیر:", "Recent experiences:"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            sp.recordedExperiences.take(3).forEach { exp ->
                                AssistChip(onClick = {}, label = { Text(exp) })
                            }
                        }
                    }

                    if (relatedMissions.isNotEmpty()) {
                        Text(
                            text = t(lang, "مأموریت‌های مرتبط:", "Related missions:"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            relatedMissions.forEach { m ->
                                SuggestionChip(
                                    onClick = { onOpenMission(m.id) },
                                    label = { Text(t(lang, "روز ${m.dayNumber}: ${m.titleFa}", "Day ${m.dayNumber}: ${m.titleEn}")) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
