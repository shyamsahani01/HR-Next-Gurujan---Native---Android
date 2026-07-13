package com.example.hrnext.ui.screens.home

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hrnext.data.AttendanceRepository
import com.example.hrnext.data.CheckinRepository
import com.example.hrnext.data.EmployeeRepository
import com.example.hrnext.data.LeaveRepository
import com.example.hrnext.data.TeamRepository
import com.example.hrnext.location.LocationProvider
import com.example.hrnext.model.LeaveBalance
import com.example.hrnext.model.OnLeaveToday
import com.example.hrnext.model.Session
import com.example.hrnext.model.TeamMilestone
import com.example.hrnext.network.SessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.YearMonth

data class HomeUiState(
    val isResolvingEmployee: Boolean = true,
    val employeeError: String? = null,

    val isLoadingCheckin: Boolean = true,
    val isCheckedIn: Boolean = false,
    val checkinSinceRaw: String? = null,
    val checkinActionInProgress: Boolean = false,
    val checkinError: String? = null,

    val isLoadingAttendance: Boolean = true,
    val attendanceSummary: Map<String, Int> = emptyMap(),
    val attendanceError: String? = null,

    val isLoadingLeave: Boolean = true,
    val leaveBalances: List<LeaveBalance> = emptyList(),
    val leaveError: String? = null,

    val isLoadingBirthdays: Boolean = true,
    val birthdays: List<TeamMilestone> = emptyList(),

    val isLoadingAnniversaries: Boolean = true,
    val anniversaries: List<TeamMilestone> = emptyList(),

    val isLoadingOnLeaveToday: Boolean = true,
    val onLeaveToday: List<OnLeaveToday> = emptyList(),
)

enum class CheckinAction { CHECK_IN, CHECK_OUT }

/** Home is the personalized "my day" surface: today's check-in status, this month's attendance
 * tally, current leave balance, and lightweight team widgets — all scoped to the logged-in employee
 * (the team widgets are company-wide and simply degrade to empty if the account lacks visibility). */
class HomeViewModel(
    private val session: Session,
    private val sessionManager: SessionManager,
    private val employeeRepository: EmployeeRepository,
    private val checkinRepository: CheckinRepository,
    private val attendanceRepository: AttendanceRepository,
    private val leaveRepository: LeaveRepository,
    private val teamRepository: TeamRepository,
) : ViewModel() {

    enum class ServiceCommand { START, STOP }

    var uiState by mutableStateOf(HomeUiState())
        private set

    private val _serviceCommands = MutableSharedFlow<ServiceCommand>(extraBufferCapacity = 4)
    val serviceCommands: SharedFlow<ServiceCommand> = _serviceCommands.asSharedFlow()

    private var employeeId: String? = null

    init {
        load()
        loadTeamWidgets()
    }

    fun load() {
        viewModelScope.launch {
            uiState = uiState.copy(isResolvingEmployee = true, employeeError = null)
            employeeRepository.currentEmployeeId(session)
                .onSuccess { id ->
                    employeeId = id
                    uiState = uiState.copy(isResolvingEmployee = false)
                    loadCheckinStatus(id)
                    loadAttendance(id)
                    loadLeave(id)
                }
                .onFailure { e ->
                    uiState = uiState.copy(
                        isResolvingEmployee = false,
                        employeeError = e.message ?: "Couldn't find an employee record for this account.",
                    )
                }
        }
    }

    private fun loadCheckinStatus(employeeId: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoadingCheckin = true, checkinError = null)
            checkinRepository.fetchTodayStatus(employeeId)
                .onSuccess { status ->
                    // Server is always the source of truth — reconciles local state after process death.
                    sessionManager.setCheckinState(
                        status.isCheckedIn,
                        if (status.isCheckedIn) System.currentTimeMillis() else null,
                    )
                    uiState = uiState.copy(
                        isLoadingCheckin = false,
                        isCheckedIn = status.isCheckedIn,
                        checkinSinceRaw = status.lastTimeRaw,
                    )
                    _serviceCommands.tryEmit(if (status.isCheckedIn) ServiceCommand.START else ServiceCommand.STOP)
                }
                .onFailure { e ->
                    uiState = uiState.copy(isLoadingCheckin = false, checkinError = e.message ?: "Couldn't load check-in status.")
                }
        }
    }

    private fun loadAttendance(employeeId: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoadingAttendance = true, attendanceError = null)
            attendanceRepository.fetchMonth(employeeId, YearMonth.now())
                .onSuccess { days ->
                    uiState = uiState.copy(
                        isLoadingAttendance = false,
                        attendanceSummary = days.groupingBy { it.status }.eachCount(),
                    )
                }
                .onFailure { e ->
                    uiState = uiState.copy(isLoadingAttendance = false, attendanceError = e.message ?: "Couldn't load attendance.")
                }
        }
    }

    private fun loadLeave(employeeId: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoadingLeave = true, leaveError = null)
            leaveRepository.fetchBalances(employeeId)
                .onSuccess { balances ->
                    uiState = uiState.copy(isLoadingLeave = false, leaveBalances = balances)
                }
                .onFailure { e ->
                    uiState = uiState.copy(isLoadingLeave = false, leaveError = e.message ?: "Couldn't load leave balance.")
                }
        }
    }

    private fun loadTeamWidgets() {
        viewModelScope.launch {
            teamRepository.fetchBirthdaysThisMonth()
                .onSuccess { uiState = uiState.copy(isLoadingBirthdays = false, birthdays = it) }
                .onFailure { uiState = uiState.copy(isLoadingBirthdays = false, birthdays = emptyList()) }
        }
        viewModelScope.launch {
            teamRepository.fetchWorkAnniversariesThisMonth()
                .onSuccess { uiState = uiState.copy(isLoadingAnniversaries = false, anniversaries = it) }
                .onFailure { uiState = uiState.copy(isLoadingAnniversaries = false, anniversaries = emptyList()) }
        }
        viewModelScope.launch {
            teamRepository.fetchOnLeaveToday()
                .onSuccess { uiState = uiState.copy(isLoadingOnLeaveToday = false, onLeaveToday = it) }
                .onFailure { uiState = uiState.copy(isLoadingOnLeaveToday = false, onLeaveToday = emptyList()) }
        }
    }

    fun performCheckinAction(action: CheckinAction, context: Context) {
        val id = employeeId ?: return
        val logType = if (action == CheckinAction.CHECK_IN) "IN" else "OUT"
        viewModelScope.launch {
            uiState = uiState.copy(checkinActionInProgress = true, checkinError = null)
            val location = LocationProvider.getCurrentLocation(context)
            checkinRepository.createCheckin(id, logType, location)
                .onSuccess {
                    uiState = uiState.copy(checkinActionInProgress = false)
                    loadCheckinStatus(id)
                }
                .onFailure { e ->
                    uiState = uiState.copy(
                        checkinActionInProgress = false,
                        checkinError = e.message ?: "Failed to update check-in status.",
                    )
                }
        }
    }
}
