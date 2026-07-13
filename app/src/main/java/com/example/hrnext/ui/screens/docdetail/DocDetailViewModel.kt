package com.example.hrnext.ui.screens.docdetail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hrnext.data.DocRepository
import com.example.hrnext.data.MetaRepository
import com.example.hrnext.model.DocField
import com.example.hrnext.model.DocTypeMeta
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

/** Fieldtypes the generic form never writes back (unsupported editors, system/table/secret fields). */
val NON_EDITABLE_FIELDTYPES = setOf(
    "Table", "Table MultiSelect", "HTML", "Button", "Password",
    "Attach", "Attach Image", "Signature", "Geolocation", "Barcode", "Code", "JSON",
    "Markdown Editor", "HTML Editor",
)

data class DocDetailUiState(
    val isLoading: Boolean = true,
    val meta: DocTypeMeta? = null,
    val originalDoc: JsonObject = JsonObject(),
    val values: Map<String, String> = emptyMap(),
    val isNew: Boolean = false,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val saveError: String? = null,
)

class DocDetailViewModel(
    private val doctype: String,
    private val name: String,
    isNewRecord: Boolean,
    private val metaRepository: MetaRepository,
    private val docRepository: DocRepository,
) : ViewModel() {

    var uiState by mutableStateOf(DocDetailUiState(isNew = isNewRecord, isEditing = isNewRecord))
        private set

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            val meta = metaRepository.fetchDocTypeMeta(doctype)
            if (uiState.isNew) {
                uiState = uiState.copy(isLoading = false, meta = meta, originalDoc = JsonObject(), values = emptyMap())
                return@launch
            }
            docRepository.getDoc(doctype, name).onSuccess { doc ->
                uiState = uiState.copy(
                    isLoading = false,
                    meta = meta,
                    originalDoc = doc,
                    values = valuesFromDoc(doc, meta),
                )
            }.onFailure { e ->
                uiState = uiState.copy(isLoading = false, error = e.message ?: "Failed to load record")
            }
        }
    }

    fun onFieldChange(fieldname: String, value: String) {
        uiState = uiState.copy(values = uiState.values + (fieldname to value))
    }

    fun startEditing() {
        uiState = uiState.copy(isEditing = true, saveError = null)
    }

    fun cancelEditing() {
        uiState = uiState.copy(
            isEditing = false,
            saveError = null,
            values = valuesFromDoc(uiState.originalDoc, uiState.meta),
        )
    }

    fun save(onCreated: (String) -> Unit) {
        val meta = uiState.meta ?: return
        viewModelScope.launch {
            uiState = uiState.copy(isSaving = true, saveError = null)
            val body = buildRequestBody(meta)
            val wasNew = uiState.isNew
            val result = if (wasNew) docRepository.create(doctype, body) else docRepository.update(doctype, name, body)
            result.onSuccess { doc ->
                val savedMeta = uiState.meta
                uiState = uiState.copy(
                    isSaving = false,
                    isEditing = false,
                    isNew = false,
                    originalDoc = doc,
                    values = valuesFromDoc(doc, savedMeta),
                )
                if (wasNew) {
                    val newName = doc.get("name")?.asString ?: name
                    onCreated(newName)
                }
            }.onFailure { e ->
                uiState = uiState.copy(isSaving = false, saveError = e.message ?: "Failed to save")
            }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            uiState = uiState.copy(isDeleting = true, saveError = null)
            docRepository.delete(doctype, name).onSuccess {
                uiState = uiState.copy(isDeleting = false)
                onDeleted()
            }.onFailure { e ->
                uiState = uiState.copy(isDeleting = false, saveError = e.message ?: "Failed to delete")
            }
        }
    }

    suspend fun searchLinkOptions(targetDoctype: String, query: String): List<String> {
        if (targetDoctype.isBlank()) return emptyList()
        return docRepository.list(
            doctype = targetDoctype,
            fields = listOf("name"),
            searchText = query,
            titleField = null,
            start = 0,
            pageSize = 20,
        ).getOrDefault(emptyList()).mapNotNull { it.get("name")?.asString }
    }

    private fun valuesFromDoc(doc: JsonObject, meta: DocTypeMeta?): Map<String, String> {
        val fields = meta?.fields ?: return emptyMap()
        return fields.associate { f ->
            val element = doc.get(f.fieldname)
            val str = if (element == null || element.isJsonNull) {
                ""
            } else if (element.isJsonPrimitive) {
                element.asString
            } else {
                ""
            }
            f.fieldname to str
        }
    }

    private fun buildRequestBody(meta: DocTypeMeta): JsonObject {
        val body = JsonObject()
        val original = uiState.originalDoc
        for (field in meta.fields) {
            if (field.readOnly || field.isLayoutOnly || field.fieldtype in NON_EDITABLE_FIELDTYPES) continue
            val raw = uiState.values[field.fieldname] ?: continue
            if (uiState.isNew) {
                if (raw.isBlank()) continue
            } else {
                val originalStr = original.get(field.fieldname)
                    ?.takeIf { it.isJsonPrimitive }
                    ?.asString
                    .orEmpty()
                if (raw == originalStr) continue
            }
            addTypedValue(body, field, raw)
        }
        return body
    }

    private fun addTypedValue(body: JsonObject, field: DocField, raw: String) {
        when (field.fieldtype) {
            "Check" -> body.addProperty(field.fieldname, raw == "1")
            "Int" -> raw.toIntOrNull()?.let { body.addProperty(field.fieldname, it) } ?: body.addProperty(field.fieldname, raw)
            "Float", "Currency", "Percent", "Duration", "Rating" ->
                raw.toDoubleOrNull()?.let { body.addProperty(field.fieldname, it) } ?: body.addProperty(field.fieldname, raw)
            else -> body.addProperty(field.fieldname, raw)
        }
    }
}
