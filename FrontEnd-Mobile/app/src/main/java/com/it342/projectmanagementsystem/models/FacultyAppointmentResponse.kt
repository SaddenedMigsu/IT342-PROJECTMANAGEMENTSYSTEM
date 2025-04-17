package com.it342.projectmanagementsystem.models

data class FacultyAppointmentResponse(
    val id: String,
    val title: String,
    val description: String,
    val startTime: TimestampObject,
    val endTime: TimestampObject,
    val status: String,
    val facultyId: String,
    val studentId: String
) 