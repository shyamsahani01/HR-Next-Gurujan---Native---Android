package com.example.hrnext.model

data class PendingLeaveRequest(
    val name: String,
    val employeeName: String,
    val leaveType: String,
    val fromDate: String,
    val toDate: String,
    val totalDays: Double,
    val isMine: Boolean,
)
