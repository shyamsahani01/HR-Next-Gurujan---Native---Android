package com.example.hrnext.model

data class TeamMilestone(
    val employeeId: String,
    val name: String,
    val image: String?,
    val designation: String?,
    val day: Int,
    val isToday: Boolean,
    val yearsOfService: Int? = null,
)
