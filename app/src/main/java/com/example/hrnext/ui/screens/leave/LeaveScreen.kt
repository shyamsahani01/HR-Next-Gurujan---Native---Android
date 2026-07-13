package com.example.hrnext.ui.screens.leave

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hrnext.di.AppContainer
import com.example.hrnext.model.LeaveBalance
import com.example.hrnext.model.PendingLeaveRequest
import com.example.hrnext.model.Session
import com.example.hrnext.ui.AppViewModelFactory
import com.example.hrnext.ui.components.iconForDoctype
import com.example.hrnext.ui.screens.doclist.DocListScreen
import com.example.hrnext.ui.theme.accentColorFor

/** Wraps the generic [DocListScreen] engine for `Leave Application`, filtered to the current
 * employee, with a pending-approvals strip and a leave-balance summary strip pinned above the list. */
@Composable
fun LeaveScreen(
    container: AppContainer,
    session: Session,
    onOpenRecord: (String) -> Unit,
    onCreateNew: () -> Unit,
) {
    val factory = remember(session.siteUrl) { AppViewModelFactory(container, siteUrl = session.siteUrl, session = session) }
    val viewModel: LeaveViewModel = viewModel(key = "leave:${session.siteUrl}:${session.username}", factory = factory)
    val state = viewModel.uiState

    DocListScreen(
        container = container,
        session = session,
        doctype = "Leave Application",
        title = "Leave",
        employeeFilter = state.employeeId,
        onOpenRecord = onOpenRecord,
        onCreateNew = onCreateNew,
        headerContent = {
            Column {
                PendingRequestsSection(
                    isLoading = state.isLoadingPending,
                    requests = state.pendingRequests,
                    error = state.pendingError,
                    onOpenRecord = onOpenRecord,
                )
                LeaveSummarySection(
                    isLoading = state.isLoadingBalances,
                    balances = state.balances,
                    error = state.balancesError ?: state.employeeError,
                )
            }
        },
    )
}

@Composable
private fun PendingRequestsSection(
    isLoading: Boolean,
    requests: List<PendingLeaveRequest>,
    error: String?,
    onOpenRecord: (String) -> Unit,
) {
    if (isLoading || error != null || requests.isEmpty()) return
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            "Pending approval",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(requests, key = { it.name }) { request ->
                PendingRequestCard(request = request, onClick = { onOpenRecord(request.name) })
            }
        }
    }
}

@Composable
private fun PendingRequestCard(request: PendingLeaveRequest, onClick: () -> Unit) {
    val accent = accentColorFor(request.leaveType)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.width(200.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.HourglassTop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (request.isMine) "Your request" else request.employeeName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Surface(
                color = accent.container,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    request.leaveType,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent.onContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${request.fromDate} → ${request.toDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "%.1f day(s)".format(request.totalDays),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LeaveSummarySection(isLoading: Boolean, balances: List<LeaveBalance>, error: String?) {
    when {
        isLoading -> Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
        error != null -> Text(
            error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        balances.isEmpty() -> Text(
            "No leave allocations found.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        else -> LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(balances, key = { it.leaveType }) { balance -> LeaveBalanceCard(balance) }
        }
    }
}

@Composable
private fun LeaveBalanceCard(balance: LeaveBalance) {
    val accent = accentColorFor(balance.leaveType)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.width(190.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(accent.container),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        iconForDoctype("Leave Type"),
                        contentDescription = null,
                        tint = accent.onContainer,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    balance.leaveType,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "%.1f of %.1f remaining".format(balance.remaining, balance.allocated),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "This month: %.1f".format(balance.usedThisMonth),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
