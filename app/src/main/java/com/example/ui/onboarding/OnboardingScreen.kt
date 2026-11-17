package com.example.ui.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.core.common.t
import com.example.core.design.*
import com.example.domain.model.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    language: AppLanguage,
    interests: List<Interest>,
    onToggleLanguage: () -> Unit,
    onComplete: (
        displayName: String,
        avatarId: String,
        avatarColorHex: Long,
        avatarAccessory: String,
        customAvatarUri: Uri?,
        selectedInterestIds: Set<String>,
        preferences: UserPreferences
    ) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var displayName by remember { mutableStateOf("") }
    var selectedAvatarId by remember { mutableStateOf("astronaut") }
    var selectedColor by remember { mutableStateOf(HeroColorOptions.first()) }
    var selectedAccessory by remember { mutableStateOf("compass") }
    var customAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var selectedInterests by remember { mutableStateOf(setOf("int_science", "int_games")) }

    var durationPref by remember { mutableStateOf(DurationPreference.ANY) }
    var stylePref by remember { mutableStateOf(ActivityStylePreference.BALANCED) }
    var socialPref by remember { mutableStateOf(SocialPreference.FLEXIBLE) }
    var difficultyPref by remember { mutableStateOf(DifficultyLevel.EASY) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) customAvatarUri = uri }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Top step & language bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 0) {
                        IconButton(
                            onClick = { step -= 1 },
                            modifier = Modifier.testTag("onboarding_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = t(language, "مرحله قبل", "Back")
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(4) { idx ->
                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(if (idx == step) 28.dp else 10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (idx <= step) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }

                    TextButton(
                        onClick = onToggleLanguage,
                        modifier = Modifier.testTag("onboarding_language_toggle")
                    ) {
                        Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (language == AppLanguage.PERSIAN) "EN" else "فا")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                AnimatedContent(
                    targetState = step,
                    modifier = Modifier.weight(1f),
                    label = "onboarding_step"
                ) { currentStep ->
                    when (currentStep) {
                        0 -> WelcomeStep(
                            language = language,
                            onStart = { step = 1 }
                        )
                        1 -> HeroBuilderStep(
                            language = language,
                            displayName = displayName,
                            onNameChange = { displayName = it },
                            selectedAvatarId = selectedAvatarId,
                            onAvatarSelect = {
                                selectedAvatarId = it
                                customAvatarUri = null
                            },
                            selectedColor = selectedColor,
                            onColorSelect = { selectedColor = it },
                            selectedAccessory = selectedAccessory,
                            onAccessorySelect = { selectedAccessory = it },
                            customAvatarUri = customAvatarUri,
                            onPickPhoto = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onClearPhoto = { customAvatarUri = null },
                            onNext = { step = 2 }
                        )
                        2 -> InterestsStep(
                            language = language,
                            interests = interests,
                            selectedIds = selectedInterests,
                            onToggle = { id ->
                                selectedInterests = if (id in selectedInterests) {
                                    selectedInterests - id
                                } else {
                                    selectedInterests + id
                                }
                            },
                            onNext = { step = 3 }
                        )
                        else -> PreferencesStep(
                            language = language,
                            durationPref = durationPref,
                            onDurationChange = { durationPref = it },
                            stylePref = stylePref,
                            onStyleChange = { stylePref = it },
                            socialPref = socialPref,
                            onSocialChange = { socialPref = it },
                            difficultyPref = difficultyPref,
                            onDifficultyChange = { difficultyPref = it },
                            onFinish = {
                                val argb = selectedColor.toArgb().toLong() and 0xFFFFFFFFL
                                onComplete(
                                    displayName,
                                    selectedAvatarId,
                                    argb,
                                    selectedAccessory,
                                    customAvatarUri,
                                    selectedInterests,
                                    UserPreferences(durationPref, stylePref, socialPref, difficultyPref)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(language: AppLanguage, onStart: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(26.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_territory_discovery),
                        contentDescription = t(language, "جهان ماجراجویی مسیر من", "My Journey Adventure World"),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, DeepNavy.copy(alpha = 0.88f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp)
                    ) {
                        Surface(
                            color = CompassGold,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = t(language, "مسیر من • My Journey", "My Journey • مسیر من"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepNavy,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = t(language, "کشف توانایی‌ها، ساختن مهارت‌ها، طراحی آینده", "Discover Abilities. Build Skills. Design Your Future."),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                ElevatedCard(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Explore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = t(language, "دعوت‌نامه سفر تو", "Your Expedition Invitation"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = t(
                                language,
                                "«یک مسیر ناشناخته پیش روی توست. در این سفر قرار نیست با دیگران مسابقه بدهی؛ قرار است توانایی‌های خودت را کشف کنی، چیزهای تازه یاد بگیری و آینده‌ای را بسازی که دوست داری.»",
                                "\"An uncharted path lies ahead of you. This journey isn't a race against anyone else—it's your space to uncover your own strengths, learn independently, and build the future you want.\""
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FeatureBadgeChip(
                        icon = Icons.Filled.Lock,
                        label = t(language, "۱۰۰٪ آفلاین و خصوصی", "100% Offline & Private"),
                        modifier = Modifier.weight(1f)
                    )
                    FeatureBadgeChip(
                        icon = Icons.Filled.Tune,
                        label = t(language, "انتخاب آزاد و بی‌جریمه", "Zero-Penalty Pace"),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("start_journey_button"),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                text = t(language, "شروع سفر", "Start Journey"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun FeatureBadgeChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroBuilderStep(
    language: AppLanguage,
    displayName: String,
    onNameChange: (String) -> Unit,
    selectedAvatarId: String,
    onAvatarSelect: (String) -> Unit,
    selectedColor: Color,
    onColorSelect: (Color) -> Unit,
    selectedAccessory: String,
    onAccessorySelect: (String) -> Unit,
    customAvatarUri: Uri?,
    onPickPhoto: () -> Unit,
    onClearPhoto: () -> Unit,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            item {
                Text(
                    text = t(language, "قهرمان سفر خودت را بساز", "Design Your Explorer"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = t(language, "هر وقت بخواهی می‌توانی از تنظیمات تغییرش بدهی.", "You can customize this anytime in Settings."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Live Hero Preview
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (customAvatarUri != null) {
                            Box(modifier = Modifier.size(76.dp)) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current).data(customAvatarUri).build(),
                                    contentDescription = t(language, "تصویر قهرمان", "Hero Photo"),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .border(2.5.dp, CompassGold, CircleShape)
                                )
                            }
                        } else {
                            HeroAvatarBadge(
                                avatarId = selectedAvatarId,
                                colorHex = (selectedColor.toArgb().toLong() and 0xFFFFFFFFL),
                                accessory = selectedAccessory,
                                customAvatarPath = null,
                                size = 76.dp
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName.ifBlank { t(language, "کاوشگر جوان", "Young Explorer") },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = t(language, "سطح ۱ • تازه‌وارد", "Level 1 • Newcomer"),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = onNameChange,
                    label = { Text(t(language, "نام نمایشی قهرمان", "Explorer Nickname")) },
                    placeholder = { Text(t(language, "مثلاً آرتین، سارا، کاوشگر...", "e.g. Alex, Sara, Nova...")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_name_input"),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            item {
                Text(
                    text = t(language, "انتخاب کاراکتر یا تصویر گالری", "Choose Avatar or Gallery Photo"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroAvatarPresets.forEach { preset ->
                        val active = customAvatarUri == null && selectedAvatarId == preset.id
                        FilterChip(
                            selected = active,
                            onClick = { onAvatarSelect(preset.id) },
                            label = { Text(t(language, preset.labelFa, preset.labelEn)) },
                            leadingIcon = { Icon(preset.icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                    OutlinedButton(
                        onClick = onPickPhoto,
                        modifier = Modifier.testTag("pick_gallery_avatar_button")
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(t(language, "تصویر از گالری", "Gallery Photo"))
                    }
                    if (customAvatarUri != null) {
                        TextButton(onClick = onClearPhoto) {
                            Text(t(language, "حذف عکس", "Remove Photo"))
                        }
                    }
                }
            }

            item {
                Text(
                    text = t(language, "رنگ مورد علاقه", "Signature Color"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HeroColorOptions.forEach { color ->
                        val isSelected = selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.White.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                                .clickable { onColorSelect(color) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = t(language, "نشان همراه شخصیت", "Character Emblem"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroAccessoryOptions.forEach { acc ->
                        FilterChip(
                            selected = selectedAccessory == acc.id,
                            onClick = { onAccessorySelect(acc.id) },
                            label = { Text(t(language, acc.labelFa, acc.labelEn)) },
                            leadingIcon = { Icon(acc.icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("hero_next_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(t(language, "ادامه: انتخاب علایق", "Next: Pick Interests"), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InterestsStep(
    language: AppLanguage,
    interests: List<Interest>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            item {
                Text(
                    text = t(language, "چه چیزهایی کنجکاوت می‌کنند؟", "What Sparks Your Curiosity?"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = t(
                        language,
                        "چند مورد را انتخاب کن؛ اگر بعداً نظرت عوض شد، راحت تغییر می‌دهی.",
                        "Pick as many as you like—you can change these anytime."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    interests.forEach { interest ->
                        val selected = interest.id in selectedIds
                        FilterChip(
                            selected = selected,
                            onClick = { onToggle(interest.id) },
                            label = {
                                Text(
                                    text = t(language, interest.nameFa, interest.nameEn),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = iconForInterestKey(interest.iconKey),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("interests_next_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(t(language, "ادامه: سبک فعالیت دلخواه", "Next: Activity Style"), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferencesStep(
    language: AppLanguage,
    durationPref: DurationPreference,
    onDurationChange: (DurationPreference) -> Unit,
    stylePref: ActivityStylePreference,
    onStyleChange: (ActivityStylePreference) -> Unit,
    socialPref: SocialPreference,
    onSocialChange: (SocialPreference) -> Unit,
    difficultyPref: DifficultyLevel,
    onDifficultyChange: (DifficultyLevel) -> Unit,
    onFinish: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            item {
                Text(
                    text = t(language, "تنظیم سبک ماجراجویی (اختیاری)", "Your Adventure Style (Optional)"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = t(language, "این گزینه‌ها کمک می‌کنند مأموریت‌های متناسب‌تری پیشنهاد شود.", "These help tailor mission suggestions to your mood."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                PreferenceGroupCard(title = t(language, "زمان فعالیت", "Preferred Time")) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            DurationPreference.SHORT to t(language, "کوتاه (۵-۱۰ دقیقه)", "Short (5-10m)"),
                            DurationPreference.MEDIUM to t(language, "متوسط (۱۵-۳۰ دقیقه)", "Medium (15-30m)"),
                            DurationPreference.LONG to t(language, "بلند (+۳۰ دقیقه)", "Long (30m+)"),
                            DurationPreference.ANY to t(language, "فرقی نمی‌کند", "Flexible")
                        ).forEach { (value, label) ->
                            FilterChip(
                                selected = durationPref == value,
                                onClick = { onDurationChange(value) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            item {
                PreferenceGroupCard(title = t(language, "نوع تجربه", "Activity Type")) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            ActivityStylePreference.PRACTICAL to t(language, "عملی و ساختنی", "Hands-on"),
                            ActivityStylePreference.REFLECTIVE to t(language, "فکری و تحلیلی", "Reflective"),
                            ActivityStylePreference.BALANCED to t(language, "ترکیبی", "Balanced")
                        ).forEach { (value, label) ->
                            FilterChip(
                                selected = stylePref == value,
                                onClick = { onStyleChange(value) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            item {
                PreferenceGroupCard(title = t(language, "شیوه انجام", "Social Style")) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            SocialPreference.SOLO to t(language, "فردی", "Solo"),
                            SocialPreference.GROUP to t(language, "گروهی / همراه خانواده", "With Others"),
                            SocialPreference.FLEXIBLE to t(language, "هر دو", "Either")
                        ).forEach { (value, label) ->
                            FilterChip(
                                selected = socialPref == value,
                                onClick = { onSocialChange(value) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            item {
                PreferenceGroupCard(title = t(language, "سطح چالش", "Starting Challenge")) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            DifficultyLevel.EASY to t(language, "ساده و آرام", "Easy"),
                            DifficultyLevel.MEDIUM to t(language, "متوسط", "Medium"),
                            DifficultyLevel.CHALLENGING to t(language, "چالشی", "Challenging")
                        ).forEach { (value, label) ->
                            FilterChip(
                                selected = difficultyPref == value,
                                onClick = { onDifficultyChange(value) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("complete_onboarding_button"),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Filled.RocketLaunch, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = t(language, "ورود به مرکز فرماندهی سفر", "Enter Command Center"),
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PreferenceGroupCard(title: String, content: @Composable () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
