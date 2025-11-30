package com.example.mobilecw.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mobilecw.database.entities.User;

import java.util.List;

@Dao
public interface UserDao {
    // Insert operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertUser(User user);
    
    // Query operations
    @Query("SELECT * FROM users")
    List<User> getAllUsers();
    
    @Query("SELECT * FROM users WHERE userId = :userId")
    User getUserById(int userId);
    
    @Query("SELECT * FROM users WHERE userEmail = :email")
    User getUserByEmail(String email);
    
    @Query("SELECT * FROM users WHERE userEmail = :email AND userPassword = :password")
    User authenticateUser(String email, String password);
    
    // Update operations
    @Update
    void updateUser(User user);
    
    // Delete operations
    @Delete
    void deleteUser(User user);
    
    @Query("DELETE FROM users WHERE userId = :userId")
    void deleteUserById(int userId);
    
    @Query("DELETE FROM users")
    void deleteAllUsers();
}

