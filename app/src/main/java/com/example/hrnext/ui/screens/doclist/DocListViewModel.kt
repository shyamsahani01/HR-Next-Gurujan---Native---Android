package com.example.hrnext.ui.screens.doclist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hrnext.data.DocRepository
import com.example.hrnext.data.MetaRepository
import com.example.hrnext.model.DocTypeMeta
import com.google.gson.JsonObject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class DocListUiState(
    val isLoadingMeta: Boolean = true,
    val meta: DocTypeMeta? = null,
    val items: List<JsonObject> = emptyList(),
    val isLoadingPage: Boolean = false,
    val endReached: Boolean = false,
    val searchText: String = "",
    val error: String? = null,
)

class DocListViewModel(
    private val doctype: String,
    private val employeeFilter: String? = null,
    private val metaRepository: MetaRepository,
    private val docRepository: DocRepository,
) : ViewModel() {

    var uiState by mutableStateOf(DocListUiState())
        private set

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            val meta = metaRepository.fetchDocTypeMeta(doctype)
            uiState = uiState.copy(isLoadingMeta = false, meta = meta)
            loadPage(reset = true)
        }
    }

    fun onSearchChange(value: String) {
        uiState = uiState.copy(searchText = value)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            loadPage(reset = true)
        }
    }

    fun loadNextPage() {
        if (uiState.isLoadingPage || uiState.endReached) return
        loadPage(reset = false)
    }

    fun refresh() {
        loadPage(reset = true)
    }

    private fun loadPage(reset: Boolean) {
        viewModelScope.launch {
            val meta = uiState.meta
            val currentItems = if (reset) emptyList() else uiState.items
            uiState = uiState.copy(isLoadingPage = true, error = null, items = currentItems)
            val fieldNames = meta?.listViewFields?.map { it.fieldname }?.ifEmpty { null } ?: listOf("name")
            val result = docRepository.list(
                doctype = doctype,
                fields = fieldNames,
                filters = employeeFilter?.let { listOf(listOf("employee", "=", it)) },
                searchText = uiState.searchText.takeIf { it.isNotBlank() },
                titleField = meta?.titleField,
                start = currentItems.size,
            )
            result.onSuccess { rows ->
                uiState = uiState.copy(
                    isLoadingPage = false,
                    items = currentItems + rows,
                    endReached = rows.size < DocRepository.PAGE_SIZE,
                )
            }.onFailure { e ->
                uiState = uiState.copy(isLoadingPage = false, error = e.message ?: "Failed to load records")
            }
        }
    }
}
