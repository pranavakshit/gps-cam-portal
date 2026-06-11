package com.pranavakshit.gpscamportal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Insert
    suspend fun insertPhoto(photo: PhotoEntity)

    @Query("SELECT * FROM photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE isUploaded = 0 ORDER BY timestamp DESC")
    fun getPendingUploads(): Flow<List<PhotoEntity>>

    @Query("UPDATE photos SET isUploaded = 1 WHERE id IN (:photoIds)")
    suspend fun markAsUploaded(photoIds: List<Int>)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deletePhoto(id: Int)
}
