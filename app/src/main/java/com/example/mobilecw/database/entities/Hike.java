package com.example.mobilecw.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;

import java.util.Date;

@Entity(tableName = "hikes",
        indices = {@Index("hikeID")})
public class Hike {
    @PrimaryKey(autoGenerate = true)
    private int hikeID;
    
    private String name;
    private String location;
    private Date date;
    private boolean parkingAvailable; // true or false
    private double length; // Length of the hike
    private String difficulty; // Easy, Medium, Hard
    private String description; // Optional field
    private String purchaseParkingPass; // Optional field
    
    // Foreign key to user (nullable for non-registered users)
    private Integer userId; // null for non-registered users
    
    // Active hike tracking
    private Boolean isActive; // true if user is currently on this hike
    private Long startTime; // timestamp when hike was started
    private Long endTime; // timestamp when hike was ended
    
    // Timestamp for cloud sync
    private Long createdAt;
    private Long updatedAt;
    private Boolean synced; // true if synced to cloud
    
    // Constructors
    public Hike() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.synced = false;
        this.isActive = false;
    }
    
    public Hike(String name, String location, Date date, boolean parkingAvailable, 
                double length, String difficulty, String description, String purchaseParkingPass) {
        this.name = name;
        this.location = location;
        this.date = date;
        this.parkingAvailable = parkingAvailable;
        this.length = length;
        this.difficulty = difficulty;
        this.description = description;
        this.purchaseParkingPass = purchaseParkingPass;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.synced = false;
        this.isActive = false;
    }
    
    // Getters and Setters
    public int getHikeID() {
        return hikeID;
    }
    
    public void setHikeID(int hikeID) {
        this.hikeID = hikeID;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public Date getDate() {
        return date;
    }
    
    public void setDate(Date date) {
        this.date = date;
    }
    
    public boolean isParkingAvailable() {
        return parkingAvailable;
    }
    
    public void setParkingAvailable(boolean parkingAvailable) {
        this.parkingAvailable = parkingAvailable;
    }
    
    public double getLength() {
        return length;
    }
    
    public void setLength(double length) {
        this.length = length;
    }
    
    public String getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getPurchaseParkingPass() {
        return purchaseParkingPass;
    }
    
    public void setPurchaseParkingPass(String purchaseParkingPass) {
        this.purchaseParkingPass = purchaseParkingPass;
    }
    
    public Integer getUserId() {
        return userId;
    }
    
    public void setUserId(Integer userId) {
        this.userId = userId;
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
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }
    
    public Long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }
}

