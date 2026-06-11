package com.pranavakshit.gpscamportal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val imageUri: String, // Local URI path
    val uploader: String,
    val isUploaded: Boolean = false
)
