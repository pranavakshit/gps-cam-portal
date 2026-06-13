package com.pranavakshit.gpscamportal.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "photos")
public class PhotoEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String locationName;
    public double latitude;
    public double longitude;
    public long timestamp;
    public String imageUri;
    public String uploader;
    public boolean isUploaded;
    public boolean isDeleted;
}
