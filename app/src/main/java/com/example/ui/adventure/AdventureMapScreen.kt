package com.example.ui.adventure

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.common.label
import com.example.core.common.t
import com.example.core.design.*
import com.example.domain.model.*
import com.example.ui.MainUiState

@Composable
fun AdventureMapScreen(
    uiState: MainUiState,
    onOpenMission: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = uiState.settings.language
    val report = uiState.progressReport
    val recommendedIds = listOfNotNull(
        uiState.dailyRecommendations?.primaryMission?.id,
        uiState.dailyRecommendations?.skillGrowthMission?.id,
        uiState.dailyRecommendations?.discoveryMission?.id
    ).toSet()

    val unlockedSet = uiState.unlockedAchievements.map { it.achievementId }.toSet()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Overall Journey Status Header
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = t(lang, "نقشه جهان ماجراجویی", "World Adventure Map"),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = t(
                                    lang,
                                    "هر مأموریتی را دوست داشتی انتخاب کن یا به قبل برگرد.",
                                    "Pick any mission freely or revisit earlier days anytime."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val overallPct = if ((report?.totalMissionsCount ?: 30) > 0) {
                            ((report?.missionsCompletedCount ?: 0) * 100) / (report?.totalMissionsCount ?: 30)
                        } else 0
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "$overallPct%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (report?.missionsCompletedCount ?: 0).toFloat() / (report?.totalMissionsCount ?: 30).coerceAtLeast(1) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(99.dp))
                    )
                }
            }
        }

        // Four Territories with Visual Path Nodes
        uiState.territories.forEach { territory ->
            item(key = territory.id) {
                val territoryMissions = uiState.missions.filter { it.territoryId == territory.id }
                val completedCount = territoryMissions.count {
                    uiState.missionProgressMap[it.id]?.status == MissionStatus.COMPLETED
                }
                val pct = if (territoryMissions.isNotEmpty()) {
                    (completedCount * 100) / territoryMissions.size
                } else 0
                val isCurrent = report?.currentTerritoryId == territory.id
                val territoryColor = Color(territory.themeColorHex)
                val territoryAchievements = uiState.achievements.filter { it.territoryId == territory.id }
                val territoryAchUnlocked = territoryAchievements.count { it.id in unlockedSet }

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("territory_card_${territory.id}"),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TerritoryBannerImage(
                            visualKey = territory.visualKey,
                            title = t(lang, territory.nameFa, territory.nameEn),
                            subtitle = t(
                                lang,
                                "روزهای ${territory.dayStart} تا ${territory.dayEnd} • $pct% تکمیل",
                                "Days ${territory.dayStart}-${territory.dayEnd} • $pct% done"
                            ),
                            accentColor = territoryColor,
                            height = 136.dp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = t(lang, territory.descriptionFa, territory.descriptionEn),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            if (isCurrent) {
                                Spacer(modifier = Modifier.width(8.dp))
                                AssistChip(
                                    onClick = {},
                                    label = { Text(t(lang, "موقعیت فعلی", "Current")) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                        }

                        if (territoryAchievements.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = CompassGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = t(
                                        lang,
                                        "دستاوردهای این سرزمین: $territoryAchUnlocked از ${territoryAchievements.size}",
                                        "Territory Badges: $territoryAchUnlocked/${territoryAchievements.size}"
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Winding Mission Trail
                        territoryMissions.forEachIndexed { index, mission ->
                            val progress = uiState.missionProgressMap[mission.id]
                            val status = progress?.status
                            val isRecommended = mission.id in recommendedIds
                            val isCompleted = status == MissionStatus.COMPLETED
                            val isActive = status == MissionStatus.IN_PROGRESS || status == MissionStatus.SELECTED
                            val staggerOffset = if (index % 2 == 0) 0.dp else 18.dp

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = staggerOffset)
                                    .clickable { onOpenMission(mission.id) }
                                    .testTag("map_mission_node_${mission.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isCompleted -> SuccessEmerald
                                                    isActive -> territoryColor
                                                    isRecommended -> CompassGold
                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                }
                                            )
                                            .border(
                                                width = if (isRecommended || isActive) 2.5.dp else 1.dp,
                                                color = if (isRecommended) DeepNavy else territoryColor.copy(alpha = 0.6f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCompleted) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        } else {
                                            Text(
                                                text = "${mission.dayNumber}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isActive || isRecommended) DeepNavy else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    if (index < territoryMissions.lastIndex) {
                                        Canvas(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(18.dp)
                                        ) {
                                            drawLine(
                                                color = if (isCompleted) SuccessEmerald else territoryColor.copy(alpha = 0.4f),
                                                start = Offset(size.width / 2f, 0f),
                                                end = Offset(size.width / 2f, size.height),
                                                strokeWidth = 5f,
                                                pathEffect = if (isCompleted) null else PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(bottom = if (index < territoryMissions.lastIndex) 10.dp else 0.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isActive || isRecommended) {
                                        territoryColor.copy(alpha = 0.13f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = t(lang, mission.titleFa, mission.titleEn),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = status?.label(lang) ?: if (isRecommended) {
                                                    t(lang, "پیشنهاد شده برای تو", "Suggested for you")
                                                } else {
                                                    t(lang, "${mission.durationMinutes} دقیقه • آماده کشف", "${mission.durationMinutes} min • Open to explore")
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isRecommended && !isCompleted) {
                                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = CompassGold, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
