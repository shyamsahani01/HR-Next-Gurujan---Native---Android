package com.example.hrnext.data

import com.example.hrnext.model.OnLeaveToday
import com.example.hrnext.model.TeamMilestone
import com.example.hrnext.util.stringOrNull
import java.time.LocalDate

/** Company-wide "who's who this month" widgets for Home — birthdays, work anniversaries, and
 * who's on leave today. Relies entirely on the backend's own ACLs; a user without read access to
 * other employees' records simply sees an empty section rather than an error. */
class TeamRepository(private val docRepository: DocRepository) {

    suspend fun fetchBirthdaysThisMonth(today: LocalDate = LocalDate.now()): Result<List<TeamMilestone>> =
        docRepository.list(
            doctype = "Employee",
            fields = listOf("employee_name", "date_of_birth", "designation", "image"),
            filters = listOf(listOf("status", "=", "Active"), monthFilter("date_of_birth", today.monthValue)),
            pageSize = 100,
        ).map { rows ->
            rows.mapNotNull { row ->
                val dob = row.stringOrNull("date_of_birth")?.toLocalDateOrNull() ?: return@mapNotNull null
                TeamMilestone(
                    employeeId = row.stringOrNull("name").orEmpty(),
                    name = row.stringOrNull("employee_name").orEmpty(),
                    image = row.stringOrNull("image"),
                    designation = row.stringOrNull("designation"),
                    day = dob.dayOfMonth,
                    isToday = dob.dayOfMonth == today.dayOfMonth,
                )
            }.sortedBy { it.day }
        }

    /** "0 years" (joining this same month/year) isn't an anniversary yet, so those are skipped. */
    suspend fun fetchWorkAnniversariesThisMonth(today: LocalDate = LocalDate.now()): Result<List<TeamMilestone>> =
        docRepository.list(
            doctype = "Employee",
            fields = listOf("employee_name", "date_of_joining", "designation", "image"),
            filters = listOf(listOf("status", "=", "Active"), monthFilter("date_of_joining", today.monthValue)),
            pageSize = 100,
        ).map { rows ->
            rows.mapNotNull { row ->
                val doj = row.stringOrNull("date_of_joining")?.toLocalDateOrNull() ?: return@mapNotNull null
                val years = today.year - doj.year
                if (years <= 0) return@mapNotNull null
                TeamMilestone(
                    employeeId = row.stringOrNull("name").orEmpty(),
                    name = row.stringOrNull("employee_name").orEmpty(),
                    image = row.stringOrNull("image"),
                    designation = row.stringOrNull("designation"),
                    day = doj.dayOfMonth,
                    isToday = doj.dayOfMonth == today.dayOfMonth,
                    yearsOfService = years,
                )
            }.sortedBy { it.day }
        }

    suspend fun fetchOnLeaveToday(today: LocalDate = LocalDate.now()): Result<List<OnLeaveToday>> =
        docRepository.list(
            doctype = "Leave Application",
            fields = listOf("employee", "employee_name", "leave_type"),
            filters = listOf(
                listOf("status", "=", "Approved"),
                listOf("from_date", "<=", today.toString()),
                listOf("to_date", ">=", today.toString()),
            ),
            pageSize = 100,
        ).map { rows ->
            rows.map { row ->
                OnLeaveToday(
                    employeeId = row.stringOrNull("employee").orEmpty(),
                    name = row.stringOrNull("employee_name") ?: row.stringOrNull("employee").orEmpty(),
                    leaveType = row.stringOrNull("leave_type").orEmpty(),
                )
            }
        }

    /** Dates are stored "YYYY-MM-DD"; "-MM-" only ever appears as the month segment since the day
     * segment has no trailing hyphen, so a plain LIKE is a safe month-only filter. */
    private fun monthFilter(field: String, month: Int): List<Any> = listOf(field, "like", "%-${"%02d".format(month)}-%")
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()
