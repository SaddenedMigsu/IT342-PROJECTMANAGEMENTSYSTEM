package com.it342.projectmanagementsystem.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Simple Tag class that will work with both Java and Kotlin
 */
@Parcelize
data class Tag(
    var name: String? = null,
    var color: String? = null
) : Parcelable {
    // No-arg constructor for Firebase
    constructor() : this(null, null)
    
    // Explicit getter and setter methods for Java compatibility with unique JVM names
    @JvmName("getNameForJava")
    fun getName(): String? {
        return name
    }
    
    @JvmName("setNameForJava")
    fun setName(name: String?) {
        this.name = name
    }
    
    @JvmName("getColorForJava")
    fun getColor(): String? {
        return color
    }
    
    @JvmName("setColorForJava")
    fun setColor(color: String?) {
        this.color = color
    }
} 