package com.example.ui.companion

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.common.t
import com.example.domain.model.AppLanguage
import com.example.domain.model.CompanionModule

// Extension-ready interface for future optional online AI advisor integration
interface CompanionAdvisor {
    suspend fun suggestNextPrompt(moduleId: String, userNotes: Map<String, String>): String?
}

@Composable
fun CompanionScreen(
    language: AppLanguage,
    modules: List<CompanionModule>,
    onSaveToJournal: (title: String, body: String) -> Unit,
    onCreateProject: (title: String, goal: String, steps: List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeModuleId by remember { mutableStateOf<String?>(null) }
    val activeModule = modules.firstOrNull { it.id == activeModuleId }

    if (activeModule != null) {
        BackHandler { activeModuleId = null }
        CompanionWorksheetView(
            language = language,
            module = activeModule,
            onBack = { activeModuleId = null },
            onSaveToJournal = onSaveToJournal,
            onCreateProject = onCreateProject,
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = t(language, "همراه من (راهنمای تعاملی آفلاین)", "My Companion (Offline Guide)"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = t(
                        language,
                        "ابزارهای فکری گام‌به‌گام برای هدف‌گذاری، حل مسئله، یادگیری، تمرکز، مدیریت پروژه و تفکر انتقادی.",
                        "Interactive step-by-step guides for goals, problem solving, learning, focus, projects, and critical thinking."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(modules, key = { it.id }) { mod ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("companion_module_${mod.id}")
                        .clickable { activeModuleId = mod.id },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = iconForCompanionKey(mod.iconKey),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = t(language, mod.titleFa, mod.titleEn),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = t(language, mod.subtitleFa, mod.subtitleEn),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = t(language, "${mod.steps.size} گام راهنما", "${mod.steps.size} interactive steps"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanionWorksheetView(
    language: AppLanguage,
    module: CompanionModule,
    onBack: () -> Unit,
    onSaveToJournal: (String, String) -> Unit,
    onCreateProject: (String, String, List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val inputs = remember(module.id) { mutableStateMapOf<String, String>() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t(language, "بازگشت", "Back"))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = t(language, module.titleFa, module.titleEn),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = t(language, module.subtitleFa, module.subtitleEn),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(module.steps, key = { it.id }) { step ->
            ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = t(language, step.titleFa, step.titleEn),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = t(language, step.promptFa, step.promptEn),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = t(language, step.tipFa, step.tipEn),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    OutlinedTextField(
                        value = inputs[step.id] ?: "",
                        onValueChange = { inputs[step.id] = it },
                        label = { Text(t(language, "پاسخ یا فکر من...", "My thought / plan...")) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val compiled = module.steps.joinToString("\n\n") { s ->
                            val title = t(language, s.titleFa, s.titleEn)
                            val ans = inputs[s.id]?.trim().orEmpty()
                            "$title:\n${ans.ifBlank { "-" }}"
                        }
                        onSaveToJournal(t(language, module.titleFa, module.titleEn), compiled)
                    },
                    modifier = Modifier.weight(1f).testTag("companion_save_journal_button")
                ) {
                    Icon(Icons.Filled.BookmarkAdded, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(t(language, "ذخیره در دفترچه", "Save to Journal"))
                }
                if (module.id == "comp_goal_setting" || module.id == "comp_project") {
                    OutlinedButton(
                        onClick = {
                            val title = inputs.values.firstOrNull { it.isNotBlank() }?.take(40)
                                ?: t(language, module.titleFa, module.titleEn)
                            val goal = inputs.values.drop(1).firstOrNull { it.isNotBlank() } ?: ""
                            val stepList = inputs.values.filter { it.isNotBlank() }.toList()
                            onCreateProject(title, goal, stepList)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(t(language, "تبدیل به پروژه", "Make Project"))
                    }
                }
            }
        }
    }
}

private fun iconForCompanionKey(key: String): ImageVector = when (key) {
    "flag" -> Icons.Filled.Flag
    "extension" -> Icons.Filled.Extension
    "school" -> Icons.Filled.School
    "center_focus" -> Icons.Filled.CenterFocusStrong
    "build" -> Icons.Filled.Build
    "fact_check" -> Icons.Filled.FactCheck
    else -> Icons.Filled.Psychology
}
