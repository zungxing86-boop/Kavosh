package com.example.ui.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.common.formatTimestamp
import com.example.core.common.label
import com.example.core.common.t
import com.example.core.design.EmptyStateCard
import com.example.core.design.SuccessEmerald
import com.example.domain.model.AppLanguage
import com.example.domain.model.JournalCategory
import com.example.domain.model.JournalEntry

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrivateJournalScreen(
    language: AppLanguage,
    entries: List<JournalEntry>,
    onSaveEntry: (JournalEntry) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<JournalCategory?>(null) }
    var editingEntry by remember { mutableStateOf<JournalEntry?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    val filtered = remember(entries, searchQuery, selectedCategory) {
        entries.filter { entry ->
            val catMatch = selectedCategory == null || entry.category == selectedCategory
            val q = searchQuery.trim()
            val textMatch = q.isEmpty() ||
                entry.title.contains(q, ignoreCase = true) ||
                entry.content.contains(q, ignoreCase = true)
            catMatch && textMatch
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingEntry = null
                    showEditor = true
                },
                icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                text = { Text(t(language, "یادداشت جدید", "New Entry")) },
                modifier = Modifier.testTag("journal_add_fab")
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
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SuccessEmerald.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = SuccessEmerald)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = t(
                                language,
                                "دفترچه خصوصی کاملاً متعلق به توست؛ اتاق خانواده هیچ دسترسی خودکاری به آن ندارد.",
                                "Your Private Journal belongs 100% to you—Family Room has zero automatic access."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(t(language, "جست‌وجو در یادداشت‌ها...", "Search your journal...")) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("journal_search_input")
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text(t(language, "همه", "All")) }
                        )
                    }
                    items(JournalCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(cat.label(language)) }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.MenuBook,
                        title = t(language, "دفترچه خصوصی منتظر نوشته‌های توست", "A quiet space for your thoughts"),
                        subtitle = t(
                            language,
                            "افکار، احساسات، ایده‌ها و برنامه‌هایت را آزادانه اینجا بنویس.",
                            "Write ideas, feelings, questions, or daily reflections completely privately."
                        ),
                        actionLabel = t(language, "نوشتن اولین یادداشت", "Write First Entry"),
                        onAction = {
                            editingEntry = null
                            showEditor = true
                        }
                    )
                }
            } else {
                items(filtered, key = { it.id }) { entry ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("journal_entry_card_${entry.id}")
                            .clickable {
                                editingEntry = entry
                                showEditor = true
                            },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = entry.category.label(language),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                IconButton(onClick = { onDeleteEntry(entry.id) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = t(language, "حذف", "Delete"))
                                }
                            }
                            Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = entry.content,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatTimestamp(entry.updatedAt, language),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        var title by remember(editingEntry) { mutableStateOf(editingEntry?.title ?: "") }
        var content by remember(editingEntry) { mutableStateOf(editingEntry?.content ?: "") }
        var category by remember(editingEntry) { mutableStateOf(editingEntry?.category ?: JournalCategory.THOUGHTS) }

        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = {
                Text(
                    if (editingEntry == null) t(language, "یادداشت خصوصی جدید", "New Private Entry")
                    else t(language, "ویرایش یادداشت", "Edit Entry")
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        JournalCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.label(language)) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(t(language, "عنوان", "Title")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("journal_title_input")
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text(t(language, "متن یادداشت", "Your thoughts")) },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag("journal_content_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() || content.isNotBlank()) {
                            onSaveEntry(
                                (editingEntry ?: JournalEntry(title = "", content = "")).copy(
                                    title = title.trim().ifBlank { category.label(language) },
                                    content = content.trim(),
                                    category = category
                                )
                            )
                            showEditor = false
                        }
                    },
                    modifier = Modifier.testTag("journal_save_button")
                ) {
                    Text(t(language, "ذخیره", "Save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditor = false }) {
                    Text(t(language, "انصراف", "Cancel"))
                }
            }
        )
    }
}
