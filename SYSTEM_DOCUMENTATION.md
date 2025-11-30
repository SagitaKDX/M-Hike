# M-Hike: Complete System Documentation

## Table of Contents
1. [System Overview](#system-overview)
2. [Architecture](#architecture)
3. [Database Design](#database-design)
4. [Features & Functions](#features--functions)
5. [Authentication & Session Management](#authentication--session-management)
6. [Data Synchronization](#data-synchronization)
7. [Vector Embeddings & Semantic Search](#vector-embeddings--semantic-search)
8. [Backend API](#backend-api)
9. [Data Flow Diagrams](#data-flow-diagrams)
10. [File Structure](#file-structure)

---

## System Overview

**M-Hike** is a comprehensive hiking management Android application that allows users to:
- Track hiking activities and observations
- Store data locally with offline-first approach
- Sync data to Firebase Cloud for registered users
- Perform semantic search using AI-powered vector embeddings
- Manage user accounts with Firebase Authentication

### Technology Stack

**Android App:**
- Language: Java 11
- UI Framework: Android Views with Material Design Components
- Local Database: Room Persistence Library (SQLite)
- Cloud Database: Firebase Cloud Firestore
- Authentication: Firebase Authentication
- HTTP Client: Volley (for backend API calls)
- Vector Embeddings: Gemini 2.5 Flash API

**Backend:**
- Framework: FastAPI (Python)
- Vector Search: Cosine similarity on Gemini embeddings
- Cloud Database: Firebase Admin SDK (Firestore)
- Embedding Model: Gemini 2.5 Flash

---

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Android Application                       │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │
│  │   Activities │    │   Services  │    │   Sync       │  │
│  │   (UI Layer) │───▶│   (Business)│───▶│   Managers   │  │
│  └──────────────┘    └──────────────┘    └──────────────┘  │
│         │                    │                    │          │
│         │                    │                    │          │
│         ▼                    ▼                    ▼          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Room Database (Local)                    │   │
│  │  ┌────────┐  ┌────────┐  ┌──────────────┐          │   │
│  │  │ Users  │  │ Hikes  │  │ Observations │          │   │
│  │  └────────┘  └────────┘  └──────────────┘          │   │
│  └──────────────────────────────────────────────────────┘   │
│                          │                                    │
│                          │ Sync (when online)                │
│                          ▼                                    │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         Firebase Cloud (Firestore + Auth)           │   │
│  │  ┌────────┐  ┌────────┐  ┌──────────────┐          │   │
│  │  │ Users  │  │ Hikes  │  │ Observations │          │   │
│  │  │        │  │        │  │ + Embeddings │          │   │
│  │  └────────┘  └────────┘  └──────────────┘          │   │
│  └──────────────────────────────────────────────────────┘   │
│                          │                                    │
│                          │ Semantic Search                   │
│                          ▼                                    │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         Python Backend (FastAPI)                     │   │
│  │  ┌──────────────────────────────────────────────┐   │   │
│  │  │  Semantic Search API (Vector Similarity)     │   │   │
│  │  └──────────────────────────────────────────────┘   │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Design Patterns

1. **Singleton Pattern**: `AppDatabase`, `FirebaseSyncManager`, `SessionManager`
2. **Repository Pattern**: DAOs (Data Access Objects) for database operations
3. **Observer Pattern**: RecyclerView adapters, callbacks for async operations
4. **Offline-First**: Local Room database as primary storage, Firebase as cloud backup

---

## Database Design

### Local Database (Room/SQLite)

**Database Name:** `mhike_database`  
**Version:** 6  
**Type Converters:** `Converters.java` (for Date types)

#### Entity 1: User

```java
@Entity(tableName = "users", indices = {@Index("userEmail")})
public class User {
    @PrimaryKey(autoGenerate = true)
    private int userId;                    // Local primary key
    
    private String firebaseUid;            // Firebase Auth UID (for cloud sync)
    private String userName;               // Full name
    private String userEmail;              // Required, unique, indexed
    private String userPassword;           // Required (should be hashed in production)
    private String userPhone;              // Optional phone number
    private Long createdAt;                // Timestamp
    private Long updatedAt;                // Timestamp
}
```

**Relationships:**
- One-to-Many with `Hike` (via `userId`)
- `userId` can be `null` for non-registered users

#### Entity 2: Hike

```java
@Entity(tableName = "hikes", indices = {@Index("hikeID")})
public class Hike {
    @PrimaryKey(autoGenerate = true)
    private int hikeID;                   // Local primary key
    
    private String name;                   // Hike name
    private String location;               // Location
    private Date date;                     // Hike date
    private boolean parkingAvailable;      // Parking availability
    private double length;                 // Length in km
    private String difficulty;             // Easy, Medium, Hard
    private String description;            // Optional description
    private String purchaseParkingPass;    // Optional parking pass info
    
    private Integer userId;                 // Foreign key to User (nullable)
    
    // Active hike tracking
    private Boolean isActive;              // Currently active hike
    private Long startTime;                // Start timestamp
    private Long endTime;                  // End timestamp
    
    // Sync tracking
    private Long createdAt;                // Creation timestamp
    private Long updatedAt;                // Update timestamp
    private Boolean synced;                // Sync status to Firebase
}
```

**Relationships:**
- Many-to-One with `User` (via `userId`)
- One-to-Many with `Observation` (via `hikeId`)

#### Entity 3: Observation

```java
@Entity(tableName = "observations",
        foreignKeys = @ForeignKey(
            entity = Hike.class,
            parentColumns = "hikeID",
            childColumns = "hikeId",
            onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("hikeId")})
public class Observation {
    @PrimaryKey(autoGenerate = true)
    private int observationID;            // Local primary key
    
    private int hikeId;                   // Foreign key to Hike
    private String observationText;       // Main observation text
    private String comments;              // Optional comments
    private String location;              // Optional location
    private Date time;                    // Observation time
    
    // Sync tracking
    private Long createdAt;                // Creation timestamp
    private Long updatedAt;                // Update timestamp
    private Boolean synced;                // Sync status to Firebase
}
```

**Relationships:**
- Many-to-One with `Hike` (via `hikeId`, CASCADE delete)

### Cloud Database (Firestore)

**Structure:** Per-user subcollections

```
users (collection)
  └─ {firebaseUid} (document)
       ├─ Profile fields:
       │   ├─ userName
       │   ├─ userEmail
       │   ├─ userPhone
       │   ├─ createdAt
       │   └─ updatedAt
       │
       ├─ hikes (subcollection)
       │    └─ {hikeId} (document)
       │         ├─ name, location, date, parkingAvailable
       │         ├─ length, difficulty, description
       │         ├─ purchaseParkingPass
       │         ├─ isActive, startTime, endTime
       │         ├─ createdAt, updatedAt
       │         ├─ embedding_vector (array of floats)  ← Vector embedding
       │         ├─ embedding_updatedAt
       │         └─ embedding_source ("gemini-2.5-flash")
       │
       └─ hikes/{hikeId}/observations (subcollection)
            └─ {observationId} (document)
                 ├─ observationText, comments, location
                 ├─ time, createdAt, updatedAt
                 ├─ embedding_vector (array of floats)  ← Vector embedding
                 ├─ embedding_updatedAt
                 └─ embedding_source ("gemini-2.5-flash")
```

**Key Points:**
- Document IDs use Firebase Auth UID (not local userId)
- Embeddings stored directly in hike/observation documents
- Hierarchical structure: `users/{uid}/hikes/{hikeId}/observations/{obsId}`

---

## Features & Functions

### 1. User Management

#### Signup (`SignupActivity`)
**Flow:**
1. User enters email, password, name, phone
2. Validate form inputs
3. Check if email exists in local Room database
4. Call `FirebaseAuth.createUserWithEmailAndPassword()`
5. On success:
   - Get `firebaseUid` from Firebase Auth
   - Insert new `User` into Room with `firebaseUid`
   - Store `userId` and `firebaseUid` in `SessionManager`
   - Call `FirebaseSyncManager.syncNow()` to sync profile
   - Navigate to `UsersActivity`

#### Login (`LoginActivity`)
**Flow:**
1. User enters email and password
2. Validate inputs
3. Call `FirebaseAuth.signInWithEmailAndPassword()`
4. On success:
   - Get `firebaseUid` from Firebase Auth
   - Find or create `User` in Room database
   - Update `firebaseUid` if user exists
   - Store `userId` and `firebaseUid` in `SessionManager`
   - Call `FirebaseSyncManager.syncNow()` to sync local data
   - Navigate to `UsersActivity`

#### Logout (`SettingsActivity`)
**Flow:**
1. User clicks logout button
2. Disable button, show "Syncing..." message
3. Call `FirebaseSyncManager.syncNow(callback)`
4. On success:
   - Clear all Room tables (`clearAllTables()`)
   - Clear `SessionManager` (remove `userId` and `firebaseUid`)
   - Show success message
   - Navigate to `UsersActivity`
5. On failure:
   - Re-enable button
   - Show error message
   - **Keep local data** (data preservation)

**Single-User Local Cache:**
- Only one user's data stored locally at a time
- Logout = sync to cloud + clear local database
- Next login starts with clean local database

### 2. Hike Management

#### Create/Edit Hike (`EnterHikeActivity`)
**Fields:**
- Name (required)
- Location (required)
- Date (DatePicker)
- Parking Available (checkbox)
- Length (km, number input)
- Difficulty (dropdown: Easy, Medium, Hard)
- Description (optional, multiline)
- Purchase Parking Pass (optional)

**Flow:**
1. User fills form
2. Validate required fields
3. Save to Room database:
   - If new: `hikeDao.insert(hike)`
   - If edit: `hikeDao.update(hike)`
4. Set `synced = false` (mark as unsynced)
5. If user logged in and online: `FirebaseSyncManager.syncNow()`
6. Navigate back to `HikingListActivity`

#### View Hike List (`HikingListActivity`)
**Features:**
- Display all hikes for current user
- Filter by name, location, date range, difficulty, parking
- Sort by date (recent first)
- Start/End hike functionality
- Delete hike
- Navigate to hike details

**Data Loading:**
- Load hikes from Room: `hikeDao.getHikesByUserId(userId)`
- If no user logged in: show sample hikes (only on first launch)
- On resume: trigger sync if user logged in and online

#### Hike Details (`HikeDetailActivity`)
**Features:**
- Display full hike information
- Start hike button (sets `isActive = true`, `startTime = now`)
- End hike button (sets `isActive = false`, `endTime = now`)
- View observations list
- Edit hike
- Delete hike

**Active Hike Tracking:**
- Only one hike can be active at a time
- Starting a new hike deactivates previous active hike
- Sync triggered after start/end operations

### 3. Observation Management

#### Create/Edit Observation (`ObservationFormActivity`)
**Fields:**
- Observation Text (required, multiline)
- Comments (optional)
- Location (optional)
- Time (DatePicker, defaults to now)

**Flow:**
1. User fills form (hikeId passed from parent activity)
2. Validate required fields
3. Save to Room database:
   - If new: `observationDao.insert(observation)`
   - If edit: `observationDao.update(observation)`
4. Set `synced = false`
5. If user logged in and online: `FirebaseSyncManager.syncNow()`
6. Navigate back to observation list

#### View Observations (`ObservationListActivity`)
**Features:**
- Display all observations for a specific hike
- Add new observation button
- Edit/Delete observations
- Navigate to observation form

### 4. Search Functionality

#### Search Activity (`SearchActivity`)
**Features:**
- **Regular Search:**
  - Name search (fuzzy matching)
  - Location filter
  - Date range filter
  - Length range filter
  - Difficulty filter
  - Parking availability filter
  
- **Semantic Search (AI-powered):**
  - Toggle switch to enable/disable
  - Requires user to be logged in
  - Sends query to Python backend
  - Backend performs vector similarity search
  - Returns semantically similar hikes
  - Falls back to regular search on error

**Search Flow:**
1. User enters query and sets filters
2. If semantic search enabled:
   - Get `firebaseUid` from `SessionManager`
   - Call `SemanticSearchService.search()`
   - Send POST request to backend API
   - Parse results and match with local hikes
   - Apply additional filters
3. If semantic search disabled or fails:
   - Use local fuzzy search
   - Apply all filters
4. Display results in RecyclerView

### 5. Home Dashboard (`HomeActivity`)

**Features:**
- **Weather Display:**
  - Fetches current weather from OpenWeatherMap API
  - Shows location, temperature, description
  - Requires API key in code

- **Activity Statistics:**
  - Hikes Completed: Count of all hikes for current user
  - Total km: Sum of all hike lengths

- **Nearby Trails:**
  - Shows up to 10 hikes from database
  - Displays name, distance, difficulty, estimated time
  - Click to view hike details

- **Bottom Navigation:**
  - Home, My Hiking, Users, Settings tabs

### 6. Settings (`SettingsActivity`)

**Features:**
- **Account Card:**
  - Display user name and email
  - Shows user initials in circular avatar

- **Preferences Section:**
  - Notifications (coming soon)
  - Units (coming soon)
  - Language (coming soon)

- **Appearance Section:**
  - Theme toggle (Light/Dark, currently shows "coming soon")

- **Privacy & Security:**
  - Privacy settings (coming soon)
  - Data management (coming soon)

- **Support:**
  - Help center (coming soon)
  - About (app info)

- **Logout Button:**
  - Syncs data to cloud
  - Clears local database
  - Logs out user

---

## Authentication & Session Management

### SessionManager

**Purpose:** Manages active user session locally

**Storage:** SharedPreferences

**Keys:**
- `KEY_USER_ID`: Local Room `userId` (int)
- `KEY_FIREBASE_UID`: Firebase Auth UID (String)

**Methods:**
```java
// Get current user
int userId = SessionManager.getCurrentUserId(context);
String firebaseUid = SessionManager.getCurrentFirebaseUid(context);

// Set current user (on login/signup)
SessionManager.setCurrentUserId(context, userId);
SessionManager.setCurrentFirebaseUid(context, firebaseUid);

// Clear session (on logout)
SessionManager.clearCurrentUser(context);
```

**Session Lifecycle:**
1. **Signup/Login:** Set both `userId` and `firebaseUid`
2. **During App Usage:** Both values available for sync operations
3. **Logout:** Both values cleared

### Firebase Authentication

**Method:** Email/Password authentication

**Integration Points:**
- `SignupActivity`: `FirebaseAuth.createUserWithEmailAndPassword()`
- `LoginActivity`: `FirebaseAuth.signInWithEmailAndPassword()`
- `SettingsActivity`: Implicit logout (clear session, no Firebase sign-out needed)

**Firebase UID:**
- Unique identifier for each user
- Used as document ID in Firestore: `users/{firebaseUid}`
- Stored in Room `User` entity and `SessionManager`

---

## Data Synchronization

### FirebaseSyncManager

**Purpose:** Synchronizes local Room data to Firebase Cloud Firestore

**Pattern:** Offline-first with selective sync

**Sync Strategy:**
- **Only syncs unsynced items** (where `synced = false`)
- After successful sync, marks items as `synced = true`
- Prevents duplicate uploads and saves bandwidth

### Sync Flow

```
┌─────────────────────────────────────────────────────────┐
│  User Action (Create/Update Hike/Observation)          │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│  Save to Room Database                                 │
│  - Set synced = false                                  │
│  - Insert/Update record                                │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│  Check: User logged in? Online?                         │
└──────────────────────┬──────────────────────────────────┘
                       │
            ┌──────────┴──────────┐
            │                     │
         Yes │                     │ No
            │                     │
            ▼                     ▼
┌──────────────────┐    ┌──────────────────┐
│  Trigger Sync    │    │  Wait for next   │
│  Immediately     │    │  sync trigger    │
└────────┬─────────┘    └──────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────┐
│  FirebaseSyncManager.syncNow()                          │
│                                                          │
│  1. Get userId and firebaseUid from SessionManager     │
│  2. Get unsynced hikes: hikeDao.getUnsyncedHikes()     │
│  3. Get unsynced observations:                         │
│     observationDao.getUnsyncedObservations()            │
│  4. Filter by userId (only user's data)                │
│  5. Create Firebase Tasks for each item                │
│  6. Write to Firestore:                                │
│     - users/{firebaseUid} (profile)                    │
│     - users/{firebaseUid}/hikes/{hikeId}               │
│     - users/{firebaseUid}/hikes/{hikeId}/              │
│       observations/{observationId}                     │
│  7. On success: mark as synced in Room                 │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│  VectorSyncManager.syncUserVectors()                    │
│  (Generates and stores embeddings)                      │
└─────────────────────────────────────────────────────────┘
```

### Sync Triggers

**Automatic Sync:**
1. After login/signup
2. After creating/updating hike
3. After creating/updating observation
4. After starting/ending hike
5. On `HikingListActivity.onResume()` (if user logged in and online)

**Manual Sync:**
- Can be triggered programmatically: `FirebaseSyncManager.syncNow()`

### Sync Callbacks

**SyncCallback Interface:**
```java
public interface SyncCallback {
    void onSuccess();
    void onFailure(Exception exception);
}
```

**Usage:**
- Logout flow uses callback to ensure sync completes before clearing data
- Other operations use fire-and-forget sync

---

## Vector Embeddings & Semantic Search

### Overview

The system uses **Gemini 2.5 Flash** to generate vector embeddings for:
- Hike descriptions (name, location, difficulty, length, description)
- Observation notes (observation text, comments, location)

These embeddings enable **semantic search** - finding hikes/observations based on meaning rather than exact keywords.

### Embedding Generation Flow

```
┌─────────────────────────────────────────────────────────┐
│  VectorSyncManager.performVectorSync()                  │
│                                                          │
│  1. Get all hikes for user:                             │
│     hikeDao.getHikesByUserId(userId)                   │
│                                                          │
│  2. For each hike:                                      │
│     a. Build text chunk:                                │
│        "Hike: {name}\nLocation: {location}\n..."       │
│     b. Call GeminiEmbeddingService.fetchEmbedding()     │
│     c. Store embedding in Firestore:                   │
│        users/{uid}/hikes/{hikeId}                       │
│        (field: embedding_vector)                       │
│                                                          │
│  3. For each observation:                              │
│     a. Build text chunk:                                │
│        "Observation: {text}\nComments: {comments}..."   │
│     b. Call GeminiEmbeddingService.fetchEmbedding()     │
│     c. Store embedding in Firestore:                   │
│        users/{uid}/hikes/{hikeId}/observations/{obsId}  │
│        (field: embedding_vector)                       │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│  GeminiEmbeddingService.fetchEmbedding()                │
│                                                          │
│  1. Build prompt:                                       │
│     "You are Gemini 2.5 Flash acting strictly as      │
│      an embedder...\nUser UID: {uid}\n..."             │
│                                                          │
│  2. HTTP POST to Gemini API:                           │
│     models/gemini-2.5-flash:embedContent               │
│                                                          │
│  3. Parse response: float[] embedding                  │
│                                                          │
│  4. Return embedding array                             │
└─────────────────────────────────────────────────────────┘
```

### Embedding Storage

**Location:** Directly in Firestore documents

**Hike Document:**
```json
{
  "name": "Mountain Peak Trail",
  "location": "Rocky Mountain",
  "embedding_vector": [0.0123, -0.4421, 0.7892, ...],
  "embedding_updatedAt": 1732812345123,
  "embedding_source": "gemini-2.5-flash"
}
```

**Observation Document:**
```json
{
  "observationText": "Saw a beautiful eagle",
  "embedding_vector": [0.1234, -0.5678, 0.9012, ...],
  "embedding_updatedAt": 1732812345123,
  "embedding_source": "gemini-2.5-flash"
}
```

### Semantic Search Flow

```
┌─────────────────────────────────────────────────────────┐
│  User enters search query in SearchActivity            │
│  "mountain trail with parking"                         │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│  SemanticSearchService.search()                        │
│  - Sends POST to backend: /search                      │
│  - Payload: {query, firebase_uid, search_type, top_k} │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│  Python Backend (FastAPI)                              │
│                                                          │
│  1. Generate query embedding:                          │
│     get_embedding(query) using Gemini API              │
│                                                          │
│  2. Fetch all hikes with embeddings from Firestore:    │
│     users/{firebase_uid}/hikes                         │
│                                                          │
│  3. Calculate cosine similarity for each hike:        │
│     cosine_similarity(query_embedding, hike_embedding)  │
│                                                          │
│  4. Sort by similarity score                           │
│                                                          │
│  5. Return top-K results with scores                   │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│  Android App receives results                          │
│                                                          │
│  1. Parse JSON response                                │
│  2. Match hike IDs with local Room database            │
│  3. Apply additional filters (location, date, etc.)    │
│  4. Display results in RecyclerView                    │
└─────────────────────────────────────────────────────────┘
```

### Cosine Similarity

**Formula:**
```
similarity = (A · B) / (||A|| × ||B||)
```

Where:
- A = query embedding vector
- B = hike/observation embedding vector
- · = dot product
- || || = vector magnitude (L2 norm)

**Range:** -1 to 1 (higher = more similar)

**Implementation:** Python backend uses NumPy for efficient computation

---

## Backend API

### Overview

**Framework:** FastAPI (Python)  
**Host:** `http://206.189.93.77:8000`  
**Purpose:** Semantic search using vector embeddings

### Endpoints

#### 1. Health Check

**GET** `/health`

**Response:**
```json
{
  "status": "healthy",
  "gemini_configured": true
}
```

#### 2. Semantic Search

**POST** `/search`

**Request Body:**
```json
{
  "query": "mountain trail with parking",
  "firebase_uid": "abc123xyz...",
  "search_type": "hikes",
  "top_k": 10
}
```

**Response:**
```json
{
  "success": true,
  "status_code": 200,
  "message": "Search completed successfully",
  "data": {
    "results": [
      {
        "id": "1",
        "type": "hike",
        "hike_id": 1,
        "name": "Mountain Peak Trail",
        "location": "Rocky Mountain",
        "description": "A challenging trail...",
        "similarity": 0.85
      }
    ],
    "total_found": 1
  }
}
```

### Backend Architecture

```
┌─────────────────────────────────────────────────────────┐
│  FastAPI Application                                    │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │  /search Endpoint                                │  │
│  │                                                   │  │
│  │  1. Receive query + firebase_uid                 │  │
│  │  2. Generate query embedding (Gemini API)        │  │
│  │  3. Fetch hikes from Firestore                   │  │
│  │  4. Calculate cosine similarity                  │  │
│  │  5. Sort and return top-K results               │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Firebase Admin SDK                              │  │
│  │  - Reads from Firestore                          │  │
│  │  - Service account authentication                │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Gemini API Client                                │  │
│  │  - Generates embeddings                          │  │
│  │  - Model: gemini-2.5-flash                       │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### Configuration

**Environment Variables:**
- `GEMINI_API_KEY`: Gemini API key for embeddings
- `FIREBASE_SERVICE_ACCOUNT_PATH`: Path to Firebase service account JSON
- `PORT`: Server port (default: 8000)
- `HOST`: Server host (default: 0.0.0.0)

---

## Data Flow Diagrams

### Complete User Journey: Create Hike

```
User Action
    │
    ▼
EnterHikeActivity
    │
    ├─► Validate Form
    │
    ├─► Create Hike Object
    │   └─► Set synced = false
    │
    ├─► Save to Room Database
    │   └─► hikeDao.insert(hike)
    │
    ├─► Check: User logged in? Online?
    │   │
    │   ├─► Yes ──► FirebaseSyncManager.syncNow()
    │   │           │
    │   │           ├─► Get unsynced hikes
    │   │           │
    │   │           ├─► Write to Firestore
    │   │           │   users/{uid}/hikes/{hikeId}
    │   │           │
    │   │           ├─► Mark as synced in Room
    │   │           │
    │   │           └─► VectorSyncManager.syncUserVectors()
    │   │               │
    │   │               ├─► Generate embedding (Gemini API)
    │   │               │
    │   │               └─► Store embedding in Firestore
    │   │                   users/{uid}/hikes/{hikeId}
    │   │                   (field: embedding_vector)
    │   │
    │   └─► No ──► Wait for next sync trigger
    │
    └─► Navigate back to HikingListActivity
```

### Semantic Search Flow

```
User enters query
    │
    ▼
SearchActivity
    │
    ├─► Semantic Search Enabled?
    │   │
    │   ├─► Yes ──► SemanticSearchService.search()
    │   │           │
    │   │           ├─► POST to backend: /search
    │   │           │   {
    │   │           │     query: "mountain trail",
    │   │           │     firebase_uid: "abc123...",
    │   │           │     search_type: "hikes",
    │   │           │     top_k: 20
    │   │           │   }
    │   │           │
    │   │           ├─► Backend processes:
    │   │           │   - Generate query embedding
    │   │           │   - Fetch hikes from Firestore
    │   │           │   - Calculate cosine similarity
    │   │           │   - Return top-K results
    │   │           │
    │   │           ├─► Parse results
    │   │           │
    │   │           ├─► Match with local Room database
    │   │           │   (by hike_id)
    │   │           │
    │   │           └─► Apply additional filters
    │   │               └─► Display results
    │   │
    │   └─► No ──► Local fuzzy search
    │               └─► Apply filters
    │                   └─► Display results
```

---

## File Structure

### Android App

```
app/src/main/
├── java/com/example/mobilecw/
│   ├── MainActivity.java                    # Entry point, launches HomeActivity
│   │
│   ├── activities/                          # UI Activities
│   │   ├── HomeActivity.java                # Dashboard with weather & stats
│   │   ├── HikingListActivity.java          # List of hikes
│   │   ├── EnterHikeActivity.java           # Create/Edit hike form
│   │   ├── HikeDetailActivity.java          # Hike details & start/end
│   │   ├── ObservationListActivity.java    # List of observations
│   │   ├── ObservationFormActivity.java    # Create/Edit observation
│   │   ├── SearchActivity.java              # Search with semantic option
│   │   ├── UsersActivity.java               # User profile & login entry
│   │   ├── LoginActivity.java               # Login form
│   │   ├── SignupActivity.java              # Signup form
│   │   └── SettingsActivity.java            # Settings & logout
│   │
│   ├── adapters/                            # RecyclerView Adapters
│   │   ├── HikeListAdapter.java             # Hike list adapter
│   │   └── NearbyTrailAdapter.java          # Nearby trails adapter
│   │
│   ├── auth/                                # Authentication
│   │   └── SessionManager.java              # Session management
│   │
│   ├── database/                            # Room Database
│   │   ├── AppDatabase.java                 # Main database class
│   │   ├── Converters.java                  # Type converters
│   │   ├── dao/                             # Data Access Objects
│   │   │   ├── HikeDao.java
│   │   │   ├── ObservationDao.java
│   │   │   └── UserDao.java
│   │   └── entities/                        # Room Entities
│   │       ├── Hike.java
│   │       ├── Observation.java
│   │       └── User.java
│   │
│   ├── services/                            # Services
│   │   └── SemanticSearchService.java       # Backend API client
│   │
│   ├── sync/                                # Sync Managers
│   │   ├── FirebaseSyncManager.java         # Firebase sync orchestrator
│   │   ├── VectorSyncManager.java           # Vector embedding manager
│   │   └── GeminiEmbeddingService.java      # Gemini API client
│   │
│   └── utils/                                # Utilities
│       ├── NetworkUtils.java                # Network connectivity check
│       └── SearchHelper.java                # Fuzzy search helper
│
├── res/
│   ├── layout/                              # XML Layouts
│   │   ├── activity_*.xml                   # Activity layouts
│   │   ├── item_*.xml                       # RecyclerView item layouts
│   │   └── bottom_navigation.xml
│   ├── values/
│   │   ├── strings.xml                       # String resources
│   │   └── colors.xml                        # Color resources
│   └── xml/
│       └── network_security_config.xml      # Network security config
│
└── AndroidManifest.xml                       # App manifest

app/
└── google-services.json                      # Firebase configuration
```

### Backend

```
backend/
├── main.py                                  # FastAPI application
├── requirements.txt                         # Python dependencies
├── .env                                     # Environment variables (not in git)
├── service-account-key.json                 # Firebase service account (not in git)
├── README.md                                # Backend documentation
└── SETUP_GUIDE.md                          # Setup instructions
```

### Documentation

```
/
├── SYSTEM_DOCUMENTATION.md                  # This file
├── FIREBASE_INTEGRATION.md                  # Firebase integration details
├── EMBEDDING_PROMPTS.md                     # Embedding prompt details
└── HOME_PAGE_SETUP.md                       # Home page setup guide
```

---

## Key Design Decisions

### 1. Offline-First Architecture
- **Rationale:** Users may hike in areas with poor connectivity
- **Implementation:** Room database as primary storage, Firebase as cloud backup
- **Benefit:** App works fully offline, syncs when online

### 2. Single-User Local Cache
- **Rationale:** Simplify data management, prevent data mixing
- **Implementation:** Logout clears all local data after sync
- **Benefit:** Clean separation between user sessions

### 3. Selective Sync (Unsynced Items Only)
- **Rationale:** Reduce bandwidth, prevent duplicate uploads
- **Implementation:** `synced` flag in entities, only sync `synced = false` items
- **Benefit:** Efficient sync, faster operations

### 4. Embeddings Stored in Firestore Documents
- **Rationale:** Keep data together, simplify queries
- **Implementation:** `embedding_vector` field directly in hike/observation documents
- **Benefit:** Single query to get data + embedding, no separate collection

### 5. Separate Backend for Semantic Search
- **Rationale:** Complex vector operations, need server-side processing
- **Implementation:** Python FastAPI backend with NumPy for similarity
- **Benefit:** Efficient computation, scalable architecture

### 6. Gemini 2.5 Flash for Embeddings
- **Rationale:** Latest model, good performance
- **Implementation:** HTTP API calls to Gemini Embedding API
- **Benefit:** High-quality embeddings for semantic search

---

## Security Considerations

### Current State
- Firebase Auth handles password authentication
- Firestore rules should restrict access (not fully implemented)
- API keys stored in `local.properties` (not committed to git)
- Backend uses Firebase Admin SDK (service account)

### Recommendations for Production
1. **Firestore Security Rules:**
   ```javascript
   match /users/{userId} {
     allow read, write: if request.auth != null && request.auth.uid == userId;
   }
   ```

2. **Password Storage:**
   - Hash passwords before storing in Room (currently plain text)
   - Use bcrypt or similar hashing algorithm

3. **API Keys:**
   - Use Android Keystore for sensitive keys
   - Rotate keys regularly
   - Use environment variables for backend

4. **Network Security:**
   - Use HTTPS for all API calls (currently HTTP for backend)
   - Implement certificate pinning

5. **Data Encryption:**
   - Encrypt sensitive data in Room database
   - Use Android's EncryptedSharedPreferences for session data

---

## Performance Optimizations

### Current Optimizations
1. **Selective Sync:** Only syncs unsynced items
2. **Background Threading:** Database operations on background threads
3. **Single Executor:** Vector sync uses single-threaded executor
4. **Efficient Queries:** Indexed fields in Room database

### Future Optimizations
1. **Pagination:** Implement pagination for large hike lists
2. **Caching:** Cache search results
3. **Batch Operations:** Batch Firestore writes
4. **Lazy Loading:** Load observations on demand
5. **Image Optimization:** Compress images before upload (if added)

---

## Testing Recommendations

### Unit Tests
- DAO operations (insert, update, delete, query)
- SessionManager methods
- SearchHelper fuzzy search
- Cosine similarity calculation

### Integration Tests
- Firebase sync flow
- Vector embedding generation
- Semantic search end-to-end
- Login/signup flow

### UI Tests
- Form validation
- Navigation flows
- Search functionality
- Hike start/end operations

---

## Future Enhancements

1. **Offline Firestore:** Enable Firestore offline persistence for direct cloud reads
2. **Image Support:** Add photo uploads for hikes and observations
3. **GPS Tracking:** Real-time location tracking during hikes
4. **Social Features:** Share hikes with other users
5. **Statistics Dashboard:** Advanced analytics and charts
6. **Export Data:** Export hikes to PDF/CSV
7. **Push Notifications:** Reminders and updates
8. **Map Integration:** Display hikes on map
9. **Weather Integration:** Historical weather data for hikes
10. **Multi-language Support:** Internationalization

---

## Conclusion

The M-Hike application is a comprehensive hiking management system with:
- **Offline-first** architecture for reliability
- **Cloud sync** for data backup and multi-device access
- **AI-powered semantic search** for intelligent discovery
- **Clean architecture** with separation of concerns
- **Scalable design** ready for future enhancements

This documentation serves as a complete reference for understanding, maintaining, and extending the system.

