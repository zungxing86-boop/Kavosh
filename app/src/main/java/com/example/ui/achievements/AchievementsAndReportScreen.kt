package com.example.ui.achievements

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.common.formatTimestamp
import com.example.core.common.t
import com.example.core.design.*
import com.example.ui.MainUiState

@Composable
fun AchievementsAndReportScreen(
    uiState: MainUiState,
    modifier: Modifier = Modifier
) {
    val lang = uiState.settings.language
    val report = uiState.progressReport
    val unlockedMap = uiState.unlockedAchievements.associateBy { it.achievementId }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = t(lang, "گزارش رشد شخصی و دستاوردها", "Personal Growth Report & Badges"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = t(
                    lang,
                    "فقط برای دیدن مسیر رشد خودت—بدون مقایسه با هیچ‌کس دیگر.",
                    "A reflection of your own path—never compared against anyone else."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Visual Progress Summary + Simple Chart
        if (report != null) {
            item {
                ElevatedCard(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = t(lang, "شاخص‌های مسیر تو", "Your Journey Snapshot"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatPill(
                                value = "${report.missionsTriedCount}/${report.missionsCompletedCount}",
                                label = t(lang, "تجربه‌شده / تکمیل", "Tried / Done"),
                                modifier = Modifier.weight(1f)
                            )
                            StatPill(
                                value = "${report.activeProjectsCount}/${report.completedProjectsCount}",
                                label = t(lang, "پروژه فعال / کامل", "Projects Act/Done"),
                                modifier = Modifier.weight(1f)
                            )
                            StatPill(
                                value = "${report.territoriesExploredCount}/4",
                                label = t(lang, "سرزمین‌ها", "Territories"),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Simple Personal Bar Chart per Territory
                        Text(
                            text = t(lang, "پیشرفت در چهار سرزمین", "Progress Across 4 Territories"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        val territoryFractions = uiState.territories.map { territory ->
                            val list = uiState.missions.filter { it.territoryId == territory.id }
                            val done = list.count {
                                uiState.missionProgressMap[it.id]?.status == com.example.domain.model.MissionStatus.COMPLETED
                            }
                            val frac = if (list.isNotEmpty()) done.toFloat() / list.size.toFloat() else 0f
                            Triple(t(lang, territory.nameFa, territory.nameEn), frac, Color(territory.themeColorHex))
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            territoryFractions.forEach { (name, frac, color) ->
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(name, style = MaterialTheme.typography.labelMedium)
                                        Text("${(frac * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                    ) {
                                        drawRoundRect(
                                            color = color.copy(alpha = 0.16f),
                                            cornerRadius = CornerRadius(12f, 12f),
                                            size = size
                                        )
                                        drawRoundRect(
                                            color = color,
                                            cornerRadius = CornerRadius(12f, 12f),
                                            size = Size(width = size.width * frac.coerceIn(0.03f, 1f), height = size.height)
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = t(lang, "مهارت‌های تمرین‌شده: ${report.practicedSkillsCount}/5", "Skills practiced: ${report.practicedSkillsCount}/5"),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = t(lang, "مجموع XP: ${report.totalXp}", "Total XP: ${report.totalXp}"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = t(
                    lang,
                    "نشان‌های تجربه (${unlockedMap.size} از ${uiState.achievements.size})",
                    "Experience Badges (${unlockedMap.size}/${uiState.achievements.size})"
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(uiState.achievements, key = { it.id }) { ach ->
            val unlocked = unlockedMap[ach.id]
            val isUnlocked = unlocked != null
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isUnlocked) {
                        CompassGold.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isUnlocked) CompassGold else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = iconForAchievementKey(ach.iconKey),
                                contentDescription = null,
                                tint = if (isUnlocked) DeepNavy else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = t(lang, ach.titleFa, ach.titleEn),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = t(lang, ach.descriptionFa, ach.descriptionEn),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isUnlocked) {
                                t(lang, "دریافت شده: ${formatTimestamp(unlocked!!.unlockedAt, lang)}", "Unlocked: ${formatTimestamp(unlocked!!.unlockedAt, lang)}")
                            } else {
                                t(lang, "شرط: ${ach.criteriaFa}", "Criteria: ${ach.criteriaEn}")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUnlocked) SuccessEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (isUnlocked) Icons.Filled.LockOpen else Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = if (isUnlocked) SuccessEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatPill(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
