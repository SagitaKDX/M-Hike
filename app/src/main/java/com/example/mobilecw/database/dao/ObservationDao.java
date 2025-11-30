package com.example.mobilecw.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mobilecw.database.entities.Observation;

import java.util.List;

@Dao
public interface ObservationDao {
    // Insert operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertObservation(Observation observation);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllObservations(List<Observation> observations);
    
    // Query operations
    @Query("SELECT * FROM observations ORDER BY time DESC")
    List<Observation> getAllObservations();
    
    @Query("SELECT * FROM observations WHERE observationID = :observationId")
    Observation getObservationById(int observationId);
    
    @Query("SELECT * FROM observations WHERE hikeId = :hikeId ORDER BY time DESC")
    List<Observation> getObservationsByHikeId(int hikeId);
    
    // Update operations
    @Update
    void updateObservation(Observation observation);
    
    // Delete operations
    @Delete
    void deleteObservation(Observation observation);
    
    @Query("DELETE FROM observations WHERE observationID = :observationId")
    void deleteObservationById(int observationId);
    
    @Query("DELETE FROM observations WHERE hikeId = :hikeId")
    void deleteObservationsByHikeId(int hikeId);
    
    @Query("DELETE FROM observations")
    void deleteAllObservations();
    
    // Sync operations
    @Query("SELECT * FROM observations WHERE synced = 0 OR synced IS NULL")
    List<Observation> getUnsyncedObservations();
    
    @Query("UPDATE observations SET synced = 1 WHERE observationID = :observationId")
    void markObservationAsSynced(int observationId);
}

