package com.example.hrnext.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.hrnext.data.AttendanceRepository
import com.example.hrnext.data.AuthRepository
import com.example.hrnext.data.CheckinRepository
import com.example.hrnext.data.DocRepository
import com.example.hrnext.data.EmployeeRepository
import com.example.hrnext.data.LeaveRepository
import com.example.hrnext.data.MetaRepository
import com.example.hrnext.data.TeamRepository
import com.example.hrnext.di.AppContainer
import com.example.hrnext.model.Session
import com.example.hrnext.ui.screens.attendance.AttendanceViewModel
import com.example.hrnext.ui.screens.docdetail.DocDetailViewModel
import com.example.hrnext.ui.screens.doclist.DocListViewModel
import com.example.hrnext.ui.screens.home.HomeViewModel
import com.example.hrnext.ui.screens.leave.LeaveViewModel
import com.example.hrnext.ui.screens.login.LoginViewModel
import com.example.hrnext.ui.screens.modules.ModulesViewModel
import com.example.hrnext.ui.screens.mylist.MyRecordsViewModel
import com.example.hrnext.ui.screens.profile.ProfileViewModel

/**
 * Manual DI: builds each screen's ViewModel with the repositories it needs from [AppContainer].
 * [siteUrl] is required for any screen that talks to Frappe once a session exists; [doctype]
 * (and [docName] for the detail screen) select which generic-engine instance to build.
 */
class AppViewModelFactory(
    private val container: AppContainer,
    private val siteUrl: String? = null,
    private val doctype: String? = null,
    private val docName: String? = null,
    private val isNewRecord: Boolean = false,
    private val session: Session? = null,
    private val employeeFilter: String? = null,
) : ViewModelProvider.Factory {

    private val authRepository by lazy { AuthRepository(container) }
    private val metaRepository by lazy { MetaRepository(requireApi()) }
    private val docRepository by lazy { DocRepository(requireApi()) }
    private val employeeRepository by lazy { EmployeeRepository(docRepository, container.sessionManager) }
    private val checkinRepository by lazy { CheckinRepository(docRepository) }
    private val attendanceRepository by lazy { AttendanceRepository(docRepository) }
    private val leaveRepository by lazy { LeaveRepository(docRepository) }
    private val teamRepository by lazy { TeamRepository(docRepository) }

    private fun requireApi() = container.apiFor(siteUrl ?: error("siteUrl is required for this screen"))
    private fun requireDoctype() = doctype ?: error("doctype is required for this screen")
    private fun requireDocName() = docName ?: error("docName is required for this screen")
    private fun requireSession() = session ?: error("session is required for this screen")

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) ->
                LoginViewModel(authRepository) as T
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(
                    requireSession(),
                    container.sessionManager,
                    employeeRepository,
                    checkinRepository,
                    attendanceRepository,
                    leaveRepository,
                    teamRepository,
                ) as T
            modelClass.isAssignableFrom(ModulesViewModel::class.java) ->
                ModulesViewModel(metaRepository) as T
            modelClass.isAssignableFrom(AttendanceViewModel::class.java) ->
                AttendanceViewModel(requireSession(), employeeRepository, attendanceRepository) as T
            modelClass.isAssignableFrom(LeaveViewModel::class.java) ->
                LeaveViewModel(requireSession(), employeeRepository, leaveRepository) as T
            modelClass.isAssignableFrom(MyRecordsViewModel::class.java) ->
                MyRecordsViewModel(requireSession(), employeeRepository) as T
            modelClass.isAssignableFrom(DocListViewModel::class.java) ->
                DocListViewModel(requireDoctype(), employeeFilter, metaRepository, docRepository) as T
            modelClass.isAssignableFrom(DocDetailViewModel::class.java) ->
                DocDetailViewModel(requireDoctype(), requireDocName(), isNewRecord, metaRepository, docRepository) as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) ->
                ProfileViewModel(requireSession(), requireApi(), authRepository, employeeRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
