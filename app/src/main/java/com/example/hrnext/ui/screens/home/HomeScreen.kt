package com.example.hrnext.ui.screens.home

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hrnext.di.AppContainer
import com.example.hrnext.model.LeaveBalance
import com.example.hrnext.model.OnLeaveToday
import com.example.hrnext.model.Session
import com.example.hrnext.model.TeamMilestone
import com.example.hrnext.service.CheckinLocationService
import com.example.hrnext.ui.AppViewModelFactory
import com.example.hrnext.ui.components.AnimatedCountText
import com.example.hrnext.ui.components.AnimatedDecimalText
import com.example.hrnext.ui.components.CrossfadeAsyncImage
import com.example.hrnext.ui.components.pressScale
import com.example.hrnext.ui.theme.accentColorFor
import com.example.hrnext.ui.theme.colorForAttendanceStatus
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    session: Session,
    onOpenProfile: () -> Unit,
    onOpenAttendance: () -> Unit,
    onOpenLeave: () -> Unit,
) {
    val factory = remember(session.siteUrl) { AppViewModelFactory(container, siteUrl = session.siteUrl, session = session) }
    val viewModel: HomeViewModel = viewModel(key = "home:${session.siteUrl}:${session.username}", factory = factory)
    val state = viewModel.uiState
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.serviceCommands.collect { command ->
            val intent = Intent(context, CheckinLocationService::class.java)
            when (command) {
                HomeViewModel.ServiceCommand.START -> ContextCompat.startForegroundService(context, intent)
                HomeViewModel.ServiceCommand.STOP -> context.stopService(intent)
            }
        }
    }

    var pendingAction by remember { mutableStateOf<CheckinAction?>(null) }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingAction?.let { viewModel.performCheckinAction(it, context) }
        pendingAction = null
    }

    val fineLocationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val fineOrCoarseGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!fineOrCoarseGranted) {
            pendingAction = null
            return@rememberLauncherForActivityResult
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            pendingAction?.let { viewModel.performCheckinAction(it, context) }
            pendingAction = null
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        fineLocationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    fun startCheckinFlow(action: CheckinAction) {
        pendingAction = action
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            fineLocationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            HomeHeader(session = session, onOpenProfile = onOpenProfile)

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    EntranceAnimated(index = 0) {
                        when {
                            state.employeeError != null -> EmployeeErrorCard(state.employeeError, onRetry = viewModel::load)
                            else -> CheckinCard(
                                state = state,
                                onCheckIn = { startCheckinFlow(CheckinAction.CHECK_IN) },
                                onCheckOut = { startCheckinFlow(CheckinAction.CHECK_OUT) },
                            )
                        }
                    }
                }
                if (state.employeeError == null) {
                    item {
                        EntranceAnimated(index = 1) {
                            AttendanceSnapshotCard(
                                isLoading = state.isLoadingAttendance,
                                summary = state.attendanceSummary,
                                error = state.attendanceError,
                                onClick = onOpenAttendance,
                            )
                        }
                    }
                    item {
                        EntranceAnimated(index = 2) {
                            LeaveSnapshotCard(
                                isLoading = state.isLoadingLeave,
                                balances = state.leaveBalances,
                                error = state.leaveError,
                                onClick = onOpenLeave,
                            )
                        }
                    }
                    if (state.onLeaveToday.isNotEmpty()) {
                        item {
                            EntranceAnimated(index = 3) {
                                OnLeaveTodayCard(state.onLeaveToday)
                            }
                        }
                    }
                    if (state.birthdays.isNotEmpty()) {
                        item {
                            EntranceAnimated(index = 4) {
                                TeamMilestoneSection(
                                    title = "🎂 Birthdays this month",
                                    milestones = state.birthdays,
                                    captionFor = { m -> if (m.isToday) "Today!" else "${currentMonthAbbrev()} ${m.day}" },
                                )
                            }
                        }
                    }
                    if (state.anniversaries.isNotEmpty()) {
                        item {
                            EntranceAnimated(index = 5) {
                                TeamMilestoneSection(
                                    title = "🎉 Work anniversaries",
                                    milestones = state.anniversaries,
                                    captionFor = { m -> "${m.yearsOfService} yr${if (m.yearsOfService == 1) "" else "s"}" },
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

private fun greetingForNow(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 4..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

private fun currentMonthAbbrev(): String =
    java.time.LocalDate.now().month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())

@Composable
private fun HomeHeader(session: Session, onOpenProfile: () -> Unit) {
    val host = remember(session.siteUrl) { runCatching { URI(session.siteUrl).host }.getOrNull() ?: session.siteUrl }
    val greeting = remember { greetingForNow() }
    val firstName = remember(session.fullName) { session.fullName.substringBefore(" ") }

    // A slow, continuous drift in the gradient's end point — a subtle "alive" ambient motion.
    val infiniteTransition = rememberInfiniteTransition(label = "headerGradient")
    val shift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Reverse),
        label = "headerShift",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                    start = Offset(0f, 0f),
                    end = Offset(700f + shift * 260f, 250f - shift * 120f),
                ),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$greeting,",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                )
                Text(
                    firstName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        host,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            val avatarInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .pressScale(avatarInteraction)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f), CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                    .clickable(interactionSource = avatarInteraction, indication = null, onClick = onOpenProfile),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    session.fullName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

private val CHECKIN_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private val DISPLAY_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

private fun formatCheckinTime(raw: String?): String? =
    raw?.let { runCatching { LocalDateTime.parse(it, CHECKIN_TIME_FORMAT).format(DISPLAY_TIME_FORMAT) }.getOrNull() }

/** Scales + fades + slides a Home card in on first composition, staggered by [index] so the whole
 * list is clearly and noticeably sequential rather than everything popping in at once. */
@Composable
private fun EntranceAnimated(index: Int, content: @Composable () -> Unit) {
    val delayMillis = index * 90
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(tween(durationMillis = 500, delayMillis = delayMillis)) +
            slideInVertically(tween(durationMillis = 500, delayMillis = delayMillis)) { it / 3 } +
            scaleIn(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                initialScale = 0.85f,
            ),
    ) {
        content()
    }
}

@Composable
private fun HomeCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp), content = { content() })
    }
}

