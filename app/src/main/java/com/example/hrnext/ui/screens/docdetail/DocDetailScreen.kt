package com.example.hrnext.ui.screens.docdetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hrnext.di.AppContainer
import com.example.hrnext.model.DocField
import com.example.hrnext.model.Session
import com.example.hrnext.ui.AppViewModelFactory
import com.example.hrnext.ui.components.FieldRenderer
import com.example.hrnext.ui.navigation.Routes
import com.example.hrnext.ui.theme.accentColorFor
import com.google.gson.JsonObject

private val SKIPPED_FIELDTYPES = setOf("Section Break", "Column Break", "Tab Break", "HTML", "Button", "Password")
private val TABLE_FIELDTYPES = setOf("Table", "Table MultiSelect")
private val SYSTEM_CHILD_KEYS = setOf(
    "name", "idx", "parent", "parentfield", "parenttype", "doctype", "owner",
    "creation", "modified", "modified_by", "docstatus",
)

private data class FieldSection(val title: String?, val fields: List<DocField>)

private fun groupIntoSections(fields: List<DocField>): List<FieldSection> {
    val sections = mutableListOf<FieldSection>()
    var current = mutableListOf<DocField>()
    var currentTitle: String? = null

    fun flush() {
        if (current.isNotEmpty()) sections += FieldSection(currentTitle, current.toList())
        current = mutableListOf()
    }

    for (field in fields) {
        when {
            field.fieldtype == "Section Break" -> {
                flush()
                currentTitle = field.label.ifBlank { null }
            }
            field.fieldtype == "Column Break" -> Unit
            field.hidden -> Unit
            field.fieldtype in SKIPPED_FIELDTYPES -> Unit
            else -> current += field
        }
    }
    flush()
    return sections
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocDetailScreen(
    container: AppContainer,
    session: Session,
    doctype: String,
    name: String,
    onBack: () -> Unit,
    onRecordCreated: (String) -> Unit = {},
) {
    val isNew = name == Routes.NEW_DOC_NAME
    val factory = remember(session.siteUrl, doctype, name) {
        AppViewModelFactory(container, siteUrl = session.siteUrl, doctype = doctype, docName = name, isNewRecord = isNew)
    }
    val viewModel: DocDetailViewModel = viewModel(key = "docdetail:${session.siteUrl}:$doctype:$name", factory = factory)
    val state = viewModel.uiState
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val titleText = if (isNew) "New $doctype" else doctype

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!state.isLoading && state.error == null) {
                        if (state.isEditing) {
                            IconButton(onClick = { if (isNew) onBack() else viewModel.cancelEditing() }, enabled = !state.isSaving) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancel")
                            }
                            TextButton(onClick = { viewModel.save(onCreated = onRecordCreated) }, enabled = !state.isSaving) {
                                if (state.isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Save")
                                }
                            }
                        } else {
                            IconButton(onClick = viewModel::startEditing) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { showDeleteConfirm = true }, enabled = !state.isDeleting) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.error != null -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            state.error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = viewModel::load, shape = RoundedCornerShape(50)) { Text("Retry") }
                    }
                }
                else -> DocDetailContent(
                    doc = state.originalDoc,
                    fields = state.meta?.fields.orEmpty(),
                    values = state.values,
                    isEditing = state.isEditing,
                    saveError = state.saveError,
                    onFieldChange = viewModel::onFieldChange,
                    onSearchLink = viewModel::searchLinkOptions,
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this record?") },
            text = { Text("This will permanently delete this $doctype. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete(onDeleted = onBack)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DocDetailContent(
    doc: JsonObject,
    fields: List<DocField>,
    values: Map<String, String>,
    isEditing: Boolean,
    saveError: String?,
    onFieldChange: (String, String) -> Unit,
    onSearchLink: suspend (String, String) -> List<String>,
) {
    val sections = remember(fields) { groupIntoSections(fields) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (saveError != null) {
            item {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                        Icon(
                            Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(saveError, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        itemsIndexed(sections) { index, section ->
            val accent = accentColorFor(section.title ?: "General $index")
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (section.title != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(width = 4.dp, height = 16.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(accent.solid),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                section.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    section.fields.forEach { field ->
                        if (field.fieldtype in TABLE_FIELDTYPES) {
                            TableFieldCard(field = field, doc = doc)
                        } else {
                            FieldRenderer(
                                field = field,
                                value = values[field.fieldname].orEmpty(),
                                isEditing = isEditing,
                                onValueChange = { onFieldChange(field.fieldname, it) },
                                onSearchLink = onSearchLink,
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun TableFieldCard(field: DocField, doc: JsonObject) {
    var expanded by remember { mutableStateOf(false) }
    val rows = remember(doc, field.fieldname) { doc.getAsJsonArray(field.fieldname)?.mapNotNull { it.asJsonObject }.orEmpty() }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(field.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    "${rows.size} row${if (rows.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                rows.forEachIndexed { index, row ->
                    val summary = row.entrySet()
                        .filter { (key, value) -> key !in SYSTEM_CHILD_KEYS && value.isJsonPrimitive }
                        .take(3)
                        .joinToString("  •  ") { (key, value) -> "$key: ${value.asString}" }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    ) {
                        Text(
                            summary.ifBlank { "Row ${index + 1}" },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
