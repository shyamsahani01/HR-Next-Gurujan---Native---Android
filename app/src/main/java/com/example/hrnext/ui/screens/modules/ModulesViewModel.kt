package com.example.hrnext.ui.screens.modules

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hrnext.data.MetaRepository
import com.example.hrnext.model.ModuleSection
import kotlinx.coroutines.launch

data class ModulesUiState(
    val isLoading: Boolean = false,
    val sections: List<ModuleSection> = emptyList(),
    val error: String? = null,
)

class ModulesViewModel(private val metaRepository: MetaRepository) : ViewModel() {

    var uiState by mutableStateOf(ModulesUiState())
        private set

    init {
        load()
    }

    fun load() {
        if (uiState.isLoading) return
        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val sections = metaRepository.fetchDashboardSections()
                uiState = uiState.copy(isLoading = false, sections = sections)
            } catch (e: Exception) {
                uiState = uiState.copy(isLoading = false, error = e.message ?: "Failed to load your HR modules.")
            }
        }
    }
}
