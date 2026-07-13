package com.example.hrnext.ui.screens.mylist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hrnext.data.EmployeeRepository
import com.example.hrnext.model.Session
import kotlinx.coroutines.launch

data class MyRecordsUiState(
    val isLoadingEmployee: Boolean = true,
    val employeeId: String? = null,
    val employeeError: String? = null,
    val showMineOnly: Boolean = true,
)

/** Backs any doctype list that should default to "just my own records" (Expense Claim, Employee
 * Advance, …) with a top-bar toggle to switch to every record the backend's ACL allows. */
class MyRecordsViewModel(
    private val session: Session,
    private val employeeRepository: EmployeeRepository,
) : ViewModel() {

    var uiState by mutableStateOf(MyRecordsUiState())
        private set

    init {
        viewModelScope.launch {
            employeeRepository.currentEmployeeId(session)
                .onSuccess { id -> uiState = uiState.copy(isLoadingEmployee = false, employeeId = id) }
                .onFailure { e ->
                    uiState = uiState.copy(
                        isLoadingEmployee = false,
                        employeeError = e.message ?: "Couldn't find an employee record for this account.",
                    )
                }
        }
    }

    fun toggleShowMineOnly() {
        uiState = uiState.copy(showMineOnly = !uiState.showMineOnly)
    }
}
