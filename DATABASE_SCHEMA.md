# M-Hike Database Schema Documentation

## Overview
This document describes the SQLite database schema for the M-Hike application using Room Persistence Library.

## Database Configuration
- **Database Name**: `mhike_database`
- **Version**: 1
- **Type Converters**: Date to Long (timestamp) conversion

## Tables

### 1. Hikes Table
**Table Name**: `hikes`

| Column Name | Type | Constraints | Description |
|------------|------|-------------|-------------|
| hikeID | INTEGER | PRIMARY KEY, AUTO_INCREMENT | Unique identifier for each hike |
| name | TEXT | NOT NULL | Name of the hike (required) |
| location | TEXT | NOT NULL | Location of the hike (required) |
| date | INTEGER | NOT NULL | Date of the hike (stored as timestamp) |
| parkingAvailable | TEXT | NOT NULL | "Yes" or "No" (required) |
| length | REAL | NOT NULL | Length of the hike in km/miles |
| difficulty | TEXT | NOT NULL | Difficulty level: Easy, Medium, Hard |
| description | TEXT | NULL | Optional description |
| purchaseParkingPass | TEXT | NULL | Optional parking pass information |
| userId | INTEGER | NULL | Foreign key to users table (null for non-registered users) |
| createdAt | INTEGER | NULL | Creation timestamp |
| updatedAt | INTEGER | NULL | Last update timestamp |
| synced | INTEGER | NULL | Sync status for cloud (0 = not synced, 1 = synced) |

**Indexes**: 
- Primary key on `hikeID`
- Index on `hikeID` for faster lookups

### 2. Observations Table
**Table Name**: `observations`

| Column Name | Type | Constraints | Description |
|------------|------|-------------|-------------|
| observationID | INTEGER | PRIMARY KEY, AUTO_INCREMENT | Unique identifier for each observation |
| observationText | TEXT | NOT NULL | Text of the observation (required) |
| time | INTEGER | NOT NULL | Time of observation (stored as timestamp, defaults to current time) |
| comments | TEXT | NULL | Optional additional comments |
| hikeId | INTEGER | NOT NULL | Foreign key to hikes table (CASCADE DELETE) |
| createdAt | INTEGER | NULL | Creation timestamp |
| updatedAt | INTEGER | NULL | Last update timestamp |
| synced | INTEGER | NULL | Sync status for cloud (0 = not synced, 1 = synced) |

**Foreign Keys**:
- `hikeId` references `hikes(hikeID)` with CASCADE DELETE

**Indexes**: 
- Primary key on `observationID`
- Index on `hikeId` for faster queries
- Index on `observationID` for faster lookups

### 3. Users Table
**Table Name**: `users`

| Column Name | Type | Constraints | Description |
|------------|------|-------------|-------------|
| userId | INTEGER | PRIMARY KEY, AUTO_INCREMENT | Unique identifier for each user |
| userEmail | TEXT | NOT NULL | User's email address (unique) |
| userPassword | TEXT | NOT NULL | User's password (should be hashed) |
| createdAt | INTEGER | NULL | Creation timestamp |
| updatedAt | INTEGER | NULL | Last update timestamp |

**Indexes**: 
- Primary key on `userId`
- Index on `userEmail` for faster authentication lookups

## Data Access Objects (DAOs)

### HikeDao
Provides CRUD operations for hikes:
- `insertHike()` - Insert a single hike
- `getAllHikes()` - Get all hikes ordered by date
- `getHikeById()` - Get hike by ID
- `getHikesByUserId()` - Get hikes for registered user
- `getHikesForNonRegisteredUsers()` - Get hikes for non-registered users
- `searchHikesByName()` - Search hikes by name (partial match)
- `advancedSearch()` - Advanced search with multiple criteria
- `searchHikesByDateRange()` - Search hikes by date range
- `updateHike()` - Update hike details
- `deleteHike()` - Delete a hike
- `deleteAllHikes()` - Delete all hikes (reset database)
- `getUnsyncedHikes()` - Get hikes not yet synced to cloud
- `markHikeAsSynced()` - Mark hike as synced

### ObservationDao
Provides CRUD operations for observations:
- `insertObservation()` - Insert a single observation
- `getAllObservations()` - Get all observations
- `getObservationById()` - Get observation by ID
- `getObservationsByHikeId()` - Get all observations for a hike
- `updateObservation()` - Update observation details
- `deleteObservation()` - Delete an observation
- `deleteObservationsByHikeId()` - Delete all observations for a hike
- `deleteAllObservations()` - Delete all observations
- `getUnsyncedObservations()` - Get observations not yet synced to cloud
- `markObservationAsSynced()` - Mark observation as synced

### UserDao
Provides CRUD operations for users:
- `insertUser()` - Insert a new user (registration)
- `getAllUsers()` - Get all users
- `getUserById()` - Get user by ID
- `getUserByEmail()` - Get user by email
- `authenticateUser()` - Authenticate user with email and password
- `updateUser()` - Update user details
- `deleteUser()` - Delete a user
- `deleteAllUsers()` - Delete all users

## User Types Support

### Registered Users
- Have a `userId` in the users table
- Hikes are linked via `userId` foreign key
- Data is synced to cloud (`synced = true`)
- Can access their data across devices

### Non-Registered Users
- No entry in users table
- Hikes have `userId = null`
- Data stored only locally (`synced = false`)
- Data is device-specific

## Usage Example

```java
// Get database instance
AppDatabase db = AppDatabase.getDatabase(context);

// Access DAOs
HikeDao hikeDao = db.hikeDao();
ObservationDao observationDao = db.observationDao();
UserDao userDao = db.userDao();

// Insert a hike
Hike hike = new Hike("Snowdon", "Wales", new Date(), "Yes", 14.5, "Hard", "Beautiful mountain hike", "Required");
long hikeId = hikeDao.insertHike(hike);

// Insert an observation
Observation obs = new Observation("Saw a red fox", new Date(), "Near the summit", (int)hikeId);
observationDao.insertObservation(obs);

// Search hikes by name
List<Hike> results = hikeDao.searchHikesByName("%Snowdon%");
```

## Notes
- All Date objects are converted to Long (timestamp) for storage using TypeConverters
- Foreign key constraints ensure data integrity
- CASCADE DELETE on observations ensures observations are deleted when a hike is deleted
- The database uses singleton pattern for efficient memory usage
- Sync fields (`synced`) are used to track cloud synchronization status

