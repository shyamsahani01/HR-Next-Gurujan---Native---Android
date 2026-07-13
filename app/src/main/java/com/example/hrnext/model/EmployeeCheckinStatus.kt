package com.example.hrnext.model

data class EmployeeCheckinStatus(
    val isCheckedIn: Boolean,
    val lastLogType: String?,
    val lastTimeRaw: String?,
)
