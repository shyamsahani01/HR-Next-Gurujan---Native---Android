package com.example.hrnext.data

import com.example.hrnext.model.Session
import com.example.hrnext.network.SessionManager
import com.google.gson.JsonObject

/** Bridges the logged-in Frappe `User` to their `Employee` record — every self-service query
 * (checkins, attendance, leave) needs the employee id, not the username. */
class EmployeeRepository(
    private val docRepository: DocRepository,
    private val sessionManager: SessionManager,
) {

    suspend fun currentEmployeeId(session: Session): Result<String> {
        session.employeeId?.let { return Result.success(it) }
        return docRepository.list(
            doctype = "Employee",
            fields = listOf("name"),
            filters = listOf(listOf("user_id", "=", session.username)),
            pageSize = 1,
        ).mapCatching { rows ->
            val id = rows.firstOrNull()?.get("name")?.asString
                ?: throw IllegalStateException("No Employee record is linked to this user.")
            sessionManager.saveEmployeeId(id)
            id
        }
    }

    suspend fun fetchEmployeeDetails(employeeId: String): Result<JsonObject> =
        docRepository.getDoc("Employee", employeeId)
}
