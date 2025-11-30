package com.example.mobilecw.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mobilecw.database.entities.Hike;

import java.util.Date;
import java.util.List;

@Dao
public interface HikeDao {
    // Insert operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertHike(Hike hike);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllHikes(List<Hike> hikes);
    
    // Query operations
    @Query("SELECT * FROM hikes ORDER BY date DESC")
    List<Hike> getAllHikes();
    
    @Query("SELECT * FROM hikes WHERE hikeID = :hikeId")
    Hike getHikeById(int hikeId);
    
    @Query("SELECT * FROM hikes WHERE userId = :userId ORDER BY date DESC")
    List<Hike> getHikesByUserId(Integer userId);
    
    @Query("SELECT * FROM hikes WHERE userId IS NULL ORDER BY date DESC")
    List<Hike> getHikesForNonRegisteredUsers();
    
    // Search operations
    @Query("SELECT * FROM hikes WHERE name LIKE :searchQuery ORDER BY date DESC")
    List<Hike> searchHikesByName(String searchQuery);
    
    @Query("SELECT * FROM hikes WHERE name LIKE :nameQuery AND location LIKE :locationQuery AND length BETWEEN :minLength AND :maxLength AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    List<Hike> advancedSearch(String nameQuery, String locationQuery, double minLength, double maxLength, long startDate, long endDate);
    
    @Query("SELECT * FROM hikes WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    List<Hike> searchHikesByDateRange(long startDate, long endDate);
    
    // Update operations
    @Update
    void updateHike(Hike hike);
    
    // Delete operations
    @Delete
    void deleteHike(Hike hike);
    
    @Query("DELETE FROM hikes WHERE hikeID = :hikeId")
    void deleteHikeById(int hikeId);
    
    @Query("DELETE FROM hikes")
    void deleteAllHikes();
    
    @Query("DELETE FROM hikes WHERE hikeID IN (:hikeIds)")
    void deleteHikesByIds(List<Integer> hikeIds);
    
    // Sync operations
    @Query("SELECT * FROM hikes WHERE synced = 0 OR synced IS NULL")
    List<Hike> getUnsyncedHikes();
    
    @Query("UPDATE hikes SET synced = 1 WHERE hikeID = :hikeId")
    void markHikeAsSynced(int hikeId);
    
    // Migration: Migrate non-registered hikes to a registered user
    @Query("UPDATE hikes SET userId = :userId, updatedAt = :updatedAt WHERE userId IS NULL")
    void migrateHikesToUser(int userId, long updatedAt);
    
    // Active hike operations
    @Query("SELECT * FROM hikes WHERE isActive = 1 LIMIT 1")
    Hike getActiveHike();
    
    @Query("UPDATE hikes SET isActive = 0, synced = 0 WHERE isActive = 1")
    void deactivateAllHikes();
    
    @Query("UPDATE hikes SET isActive = 1, startTime = :startTime, updatedAt = :updatedAt, synced = 0 WHERE hikeID = :hikeId")
    void startHike(int hikeId, long startTime, long updatedAt);
    
    @Query("UPDATE hikes SET isActive = 0, endTime = :endTime, updatedAt = :updatedAt, synced = 0 WHERE hikeID = :hikeId")
    void endHike(int hikeId, long endTime, long updatedAt);
}

