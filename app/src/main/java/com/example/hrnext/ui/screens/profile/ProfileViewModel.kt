package com.example.hrnext.ui.screens.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hrnext.data.AuthRepository
import com.example.hrnext.data.EmployeeRepository
import com.example.hrnext.model.EmployeeSummary
import com.example.hrnext.model.Session
import com.example.hrnext.network.FrappeApi
import com.example.hrnext.util.stringOrNull
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val fullName: String = "",
    val userImage: String? = null,
    val isLoggingOut: Boolean = false,
    val isLoadingEmployee: Boolean = true,
    val employee: EmployeeSummary? = null,
    val employeeError: String? = null,
)

class ProfileViewModel(
    private val session: Session,
    private val api: FrappeApi,
    private val authRepository: AuthRepository,
    private val employeeRepository: EmployeeRepository,
) : ViewModel() {

    var uiState by mutableStateOf(ProfileUiState(fullName = session.fullName, userImage = session.userImage))
        private set

    init {
        loadProfile()
        loadEmployee()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            runCatching {
                api.getUser(session.username, fields = """["full_name","user_image"]""")
            }.getOrNull()?.let { response ->
                if (response.isSuccessful) {
                    val data = response.body()?.getAsJsonObject("data")
                    uiState = uiState.copy(
                        isLoading = false,
                        fullName = data?.stringOrNull("full_name") ?: session.fullName,
                        userImage = data?.stringOrNull("user_image"),
                    )
                    return@launch
                }
            }
            uiState = uiState.copy(isLoading = false)
        }
    }

    private fun loadEmployee() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoadingEmployee = true, employeeError = null)
            employeeRepository.currentEmployeeId(session)
                .mapCatching { id -> employeeRepository.fetchEmployeeDetails(id).getOrThrow() }
                .onSuccess { doc ->
                    uiState = uiState.copy(isLoadingEmployee = false, employee = doc.toEmployeeSummary())
                }
                .onFailure { e ->
                    uiState = uiState.copy(
                        isLoadingEmployee = false,
                        employeeError = e.message ?: "Couldn't load employee details.",
                    )
                }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoggingOut = true)
            authRepository.logout(session.siteUrl)
            uiState = uiState.copy(isLoggingOut = false)
            onLoggedOut()
        }
    }
}

private fun JsonObject.toEmployeeSummary(): EmployeeSummary = EmployeeSummary(
    id = stringOrNull("name").orEmpty(),
    name = stringOrNull("employee_name").orEmpty(),
    designation = stringOrNull("designation"),
    department = stringOrNull("department"),
    company = stringOrNull("company"),
    dateOfJoining = stringOrNull("date_of_joining"),
    cellNumber = stringOrNull("cell_number"),
    personalEmail = stringOrNull("personal_email"),
    companyEmail = stringOrNull("company_email"),
    branch = stringOrNull("branch"),
    status = stringOrNull("status"),
    image = stringOrNull("image"),
    reportsTo = stringOrNull("reports_to"),
    gender = stringOrNull("gender"),
    dateOfBirth = stringOrNull("date_of_birth"),
)
