package com.example.mobilecw.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.mobilecw.database.dao.HikeDao;
import com.example.mobilecw.database.dao.ObservationDao;
import com.example.mobilecw.database.dao.UserDao;
import com.example.mobilecw.database.entities.Hike;
import com.example.mobilecw.database.entities.Observation;
import com.example.mobilecw.database.entities.User;

/**
 * Main Room Database class
 * Defines the database configuration and provides access to DAOs
 */
@Database(
        entities = {Hike.class, Observation.class, User.class},
        version = 6,
        exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    
    // DAO access methods
    public abstract HikeDao hikeDao();
    public abstract ObservationDao observationDao();
    public abstract UserDao userDao();
    
    // Singleton instance
    private static volatile AppDatabase INSTANCE;
    private static final String DATABASE_NAME = "mhike_database";
    
    /**
     * Get database instance (Singleton pattern)
     * @param context Application context
     * @return AppDatabase instance
     */
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration() // For development - allows schema changes
                    .build();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * Close database connection
     */
    public static void closeDatabase() {
        if (INSTANCE != null && INSTANCE.isOpen()) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}

