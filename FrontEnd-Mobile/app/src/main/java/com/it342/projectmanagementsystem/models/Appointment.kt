package com.it342.projectmanagementsystem.models

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.gson.annotations.SerializedName
import com.it342.projectmanagementsystem.models.TimestampObject
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Parcelize
data class Appointment(
    @SerializedName("appointmentId")
    var id: String? = null,

    @SerializedName("title")
    var title: String? = null,

    @SerializedName("description")
    var description: String? = null,

    @SerializedName("startTime")
    var startTime: Timestamp? = null,

    @SerializedName("endTime")
    var endTime: Timestamp? = null,

    @SerializedName("status")
    var status: String? = null,

    @SerializedName("facultyId")
    var facultyId: String? = null,

    @SerializedName("studentId")
    var studentId: String? = null,

    @SerializedName("createdBy")
    var createdBy: String? = null,

    @SerializedName("creatorName")
    var creatorName: String? = null,

    @SerializedName("userRole")
    var userRole: String? = null,

    @SerializedName("userStatus")
    var userStatus: String? = null,

    @SerializedName("hasApproved")
    var hasApproved: Boolean? = null,

    @SerializedName("participants")
    var participants: List<String>? = null,

    @SerializedName("createdAt")
    var createdAt: Timestamp? = null,

    @SerializedName("updatedAt")
    var updatedAt: Timestamp? = null,

    @SerializedName("appointmentType")
    var appointmentType: String? = null,

    @SerializedName("timezone")
    var timezone: String? = null,

    @SerializedName("facultyApprovals")
    var facultyApprovals: Map<String, Boolean>? = null,

    @SerializedName("requiresApproval")
    var requiresApproval: Boolean? = null,
    
    @SerializedName("tags")
    var tags: Map<String, Tag>? = null
) : Parcelable {
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

    @JvmName("getCreatorNameValue")
    fun getCreatorName() = creatorName
    
    @JvmName("getTagsValue")
    fun getTags() = tags
    
    @JvmName("setTagsValue")
    fun setTags(value: Map<String, Tag>) {
        tags = value
    }

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
    fun setStartTime(value: Timestamp) {
        startTime = value
    }

    @JvmName("setEndTimeValue")
    fun setEndTime(value: Timestamp) {
        endTime = value
    }

    @JvmName("setStatusValue")
    fun setStatus(value: String) {
        status = value
    }
    
    @JvmName("setParticipantsValue")
    fun setParticipants(value: List<String>) {
        participants = value
    }
    
    // Tag-related utilities
    @JvmName("addTag")
    fun addTag(name: String, tag: Tag) {
        if (tags == null) {
            tags = mutableMapOf()
        }
        (tags as MutableMap<String, Tag>)[name] = tag
    }
    
    @JvmName("removeTag")
    fun removeTag(name: String) {
        if (tags != null) {
            (tags as MutableMap<String, Tag>).remove(name)
        }
    }
    
    @JvmName("getTag")
    fun getTag(name: String): Tag? {
        return tags?.get(name)
    }
    
    @JvmName("hasTag")
    fun hasTag(name: String): Boolean {
        return tags?.containsKey(name) ?: false
    }
    
    @JvmName("getTagList")
    fun getTagList(): List<Tag> {
        return tags?.values?.toList() ?: listOf()
    }

    fun getRemainingTime(): String {
        startTime?.let { timestamp ->
            val currentTime = System.currentTimeMillis()
            val appointmentTime = timestamp.toDate().time
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
            val date = timestamp.toDate()
            val formatter = SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("Asia/Manila")
            }
            return formatter.format(date)
        } ?: run {
            return "Date not available"
        }
    }

    companion object {
        private const val DATE_FORMAT = "MMM dd, yyyy HH:mm"
    }
} 