package com.example.hrnext.data

import com.example.hrnext.model.AttendanceDay
import com.example.hrnext.util.doubleOrZero
import com.example.hrnext.util.stringOrNull
import java.time.LocalDate
import java.time.YearMonth

class AttendanceRepository(private val docRepository: DocRepository) {

    suspend fun fetchMonth(employeeId: String, yearMonth: YearMonth): Result<List<AttendanceDay>> =
        docRepository.list(
            doctype = "Attendance",
            fields = listOf("attendance_date", "status", "in_time", "out_time", "working_hours"),
            filters = listOf(
                listOf("employee", "=", employeeId),
                listOf(
                    "attendance_date",
                    "between",
                    listOf(yearMonth.atDay(1).toString(), yearMonth.atEndOfMonth().toString()),
                ),
            ),
            orderBy = "attendance_date asc",
            pageSize = 40,
        ).map { rows ->
            rows.mapNotNull { row ->
                val dateStr = row.stringOrNull("attendance_date") ?: return@mapNotNull null
                val status = row.stringOrNull("status") ?: return@mapNotNull null
                runCatching {
                    AttendanceDay(
                        date = LocalDate.parse(dateStr),
                        status = status,
                        inTime = row.stringOrNull("in_time"),
                        outTime = row.stringOrNull("out_time"),
                        workingHours = row.doubleOrZero("working_hours").takeIf { it > 0 },
                    )
                }.getOrNull()
            }
        }
}
