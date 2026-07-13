package com.example.hrnext.data

import android.location.Location
import com.example.hrnext.model.EmployeeCheckinStatus
import com.example.hrnext.util.stringOrNull
import com.google.gson.JsonObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** `Employee Checkin` is Frappe HR's log of presence events — used both for the check-in/out
 * button and, reused as-is, for the every-2-minute background location pings while checked in. */
class CheckinRepository(private val docRepository: DocRepository) {

    /** Scoped to *today only* by design: a forgotten checkout from a prior day reads as "not checked in". */
    suspend fun fetchTodayStatus(employeeId: String): Result<EmployeeCheckinStatus> {
        val today = LocalDate.now().toString()
        return docRepository.list(
            doctype = "Employee Checkin",
            fields = listOf("log_type", "time"),
            filters = listOf(
                listOf("employee", "=", employeeId),
                listOf("time", ">=", "$today 00:00:00"),
                listOf("time", "<=", "$today 23:59:59"),
            ),
            orderBy = "time desc",
            pageSize = 1,
        ).map { rows ->
            val latest = rows.firstOrNull()
            EmployeeCheckinStatus(
                isCheckedIn = latest?.stringOrNull("log_type") == "IN",
                lastLogType = latest?.stringOrNull("log_type"),
                lastTimeRaw = latest?.stringOrNull("time"),
            )
        }
    }

    suspend fun createCheckin(employeeId: String, logType: String, location: Location?): Result<JsonObject> {
        val body = JsonObject().apply {
            addProperty("employee", employeeId)
            addProperty("log_type", logType)
            addProperty("time", LocalDateTime.now().format(DATETIME_FORMAT))
            location?.let {
                addProperty("latitude", it.latitude)
                addProperty("longitude", it.longitude)
            }
        }
        return docRepository.create("Employee Checkin", body)
    }

    private companion object {
        /** Frappe `Datetime` fields expect site-local "yyyy-MM-dd HH:mm:ss" — not ISO-8601/Instant. */
        val DATETIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
