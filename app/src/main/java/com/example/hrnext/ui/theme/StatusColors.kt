package com.example.hrnext.ui.theme

import androidx.compose.ui.graphics.Color

/** Fixed semantic color per Attendance status — unlike [accentColorFor], this must never rotate
 * (red always means Absent), so it's a dedicated mapping rather than the hash-based doctype picker. */
fun colorForAttendanceStatus(status: String): Color = when (status) {
    "Present" -> AccentGreen
    "Absent" -> ErrorRed
    "Half Day" -> AccentAmber
    "On Leave" -> AccentViolet
    "Work From Home" -> AccentSky
    else -> Color.Gray
}
