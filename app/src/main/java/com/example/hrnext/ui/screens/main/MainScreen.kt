package com.example.hrnext.ui.screens.main

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.hrnext.di.AppContainer
import com.example.hrnext.model.Session
import com.example.hrnext.ui.screens.attendance.AttendanceScreen
import com.example.hrnext.ui.screens.doclist.DocListScreen
import com.example.hrnext.ui.screens.home.HomeScreen
import com.example.hrnext.ui.screens.leave.LeaveScreen
import com.example.hrnext.ui.screens.modules.ModulesScreen
import com.example.hrnext.ui.screens.profile.ProfileScreen

/** The app's root bottom-tab set, shown once the user is logged in — Home is the landing tab. */
private enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    ATTENDANCE("Attendance", Icons.Filled.HowToReg),
    LEAVE("Leave", Icons.Filled.BeachAccess),
    PAYROLL("Payroll", Icons.Filled.Payments),
    MORE("More", Icons.Filled.Widgets),
    PROFILE("Profile", Icons.Filled.Person),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    container: AppContainer,
    session: Session,
    onOpenDocType: (String) -> Unit,
    onOpenRecord: (doctype: String, name: String) -> Unit,
    onCreateRecord: (doctype: String) -> Unit,
    onLoggedOut: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.HOME) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                MainTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.18f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "navIconScale",
                    )
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.scale(iconScale)) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                MainTab.HOME -> HomeScreen(
                    container = container,
                    session = session,
                    onOpenProfile = { selectedTab = MainTab.PROFILE },
                    onOpenAttendance = { selectedTab = MainTab.ATTENDANCE },
                    onOpenLeave = { selectedTab = MainTab.LEAVE },
                )
                MainTab.ATTENDANCE -> AttendanceScreen(
                    container = container,
                    session = session,
                )
                MainTab.LEAVE -> LeaveScreen(
                    container = container,
                    session = session,
                    onOpenRecord = { name -> onOpenRecord("Leave Application", name) },
                    onCreateNew = { onCreateRecord("Leave Application") },
                )
                MainTab.PAYROLL -> DocListScreen(
                    container = container,
                    session = session,
                    doctype = "Salary Slip",
                    title = "Payroll",
                    employeeFilter = session.employeeId,
                    allowCreate = false,
                    onOpenRecord = { name -> onOpenRecord("Salary Slip", name) },
                    onCreateNew = {},
                )
                MainTab.MORE -> ModulesScreen(
                    container = container,
                    session = session,
                    onOpenDocType = onOpenDocType,
                )
                MainTab.PROFILE -> ProfileScreen(
                    container = container,
                    session = session,
                    onLoggedOut = onLoggedOut,
                )
            }
        }
    }
}
