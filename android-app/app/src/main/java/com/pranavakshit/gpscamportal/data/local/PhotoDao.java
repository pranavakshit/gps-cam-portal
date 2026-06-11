package com.pranavakshit.gpscamportal.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface PhotoDao {
    @Insert
    void insertPhoto(PhotoEntity photo);

    @Query("SELECT * FROM photos ORDER BY timestamp DESC")
    LiveData<List<PhotoEntity>> getAllPhotos();

    @Query("SELECT * FROM photos WHERE isUploaded = 0 ORDER BY timestamp DESC")
    LiveData<List<PhotoEntity>> getPendingUploads();

    @Query("UPDATE photos SET isUploaded = 1 WHERE id IN (:photoIds)")
    void markAsUploaded(List<Integer> photoIds);

    @Query("DELETE FROM photos WHERE id = :id")
    void deletePhoto(int id);
}
