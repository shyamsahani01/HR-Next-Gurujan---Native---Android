package com.example.hrnext.model

data class LeaveBalance(
    val leaveType: String,
    val allocated: Double,
    val usedOverall: Double,
    val usedThisMonth: Double,
    val remaining: Double,
)