/** Small accent-tinted icon badge + title, used to give each Home card an identity at a glance
 * instead of a plain text heading. */
@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CheckinCard(
    state: HomeUiState,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
) {
    HomeCard {
        SectionHeader(Icons.Filled.HowToReg, "Today")
        Spacer(Modifier.height(10.dp))
        AnimatedContent(
            targetState = Triple(state.isLoadingCheckin, state.isCheckedIn, state.checkinSinceRaw),
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
            label = "checkinStatus",
        ) { (isLoading, isCheckedIn, sinceRaw) ->
            when {
                isLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Checking status…", style = MaterialTheme.typography.bodyMedium)
                }
                isCheckedIn -> Text(
                    "Checked in since ${formatCheckinTime(sinceRaw) ?: "today"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> Text(
                    "Not checked in yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (state.checkinError != null) {
            Spacer(Modifier.height(6.dp))
            Text(state.checkinError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(14.dp))
        val buttonColor by animateColorAsState(
            targetValue = if (state.isCheckedIn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            animationSpec = tween(300),
            label = "checkinButtonColor",
        )
        // Gentle breathing pulse while idle & not-yet-checked-in, to draw the eye to the action.
        val idlePulseTransition = rememberInfiniteTransition(label = "checkinPulse")
        val idlePulse by idlePulseTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.035f,
            animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "idlePulseScale",
        )
        val shouldPulse = !state.isCheckedIn && !state.checkinActionInProgress && !state.isLoadingCheckin
        Button(
            onClick = if (state.isCheckedIn) onCheckOut else onCheckIn,
            enabled = !state.checkinActionInProgress && !state.isLoadingCheckin,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .graphicsLayer {
                    val s = if (shouldPulse) idlePulse else 1f
                    scaleX = s
                    scaleY = s
                },
        ) {
            AnimatedContent(
                targetState = state.checkinActionInProgress to state.isCheckedIn,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                label = "checkinButtonContent",
            ) { (inProgress, isCheckedIn) ->
                if (inProgress) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (isCheckedIn) "Check Out" else "Check In", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Checking in starts location updates every 2 minutes until you check out.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmployeeErrorCard(message: String, onRetry: () -> Unit) {
    HomeCard {
        Text("Couldn't load your profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry, shape = RoundedCornerShape(50)) { Text("Retry") }
    }
}

@Composable
private fun AttendanceSnapshotCard(
    isLoading: Boolean,
    summary: Map<String, Int>,
    error: String?,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth().pressScale(interactionSource),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            SectionHeader(Icons.Filled.CalendarMonth, "This month's attendance")
            Spacer(Modifier.height(12.dp))
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                error != null -> Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                summary.isEmpty() -> Text(
                    "No attendance recorded yet this month.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    summary.entries.sortedByDescending { it.value }.forEach { (status, count) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedCountText(
                                target = count,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colorForAttendanceStatus(status),
                            )
                            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaveSnapshotCard(
    isLoading: Boolean,
    balances: List<LeaveBalance>,
    error: String?,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth().pressScale(interactionSource),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            SectionHeader(Icons.Filled.BeachAccess, "Leave balance")
            Spacer(Modifier.height(12.dp))
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                error != null -> Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                balances.isEmpty() -> Text(
                    "No leave allocations found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> {
                    val totalRemaining = balances.sumOf { it.remaining }
                    Row(verticalAlignment = Alignment.Bottom) {
                        AnimatedDecimalText(
                            target = totalRemaining,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(" days remaining overall", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap to see leave type breakdown",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun OnLeaveTodayCard(entries: List<OnLeaveToday>) {
    HomeCard {
        SectionHeader(Icons.Filled.EventBusy, "On leave today")
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            entries.take(6).forEach { entry ->
                val accent = accentColorFor(entry.name)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(accent.container),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            entry.name.take(1).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent.onContainer,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(entry.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Surface(color = accent.container, shape = RoundedCornerShape(50)) {
                        Text(
                            entry.leaveType,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent.onContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            if (entries.size > 6) {
                Text(
                    "+${entries.size - 6} more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TeamMilestoneSection(
    title: String,
    milestones: List<TeamMilestone>,
    captionFor: (TeamMilestone) -> String,
) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(milestones, key = { it.employeeId }) { milestone ->
                TeamMilestoneChip(milestone = milestone, caption = captionFor(milestone))
            }
        }
    }
}

@Composable
private fun TeamMilestoneChip(milestone: TeamMilestone, caption: String) {
    val accent = accentColorFor(milestone.name)
    val imageUrl = milestone.image?.takeIf { it.isNotBlank() }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(if (milestone.isToday) 1.5.dp else 1.dp, if (milestone.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.width(116.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accent.container),
                contentAlignment = Alignment.Center,
            ) {
                if (imageUrl != null) {
                    CrossfadeAsyncImage(
                        model = imageUrl,
                        contentDescription = milestone.name,
                        modifier = Modifier.size(48.dp).clip(CircleShape),
                    )
                } else {
                    Text(
                        milestone.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent.onContainer,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                milestone.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = if (milestone.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (milestone.isToday) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}
