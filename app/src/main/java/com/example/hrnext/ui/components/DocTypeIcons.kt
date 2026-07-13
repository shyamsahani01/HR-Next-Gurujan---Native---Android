package com.example.hrnext.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/** Best-effort icon per doctype name; anything not in this map falls back to a generic document icon. */
fun iconForDoctype(doctype: String): ImageVector = when (doctype) {
    "Employee" -> Icons.Filled.Person
    "Employee Onboarding", "Employee Separation" -> Icons.Filled.Groups
    "Attendance" -> Icons.Filled.HowToReg
    "Employee Checkin" -> Icons.Filled.Fingerprint
    "Attendance Request" -> Icons.Filled.EventAvailable
    "Leave Application" -> Icons.Filled.BeachAccess
    "Leave Allocation", "Leave Type", "Leave Period", "Compensatory Leave Request" -> Icons.Filled.EventRepeat
    "Holiday List" -> Icons.Filled.CalendarMonth
    "Shift Assignment", "Shift Type", "Shift Request" -> Icons.Filled.Schedule
    "Salary Slip", "Salary Structure", "Salary Structure Assignment", "Payroll Entry" -> Icons.Filled.Payments
    "Expense Claim", "Expense Claim Type" -> Icons.AutoMirrored.Filled.ReceiptLong
    "Employee Advance" -> Icons.Filled.AttachMoney
    "Loan", "Loan Application", "Loan Type" -> Icons.Filled.AccountBalance
    "Job Opening" -> Icons.Filled.Work
    "Job Applicant" -> Icons.Filled.PersonSearch
    "Job Offer" -> Icons.Filled.Badge
    "Interview", "Interview Round" -> Icons.Filled.RecordVoiceOver
    "Appraisal", "Appraisal Cycle", "Appraisal Template" -> Icons.Filled.Assessment
    "Goal", "Employee Performance Feedback" -> Icons.Filled.Star
    "Training Program", "Training Event", "Training Result", "Training Feedback" -> Icons.Filled.School
    "Travel Request" -> Icons.Filled.Flight
    "Department" -> Icons.Filled.AccountTree
    "Designation" -> Icons.Filled.Badge
    "Company" -> Icons.Filled.Business
    else -> Icons.Filled.Description
}
