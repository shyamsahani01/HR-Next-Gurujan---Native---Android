package com.example.hrnext.model

data class Session(
    val siteUrl: String,
    val username: String,
    val fullName: String,
    val userImage: String?,
    val employeeId: String? = null,
)
