package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.common.label
import com.example.core.common.t
import com.example.core.design.*
import com.example.core.navigation.Routes
import com.example.domain.model.*
import com.example.ui.MainUiState

@Composable
fun DashboardScreen(
    uiState: MainUiState,
    onOpenMission: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onMarkMessagesRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = uiState.settings.language
    val profile = uiState.userProfile
    val report = uiState.progressReport
    val recs = uiState.dailyRecommendations
    val currentTerritory = uiState.territories.firstOrNull { it.id == report?.currentTerritoryId }
        ?: uiState.territories.firstOrNull()
    val unreadMessages = uiState.supportMessages.filter { !it.isRead }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Command Banner
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    DeepNavy,
                                    SlateNavy
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (profile != null) {
                                HeroAvatarBadge(
                                    avatarId = profile.avatarId,
                                    colorHex = profile.avatarColorHex,
                                    accessory = profile.avatarAccessory,
                                    customAvatarPath = profile.customAvatarPath,
                                    size = 62.dp
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile?.displayName ?: t(lang, "کاوشگر", "Explorer"),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = report?.let {
                                        t(
                                            lang,
                                            "سطح ${it.currentLevel.levelNumber}: ${it.currentLevel.nameFa}",
                                            "Level ${it.currentLevel.levelNumber}: ${it.currentLevel.nameEn}"
                                        )
                                    } ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CompassGold,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Surface(
                                color = TurquoisePrimary.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${report?.totalXp ?: 0} XP",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TurquoisePrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = t(
                                            lang,
                                            "امروز ${report?.todayXp ?: 0}/${report?.dailyXpCap ?: 5}",
                                            "Today ${report?.todayXp ?: 0}/${report?.dailyXpCap ?: 5}"
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextLightSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Level Progress Bar
                        val prog = report?.xpProgressToNext ?: 0f
                        LinearProgressIndicator(
                            progress = { prog },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(9.dp)
                                .clip(RoundedCornerShape(99.dp)),
                            color = CompassGold,
                            trackColor = Color.White.copy(alpha = 0.16f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = currentTerritory?.let { t(lang, it.nameFa, it.nameEn) } ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextLightSecondary
                            )
                            Text(
                                text = t(
                                    lang,
                                    "${report?.missionsCompletedCount ?: 0} از ${report?.totalMissionsCount ?: 30} مأموریت",
                                    "${report?.missionsCompletedCount ?: 0}/${report?.totalMissionsCount ?: 30} missions"
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextLightPrimary
                            )
                        }
                    }
                }
            }
        }

        // 2. Family Encouragement Message Banner (if any unread)
        if (unreadMessages.isNotEmpty()) {
            item {
                val msg = unreadMessages.first()
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = t(lang, "پیام حمایتی از ${msg.senderLabel}", "Support note from ${msg.senderLabel}"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(msg.message, style = MaterialTheme.typography.bodyMedium)
                        }
                        TextButton(onClick = onMarkMessagesRead) {
                            Text(t(lang, "دیدم", "Got it"))
                        }
                    }
                }
            }
        }

        // 3. Highlighted Main Daily Mission
        if (recs != null) {
            item {
                val primary = recs.primaryMission
                val primaryProgress = uiState.missionProgressMap[primary.id]
                Text(
                    text = t(lang, "مأموریت پیشنهادی امروز", "Today's Featured Mission"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_primary_mission_card")
                        .clickable { onOpenMission(primary.id) },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (currentTerritory != null) {
                            TerritoryBannerImage(
                                visualKey = primary.imageKey,
                                title = t(lang, primary.titleFa, primary.titleEn),
                                subtitle = t(lang, "روز ${primary.dayNumber} • ${primary.durationMinutes} دقیقه", "Day ${primary.dayNumber} • ${primary.durationMinutes} min"),
                                accentColor = CompassGold,
                                height = 148.dp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = t(lang, primary.shortDescFa, primary.shortDescEn),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(
                                    onClick = { onOpenMission(primary.id) },
                                    label = { Text(primary.difficulty.label(lang)) }
                                )
                                if (primaryProgress != null) {
                                    AssistChip(
                                        onClick = { onOpenMission(primary.id) },
                                        label = { Text(primaryProgress.status.label(lang)) }
                                    )
                                }
                            }
                            Button(
                                onClick = { onOpenMission(primary.id) },
                                modifier = Modifier.testTag("dashboard_open_primary_mission")
                            ) {
                                Text(
                                    text = if (primaryProgress?.status == MissionStatus.IN_PROGRESS) {
                                        t(lang, "ادامه مأموریت", "Continue")
                                    } else {
                                        t(lang, "مشاهده و شروع", "Explore")
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // 4. Two Alternative Mission Recommendations
            val alternatives = listOfNotNull(
                recs.skillGrowthMission?.let { it to t(lang, "تقویت مهارت کمتر تمرین‌شده", "Grow a Quiet Skill") },
                recs.discoveryMission?.let { it to t(lang, "کشف تجربه متفاوت", "Try Something Fresh") }
            )
            if (alternatives.isNotEmpty()) {
                item {
                    Text(
                        text = t(lang, "دو انتخاب جایگزین (کاملاً به انتخاب تو)", "Two Alternative Quests (Your Choice)"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        alternatives.forEach { (mission, badge) ->
                            ElevatedCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onOpenMission(mission.id) },
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = badge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = t(lang, mission.titleFa, mission.titleEn),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = t(lang, "روز ${mission.dayNumber} • ${mission.durationMinutes} دق", "Day ${mission.dayNumber} • ${mission.durationMinutes}m"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Command Hub Shortcuts Grid
        item {
            Text(
                text = t(lang, "بخش‌های جهان من", "Explore Your World"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            val shortcuts = listOf(
                ShortcutItem(Routes.ADVENTURE_MAP, t(lang, "نقشه ماجراجویی", "Adventure Map"), Icons.Filled.Map, TurquoisePrimary, "shortcut_map"),
                ShortcutItem(Routes.PROJECTS, t(lang, "آزمایشگاه پروژه‌ها", "Project Lab"), Icons.Filled.Build, AdventureOrange, "shortcut_projects"),
                ShortcutItem(Routes.SKILLS, t(lang, "مهارت‌های من", "Skills"), Icons.Filled.AutoGraph, Color(0xFF6C63FF), "shortcut_skills"),
                ShortcutItem(Routes.ACHIEVEMENTS, t(lang, "دستاوردها و گزارش", "Badges & Report"), Icons.Filled.EmojiEvents, CompassGold, "shortcut_achievements"),
                ShortcutItem(Routes.JOURNAL, t(lang, "دفترچه خصوصی", "Private Journal"), Icons.Filled.Lock, SuccessEmerald, "shortcut_journal"),
                ShortcutItem(Routes.COMPANION, t(lang, "همراه من", "My Companion"), Icons.Filled.Psychology, TurquoiseDeep, "shortcut_companion"),
                ShortcutItem(Routes.FAMILY_ROOM, t(lang, "اتاق خانواده", "Family Room"), Icons.Filled.FamilyRestroom, Color(0xFFE07A5F), "shortcut_family"),
                ShortcutItem(Routes.SETTINGS, t(lang, "تنظیمات و پشتیبان", "Settings & Backup"), Icons.Filled.Settings, SlateNavy, "shortcut_settings")
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                shortcuts.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { item ->
                            ElevatedCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 78.dp)
                                    .testTag(item.tag)
                                    .clickable { onNavigate(item.route) },
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = item.tint.copy(alpha = 0.16f),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(item.icon, contentDescription = null, tint = item.tint)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ShortcutItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val tag: String
)
