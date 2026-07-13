package com.example.hrnext.ui.screens.profile

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hrnext.di.AppContainer
import com.example.hrnext.model.EmployeeSummary
import com.example.hrnext.model.Session
import com.example.hrnext.service.CheckinLocationService
import com.example.hrnext.ui.AppViewModelFactory
import com.example.hrnext.ui.components.CrossfadeAsyncImage
import kotlinx.coroutines.launch
import java.net.URI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    container: AppContainer,
    session: Session,
    onLoggedOut: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val factory = remember(session.siteUrl) { AppViewModelFactory(container, siteUrl = session.siteUrl, session = session) }
    val viewModel: ProfileViewModel = viewModel(key = "profile:${session.siteUrl}:${session.username}", factory = factory)
    val state = viewModel.uiState
    var showLogoutConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val themeMode by container.sessionManager.themeModeFlow.collectAsState(initial = "system")

    val host = remember(session.siteUrl) { runCatching { URI(session.siteUrl).host }.getOrNull() ?: session.siteUrl }
    val imageUrl = remember(state.userImage, session.siteUrl) {
        state.userImage?.takeIf { it.isNotBlank() }?.let { path ->
            if (path.startsWith("http")) path else session.siteUrl.trimEnd('/') + path
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                            start = Offset(0f, 0f),
                            end = Offset(700f, 300f),
                        ),
                    )
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .border(3.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (imageUrl != null) {
                            CrossfadeAsyncImage(
                                model = imageUrl,
                                contentDescription = "Profile photo",
                                modifier = Modifier.size(86.dp).clip(CircleShape),
                            )
                        } else {
                            Text(
                                state.fullName.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        state.fullName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text(
                        session.username,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Connected site",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(host, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                EmployeeInfoSection(
                    isLoading = state.isLoadingEmployee,
                    employee = state.employee,
                    error = state.employeeError,
                )

                Spacer(Modifier.height(14.dp))

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(14.dp))
                        val options = listOf("system" to "System", "light" to "Light", "dark" to "Dark")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            options.forEachIndexed { index, (mode, label) ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                    selected = themeMode == mode,
                                    onClick = { scope.launch { container.sessionManager.setThemeMode(mode) } },
                                ) { Text(label) }
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Surface(
                    onClick = { showLogoutConfirm = true },
                    enabled = !state.isLoggingOut,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (state.isLoggingOut) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Log Out",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Log out?") },
            text = { Text("You'll need your username and password to log back in.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    // Stop any running check-in tracking so it doesn't keep pinging under a logged-out session.
                    context.stopService(Intent(context, CheckinLocationService::class.java))
                    scope.launch { container.sessionManager.setCheckinState(false, null) }
                    viewModel.logout(onLoggedOut)
                }) { Text("Log Out") }
            },
            dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun EmployeeInfoSection(isLoading: Boolean, employee: EmployeeSummary?, error: String?) {
    when {
        isLoading -> Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Loading employee details…", style = MaterialTheme.typography.bodyMedium)
            }
        }
        error != null -> Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        employee != null -> Column {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow(Icons.Filled.Work, "Designation", employee.designation ?: "—")
                    Spacer(Modifier.height(12.dp))
                    InfoRow(Icons.Filled.Badge, "Employee ID", employee.id)
                    if (employee.department != null) {
                        Spacer(Modifier.height(12.dp))
                        InfoRow(Icons.Filled.Work, "Department", employee.department)
                    }
                    if (employee.dateOfJoining != null) {
                        Spacer(Modifier.height(12.dp))
                        InfoRow(Icons.Filled.CalendarMonth, "Date of joining", employee.dateOfJoining)
                    }
                }
            }

            val contactRows = listOfNotNull(
                employee.cellNumber?.let { Icons.Filled.Phone to ("Mobile" to it) },
                employee.companyEmail?.let { Icons.Filled.Email to ("Company email" to it) },
                employee.personalEmail?.let { Icons.Filled.Email to ("Personal email" to it) },
            )
            if (contactRows.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        contactRows.forEachIndexed { index, (icon, pair) ->
                            if (index > 0) Spacer(Modifier.height(12.dp))
                            InfoRow(icon, pair.first, pair.second)
                        }
                    }
                }
            }
        }
        else -> Unit
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}
