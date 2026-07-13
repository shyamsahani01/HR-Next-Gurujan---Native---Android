package com.example.hrnext.data

import com.example.hrnext.model.LeaveBalance
import com.example.hrnext.model.PendingLeaveRequest
import com.example.hrnext.util.doubleOrZero
import com.example.hrnext.util.stringOrNull
import java.time.LocalDate
import java.time.YearMonth

/** Leave balance = allocated (from `Leave Allocation`) minus approved days taken (from
 * `Leave Application`), grouped by leave type, computed client-side since Frappe doesn't expose
 * this as a single generic-REST-friendly endpoint. */
class LeaveRepository(private val docRepository: DocRepository) {

    private data class Allocation(val leaveType: String, val allocated: Double, val from: LocalDate?, val to: LocalDate?)

    suspend fun fetchBalances(employeeId: String, today: LocalDate = LocalDate.now()): Result<List<LeaveBalance>> {
        val allocations = docRepository.list(
            doctype = "Leave Allocation",
            fields = listOf("leave_type", "total_leaves_allocated", "from_date", "to_date"),
            filters = listOf(listOf("employee", "=", employeeId), listOf("docstatus", "=", 1)),
            pageSize = 100,
        ).getOrElse { return Result.failure(it) }
            .map { row ->
                Allocation(
                    leaveType = row.stringOrNull("leave_type").orEmpty(),
                    allocated = row.doubleOrZero("total_leaves_allocated"),
                    from = row.stringOrNull("from_date")?.toLocalDateOrNull(),
                    to = row.stringOrNull("to_date")?.toLocalDateOrNull(),
                )
            }
            .filter { it.leaveType.isNotBlank() }

        val applications = docRepository.list(
            doctype = "Leave Application",
            fields = listOf("leave_type", "from_date", "total_leave_days", "status"),
            filters = listOf(listOf("employee", "=", employeeId), listOf("status", "=", "Approved")),
            pageSize = 200,
        ).getOrElse { return Result.failure(it) }

        // Prefer the allocation window covering today; fall back to the most recent one.
        val allocationByType = allocations.groupBy { it.leaveType }.mapValues { (_, rows) ->
            rows.firstOrNull { it.from != null && it.to != null && !today.isBefore(it.from) && !today.isAfter(it.to) }
                ?: rows.maxByOrNull { it.to ?: LocalDate.MIN }
        }

        val currentMonth = YearMonth.from(today)
        val leaveTypes = allocationByType.keys + applications.mapNotNull { it.stringOrNull("leave_type") }

        val balances = leaveTypes.map { type ->
            val allocation = allocationByType[type]
            val appsForType = applications.filter { it.stringOrNull("leave_type") == type }

            val usedOverall = appsForType.filter { app ->
                val from = app.stringOrNull("from_date")?.toLocalDateOrNull()
                val windowFrom = allocation?.from
                val windowTo = allocation?.to
                from != null && (windowFrom == null || windowTo == null || (!from.isBefore(windowFrom) && !from.isAfter(windowTo)))
            }.sumOf { it.doubleOrZero("total_leave_days") }

            val usedThisMonth = appsForType.filter { app ->
                val from = app.stringOrNull("from_date")?.toLocalDateOrNull()
                from != null && YearMonth.from(from) == currentMonth
            }.sumOf { it.doubleOrZero("total_leave_days") }

            val allocated = allocation?.allocated ?: 0.0
            LeaveBalance(
                leaveType = type,
                allocated = allocated,
                usedOverall = usedOverall,
                usedThisMonth = usedThisMonth,
                remaining = allocated - usedOverall,
            )
        }.sortedBy { it.leaveType }

        return Result.success(balances)
    }

    /** Leave requests still awaiting a decision: my own open applications, plus anyone else's
     * where I'm the assigned approver — combined via `or_filters` in a single query. */
    suspend fun fetchPendingRequests(employeeId: String, username: String): Result<List<PendingLeaveRequest>> =
        docRepository.list(
            doctype = "Leave Application",
            fields = listOf("employee", "employee_name", "leave_type", "from_date", "to_date", "total_leave_days", "leave_approver"),
            filters = listOf(listOf("status", "=", "Open")),
            orFilters = listOf(listOf("employee", "=", employeeId), listOf("leave_approver", "=", username)),
            orderBy = "from_date asc",
            pageSize = 100,
        ).map { rows ->
            rows.map { row ->
                PendingLeaveRequest(
                    name = row.stringOrNull("name").orEmpty(),
                    employeeName = row.stringOrNull("employee_name") ?: row.stringOrNull("employee").orEmpty(),
                    leaveType = row.stringOrNull("leave_type").orEmpty(),
                    fromDate = row.stringOrNull("from_date").orEmpty(),
                    toDate = row.stringOrNull("to_date").orEmpty(),
                    totalDays = row.doubleOrZero("total_leave_days"),
                    isMine = row.stringOrNull("employee") == employeeId,
                )
            }
        }
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()
