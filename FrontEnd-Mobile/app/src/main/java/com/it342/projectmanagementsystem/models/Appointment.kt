package com.it342.projectmanagementsystem.models

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class Appointment(
    @SerializedName("id")
    var id: String? = null,

    @SerializedName("title")
    var title: String? = null,

    @SerializedName("description")
    var description: String? = null,

    @SerializedName("startTime")
    var startTime: TimestampObject? = null,

    @SerializedName("endTime")
    var endTime: TimestampObject? = null,

    @SerializedName("status")
    var status: String? = null,

    @SerializedName("facultyId")
    var facultyId: String? = null,

    @SerializedName("studentId")
    var studentId: String? = null,

    @SerializedName("createdBy")
    var createdBy: String? = null
) {
    // Java interop methods
    @JvmName("getIdValue")
    fun getId() = id

    @JvmName("getTitleValue")
    fun getTitle() = title

    @JvmName("getDescriptionValue")
    fun getDescription() = description

    @JvmName("getStartTimeValue")
    fun getStartTime() = startTime

    @JvmName("getEndTimeValue")
    fun getEndTime() = endTime

    @JvmName("getStatusValue")
    fun getStatus() = status

    @JvmName("getFacultyIdValue")
    fun getFacultyId() = facultyId

    @JvmName("getStudentIdValue")
    fun getStudentId() = studentId

    @JvmName("getCreatedByValue")
    fun getCreatedBy() = createdBy

    // Setter methods
    @JvmName("setTitleValue")
    fun setTitle(value: String) {
        title = value
    }

    @JvmName("setDescriptionValue")
    fun setDescription(value: String) {
        description = value
    }

    @JvmName("setStartTimeValue")
    fun setStartTime(value: TimestampObject) {
        startTime = value
    }

    @JvmName("setEndTimeValue")
    fun setEndTime(value: TimestampObject) {
        endTime = value
    }

    @JvmName("setStatusValue")
    fun setStatus(value: String) {
        status = value
    }

    fun getRemainingTime(): String {
        startTime?.let { timestamp ->
            val currentTime = System.currentTimeMillis()
            val appointmentTime = timestamp.toMillis()
            val timeDiff = appointmentTime - currentTime

            return when {
                timeDiff <= 0 -> "Appointment has passed"
                timeDiff < TimeUnit.HOURS.toMillis(1) -> {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(timeDiff)
                    "$minutes minutes remaining"
                }
                timeDiff < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(timeDiff)
                    "$hours hours remaining"
                }
                else -> {
                    val days = TimeUnit.MILLISECONDS.toDays(timeDiff)
                    "$days days remaining"
                }
            }
        } ?: run {
            return "Time not available"
        }
    }

    fun getFormattedDateTime(): String {
        startTime?.let { timestamp ->
            val date = Date(timestamp.toMillis())
            return SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(date)
        } ?: run {
            return "Date not available"
        }
    }

    companion object {
        private const val DATE_FORMAT = "MMM dd, yyyy HH:mm"
    }
} 