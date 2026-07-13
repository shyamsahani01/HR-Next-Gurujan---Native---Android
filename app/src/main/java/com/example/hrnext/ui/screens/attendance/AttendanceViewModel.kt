package com.example.hrnext.ui.screens.attendance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hrnext.data.AttendanceRepository
import com.example.hrnext.data.EmployeeRepository
import com.example.hrnext.model.AttendanceDay
import com.example.hrnext.model.Session
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class AttendanceUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val days: Map<LocalDate, AttendanceDay> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class AttendanceViewModel(
    private val session: Session,
    private val employeeRepository: EmployeeRepository,
    private val attendanceRepository: AttendanceRepository,
) : ViewModel() {

    var uiState by mutableStateOf(AttendanceUiState())
        private set

    private var employeeId: String? = null

    init {
        viewModelScope.launch {
            employeeRepository.currentEmployeeId(session)
                .onSuccess { id ->
                    employeeId = id
                    loadMonth(uiState.yearMonth)
                }
                .onFailure { e ->
                    uiState = uiState.copy(
                        isLoading = false,
                        error = e.message ?: "Couldn't find an employee record for this account.",
                    )
                }
        }
    }

    fun goToMonth(yearMonth: YearMonth) {
        uiState = uiState.copy(yearMonth = yearMonth)
        loadMonth(yearMonth)
    }

    private fun loadMonth(yearMonth: YearMonth) {
        val id = employeeId ?: return
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            attendanceRepository.fetchMonth(id, yearMonth)
                .onSuccess { list ->
                    uiState = uiState.copy(isLoading = false, days = list.associateBy { it.date })
                }
                .onFailure { e ->
                    uiState = uiState.copy(isLoading = false, error = e.message ?: "Couldn't load attendance.")
                }
        }
    }
}
