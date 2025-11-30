package com.example.mobilecw.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;

import java.util.Date;

@Entity(tableName = "observations",
        foreignKeys = @ForeignKey(
                entity = Hike.class,
                parentColumns = "hikeID",
                childColumns = "hikeId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("hikeId"), @Index("observationID")})
public class Observation {
    @PrimaryKey(autoGenerate = true)
    private int observationID;
    
    private String observationText; // Required field
    private Date time; // Required field, defaults to current time
    private String comments; // Optional field
    private String location; // Optional field - GPS coordinates or location name
    private String picture; // Optional field - file path or URI to image
    private int hikeId; // Foreign key to Hike
    
    // Timestamp for cloud sync
    private Long createdAt;
    private Long updatedAt;
    private Boolean synced; // true if synced to cloud
    
    // Constructors
    public Observation() {
        this.time = new Date(); // Default to current time
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.synced = false;
    }
    
    public Observation(String observationText, Date time, String comments, int hikeId) {
        this.observationText = observationText;
        this.time = time != null ? time : new Date();
        this.comments = comments;
        this.hikeId = hikeId;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.synced = false;
    }
    
    // Getters and Setters
    public int getObservationID() {
        return observationID;
    }
    
    public void setObservationID(int observationID) {
        this.observationID = observationID;
    }
    
    public String getObservationText() {
        return observationText;
    }
    
    public void setObservationText(String observationText) {
        this.observationText = observationText;
    }
    
    public Date getTime() {
        return time;
    }
    
    public void setTime(Date time) {
        this.time = time;
    }
    
    public String getComments() {
        return comments;
    }
    
    public void setComments(String comments) {
        this.comments = comments;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getPicture() {
        return picture;
    }
    
    public void setPicture(String picture) {
        this.picture = picture;
    }
    
    public int getHikeId() {
        return hikeId;
    }
    
    public void setHikeId(int hikeId) {
        this.hikeId = hikeId;
    }
    
    public Long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    public Long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Boolean getSynced() {
        return synced;
    }
    
    public void setSynced(Boolean synced) {
        this.synced = synced;
    }
}

