package com.example.hrnext.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.hrnext.model.DocField
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Fieldtypes that are never worth trying to edit inline; shown as read-only text instead. */
private val READONLY_DISPLAY_FIELDTYPES = setOf(
    "Attach", "Attach Image", "Signature", "Geolocation", "Barcode", "Code", "JSON",
    "Markdown Editor", "HTML Editor",
)

@Composable
fun FieldRenderer(
    field: DocField,
    value: String,
    isEditing: Boolean,
    onValueChange: (String) -> Unit,
    onSearchLink: suspend (targetDoctype: String, query: String) -> List<String> = { _, _ -> emptyList() },
) {
    val editable = isEditing && !field.readOnly && field.fieldtype !in READONLY_DISPLAY_FIELDTYPES

    if (!editable) {
        ReadOnlyFieldRow(field, value)
        return
    }

    when (field.fieldtype) {
        "Check" -> CheckField(field, value, onValueChange)
        "Select" -> SelectField(field, value, onValueChange)
        "Date" -> DateField(field, value, onValueChange)
        "Link" -> LinkField(field, value, onValueChange, onSearchLink)
        "Text", "Small Text", "Long Text" -> TextField(field, value, onValueChange, singleLine = false)
        "Int" -> NumberField(field, value, onValueChange, allowDecimal = false)
        "Float", "Currency", "Percent", "Duration", "Rating" -> NumberField(field, value, onValueChange, allowDecimal = true)
        else -> TextField(field, value, onValueChange, singleLine = true)
    }
}

@Composable
private fun ReadOnlyFieldRow(field: DocField, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(field.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        val displayValue = when (field.fieldtype) {
            "Check" -> if (value == "1" || value.equals("true", ignoreCase = true)) "Yes" else "No"
            else -> value.ifBlank { "—" }
        }
        if (field.fieldtype == "Color" && value.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrDefault(Color.Gray)),
                )
                Spacer(Modifier.width(8.dp))
                Text(displayValue, style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Text(displayValue, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun CheckField(field: DocField, value: String, onValueChange: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(field.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = value == "1", onCheckedChange = { onValueChange(if (it) "1" else "0") })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectField(field: DocField, value: String, onValueChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember(field.options) {
        field.options.orEmpty().split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.padding(vertical = 6.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(field.label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(field: DocField, value: String, onValueChange: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(field.label) },
            trailingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDialog = true },
        )
    }

    if (showDialog) {
        val initialMillis = value.takeIf { it.isNotBlank() }?.let { runCatching { DATE_FORMAT.parse(it)?.time }.getOrNull() }
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onValueChange(DATE_FORMAT.format(java.util.Date(it))) }
                    showDialog = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun LinkField(
    field: DocField,
    value: String,
    onValueChange: (String) -> Unit,
    onSearchLink: suspend (String, String) -> List<String>,
) {
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val targetDoctype = field.options.orEmpty()

    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                onValueChange(newValue)
                showSuggestions = true
                scope.launch {
                    suggestions = onSearchLink(targetDoctype, newValue)
                }
            },
            label = { Text("${field.label} (${targetDoctype})") },
            trailingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        if (showSuggestions && suggestions.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Column {
                    suggestions.take(6).forEach { option ->
                        Text(
                            option,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onValueChange(option)
                                    showSuggestions = false
                                }
                                .padding(12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberField(field: DocField, value: String, onValueChange: (String) -> Unit, allowDecimal: Boolean) {
    val pattern = remember(allowDecimal) { Regex(if (allowDecimal) "^-?\\d*\\.?\\d*$" else "^-?\\d*$") }
    OutlinedTextField(
        value = value,
        onValueChange = { newValue -> if (newValue.isEmpty() || pattern.matches(newValue)) onValueChange(newValue) },
        label = { Text(field.label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (allowDecimal) KeyboardType.Decimal else KeyboardType.Number),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    )
}

@Composable
private fun TextField(field: DocField, value: String, onValueChange: (String) -> Unit, singleLine: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(field.label) },
        singleLine = singleLine,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    )
}
