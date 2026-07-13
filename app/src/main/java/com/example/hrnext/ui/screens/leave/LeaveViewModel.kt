package com.example.hrnext.ui.screens.leave

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hrnext.data.EmployeeRepository
import com.example.hrnext.data.LeaveRepository
import com.example.hrnext.model.LeaveBalance
import com.example.hrnext.model.PendingLeaveRequest
import com.example.hrnext.model.Session
import kotlinx.coroutines.launch

data class LeaveUiState(
    val isLoadingEmployee: Boolean = true,
    val employeeId: String? = null,
    val employeeError: String? = null,
    val isLoadingBalances: Boolean = true,
    val balances: List<LeaveBalance> = emptyList(),
    val balancesError: String? = null,
    val isLoadingPending: Boolean = true,
    val pendingRequests: List<PendingLeaveRequest> = emptyList(),
    val pendingError: String? = null,
)

class LeaveViewModel(
    private val session: Session,
    private val employeeRepository: EmployeeRepository,
    private val leaveRepository: LeaveRepository,
) : ViewModel() {

    var uiState by mutableStateOf(LeaveUiState())
        private set

    init {
        viewModelScope.launch {
            uiState = uiState.copy(isLoadingEmployee = true, employeeError = null)
            employeeRepository.currentEmployeeId(session)
                .onSuccess { id ->
                    uiState = uiState.copy(isLoadingEmployee = false, employeeId = id)
                    loadBalances(id)
                    loadPendingRequests(id)
                }
                .onFailure { e ->
                    uiState = uiState.copy(
                        isLoadingEmployee = false,
                        employeeError = e.message ?: "Couldn't find an employee record for this account.",
                    )
                }
        }
    }

    private fun loadBalances(employeeId: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoadingBalances = true, balancesError = null)
            leaveRepository.fetchBalances(employeeId)
                .onSuccess { balances -> uiState = uiState.copy(isLoadingBalances = false, balances = balances) }
                .onFailure { e ->
                    uiState = uiState.copy(isLoadingBalances = false, balancesError = e.message ?: "Couldn't load leave balance.")
                }
        }
    }

    private fun loadPendingRequests(employeeId: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoadingPending = true, pendingError = null)
            leaveRepository.fetchPendingRequests(employeeId, session.username)
                .onSuccess { pending -> uiState = uiState.copy(isLoadingPending = false, pendingRequests = pending) }
                .onFailure { e ->
                    uiState = uiState.copy(isLoadingPending = false, pendingError = e.message ?: "Couldn't load pending requests.")
                }
        }
    }
}
