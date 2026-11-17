package com.example.core.design

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import java.io.File

data class AvatarPreset(
    val id: String,
    val labelFa: String,
    val labelEn: String,
    val icon: ImageVector
)

val HeroAvatarPresets = listOf(
    AvatarPreset("astronaut", "کاوشگر فضا", "Space Explorer", Icons.Filled.RocketLaunch),
    AvatarPreset("scientist", "دانشمند جوان", "Young Scientist", Icons.Filled.Science),
    AvatarPreset("navigator", "دریانورد", "Navigator", Icons.Filled.Explore),
    AvatarPreset("inventor", "مخترع", "Inventor", Icons.Filled.Engineering),
    AvatarPreset("artist", "طراح خلاق", "Creative Artist", Icons.Filled.Palette),
    AvatarPreset("coder", "برنامه‌نویس", "Code Builder", Icons.Filled.Terminal)
)

data class AccessoryOption(
    val id: String,
    val labelFa: String,
    val labelEn: String,
    val icon: ImageVector
)

val HeroAccessoryOptions = listOf(
    AccessoryOption("compass", "قطب‌نما", "Compass", Icons.Filled.Explore),
    AccessoryOption("star", "ستاره", "Star", Icons.Filled.AutoAwesome),
    AccessoryOption("bolt", "انرژی", "Spark", Icons.Filled.Bolt),
    AccessoryOption("shield", "نشان کاوش", "Crest", Icons.Filled.Shield)
)

@DrawableRes
fun territoryDrawableForKey(key: String): Int = when (key) {
    "img_territory_discovery" -> R.drawable.img_territory_discovery
    "img_territory_learning" -> R.drawable.img_territory_learning
    "img_territory_mind" -> R.drawable.img_territory_mind
    "img_territory_future" -> R.drawable.img_territory_future
    else -> R.drawable.img_territory_discovery
}

fun iconForSkillKey(key: String): ImageVector = when (key) {
    "explore" -> Icons.Filled.Explore
    "center_focus" -> Icons.Filled.CenterFocusStrong
    "lightbulb" -> Icons.Filled.Lightbulb
    "verified_user" -> Icons.Filled.VerifiedUser
    "route" -> Icons.Filled.Route
    else -> Icons.Filled.Star
}

fun iconForInterestKey(key: String): ImageVector = when (key) {
    "science" -> Icons.Filled.Science
    "code" -> Icons.Filled.Code
    "smart_toy" -> Icons.Filled.SmartToy
    "eco" -> Icons.Filled.Eco
    "rocket" -> Icons.Filled.RocketLaunch
    "palette" -> Icons.Filled.Palette
    "music_note" -> Icons.Filled.MusicNote
    "sports_soccer" -> Icons.Filled.SportsSoccer
    "build" -> Icons.Filled.Build
    "menu_book" -> Icons.Filled.MenuBook
    "account_balance" -> Icons.Filled.AccountBalance
    "extension" -> Icons.Filled.Extension
    "storefront" -> Icons.Filled.Storefront
    "engineering" -> Icons.Filled.Engineering
    else -> Icons.Filled.AutoAwesome
}

fun iconForAchievementKey(key: String): ImageVector = when (key) {
    "flag" -> Icons.Filled.Flag
    "psychology" -> Icons.Filled.Psychology
    "explore" -> Icons.Filled.Explore
    "school" -> Icons.Filled.School
    "record_voice_over" -> Icons.Filled.RecordVoiceOver
    "build" -> Icons.Filled.Build
    "extension" -> Icons.Filled.Extension
    "center_focus_strong" -> Icons.Filled.CenterFocusStrong
    "auto_awesome" -> Icons.Filled.AutoAwesome
    "menu_book" -> Icons.Filled.MenuBook
    "rocket_launch" -> Icons.Filled.RocketLaunch
    "emoji_events" -> Icons.Filled.EmojiEvents
    else -> Icons.Filled.MilitaryTech
}

@Composable
fun HeroAvatarBadge(
    avatarId: String,
    colorHex: Long,
    accessory: String,
    customAvatarPath: String?,
    size: Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    val accent = Color(colorHex)
    val presetIcon = HeroAvatarPresets.firstOrNull { it.id == avatarId }?.icon ?: Icons.Filled.Explore
    val badgeIcon = HeroAccessoryOptions.firstOrNull { it.id == accessory }?.icon

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(accent.copy(alpha = 0.92f), accent.copy(alpha = 0.55f))
                    )
                )
                .border(2.5.dp, CompassGold.copy(alpha = 0.85f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val file = customAvatarPath?.let { File(it) }
            if (file != null && file.exists()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(file).crossfade(true).build(),
                    contentDescription = "Hero Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = presetIcon,
                    contentDescription = "Hero Avatar",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.52f)
                )
            }
        }
        if (badgeIcon != null) {
            Box(
                modifier = Modifier
                    .size(size * 0.34f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(DeepNavy)
                    .border(1.5.dp, CompassGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = CompassGold,
                    modifier = Modifier.size(size * 0.2f)
                )
            }
        }
    }
}

@Composable
fun TerritoryBannerImage(
    visualKey: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 156.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(22.dp))
    ) {
        Image(
            painter = painterResource(id = territoryDrawableForKey(visualKey)),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            DeepNavy.copy(alpha = 0.45f),
                            DeepNavy.copy(alpha = 0.9f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.9f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepNavy,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}
