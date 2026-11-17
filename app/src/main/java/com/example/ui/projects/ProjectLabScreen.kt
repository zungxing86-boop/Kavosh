package com.example.ui.projects

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.common.formatFileSize
import com.example.core.common.formatTimestamp
import com.example.core.common.label
import com.example.core.common.t
import com.example.core.design.EmptyStateCard
import com.example.core.design.SuccessEmerald
import com.example.domain.model.*
import kotlinx.coroutines.launch
import java.io.File

private val ProjectTemplates = listOf(
    "ربات ساده" to "Simple Robot",
    "طراحی بازی" to "Game Design",
    "ساخت وسیله" to "DIY Gadget",
    "تحقیق علمی" to "Science Investigation",
    "طراحی پوستر" to "Poster Design",
    "یادگیری برنامه‌نویسی" to "Learn Coding",
    "ساخت ماکت" to "Scale Model",
    "نوشتن داستان" to "Story Writing"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProjectLabListScreen(
    language: AppLanguage,
    projects: List<Project>,
    onOpenProject: (Long) -> Unit,
    onCreateProject: suspend (Project) -> Long,
    onDeleteProject: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(t(language, "پروژه جدید", "New Project")) },
                modifier = Modifier.testTag("create_project_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = t(language, "آزمایشگاه پروژه‌ها", "Project Lab"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = t(
                        language,
                        "ایده‌هایت را بساز، مراحل را دنبال کن، عکس و فایل اضافه کن و فقط اگر خواستی با خانواده هم‌رسانی کن.",
                        "Build your ideas, track milestones, attach files, and keep projects private unless you choose to share."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (projects.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.Build,
                        title = t(language, "هنوز پروژه‌ای نساخته‌ای!", "Your Project Lab is ready"),
                        subtitle = t(
                            language,
                            "از یک نمونه مثل ساخت ماکت، بازی یا تحقیق علمی شروع کن یا ایده خودت را بنویس.",
                            "Launch a game, science research, scale model, story, or your own invention."
                        ),
                        actionLabel = t(language, "ساخت اولین پروژه", "Create First Project"),
                        onAction = { showCreateDialog = true }
                    )
                }
            } else {
                items(projects, key = { it.id }) { project ->
                    val doneSteps = project.steps.count { it.isDone }
                    val totalSteps = project.steps.size
                    val pct = if (totalSteps > 0) doneSteps.toFloat() / totalSteps.toFloat() else 0f

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_card_${project.id}")
                            .clickable { onOpenProject(project.id) },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = project.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { onDeleteProject(project.id) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = t(language, "حذف پروژه", "Delete project"))
                                }
                            }
                            if (project.goal.isNotBlank()) {
                                Text(project.goal, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (totalSteps > 0) {
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(99.dp))
                                )
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(onClick = { onOpenProject(project.id) }, label = { Text(project.status.label(language)) })
                                AssistChip(
                                    onClick = { onOpenProject(project.id) },
                                    label = {
                                        Text(
                                            if (project.sharedWithFamily) t(language, "هم‌رسانی با خانواده", "Shared with Family")
                                            else t(language, "خصوصی", "Private")
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (project.sharedWithFamily) Icons.Filled.Group else Icons.Filled.Lock,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var title by remember { mutableStateOf("") }
        var goal by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(t(language, "ساخت پروژه جدید", "Create New Project")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(t(language, "نمونه‌های آماده:", "Quick starters:"), style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ProjectTemplates.forEach { (fa, en) ->
                            SuggestionChip(
                                onClick = { title = t(language, fa, en) },
                                label = { Text(t(language, fa, en)) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(t(language, "عنوان پروژه", "Project Title")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_project_title_input")
                    )
                    OutlinedTextField(
                        value = goal,
                        onValueChange = { goal = it },
                        label = { Text(t(language, "هدف پروژه", "Project Goal")) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("new_project_goal_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            scope.launch {
                                val newId = onCreateProject(
                                    Project(
                                        title = title.trim(),
                                        description = "",
                                        goal = goal.trim(),
                                        steps = listOf(
                                            ProjectStep("s1", t(language, "ایده و طراحی اولیه", "Initial sketch & plan")),
                                            ProjectStep("s2", t(language, "ساخت نسخه اول", "Build first version")),
                                            ProjectStep("s3", t(language, "آزمایش و تکمیل", "Test & polish"))
                                        ),
                                        status = ProjectStatus.IN_PROGRESS
                                    )
                                )
                                showCreateDialog = false
                                onOpenProject(newId)
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_project_button")
                ) {
                    Text(t(language, "ساخت و باز کردن", "Create & Open"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(t(language, "انصراف", "Cancel"))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProjectDetailScreen(
    projectId: Long,
    language: AppLanguage,
    projects: List<Project>,
    viewModel: ProjectLabViewModel,
    onBack: () -> Unit
) {
    val project = projects.firstOrNull { it.id == projectId }
    val attachments by viewModel.observeAttachments(projectId).collectAsState(initial = emptyList())
    val statusMsg by viewModel.message.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (project == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(t(language, "جزئیات پروژه", "Project Detail")) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t(language, "بازگشت", "Back"))
                        }
                    }
                )
            }
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text(t(language, "پروژه پیدا نشد.", "Project not found."))
            }
        }
        return
    }

    var title by remember(project.id) { mutableStateOf(project.title) }
    var description by remember(project.id) { mutableStateOf(project.description) }
    var goal by remember(project.id) { mutableStateOf(project.goal) }
    var notes by remember(project.id) { mutableStateOf(project.notes) }
    var finalOutcome by remember(project.id) { mutableStateOf(project.finalOutcome) }
    var status by remember(project.id) { mutableStateOf(project.status) }
    var sharedWithFamily by remember(project.id) { mutableStateOf(project.sharedWithFamily) }
    var steps by remember(project.id) { mutableStateOf(project.steps) }
    var newStepTitle by remember { mutableStateOf("") }
    var previewAttachment by remember { mutableStateOf<ProjectAttachment?>(null) }

    fun autoSave(
        nextTitle: String = title,
        nextDesc: String = description,
        nextGoal: String = goal,
        nextNotes: String = notes,
        nextOutcome: String = finalOutcome,
        nextStatus: ProjectStatus = status,
        nextShared: Boolean = sharedWithFamily,
        nextSteps: List<ProjectStep> = steps
    ) {
        scope.launch {
            viewModel.saveProject(
                project.copy(
                    title = nextTitle,
                    description = nextDesc,
                    goal = nextGoal,
                    notes = nextNotes,
                    finalOutcome = nextOutcome,
                    status = nextStatus,
                    sharedWithFamily = nextShared,
                    steps = nextSteps
                ),
                isPersian = language == AppLanguage.PERSIAN
            )
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.attachFileFromUri(project.id, uri, language == AppLanguage.PERSIAN)
        }
    }

    val safDocumentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.attachFileFromUri(project.id, uri, language == AppLanguage.PERSIAN)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title.ifBlank { t(language, "پروژه", "Project") }, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("project_detail_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t(language, "بازگشت", "Back"))
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
            if (statusMsg != null) {
                item {
                    AssistChip(
                        onClick = { viewModel.clearMessage() },
                        label = { Text(statusMsg!!) },
                        leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) }
                    )
                }
            }

            item {
                ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = t(language, "شروع پروژه: ${formatTimestamp(project.startedAt, language)} • ذخیره خودکار فعال", "Started: ${formatTimestamp(project.startedAt, language)} • Auto-save active"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                title = it
                                autoSave(nextTitle = it)
                            },
                            label = { Text(t(language, "عنوان", "Title")) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = goal,
                            onValueChange = {
                                goal = it
                                autoSave(nextGoal = it)
                            },
                            label = { Text(t(language, "هدف پروژه", "Goal")) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = {
                                description = it
                                autoSave(nextDesc = it)
                            },
                            label = { Text(t(language, "توضیحات", "Description")) },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Status & Teen-Controlled Sharing Toggle
            item {
                ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(t(language, "وضعیت پروژه", "Project Status"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProjectStatus.entries.forEach { st ->
                                FilterChip(
                                    selected = status == st,
                                    onClick = {
                                        status = st
                                        autoSave(nextStatus = st)
                                    },
                                    label = { Text(st.label(language)) }
                                )
                            }
                        }
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = t(language, "نمایش در اتاق خانواده", "Share in Family Room"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = t(
                                        language,
                                        "فقط اگر این گزینه را روشن کنی والدین عنوان و نتیجه این پروژه را می‌بینند.",
                                        "Only visible in Family Room if you turn this ON."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = sharedWithFamily,
                                onCheckedChange = {
                                    sharedWithFamily = it
                                    autoSave(nextShared = it)
                                },
                                modifier = Modifier.testTag("project_share_family_switch")
                            )
                        }
                    }
                }
            }

            // Milestones / Steps
            item {
                ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(t(language, "مراحل پروژه", "Project Steps"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        steps.forEachIndexed { idx, step ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = step.isDone,
                                    onCheckedChange = { checked ->
                                        val updated = steps.toMutableList().also {
                                            it[idx] = step.copy(isDone = checked)
                                        }
                                        steps = updated
                                        autoSave(nextSteps = updated)
                                    }
                                )
                                Text(step.title, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    val updated = steps.filterIndexed { i, _ -> i != idx }
                                    steps = updated
                                    autoSave(nextSteps = updated)
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = t(language, "حذف مرحله", "Remove step"), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newStepTitle,
                                onValueChange = { newStepTitle = it },
                                label = { Text(t(language, "مرحله جدید...", "Add milestone...")) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (newStepTitle.isNotBlank()) {
                                        val updated = steps + ProjectStep("step_${System.currentTimeMillis()}", newStepTitle.trim())
                                        steps = updated
                                        newStepTitle = ""
                                        autoSave(nextSteps = updated)
                                    }
                                },
                                modifier = Modifier.testTag("add_project_step_button")
                            ) {
                                Icon(Icons.Filled.AddCircle, contentDescription = t(language, "افزودن مرحله", "Add step"))
                            }
                        }
                    }
                }
            }

            // Attachments (Photo Picker + SAF OpenDocument)
            item {
                ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(t(language, "تصاویر و فایل‌های پروژه", "Photos & Attachments"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier.weight(1f).testTag("project_add_photo_button")
                            ) {
                                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(t(language, "افزودن تصویر", "Add Photo"))
                            }
                            OutlinedButton(
                                onClick = {
                                    safDocumentPicker.launch(arrayOf("application/pdf", "text/plain", "image/*", "*/*"))
                                },
                                modifier = Modifier.weight(1f).testTag("project_add_file_button")
                            ) {
                                Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(t(language, "افزودن فایل", "Add File"))
                            }
                        }

                        attachments.forEach { att ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { previewAttachment = att }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (att.mimeType.startsWith("image/")) Icons.Filled.Image else Icons.Filled.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(att.fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${formatFileSize(att.fileSizeBytes, language)} • ${formatTimestamp(att.createdAt, language)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { viewModel.removeAttachment(att.id) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = t(language, "حذف ضمیمه", "Delete attachment"))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Notes & Final Outcome
            item {
                ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = {
                                notes = it
                                autoSave(nextNotes = it)
                            },
                            label = { Text(t(language, "یادداشت‌های ساخت و آزمایش", "Build Notes")) },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = finalOutcome,
                            onValueChange = {
                                finalOutcome = it
                                autoSave(nextOutcome = it)
                            },
                            label = { Text(t(language, "نتیجه نهایی پروژه", "Final Outcome")) },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // Internal Safe Preview Dialog for JPG/PNG/TXT/PDF
    previewAttachment?.let { att ->
        val file = File(att.internalPath)
        AlertDialog(
            onDismissRequest = { previewAttachment = null },
            title = { Text(att.fileName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        att.mimeType.startsWith("image/") && file.exists() -> {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(file).build(),
                                contentDescription = att.fileName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 260.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                        att.mimeType.startsWith("text/") && file.exists() -> {
                            val snippet = runCatching { file.readText().take(1200) }.getOrDefault("")
                            Text(snippet, style = MaterialTheme.typography.bodySmall)
                        }
                        else -> {
                            Text(
                                text = "${att.mimeType} • ${formatFileSize(att.fileSizeBytes, language)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (file.exists()) {
                                OutlinedButton(onClick = {
                                    runCatching {
                                        val contentUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(contentUri, att.mimeType)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    }
                                }) {
                                    Text(t(language, "باز کردن با نمایشگر دستگاه", "Open with system viewer"))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { previewAttachment = null }) {
                    Text(t(language, "بستن", "Close"))
                }
            }
        )
    }
}
