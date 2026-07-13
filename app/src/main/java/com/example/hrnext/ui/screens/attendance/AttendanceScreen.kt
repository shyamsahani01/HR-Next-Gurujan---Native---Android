package com.example.hrnext.ui.screens.attendance

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hrnext.di.AppContainer
import com.example.hrnext.model.AttendanceDay
import com.example.hrnext.model.Session
import com.example.hrnext.ui.AppViewModelFactory
import com.example.hrnext.ui.components.pressScale
import com.example.hrnext.ui.theme.colorForAttendanceStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ATTENDANCE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private val SHORT_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DISPLAY_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

private fun String.toShortTime(): String? = runCatching { LocalDateTime.parse(this, ATTENDANCE_TIME_FORMAT).format(SHORT_TIME_FORMAT) }.getOrNull()
private fun String.toDisplayTime(): String? = runCatching { LocalDateTime.parse(this, ATTENDANCE_TIME_FORMAT).format(DISPLAY_TIME_FORMAT) }.getOrNull()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    container: AppContainer,
    session: Session,
) {
    val factory = remember(session.siteUrl) { AppViewModelFactory(container, siteUrl = session.siteUrl, session = session) }
    val viewModel: AttendanceViewModel = viewModel(key = "attendance:${session.siteUrl}:${session.username}", factory = factory)
    val state = viewModel.uiState
    var selectedDay by remember { mutableStateOf<AttendanceDay?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        IconButton(onClick = { viewModel.goToMonth(state.yearMonth.minusMonths(1)) }) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
                        }
                        Text(
                            monthLabel(state.yearMonth),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        IconButton(onClick = { viewModel.goToMonth(state.yearMonth.plusMonths(1)) }) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Surface(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.error != null && state.days.isEmpty() -> AttendanceErrorState(state.error)
                else -> Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    StatusSummary(days = state.days.values.toList())
                    Spacer(Modifier.height(14.dp))
                    WeekdayHeader()
                    Spacer(Modifier.height(6.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = state.yearMonth,
                            transitionSpec = {
                                val forward = targetState.isAfter(initialState)
                                val dir = if (forward) 1 else -1
                                (slideInHorizontally(tween(280)) { it * dir } + fadeIn(tween(280))) togetherWith
                                    (slideOutHorizontally(tween(280)) { -it * dir } + fadeOut(tween(280)))
                            },
                            label = "monthGrid",
                        ) { yearMonth ->
                            CalendarGrid(yearMonth = yearMonth, days = state.days, onDayClick = { selectedDay = it })
                        }
                        if (state.isLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Legend()
                }
            }
        }
    }

    selectedDay?.let { day ->
        DayDetailDialog(day = day, onDismiss = { selectedDay = null })
    }
}

private fun monthLabel(yearMonth: YearMonth): String =
    "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${yearMonth.year}"

@Composable
private fun WeekdayHeader() {
    val labels = listOf(
        DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
    )
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEach { day ->
            Text(
                day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CalendarGrid(yearMonth: YearMonth, days: Map<LocalDate, AttendanceDay>, onDayClick: (AttendanceDay) -> Unit) {
    val today = remember { LocalDate.now() }
    val leadBlanks = yearMonth.atDay(1).dayOfWeek.value % 7
    val daysInMonth = yearMonth.lengthOfMonth()
    val cells: List<LocalDate?> = List(leadBlanks) { null } + (1..daysInMonth).map { yearMonth.atDay(it) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(cells) { date ->
            if (date == null) {
                Box(modifier = Modifier.aspectRatio(0.78f))
            } else {
                DayCell(date = date, day = days[date], isToday = date == today, onClick = onDayClick)
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, day: AttendanceDay?, isToday: Boolean, onClick: (AttendanceDay) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val status = day?.status
    val background = status?.let { colorForAttendanceStatus(it).copy(alpha = 0.16f) }
        ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)

    Card(
        onClick = { day?.let(onClick) },
        enabled = day != null,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = background, disabledContainerColor = background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (isToday) BorderStroke(1.6.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .aspectRatio(0.78f)
            .pressScale(interactionSource),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                color = status?.let { colorForAttendanceStatus(it) } ?: MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            val inShort = day?.inTime?.toShortTime()
            val outShort = day?.outTime?.toShortTime()
            if (inShort != null) {
                Text(
                    inShort,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (outShort != null) {
                Text(
                    outShort,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun DayDetailDialog(day: AttendanceDay, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(day.date.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")), style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colorForAttendanceStatus(day.status)),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(day.status, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
                if (day.inTime != null || day.outTime != null) {
                    Spacer(Modifier.height(12.dp))
                    day.inTime?.toDisplayTime()?.let {
                        Text("Checked in: $it", style = MaterialTheme.typography.bodyMedium)
                    }
                    day.outTime?.toDisplayTime()?.let {
                        Spacer(Modifier.height(4.dp))
                        Text("Checked out: $it", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                day.workingHours?.let {
                    Spacer(Modifier.height(4.dp))
                    Text("Working hours: %.1f".format(it), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun StatusSummary(days: List<AttendanceDay>) {
    val counts = days.groupingBy { it.status }.eachCount()
    if (counts.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        counts.entries.sortedByDescending { it.value }.forEach { (status, count) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colorForAttendanceStatus(status))
                Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Legend() {
    val statuses = listOf("Present", "Absent", "Half Day", "On Leave", "Work From Home")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(statuses) { status ->
            Surface(
                color = colorForAttendanceStatus(status).copy(alpha = 0.14f),
                shape = RoundedCornerShape(50),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colorForAttendanceStatus(status)),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AttendanceErrorState(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
