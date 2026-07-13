package com.example.hrnext.model

data class EmployeeSummary(
    val id: String,
    val name: String,
    val designation: String?,
    val department: String?,
    val company: String?,
    val dateOfJoining: String?,
    val cellNumber: String?,
    val personalEmail: String?,
    val companyEmail: String?,
    val branch: String?,
    val status: String?,
    val image: String?,
    val reportsTo: String?,
    val gender: String?,
    val dateOfBirth: String?,
)
