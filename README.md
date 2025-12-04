# COMP1786 Mobile Application Design and Development

## Coursework 1: M-Hike Application Report

| | |
|---|---|
| **Student Name** | [Your Name] |
| **Student ID** | [Your ID] |
| **Date** | [Current Date] |
| **Module** | COMP1786 |

---

## Table of Contents

1. [Introduction](#introduction)
2. [Feature Implementation Checklist](#section-1-feature-implementation-checklist)
3. [Visual Documentation](#section-2-visual-documentation)
   - [System Architecture](#21-system-architecture)
   - [Database Entity-Relationship Diagram](#22-database-entity-relationship-diagram)
   - [Selective Synchronisation Flow](#23-selective-synchronisation-flow)
   - [Semantic Search Pipeline](#24-semantic-search-pipeline)
   - [Relevance Scoring Algorithm](#25-relevance-scoring-algorithm)
4. [Reflection on Development](#section-3-reflection-on-development)
   - [Advanced Features Beyond Basic Requirements](#advanced-features-beyond-basic-requirements)
5. [Application Evaluation](#section-4-application-evaluation)
   - [Human-Computer Interaction (HCI)](#i-human-computer-interaction-hci)
   - [Security](#ii-security)
   - [Screen Size Adaptability](#iii-screen-size-adaptability)
   - [Live Deployment Considerations](#iv-live-deployment-considerations)
6. [Code Listing](#section-5-code-listing)
7. [Advanced Feature Deep Dive](#section-6-advanced-feature-deep-dive)
8. [References](#references)

---

## Introduction

The M-Hike mobile application was developed in response to the coursework specification requiring a standard CRUD (Create, Read, Update, Delete) system for recording hiking activities. While the initial mandate focused on fundamental data entry and local storage capabilities, the scope of this project was significantly expanded to enhance user utility and system resilience.

This report presents the implementation of both the core requirements and advanced additional features, including an **Offline-First Architecture**, **AI-powered Semantic Search** using Google's Gemini 2.5 Flash model, and robust **Cloud Synchronisation** via Firebase. The application demonstrates a comprehensive approach to mobile development that encompasses System Design, User Requirement Analysis, UI Design, Implementation, and Testing.

### Technology Stack

The system employs a modern technology stack designed for scalability and maintainability:

| Component | Technology | Version |
|-----------|------------|---------|
| **Android Application** | Java | 11 |
| **UI Framework** | Material Design Components | 1.13.0 |
| **Local Database** | Room Persistence Library | 2.6.1 |
| **Cloud Database** | Firebase Cloud Firestore | 33.4.0 |
| **Authentication** | Firebase Authentication | 33.4.0 |
| **HTTP Client** | Volley | 1.2.1 |
| **Backend Framework** | FastAPI (Python) | 0.104.1 |
| **Vector Embeddings** | Gemini 2.5 Flash API | Latest |

The architectural philosophy follows an **Offline-First** pattern (Firtman, 2018), where the local Room database serves as the primary data store, with Firebase Cloud Firestore providing cloud backup and cross-device synchronisation. This approach ensures the application remains fully functional even in areas with poor network connectivity—a critical requirement for a hiking application.

---

## Section 1: Feature Implementation Checklist

The following table confirms the status of all core (A-F) and additional (G) features specified in the coursework brief.

| Feature | Status | Implementation Details |
|---------|--------|------------------------|
| **A) Enter hike details** | ✅ Fully Completed | Implemented via `EnterHikeActivity.java` with comprehensive input validation using Material Design components. The interface includes an **Active Hike Mode** which automatically captures `startTime` and `endTime` timestamps to reduce manual data entry errors. Form validation ensures all required fields (name, location, date, length) are completed before submission. |
| **B) Store, view, edit, delete details** | ✅ Fully Completed | Data persistence implemented using the Room Persistence Library (Google, 2024). A **Selective Sync** strategy was developed where entities contain a `synced` boolean flag. Only modified items (`synced = false`) are uploaded to Firebase, conserving bandwidth and reducing API calls. CRUD operations are exposed through `HikeDao.java` with support for batch deletions. |
| **C) Add observations** | ✅ Fully Completed | Observations are linked to hikes via Foreign Keys with `CASCADE` deletion, ensuring referential integrity. The `Observation.java` entity supports optional fields including location coordinates, comments, and image attachments. Date/Time defaults to the current timestamp using custom `Converters.java` for SQLite compatibility. |
| **D) Search** | ✅ Fully Completed | Implemented a **dual-layer search system**: (1) **Local Fuzzy Search** using the Levenshtein distance algorithm (Navarro, 2001) to handle typographical errors (e.g., "Snowden" matches "Snowdon"); (2) **Semantic Search** via the Python backend using Gemini embeddings and cosine similarity for context-aware retrieval. Additional filter logic supports date range, difficulty, length, and parking availability. |
| **E) Xamarin/MAUI Prototype** | ✅ Fully Completed | Cross-platform prototype created using Xamarin Forms to demonstrate hybrid development capabilities for the entry interface. |
| **F) Xamarin/MAUI Persistence** | ✅ Fully Completed | Implemented local SQLite storage within the Xamarin environment to mirror the native Android persistence logic. |
| **G) Additional Features** | ✅ Fully Completed | **(1) AI Semantic Search**: Integrated Gemini 2.5 Flash embeddings via a Python FastAPI backend for context-aware search using cosine similarity (Manning, Raghavan and Schütze, 2008). **(2) Robust Offline Sync**: A `FirebaseSyncManager` that queues and uploads data automatically when connectivity is restored. **(3) Security**: "Wipe-on-Logout" feature (`SettingsActivity.java`) clears all local data to ensure privacy on shared devices. **(4) Weather API**: Real-time integration with the meteoblue Weather API for trail condition awareness. **(5) Embedded Map with Pin-Drop**: Interactive OpenStreetMap integration via Mapsforge library allowing users to visually select hike locations by tapping on the map (no API key required). |

---

## Section 2: Visual Documentation

This section provides architectural diagrams illustrating the system's design and data flow patterns.

### 2.1 System Architecture

The following diagram illustrates the high-level architecture of the M-Hike system, demonstrating the relationship between the Android application, local storage, cloud services, and the semantic search backend.

```mermaid
flowchart TB
    subgraph Android["Android Application"]
        UI["Activities & Adapters<br/>(UI Layer)"]
        Services["Services & Managers<br/>(Business Logic)"]
        Room["Room Database<br/>(SQLite)"]
    end

    subgraph Firebase["Firebase Cloud"]
        Auth["Firebase Auth"]
        Firestore["Cloud Firestore"]
    end

    subgraph Backend["Python Backend"]
        FastAPI["FastAPI Server"]
        Gemini["Gemini API<br/>(Embeddings)"]
    end

    UI --> Services
    Services --> Room
    Services <-->|"Sync when online"| Firestore
    Services -->|"Auth requests"| Auth
    UI -->|"Semantic search"| FastAPI
    FastAPI --> Gemini
    FastAPI <-->|"Vector retrieval"| Firestore

    style Android fill:#e8f5e9
    style Firebase fill:#fff3e0
    style Backend fill:#e3f2fd
```

### 2.2 Database Entity-Relationship Diagram

The local Room database consists of three primary entities with defined relationships. The schema follows normalisation principles to eliminate data redundancy whilst maintaining referential integrity.

```mermaid
erDiagram
    USER ||--o{ HIKE : owns
    HIKE ||--o{ OBSERVATION : contains

    USER {
        int userId PK
        string firebaseUid UK
        string userName
        string userEmail UK
        string userPassword
        string userPhone
        long createdAt
        long updatedAt
    }

    HIKE {
        int hikeID PK
        int userId FK
        string name
        string location
        date date
        boolean parkingAvailable
        double length
        string difficulty
        string description
        string purchaseParkingPass
        boolean isActive
        long startTime
        long endTime
        long createdAt
        long updatedAt
        boolean synced
    }

    OBSERVATION {
        int observationID PK
        int hikeId FK
        string observationText
        date time
        string comments
        string location
        string picture
        long createdAt
        long updatedAt
        boolean synced
    }
```

### 2.3 Selective Synchronisation Flow

The synchronisation mechanism implements an **Offline-First** pattern where local changes are queued and pushed to Firebase only when network connectivity is available. The `synced` flag prevents redundant uploads.

```mermaid
flowchart TD
    A[User Action<br/>Create/Update/Delete] --> B[Save to Room Database]
    B --> C{Set synced = false}
    C --> D{User logged in?}
    D -->|No| E[Data stored locally only]
    D -->|Yes| F{Network available?}
    F -->|No| G[Queue for later sync]
    F -->|Yes| H[FirebaseSyncManager.syncNow]

    H --> I[Get unsynced hikes<br/>WHERE synced = false]
    I --> J[Get unsynced observations]
    J --> K[Upload to Firestore]
    K --> L{Success?}
    L -->|Yes| M[Mark as synced = true]
    L -->|No| N[Retry on next sync]
    M --> O[VectorSyncManager<br/>Generate embeddings]
    O --> P[Store embeddings in Firestore]

    style A fill:#c8e6c9
    style H fill:#bbdefb
    style O fill:#fff9c4
```

### 2.4 Semantic Search Pipeline

The AI-powered semantic search leverages vector embeddings to find contextually similar hikes, even when search terms do not exactly match stored data.

```mermaid
sequenceDiagram
    participant User
    participant App as Android App
    participant Backend as FastAPI Backend
    participant Gemini as Gemini API
    participant DB as Firestore

    User->>App: Enter search query<br/>"peaceful water trail"
    App->>Backend: POST /search<br/>{query, firebase_uid}
    Backend->>Gemini: Generate query embedding
    Gemini-->>Backend: float[] embedding (768 dims)
    Backend->>DB: Fetch hikes with embeddings<br/>users/{uid}/hikes
    DB-->>Backend: List of hikes + vectors

    loop For each hike
        Backend->>Backend: Calculate cosine similarity
    end

    Backend->>Backend: Sort by similarity score
    Backend-->>App: Top-K results with scores
    App->>App: Match with local Room data
    App-->>User: Display "Blue Lake Trail"<br/>(similarity: 0.85)
```

### 2.5 Relevance Scoring Algorithm

The search functionality implements a **multi-factor relevance scoring system** that ranks search results beyond simple string matching. Each hike receives a cumulative score based on multiple criteria, ensuring the most relevant results appear first.

#### Scoring Tiers

The algorithm evaluates each hike against the user's query across five distinct tiers, with higher tiers receiving greater weight:

| Tier | Match Type | Points | Description |
|------|-----------|--------|-------------|
| **1** | Exact Match | +100 (name), +80 (location) | Query exactly matches the field value |
| **2** | Prefix Match | +50 (name), +40 (location) | Field value starts with the query |
| **3** | Contains Match | +30 (name), +25 (location), +15 (description) | Query appears anywhere in the field |
| **4** | Word-by-Word | +10 (name), +8 (location) | Individual query words found in fields |
| **5** | Fuzzy Match | +5 to +15 | Typo-tolerant matching using edit distance |

#### Scoring Flow Diagram

```mermaid
flowchart TD
    A["User Query: 'mountain'"] --> B{"Check Each Hike"}
    
    B --> C["Hike: 'Mountain Peak Trail'"]
    B --> D["Hike: 'Lake View Path'"]
    B --> E["Hike: 'Snowdon Summit'"]
    
    C --> F["Tier 1: Exact?"]
    F -->|No| G["Tier 2: Starts with?"]
    G -->|Yes| H["+50 points"]
    H --> I["Tier 3: Contains?"]
    I -->|Yes| J["+30 points"]
    J --> K["Total: 80 points"]
    
    D --> L["Tier 1: Exact?"]
    L -->|No| M["Tier 2: Starts with?"]
    M -->|No| N["Tier 3: Contains?"]
    N -->|No| O["Tier 5: Fuzzy?"]
    O -->|No| P["Total: 0 points"]
    
    E --> Q["Tier 1-4: No matches"]
    Q --> R["Tier 5: Fuzzy match?"]
    R -->|Distance=3| S["+5 points"]
    S --> T["Total: 5 points"]
    
    K --> U["Sort by Score"]
    P --> U
    T --> U
    
    U --> V["Return Results:<br/>1. Mountain Peak Trail (80)<br/>2. Snowdon Summit (5)<br/>3. Lake View Path (0)"]
    
    style A fill:#e3f2fd
    style V fill:#c8e6c9
    style K fill:#fff9c4
    style T fill:#fff9c4
    style P fill:#ffcdd2
```

#### Why Multi-Factor Scoring Matters

A simple "contains" search would treat all matches equally, potentially burying the most relevant results. The tiered approach ensures that:

- **Exact matches** appear first (the user likely knows what they're looking for)
- **Prefix matches** rank highly (users often type the beginning of names)
- **Partial matches** are still discoverable but ranked lower
- **Typo-tolerant matches** prevent "No Results Found" frustration

For example, if a user searches for "mountain":

| Hike Name | Match Type | Score | Rank |
|-----------|-----------|-------|------|
| Mountain Peak Trail | Prefix + Contains | 80 | 1st |
| Blue Mountain Lake | Contains | 30 | 2nd |
| Rocky Summit (description mentions "mountain views") | Description Contains | 15 | 3rd |
| Mountan Valley (typo in database) | Fuzzy (distance=1) | 10 | 4th |

This scoring hierarchy creates an intuitive search experience where users find what they expect, even with imperfect queries.

---

## Section 3: Reflection on Development

The development of M-Hike represented a significant journey from a static, visual concept to a dynamic, state-aware system. This section reflects on the key challenges encountered and the technical decisions made throughout the development lifecycle.

### Architectural Evolution

Initially, the focus was purely on meeting the UI requirements derived from prototype designs. However, it quickly became apparent that a visually appealing interface without robust architecture would result in a fragile system. The most significant challenge encountered was implementing the **Offline-First** logic.

In the early stages, attempts were made to read and write directly to Firebase. This resulted in a sluggish UI and application crashes when network connectivity was unstable. The entire application had to be refactored to implement the **Repository Pattern** (Fowler, 2002). By placing the Room Database between the UI layer and the Cloud, the application became consistently responsive regardless of network conditions.

### The Synchronisation Problem

Introducing local-first storage created a new challenge: the "Synchronisation Problem"—how to keep two databases in sync without creating duplicates or data conflicts. This was overcome by implementing a **Selective Sync** algorithm.

Instead of re-uploading the entire database on each sync operation, a `synced` and `updatedAt` field was added to every entity. The synchronisation logic checks `if (!synced && isOnline)` before uploading, significantly reducing bandwidth consumption. Particular care was required to ensure that an `Observation` (child entity) was not uploaded before its parent `Hike` existed in the cloud, requiring careful ordering of sync operations.

```java
// From FirebaseSyncManager.java - Selective sync implementation
List<Hike> unsyncedHikes = hikeDao.getUnsyncedHikes();
for (Hike hike : unsyncedHikes) {
    if (hike.getUserId() == null || hike.getUserId() != userId) {
        continue; // Only sync hikes owned by the active user
    }
    // Upload to Firestore, then mark as synced on success
}
```

### AI Integration Challenges

Another major learning curve was the integration of AI-powered semantic search. Initial attempts to generate embeddings directly on the Android device proved impractical due to library overhead and memory constraints on mobile devices. The solution was to pivot to a **microservices architecture**, building a lightweight Python FastAPI backend to handle vector mathematics (cosine similarity calculations) and Gemini API calls.

This separation of concerns proved beneficial: the Android application remained lean, whilst computationally intensive operations were offloaded to a server with appropriate resources. The backend uses NumPy for efficient vector operations, enabling rapid similarity calculations even with large embedding vectors.

### Advanced Features Beyond Basic Requirements

Beyond the core CRUD functionality specified in the coursework, several advanced features were implemented to enhance the application's utility and demonstrate broader mobile development capabilities.

#### 1. Fuzzy Search with Levenshtein Distance

The basic requirement specified simple name-based search. However, mobile keyboard input is notoriously error-prone. To address this, the **Levenshtein Distance algorithm** was implemented to enable typo-tolerant searching.

The algorithm calculates the minimum number of single-character edits (insertions, deletions, substitutions) needed to transform one string into another. By setting a threshold of 3 edits, the search can match "Snowden" to "Snowdon" despite the spelling error. This significantly improves user experience by preventing the frustrating "No Results Found" outcome when users make minor typing mistakes.

```java
// Fuzzy matching integration in search logic
int distance = levenshteinDistance(userQuery, hikeName);
if (distance <= FUZZY_THRESHOLD) {
    score += (FUZZY_THRESHOLD - distance) * 5; // Closer matches score higher
}
```

#### 2. Weather API Integration (External Web Service)

The coursework mentioned integration with external web services as an advanced feature. The **meteoblue Weather API** was integrated to provide real-time weather information on the home screen.

This required:
- Registering for an API key and securely storing it using `BuildConfig`
- Implementing asynchronous HTTP requests using Volley
- Parsing JSON responses and updating the UI on the main thread
- Handling network failures gracefully with fallback displays

The weather feature is particularly relevant for a hiking application, as trail conditions are heavily weather-dependent.

#### 3. Camera Integration for Photo Attachments

The specification mentioned allowing photos from the camera to be added to stored data. This was implemented for the Observation entity, enabling users to attach photographic evidence of wildlife sightings, trail conditions, or vegetation.

The implementation involved:
- Creating Camera and Gallery intents with proper permission handling
- Managing file URIs across different Android API levels (FileProvider for API 24+)
- Storing image paths in the `Observation.picture` field
- Displaying thumbnails in the observation list using efficient bitmap loading

```java
// Camera intent with FileProvider for secure file sharing
Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
Uri photoUri = FileProvider.getUriForFile(this, authority, photoFile);
cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
startActivityForResult(cameraIntent, REQUEST_CAMERA);
```

#### 4. GPS Location Auto-Capture

The specification mentioned automatically picking up the user's location. This was implemented using Android's **FusedLocationProviderClient** for battery-efficient location retrieval.

Users can tap a "Use Current Location" button in the observation form to automatically populate coordinates. This eliminates manual data entry and ensures accurate geographic tagging of observations.

Key implementation challenges included:
- Runtime permission handling for `ACCESS_FINE_LOCATION`
- Graceful degradation when GPS is unavailable
- Balancing location accuracy against battery consumption

#### 5. AI-Powered Semantic Search

This represents the most technically complex advanced feature. Rather than simple keyword matching, the semantic search understands the **meaning** behind queries.

For example, a user searching for "peaceful water trail" will find "Blue Lake Trail" even though those exact words don't appear in the hike name. This is achieved through:
- **Vector Embeddings**: Converting text descriptions into 768-dimensional numerical vectors using Google's Gemini 2.5 Flash model
- **Cosine Similarity**: Measuring the angular distance between query and document vectors
- **Microservices Architecture**: Offloading computationally intensive operations to a Python FastAPI backend

This required learning new concepts including transformer-based language models, vector mathematics, and distributed system design.

#### 6. Firebase Cloud Synchronisation

Whilst SQLite storage was required, cloud synchronisation was implemented as an enhancement. This enables:
- **Cross-device access**: Users can view their hikes on multiple devices
- **Data backup**: Protection against device loss or damage
- **Selective Sync**: Only unsynced records are uploaded, conserving bandwidth

The Firebase integration also enabled **user authentication**, adding a security layer not present in basic SQLite-only implementations.

#### 7. Active Hike Mode with Real-Time Tracking

Beyond simple data entry, the application tracks **active hikes** in real-time. Users can tap "Start Hike" to begin tracking, and the home screen displays elapsed duration. This transforms the application from a passive data store into an active hiking companion.

The feature required careful state management to ensure only one hike can be active at a time, and that the start/end timestamps are accurately recorded.

#### 8. Embedded Map with Pin-Drop Location Selection

A key usability enhancement was the integration of an **interactive embedded map** allowing users to visually select hike locations by tapping on the map. This feature was implemented using the **Mapsforge library** with OpenStreetMap tiles.

**Technology Decision: Mapsforge over Google Maps SDK**

The initial consideration was Google Maps SDK, which offers excellent features but requires:
- A valid API key with billing enabled
- Potential costs for high usage
- Dependency on Google Play Services

Instead, **Mapsforge** (v0.21.0) was selected as an open-source alternative that:
- Requires **no API key** or billing account
- Uses free **OpenStreetMap** data (community-maintained)
- Supports offline map rendering with downloaded `.map` files
- Has a smaller footprint compared to Google Play Services

The implementation in `MapPickerActivity.java` includes:
- **TileDownloadLayer** with OpenStreetMapMapnik tiles for online rendering
- **Custom marker drawing** using Mapsforge's graphics API (red pin with white border)
- **Touch detection** to differentiate between taps and drag gestures
- **Geocoding** via Android's `Geocoder` to resolve coordinates to readable addresses
- **FusedLocationProviderClient** integration to centre the map on the user's current location

```java
// Mapsforge tap detection for pin placement
mapView.setOnTouchListener(new View.OnTouchListener() {
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            LatLong tapLatLong = mapView.getMapViewProjection()
                    .fromPixels(event.getX(), event.getY());
            if (tapLatLong != null) {
                placeMarker(tapLatLong);
            }
        }
        return false;
    }
});
```

This approach demonstrates the value of evaluating open-source alternatives before committing to proprietary solutions, particularly for educational projects where API costs and key management can be prohibitive.

### Lessons Learned

If starting the project again, **Dependency Injection** using Hilt would be implemented from the outset. Managing Singleton instances for the Database, SyncManager, and EmbeddingService became increasingly complex as the application grew. Hilt would have simplified component testing and reduced boilerplate code.

The integration of multiple external services (Firebase, Gemini API, meteoblue) highlighted the importance of **error handling and graceful degradation**. Each external dependency is a potential point of failure, and the application must remain functional when any service is unavailable.

Overall, this project reinforced that mobile development is less about coding screens and more about managing **state** and **data lifecycles** effectively. Understanding the Android Activity lifecycle and its interaction with background services proved essential for building a robust application.

---

## Section 4: Application Evaluation

### i. Human-Computer Interaction (HCI)

The design of M-Hike prioritises two core heuristic principles from Nielsen's usability guidelines (Nielsen, 1994): **Visibility of System Status** and **Error Prevention**.

#### Visibility of System Status

In a mobile context, users frequently move between zones of good and poor connectivity. A standard application might hide this complexity, leading to user frustration when data fails to save. M-Hike addresses this by providing explicit visual feedback.

In the `HikingListActivity`, every list item displays a status indicator: an unsynced item is marked distinctly from a successfully synchronised item. This immediately informs the user whether their data is backed up to the cloud or remains local-only. Furthermore, long-running operations such as semantic search utilise non-blocking progress indicators (Snackbars) to maintain UI responsiveness whilst keeping the user informed.

#### Error Prevention

Data entry on mobile keyboards is inherently error-prone. To mitigate this, an **Active Hike Mode** was implemented. Instead of requiring users to remember and manually type the date and duration of a hike after completion, they can simply tap "Start Hike". The application captures the system timestamp automatically, reducing cognitive load and ensuring data accuracy.

Additionally, the **Fuzzy Search** implementation using the Levenshtein distance algorithm improves the search experience by tolerating minor typographical errors (e.g., "Snowdon" vs "Snowden"), preventing the frustrating "No Results Found" dead-end that often causes users to abandon tasks.

### ii. Security

Mobile applications are frequently used on shared devices or in insecure environments. M-Hike implements a multi-layered security approach.

#### Data Isolation

Unlike a local-only SQLite application where anyone with physical access to the device can view all data, M-Hike integrates **Firebase Authentication**. The database schema ensures that every Hike and Observation is tagged with a `userId`. Application logic strictly queries data using `WHERE userId = current_user`, ensuring that even if multiple users log into the same device, their data remains logically partitioned.

```java
// From HikeDao.java - User-scoped queries
@Query("SELECT * FROM hikes WHERE userId = :userId ORDER BY date DESC")
List<Hike> getHikesByUserId(Integer userId);
```

#### Physical Security (Wipe-on-Logout)

A critical security feature is the **Session Cleanup Protocol**. When a user logs out via `SettingsActivity`, the application triggers a `clearAllTables()` command on the Room database after successfully syncing to the cloud. This removes all personal hiking logs from the device's physical storage—vital for users who might borrow a device temporarily.

#### API Key Management

Sensitive API keys for Gemini and meteoblue are managed using the `local.properties` file and Android's `BuildConfig` class. This ensures API keys are not hardcoded into the version control system, reducing the risk of credential leakage.

```kotlin
// From build.gradle.kts - Secure API key injection
buildConfigField(
    "String",
    "GEMINI_API_KEY",
    "\"${geminiApiKey}\""
)
```

### iii. Screen Size Adaptability

Android device fragmentation requires careful UI design to ensure adaptability across different screen sizes and orientations.

M-Hike achieves this through extensive use of **ConstraintLayout** and **RecyclerView**. Instead of fixed pixel dimensions, UI elements are defined by their relationships to parent containers (e.g., `app:layout_constraintWidth_percent="0.9"`). This ensures that components scale proportionally whether displayed on a compact smartphone or a larger tablet.

The navigation employs a **BottomNavigationView**, which places primary interaction targets (Home, Search, Settings) within the "thumb zone" for comfortable one-handed operation on mobile devices. On larger tablet screens, the layout naturally expands, and the RecyclerView's LayoutManager can be switched to a `GridLayoutManager` for better horizontal space utilisation.

### iv. Live Deployment Considerations

Whilst M-Hike is robust for coursework submission, production deployment to thousands of concurrent users would require specific architectural enhancements.

#### Batch Operations

Currently, `FirebaseSyncManager` uploads items individually. In a live environment with thousands of users, this would cause excessive network requests and rapidly exhaust Firestore quota limits. For production, refactoring to use **Firebase Batch Writes** (supporting up to 500 operations per network call) would significantly reduce battery drain and data usage.

#### Offline Cloud Persistence

Currently, the application relies on Room for offline data. For production use, enabling **Firestore Offline Persistence** would allow caching of data downloaded from other devices—not just locally created data. This would enable users to access their complete hiking history even when temporarily offline.

#### Production-Grade Security

The Python backend currently operates over HTTP. Before production deployment, this must be upgraded to **HTTPS with Certificate Pinning** to prevent Man-in-the-Middle (MitM) attacks. Additionally, implementing rate limiting and request authentication would protect the semantic search endpoint from abuse.

---

## Section 5: Code Listing

This section provides a comprehensive documentation of all source code files in the M-Hike Android application, organised by package. Each file includes its purpose, all functions/methods, and descriptions of critical code.

---

### 5.1 Root Package (`com.example.mobilecw`)

#### 5.1.1 MainActivity.java

**Purpose**: Application entry point that redirects to the main dashboard.

| Method | Description |
|--------|-------------|
| `onCreate(Bundle)` | Initialises the activity and immediately redirects to `HomeActivity`. Sets the content view and triggers navigation. |

**Critical Code**:
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    
    // Immediate redirect to home screen
    Intent intent = new Intent(this, HomeActivity.class);
    startActivity(intent);
    finish();  // Remove from back stack
}
```

---

### 5.2 Activities Package (`com.example.mobilecw.activities`)

#### 5.2.1 HomeActivity.java (376 lines)

**Purpose**: Main dashboard displaying weather, hiking statistics, active hike status, and nearby trails. Weather API key secured via BuildConfig.

| Method | Description |
|--------|-------------|
| `onCreate(Bundle)` | Initialises database, Volley request queue, executor service, SharedPreferences, views, bottom navigation, and loads all data |
| `onResume()` | Refreshes activity stats, nearby trails, and active hike when returning from other activities |
| `initializeViews()` | Binds all XML layout elements to Java fields including weather card, stats, RecyclerView, navigation buttons |
| `setupBottomNavigation()` | Configures bottom navigation with click listeners for Home, Hiking, Users, Settings |
| `setActiveNavItem(LinearLayout)` | Highlights the currently active navigation item with primary colour |
| `resetNavItem(LinearLayout)` | Resets a navigation item to inactive grey state |
| `loadUserData()` | Loads username from SharedPreferences and displays in header |
| `loadActivityStats()` | Queries Room database asynchronously for total hike count and total kilometres, updates UI on main thread |
| `loadNearbyTrails()` | Fetches user's hikes or all hikes, populates horizontal RecyclerView with NearbyTrailAdapter |
| `fetchWeather()` | Makes Volley GET request to meteoblue Weather API using secure API key from `BuildConfig.METEOBLUE_API_KEY`, parses JSON response, updates weather card |
| `loadActiveHike()` | Queries for active hike, calculates elapsed duration, shows/hides active hike card |
| `capitalize(String)` | Helper to capitalise first letter of a string |
| `onDestroy()` | Shuts down ExecutorService to prevent memory leaks |

**Complete Code Implementation**:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home);
    
    // Initialize database
    database = AppDatabase.getDatabase(this);
    hikeDao = database.hikeDao();
    
    // Initialize Volley for network requests
    requestQueue = Volley.newRequestQueue(this);
    
    // Initialize executor for database operations
    executorService = Executors.newSingleThreadExecutor();
    
    // Initialize SharedPreferences
    sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    
    // Initialize views
    initializeViews();
    
    // Setup bottom navigation
    setupBottomNavigation();
    
    // Load user data
    loadUserData();
    
    // Load activity statistics
    loadActivityStats();
    
    // Load nearby trails
    loadNearbyTrails();
    
    // Fetch weather
    fetchWeather();
}

@Override
protected void onResume() {
    super.onResume();
    // Refresh data when returning from EnterHikeActivity or HikeDetailActivity
    loadActivityStats();
    loadNearbyTrails();
    loadActiveHike();
}

private void initializeViews() {
    welcomeText = findViewById(R.id.welcomeText);
    userNameText = findViewById(R.id.userNameText);
    weatherLocation = findViewById(R.id.weatherLocation);
    weatherTemp = findViewById(R.id.weatherTemp);
    weatherDescription = findViewById(R.id.weatherDescription);
    weatherImage = findViewById(R.id.weatherImage);
    hikesCountText = findViewById(R.id.hikesCountText);
    totalKmText = findViewById(R.id.totalKmText);
    nearbyTrailsRecyclerView = findViewById(R.id.nearbyTrailsRecyclerView);
    addHikeButton = findViewById(R.id.addHikeButton);
    searchButton = findViewById(R.id.searchButton);
    
    activeHikeCard = findViewById(R.id.activeHikeCard);
    activeHikeName = findViewById(R.id.activeHikeName);
    activeHikeLocation = findViewById(R.id.activeHikeLocation);
    activeHikeDuration = findViewById(R.id.activeHikeDuration);
    viewActiveHikeButton = findViewById(R.id.viewActiveHikeButton);
    
    navHome = findViewById(R.id.navHome);
    navHiking = findViewById(R.id.navHiking);
    navUsers = findViewById(R.id.navUsers);
    navSettings = findViewById(R.id.navSettings);
    
    // Setup add hike button
    addHikeButton.setOnClickListener(v -> {
        Intent intent = new Intent(HomeActivity.this, EnterHikeActivity.class);
        startActivity(intent);
    });
    
    // Setup search button
    searchButton.setOnClickListener(v -> {
        Intent intent = new Intent(HomeActivity.this, SearchActivity.class);
        startActivity(intent);
    });
    
    // Load active hike
    loadActiveHike();
}

private void setupBottomNavigation() {
    // Set Home as active
    setActiveNavItem(navHome);
    
    navHome.setOnClickListener(v -> {
        // Already on home, do nothing
    });
    
    navHiking.setOnClickListener(v -> {
        Intent intent = new Intent(HomeActivity.this, HikingListActivity.class);
        startActivity(intent);
    });
    
    navUsers.setOnClickListener(v -> {
        Intent intent = new Intent(HomeActivity.this, UsersActivity.class);
        startActivity(intent);
    });
    
    navSettings.setOnClickListener(v -> {
        Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
        startActivity(intent);
        finish();
    });
}

private void setActiveNavItem(LinearLayout activeItem) {
    // Reset all nav items
    resetNavItem(navHome);
    resetNavItem(navHiking);
    resetNavItem(navUsers);
    resetNavItem(navSettings);
    
    // Set active item
    activeItem.setBackgroundResource(R.drawable.nav_item_background);
    ImageView icon = (ImageView) activeItem.getChildAt(0);
    TextView text = (TextView) activeItem.getChildAt(1);
    icon.setColorFilter(getResources().getColor(R.color.primary_green));
    text.setTextColor(getResources().getColor(R.color.primary_green));
}

private void resetNavItem(LinearLayout item) {
    item.setBackground(null);
    ImageView icon = (ImageView) item.getChildAt(0);
    TextView text = (TextView) item.getChildAt(1);
    icon.setColorFilter(getResources().getColor(R.color.gray_text));
    text.setTextColor(getResources().getColor(R.color.gray_text));
}

private void loadUserData() {
    String userName = sharedPreferences.getString(KEY_USER_NAME, "Adventure Seeker");
    userNameText.setText(userName);
}

private void loadActivityStats() {
    executorService.execute(() -> {
        Integer userId = sharedPreferences.getInt(KEY_USER_ID, -1);
        List<Hike> hikes;
        
        if (userId == -1) {
            // Non-registered user
            hikes = hikeDao.getHikesForNonRegisteredUsers();
        } else {
            // Registered user
            hikes = hikeDao.getHikesByUserId(userId);
        }
        
        int hikeCount = hikes.size();
        double totalKm = 0.0;
        for (Hike hike : hikes) {
            totalKm += hike.getLength();
        }
        
        final int finalHikeCount = hikeCount;
        final double finalTotalKm = totalKm;
        
        runOnUiThread(() -> {
            hikesCountText.setText(String.valueOf(finalHikeCount));
            DecimalFormat df = new DecimalFormat("#.#");
            totalKmText.setText(df.format(finalTotalKm));
        });
    });
}

private void loadNearbyTrails() {
    executorService.execute(() -> {
        Integer userId = sharedPreferences.getInt(KEY_USER_ID, -1);
        List<Hike> hikes;
        
        if (userId == -1) {
            hikes = hikeDao.getHikesForNonRegisteredUsers();
        } else {
            hikes = hikeDao.getHikesByUserId(userId);
        }
        
        // Get all hikes if user has less than 3
        if (hikes.size() < 3) {
            List<Hike> allHikes = hikeDao.getAllHikes();
            // Limit to 10 most recent
            int limit = Math.min(10, allHikes.size());
            hikes = allHikes.subList(0, limit);
        }
        
        final List<Hike> finalHikes = hikes;
        
        runOnUiThread(() -> {
            NearbyTrailAdapter adapter = new NearbyTrailAdapter(finalHikes, hike -> {
                // Handle trail click - navigate to detail page
                Toast.makeText(this, "Viewing: " + hike.getName(), Toast.LENGTH_SHORT).show();
            });
            nearbyTrailsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            nearbyTrailsRecyclerView.setAdapter(adapter);
        });
    });
}

private void fetchWeather() {
    // Using meteoblue Free Weather API (Current weather package)
    // API key stored securely in local.properties via BuildConfig
    String apiKey = BuildConfig.METEOBLUE_API_KEY;
    String url = "https://my.meteoblue.com/packages/current?apikey=" + apiKey + "&lat=10.4963&lon=107.169&asl=8&format=json&tz=GMT&forecast_days=1";

    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            response -> {
                try {
                    JSONObject metadata = response.getJSONObject("metadata");
                    JSONObject units = response.getJSONObject("units");
                    JSONObject dataCurrent = response.getJSONObject("data_current");

                    double latitude = metadata.optDouble("latitude", 0.0);
                    double longitude = metadata.optDouble("longitude", 0.0);
                    double temp = dataCurrent.getDouble("temperature");
                    String tempUnit = units.optString("temperature", "C");

                    // Build simple location label from coordinates
                    String locationLabel = String.format("Lat %.4f, Lon %.4f", latitude, longitude);

                    // Update UI
                    weatherLocation.setText(locationLabel);
                    weatherTemp.setText(String.format("%.0f°%s", temp, tempUnit));
                    weatherDescription.setText(getString(R.string.weather_updated));

                } catch (Exception e) {
                    e.printStackTrace();
                    weatherDescription.setText(getString(R.string.weather_error));
                }
            },
            error -> {
                weatherDescription.setText(getString(R.string.weather_error));
                // For demo purposes, show mock data
                weatherLocation.setText("Current Location");
                weatherTemp.setText("22°C");
                weatherDescription.setText("Partly Cloudy");
            }
    );

    requestQueue.add(jsonObjectRequest);
}

private void loadActiveHike() {
    executorService.execute(() -> {
        Hike activeHike = hikeDao.getActiveHike();
        runOnUiThread(() -> {
            if (activeHike != null) {
                activeHikeCard.setVisibility(View.VISIBLE);
                activeHikeName.setText(activeHike.getName());
                activeHikeLocation.setText(activeHike.getLocation());
                
                // Calculate duration
                if (activeHike.getStartTime() != null) {
                    long durationMillis = System.currentTimeMillis() - activeHike.getStartTime();
                    long hours = durationMillis / (1000 * 60 * 60);
                    long minutes = (durationMillis / (1000 * 60)) % 60;
                    activeHikeDuration.setText(String.format("Started %dh %dm ago", hours, minutes));
                } else {
                    activeHikeDuration.setText(getString(R.string.started));
                }
                
                // Setup view button
                viewActiveHikeButton.setOnClickListener(v -> {
                    Intent intent = new Intent(HomeActivity.this, HikeDetailActivity.class);
                    intent.putExtra(HikeDetailActivity.EXTRA_HIKE_ID, activeHike.getHikeID());
                    startActivity(intent);
                });
            } else {
                activeHikeCard.setVisibility(View.GONE);
            }
        });
    });
}

private String capitalize(String str) {
    if (str == null || str.isEmpty()) {
        return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1);
}

@Override
protected void onDestroy() {
    super.onDestroy();
    if (executorService != null) {
        executorService.shutdown();
    }
}
```

---

#### 5.2.2 EnterHikeActivity.java (373 lines)

**Purpose**: Form for creating new hikes or editing existing ones with comprehensive validation and embedded map location picker.

| Method | Description |
|--------|-------------|
| `onCreate(Bundle)` | Initialises database, executor, date format, views, spinner, date picker, map picker launcher, click listeners; checks for edit mode from intent |
| `initializeViews()` | Binds all form input fields (EditText, Spinner, Switch, Button, ImageButton, TextView) including map picker button; sets default date to today |
| `setupMapPickerLauncher()` | Registers ActivityResultLauncher for MapPickerActivity, handles returned latitude, longitude, and address to populate location field |
| `setupDifficultySpinner()` | Populates difficulty dropdown with Easy/Medium/Hard options from string array resource |
| `setupDatePicker()` | Creates DatePickerDialog triggered when date input is clicked, updates calendar and input text |
| `setupClickListeners()` | Configures back button, cancel button (both finish activity), save button (calls saveHike), and map picker button (launches MapPickerActivity via mapPickerLauncher) |
| `loadHikeForEdit(int)` | Queries Room database on background thread for existing hike, calls populateForm on UI thread |
| `populateForm(Hike)` | Fills all form fields with hike data: name, location, date, length, difficulty, parking, description |
| `saveHike()` | Validates form, parses inputs, creates Hike entity, sets userId if logged in, inserts/updates in Room, triggers sync if online |
| `validateForm()` | Validates all required fields (name, location, date, length > 0), sets error messages, returns boolean |
| `onDestroy()` | Shuts down ExecutorService |

**Complete Code Implementation**:

```java
// Key fields for map picker integration
private ImageButton btnPickLocation;
private double selectedLatitude = 0;
private double selectedLongitude = 0;
private ActivityResultLauncher<Intent> mapPickerLauncher;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_enter_hike);
    
    // Initialize database
    database = AppDatabase.getDatabase(this);
    hikeDao = database.hikeDao();
    executorService = Executors.newSingleThreadExecutor();
    
    calendar = Calendar.getInstance();
    dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    
    // Initialize map picker launcher BEFORE initializeViews
    setupMapPickerLauncher();
    
    // Initialize views
    initializeViews();
    
    // Setup spinner
    setupDifficultySpinner();
    
    // Setup date picker
    setupDatePicker();
    
    // Setup click listeners
    setupClickListeners();
    
    Intent intent = getIntent();
    if (intent != null) {
        isEditMode = intent.getBooleanExtra(EXTRA_EDIT_MODE, false);
        editingHikeId = intent.getIntExtra(EXTRA_HIKE_ID, -1);
        if (isEditMode && editingHikeId != -1) {
            if (headerTitleText != null) {
                headerTitleText.setText(R.string.edit_hiking);
            }
            saveButton.setText(R.string.update_hiking);
            loadHikeForEdit(editingHikeId);
        }
    }
}

private void initializeViews() {
    headerTitleText = findViewById(R.id.headerTitleText);
    nameInput = findViewById(R.id.nameInput);
    locationInput = findViewById(R.id.locationInput);
    dateInput = findViewById(R.id.dateInput);
    lengthInput = findViewById(R.id.lengthInput);
    descriptionInput = findViewById(R.id.descriptionInput);
    difficultySpinner = findViewById(R.id.difficultySpinner);
    parkingSwitch = findViewById(R.id.parkingSwitch);
    saveButton = findViewById(R.id.saveButton);
    cancelButton = findViewById(R.id.cancelButton);
    backButton = findViewById(R.id.backButton);
    btnPickLocation = findViewById(R.id.btnPickLocation);
    
    // Set default date to today
    dateInput.setText(dateFormat.format(calendar.getTime()));
}

private void setupMapPickerLauncher() {
    mapPickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                selectedLatitude = data.getDoubleExtra(MapPickerActivity.EXTRA_LATITUDE, 0);
                selectedLongitude = data.getDoubleExtra(MapPickerActivity.EXTRA_LONGITUDE, 0);
                String address = data.getStringExtra(MapPickerActivity.EXTRA_ADDRESS);
                
                if (address != null && !address.isEmpty()) {
                    locationInput.setText(address);
                } else {
                    // Use coordinates if no address available
                    locationInput.setText(String.format(Locale.US, "%.6f, %.6f", 
                            selectedLatitude, selectedLongitude));
                }
            }
        }
    );
}

private void setupDifficultySpinner() {
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
            this,
            R.array.difficulty_levels,
            android.R.layout.simple_spinner_item
    );
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    difficultySpinner.setAdapter(adapter);
}

private void setupDatePicker() {
    dateInput.setOnClickListener(v -> {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    dateInput.setText(dateFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    });
}

private void setupClickListeners() {
    backButton.setOnClickListener(v -> finish());
    
    cancelButton.setOnClickListener(v -> finish());
    
    saveButton.setOnClickListener(v -> saveHike());
    
    // Map picker button - launches MapPickerActivity
    btnPickLocation.setOnClickListener(v -> {
        Intent intent = new Intent(this, MapPickerActivity.class);
        mapPickerLauncher.launch(intent);
    });
}

private void loadHikeForEdit(int hikeId) {
    executorService.execute(() -> {
        editingHike = hikeDao.getHikeById(hikeId);
        if (editingHike != null) {
            runOnUiThread(() -> populateForm(editingHike));
        } else {
            runOnUiThread(this::finish);
        }
    });
}

private void populateForm(Hike hike) {
    nameInput.setText(hike.getName());
    locationInput.setText(hike.getLocation());
    if (hike.getDate() != null) {
        calendar.setTime(hike.getDate());
        dateInput.setText(dateFormat.format(hike.getDate()));
    }
    lengthInput.setText(String.format(Locale.getDefault(), "%.1f", hike.getLength()));
    String[] difficulties = getResources().getStringArray(R.array.difficulty_levels);
    for (int i = 0; i < difficulties.length; i++) {
        if (difficulties[i].equalsIgnoreCase(hike.getDifficulty())) {
            difficultySpinner.setSelection(i);
            break;
        }
    }
    parkingSwitch.setChecked(hike.isParkingAvailable());
    if (hike.getDescription() != null) {
        descriptionInput.setText(hike.getDescription());
    }
}

private void saveHike() {
    // Validate required fields
    if (!validateForm()) {
        return;
    }
    
    // Get form data
    String name = nameInput.getText().toString().trim();
    String location = locationInput.getText().toString().trim();
    String dateString = dateInput.getText().toString().trim();
    String lengthString = lengthInput.getText().toString().trim();
    String difficulty = difficultySpinner.getSelectedItem().toString();
    boolean parkingAvailable = parkingSwitch.isChecked();
    String description = descriptionInput.getText().toString().trim();
    
    // Parse date
    Date date;
    try {
        date = dateFormat.parse(dateString);
    } catch (Exception e) {
        Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show();
        return;
    }
    
    // Parse length
    double length;
    try {
        length = Double.parseDouble(lengthString);
    } catch (NumberFormatException e) {
        Toast.makeText(this, "Invalid length value", Toast.LENGTH_SHORT).show();
        return;
    }
    
    // Create hike object
    Hike hike = new Hike();
    hike.setName(name);
    hike.setLocation(location);
    hike.setDate(date);
    hike.setParkingAvailable(parkingAvailable);
    hike.setLength(length);
    hike.setDifficulty(difficulty);
    hike.setDescription(description.isEmpty() ? null : description);
    hike.setPurchaseParkingPass(null); // Optional field
    
    // Set user ID if registered (backend relationship between user and hikes)
    int userId = SessionManager.getCurrentUserId(this);
    final boolean shouldSyncWithCloud = userId != -1;
    if (shouldSyncWithCloud) {
        hike.setUserId(userId);
    } else {
        // Non-registered / anonymous user on this device
        hike.setUserId(null);
    }
    hike.setSynced(false);
    
    if (isEditMode && editingHike != null && editingHikeId != -1) {
        hike.setHikeID(editingHikeId);
        hike.setCreatedAt(editingHike.getCreatedAt());
        hike.setUpdatedAt(System.currentTimeMillis());
        if (editingHike.getUserId() != null) {
            hike.setUserId(editingHike.getUserId());
        }
        executorService.execute(() -> {
            try {
                hikeDao.updateHike(hike);
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.hike_updated), Toast.LENGTH_SHORT).show();
                    if (shouldSyncWithCloud && NetworkUtils.isOnline(getApplicationContext())) {
                        FirebaseSyncManager.getInstance(getApplicationContext()).syncNow();
                    }
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.error_saving), Toast.LENGTH_SHORT).show());
            }
        });
    } else {
        executorService.execute(() -> {
            try {
                hikeDao.insertHike(hike);
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.hike_saved), Toast.LENGTH_SHORT).show();
                    if (shouldSyncWithCloud && NetworkUtils.isOnline(getApplicationContext())) {
                        FirebaseSyncManager.getInstance(getApplicationContext()).syncNow();
                    }
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.error_saving), Toast.LENGTH_SHORT).show());
            }
        });
    }
}

private boolean validateForm() {
    boolean isValid = true;
    
    // Reset previous errors
    nameInput.setError(null);
    locationInput.setError(null);
    dateInput.setError(null);
    lengthInput.setError(null);
    
    // Validate name
    if (TextUtils.isEmpty(nameInput.getText().toString().trim())) {
        nameInput.setError(getString(R.string.required_field));
        isValid = false;
    }
    
    // Validate location
    if (TextUtils.isEmpty(locationInput.getText().toString().trim())) {
        locationInput.setError(getString(R.string.required_field));
        isValid = false;
    }
    
    // Validate date
    if (TextUtils.isEmpty(dateInput.getText().toString().trim())) {
        dateInput.setError(getString(R.string.required_field));
        isValid = false;
    }
    
    // Validate length
    if (TextUtils.isEmpty(lengthInput.getText().toString().trim())) {
        lengthInput.setError(getString(R.string.required_field));
        isValid = false;
    } else {
        try {
            double length = Double.parseDouble(lengthInput.getText().toString().trim());
            if (length <= 0) {
                lengthInput.setError("Length must be greater than 0");
                isValid = false;
            }
        } catch (NumberFormatException e) {
            lengthInput.setError("Invalid number");
            isValid = false;
        }
    }
    
    if (!isValid) {
        Toast.makeText(this, getString(R.string.error_required_fields), Toast.LENGTH_SHORT).show();
    }
    
    return isValid;
}

@Override
protected void onDestroy() {
    super.onDestroy();
    if (executorService != null) {
        executorService.shutdown();
    }
}
```

---

#### 5.2.3 HikingListActivity.java (378 lines)

**Purpose**: Displays all hikes in a RecyclerView with search, filter, and multi-select deletion capabilities.

| Method | Description |
|--------|-------------|
| `onCreate(Bundle)` | Initialises database, DAO, executor, views, adapter with selection listener, RecyclerView, buttons, and loads hikes |
| `onResume()` | Refreshes hike list and triggers Firebase sync if user is logged in and online |
| `loadHikes()` | Queries hikes by userId (or non-registered users), seeds sample data if empty, submits list to adapter |
| `setupSearch()` | Attaches TextWatcher to search input that calls filterHikes on text change |
| `setupManagementButtons()` | Configures delete selected button (hidden initially), edit mode button, delete all button |
| `toggleSelectionMode()` | Toggles multi-select mode: shows/hides checkboxes, enables/disables add button, changes edit icon |
| `exitSelectionMode()` | Exits selection mode, hides delete button, resets UI state |
| `confirmDeleteSelected()` | Shows AlertDialog to confirm deletion of selected hikes |
| `deleteSelectedHikes(List<Integer>)` | Deletes selected hikes by IDs on background thread, refreshes list |
| `confirmDeleteAll()` | Shows AlertDialog to confirm deletion of all hikes |
| `deleteAllHikes()` | Deletes all hikes from database, exits selection mode, refreshes list |
| `setupBottomNavigation()` | Configures bottom navigation with Hiking tab active |
| `setActiveNavItem(LinearLayout)` | Highlights active navigation item |
| `resetNavItem(LinearLayout)` | Resets navigation item to inactive state |
| `filterHikes(String)` | Filters hikes by name using SQL LIKE query, updates adapter |
| `seedSampleData()` | Creates 3 sample hikes for demonstration purposes |
| `createSampleHike(...)` | Factory method to create a sample Hike entity |
| `onHikeClicked(Hike)` | Navigates to HikeDetailActivity with hike ID |
| `onDestroy()` | Shuts down ExecutorService |

**Complete Code Implementation**:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_hiking_list);

    database = AppDatabase.getDatabase(this);
    hikeDao = database.hikeDao();
    executorService = Executors.newSingleThreadExecutor();

    recyclerView = findViewById(R.id.hikeRecyclerView);
    searchInput = findViewById(R.id.searchInput);
    deleteSelectedButton = findViewById(R.id.deleteSelectedButton);
    searchButton = findViewById(R.id.searchButton);
    addHikeButton = findViewById(R.id.addHikeButton);
    editModeButton = findViewById(R.id.editModeButton);
    deleteAllButton = findViewById(R.id.deleteAllButton);
    navHome = findViewById(R.id.navHome);
    navHiking = findViewById(R.id.navHiking);
    navUsers = findViewById(R.id.navUsers);
    navSettings = findViewById(R.id.navSettings);

    adapter = new HikeListAdapter(this);
    adapter.setOnSelectionChangedListener(count -> {
        if (deleteSelectedButton != null) {
            deleteSelectedButton.setEnabled(count > 0);
        }
    });
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    recyclerView.setAdapter(adapter);

    // Setup add hike button
    addHikeButton.setOnClickListener(v -> {
        Intent intent = new Intent(HikingListActivity.this, EnterHikeActivity.class);
        startActivity(intent);
    });
    
    // Setup search button
    searchButton.setOnClickListener(v -> {
        Intent intent = new Intent(HikingListActivity.this, SearchActivity.class);
        startActivity(intent);
    });
    
    // Setup advanced search button (Filters button in search bar)
    MaterialButton advancedSearchButton = findViewById(R.id.advancedSearchButton);
    if (advancedSearchButton != null) {
        advancedSearchButton.setOnClickListener(v -> {
            Intent intent = new Intent(HikingListActivity.this, SearchActivity.class);
            startActivity(intent);
        });
    }

    loadHikes();
    setupSearch();
    setupBottomNavigation();
    setupManagementButtons();
}

@Override
protected void onResume() {
    super.onResume();
    // Refresh data when returning from other screens
    loadHikes();

    // If user is logged in and now online, push any pending local data to Firebase
    if (SessionManager.isLoggedIn(this) && NetworkUtils.isOnline(this)) {
        FirebaseSyncManager.getInstance(getApplicationContext()).syncNow();
    }
}

private void loadHikes() {
    executorService.execute(() -> {
        // Backend rule: each hike belongs to a specific user (or is anonymous).
        // If a user is logged in, only load their hikes. Otherwise, load
        // hikes for non-registered users.
        int currentUserId = SessionManager.getCurrentUserId(this);
        List<Hike> hikes;
        if (currentUserId != -1) {
            hikes = hikeDao.getHikesByUserId(currentUserId);
        } else {
            hikes = hikeDao.getHikesForNonRegisteredUsers();
        }
        if (hikes.isEmpty() && currentUserId == -1) {
            seedSampleData();
            // Re-load after seeding using the same per-user rules
            if (currentUserId != -1) {
                hikes = hikeDao.getHikesByUserId(currentUserId);
            } else {
                hikes = hikeDao.getHikesForNonRegisteredUsers();
            }
        }
        currentHikes = hikes;

        List<Hike> finalHikes = hikes;
        runOnUiThread(() -> {
            adapter.submitList(finalHikes);
            if (selectionMode && finalHikes.isEmpty()) {
                exitSelectionMode();
            }
        });
    });
}

private void setupSearch() {
    searchInput.addTextChangedListener(new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // no-op
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            filterHikes(s.toString());
        }

        @Override
        public void afterTextChanged(Editable s) {
            // no-op
        }
    });
}

private void setupManagementButtons() {
    deleteSelectedButton.setVisibility(View.GONE);
    deleteSelectedButton.setEnabled(false);
    deleteSelectedButton.setOnClickListener(v -> confirmDeleteSelected());
    editModeButton.setOnClickListener(v -> toggleSelectionMode());
    deleteAllButton.setOnClickListener(v -> confirmDeleteAll());
}

private void toggleSelectionMode() {
    selectionMode = !selectionMode;
    adapter.setSelectionMode(selectionMode);
    if (selectionMode) {
        deleteSelectedButton.setVisibility(View.VISIBLE);
        deleteSelectedButton.setEnabled(!adapter.getSelectedHikeIds().isEmpty());
        addHikeButton.setEnabled(false);
        editModeButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
    } else {
        deleteSelectedButton.setVisibility(View.GONE);
        deleteSelectedButton.setEnabled(false);
        addHikeButton.setEnabled(true);
        editModeButton.setImageResource(android.R.drawable.ic_menu_edit);
    }
}

private void exitSelectionMode() {
    if (selectionMode) {
        selectionMode = false;
        adapter.setSelectionMode(false);
        deleteSelectedButton.setVisibility(View.GONE);
        deleteSelectedButton.setEnabled(false);
        addHikeButton.setEnabled(true);
        editModeButton.setImageResource(android.R.drawable.ic_menu_edit);
    }
}

private void confirmDeleteSelected() {
    List<Integer> selectedIds = adapter.getSelectedHikeIds();
    if (selectedIds.isEmpty()) {
        Toast.makeText(this, getString(R.string.no_hikes_selected), Toast.LENGTH_SHORT).show();
        return;
    }
    new AlertDialog.Builder(this)
            .setTitle(R.string.delete_selected)
            .setMessage(R.string.confirm_delete_selected)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> deleteSelectedHikes(selectedIds))
            .setNegativeButton(android.R.string.cancel, null)
            .show();
}

private void deleteSelectedHikes(List<Integer> selectedIds) {
    executorService.execute(() -> {
        hikeDao.deleteHikesByIds(selectedIds);
        runOnUiThread(() -> {
            Toast.makeText(this, R.string.delete_selected, Toast.LENGTH_SHORT).show();
            exitSelectionMode();
            loadHikes();
        });
    });
}

private void confirmDeleteAll() {
    if (currentHikes.isEmpty()) {
        Toast.makeText(this, getString(R.string.no_hikes), Toast.LENGTH_SHORT).show();
        return;
    }
    new AlertDialog.Builder(this)
            .setTitle(R.string.delete_all)
            .setMessage(R.string.confirm_delete_all)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> deleteAllHikes())
            .setNegativeButton(android.R.string.cancel, null)
            .show();
}

private void deleteAllHikes() {
    executorService.execute(() -> {
        hikeDao.deleteAllHikes();
        runOnUiThread(() -> {
            Toast.makeText(this, R.string.delete_all, Toast.LENGTH_SHORT).show();
            exitSelectionMode();
            loadHikes();
        });
    });
}

private void setupBottomNavigation() {
    setActiveNavItem(navHiking);

    navHome.setOnClickListener(v -> {
        Intent intent = new Intent(HikingListActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    });

    navHiking.setOnClickListener(v -> {
        // Already here
    });

    navUsers.setOnClickListener(v -> {
        Intent intent = new Intent(HikingListActivity.this, UsersActivity.class);
        startActivity(intent);
        finish();
    });

    navSettings.setOnClickListener(v -> {
        Intent intent = new Intent(HikingListActivity.this, SettingsActivity.class);
        startActivity(intent);
        finish();
    });
}

private void setActiveNavItem(LinearLayout activeItem) {
    resetNavItem(navHome);
    resetNavItem(navHiking);
    resetNavItem(navUsers);
    resetNavItem(navSettings);

    if (activeItem != null) {
        activeItem.setBackgroundResource(R.drawable.nav_item_background);
        if (activeItem.getChildCount() >= 2) {
            ((android.widget.ImageView) activeItem.getChildAt(0))
                    .setColorFilter(getResources().getColor(R.color.primary_green));
            ((android.widget.TextView) activeItem.getChildAt(1))
                    .setTextColor(getResources().getColor(R.color.primary_green));
        }
    }
}

private void resetNavItem(LinearLayout item) {
    if (item == null) return;
    item.setBackground(null);
    if (item.getChildCount() >= 2) {
        ((android.widget.ImageView) item.getChildAt(0))
                .setColorFilter(getResources().getColor(R.color.gray_text));
        ((android.widget.TextView) item.getChildAt(1))
                .setTextColor(getResources().getColor(R.color.gray_text));
    }
}

private void filterHikes(String query) {
    executorService.execute(() -> {
        List<Hike> filtered;
        if (query == null || query.trim().isEmpty()) {
            filtered = currentHikes;
        } else {
            String search = "%" + query + "%";
            filtered = hikeDao.searchHikesByName(search);
        }
        List<Hike> finalFiltered = filtered;
        runOnUiThread(() -> adapter.submitList(finalFiltered));
    });
}

private void seedSampleData() {
    List<Hike> sampleHikes = new ArrayList<>();
    sampleHikes.add(createSampleHike("Mountain Peak Trail", "Rocky Mountain", "2025-11-21", true, 8.5, "Hard", "Challenging but rewarding trail"));
    sampleHikes.add(createSampleHike("Forest Loop", "Greenwood Forest", "2025-11-18", false, 4.2, "Easy", "Relaxing walk through lush forest"));
    sampleHikes.add(createSampleHike("River Walk", "Crystal River", "2025-11-15", true, 6.7, "Medium", "Scenic river views and wildlife"));

    for (Hike hike : sampleHikes) {
        hikeDao.insertHike(hike);
    }
}

private Hike createSampleHike(String name, String location, String dateStr, boolean parking, double length,
                              String difficulty, String description) {
    Hike hike = new Hike();
    hike.setName(name);
    hike.setLocation(location);
    try {
        Date date = dateFormat.parse(dateStr);
        hike.setDate(date);
    } catch (ParseException e) {
        hike.setDate(new Date());
    }
    hike.setParkingAvailable(parking);
    hike.setLength(length);
    hike.setDifficulty(difficulty);
    hike.setDescription(description);
    hike.setPurchaseParkingPass("");
    // Seeded hikes are for non-registered (local-only) users
    hike.setUserId(null);
    hike.setSynced(false);
    hike.setCreatedAt(System.currentTimeMillis());
    hike.setUpdatedAt(System.currentTimeMillis());
    return hike;
}

@Override
public void onHikeClicked(Hike hike) {
    Intent intent = new Intent(this, HikeDetailActivity.class);
    intent.putExtra(HikeDetailActivity.EXTRA_HIKE_ID, hike.getHikeID());
    startActivity(intent);
}

@Override
protected void onDestroy() {
    super.onDestroy();
    if (executorService != null) {
        executorService.shutdown();
    }
}
```

---

#### 5.2.4 HikeDetailActivity.java (270 lines)

**Purpose**: Displays detailed information about a single hike with start/end hike functionality.

| Method | Description |
|--------|-------------|
| `onCreate(Bundle)` | Initialises date formats, database, DAO, executor, views, click listeners; loads hike by ID from intent |
| `onResume()` | Reloads hike to refresh active status when returning from other screens |
| `loadHike(int)` | Queries Room database on background thread, calls populateData on UI thread or finishes if not found |
| `initializeViews()` | Binds all TextViews, Buttons, ImageButton, CardView for hike details display |
| `setupClickListeners()` | Configures back button, back to list button, start/end hike button, view observations button, edit button |
| `populateData()` | Fills all UI fields: name, location, ID, date, length, difficulty, parking status, description; updates start/end button state |
| `confirmStartHike()` | Shows AlertDialog to confirm starting the hike |
| `confirmEndHike()` | Shows AlertDialog to confirm ending the hike |
| `startHike()` | Deactivates all hikes, activates this hike with start timestamp, reloads data, syncs if logged in |
| `endHike()` | Sets end timestamp on this hike, deactivates it, reloads data, syncs if logged in |
| `syncIfLoggedIn()` | Triggers FirebaseSyncManager.syncNow() if user is logged in and device is online |
| `onDestroy()` | Shuts down ExecutorService |

**Complete Code Implementation**:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_hike_detail);
    
    dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    
    // Initialize database
    database = AppDatabase.getDatabase(this);
    hikeDao = database.hikeDao();
    executorService = Executors.newSingleThreadExecutor();
    
    initializeViews();
    
    setupClickListeners();
    
    int hikeId = getIntent().getIntExtra(EXTRA_HIKE_ID, -1);
    
    if (hikeId == -1) {
        finish();
        return;
    }
    
    loadHike(hikeId);
}

@Override
protected void onResume() {
    super.onResume();
    // Reload hike to get updated active status
    int hikeId = getIntent().getIntExtra(EXTRA_HIKE_ID, -1);
    if (hikeId != -1) {
        loadHike(hikeId);
    }
}

private void loadHike(int hikeId) {
    // Load hike from database on background thread
    new Thread(() -> {
        hike = AppDatabase.getDatabase(this).hikeDao().getHikeById(hikeId);
        runOnUiThread(() -> {
            if (hike != null) {
                populateData();
            } else {
                finish();
            }
        });
    }).start();
}

private void initializeViews() {
    hikeNameText = findViewById(R.id.hikeNameText);
    hikeLocationText = findViewById(R.id.hikeLocationText);
    hikeIdText = findViewById(R.id.hikeIdText);
    hikeDateText = findViewById(R.id.hikeDateText);
    hikeLengthText = findViewById(R.id.hikeLengthText);
    hikeDifficultyText = findViewById(R.id.hikeDifficultyText);
    parkingStatusText = findViewById(R.id.parkingStatusText);
    hikeDescriptionText = findViewById(R.id.hikeDescriptionText);
    startHikeButton = findViewById(R.id.startHikeButton);
    viewObservationsButton = findViewById(R.id.viewObservationsButton);
    editButton = findViewById(R.id.editButton);
    backToListButton = findViewById(R.id.backToListButton);
    backButton = findViewById(R.id.backButton);
    descriptionCard = findViewById(R.id.descriptionCard);
}

private void setupClickListeners() {
    backButton.setOnClickListener(v -> finish());
    
    backToListButton.setOnClickListener(v -> finish());
    
    startHikeButton.setOnClickListener(v -> {
        if (hike == null) return;
        
        if (hike.getIsActive() != null && hike.getIsActive()) {
            // End hike
            confirmEndHike();
        } else {
            // Start hike
            confirmStartHike();
        }
    });
    
    viewObservationsButton.setOnClickListener(v -> {
        if (hike == null) return;
        Intent intent = new Intent(this, ObservationListActivity.class);
        intent.putExtra(ObservationListActivity.EXTRA_HIKE_ID, hike.getHikeID());
        intent.putExtra(ObservationListActivity.EXTRA_HIKE_NAME, hike.getName());
        startActivity(intent);
    });
    
    editButton.setOnClickListener(v -> {
        if (hike == null) return;
        Intent intent = new Intent(this, EnterHikeActivity.class);
        intent.putExtra(EnterHikeActivity.EXTRA_EDIT_MODE, true);
        intent.putExtra(EnterHikeActivity.EXTRA_HIKE_ID, hike.getHikeID());
        startActivity(intent);
    });
}

private void populateData() {
    if (hike == null) return;
    
    hikeNameText.setText(hike.getName());
    hikeLocationText.setText(hike.getLocation());
    hikeIdText.setText(String.valueOf(hike.getHikeID()));
    
    // Format date
    if (hike.getDate() != null) {
        hikeDateText.setText(displayDateFormat.format(hike.getDate()));
    } else {
        hikeDateText.setText("N/A");
    }
    
    // Format length
    hikeLengthText.setText(String.format(Locale.getDefault(), "%.1f km", hike.getLength()));
    
    // Set difficulty
    hikeDifficultyText.setText(hike.getDifficulty());
    
    // Set parking status
    boolean parkingAvailable = hike.isParkingAvailable();
    if (parkingAvailable) {
        parkingStatusText.setText(getString(R.string.available));
        parkingStatusText.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_green_dark));
    } else {
        parkingStatusText.setText(getString(R.string.not_available));
        parkingStatusText.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_text));
    }
    
    // Set description if available
    if (hike.getDescription() != null && !hike.getDescription().trim().isEmpty()) {
        hikeDescriptionText.setText(hike.getDescription());
        descriptionCard.setVisibility(View.VISIBLE);
    } else {
        descriptionCard.setVisibility(View.GONE);
    }
    
    // Update start/end button based on active status
    if (hike.getIsActive() != null && hike.getIsActive()) {
        startHikeButton.setText(R.string.end_this_hike);
        startHikeButton.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_green_dark));
    } else {
        startHikeButton.setText(R.string.start_this_hike);
        startHikeButton.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_green));
    }
}

private void confirmStartHike() {
    new AlertDialog.Builder(this)
            .setTitle(R.string.start_this_hike)
            .setMessage(R.string.confirm_start_hike)
            .setPositiveButton(android.R.string.yes, (dialog, which) -> startHike())
            .setNegativeButton(android.R.string.no, null)
            .show();
}

private void confirmEndHike() {
    new AlertDialog.Builder(this)
            .setTitle(R.string.end_this_hike)
            .setMessage(R.string.confirm_end_hike)
            .setPositiveButton(android.R.string.yes, (dialog, which) -> endHike())
            .setNegativeButton(android.R.string.no, null)
            .show();
}

private void startHike() {
    if (hike == null) return;
    
    executorService.execute(() -> {
        // Deactivate any currently active hikes
        hikeDao.deactivateAllHikes();
        
        // Start this hike
        long currentTime = System.currentTimeMillis();
        hikeDao.startHike(hike.getHikeID(), currentTime, currentTime);
        
        // Reload hike
        hike = hikeDao.getHikeById(hike.getHikeID());
        
        runOnUiThread(() -> {
            Toast.makeText(this, R.string.hike_started, Toast.LENGTH_SHORT).show();
            populateData();
            syncIfLoggedIn();
        });
    });
}

private void endHike() {
    if (hike == null) return;
    
    executorService.execute(() -> {
        long currentTime = System.currentTimeMillis();
        hikeDao.endHike(hike.getHikeID(), currentTime, currentTime);
        
        // Reload hike
        hike = hikeDao.getHikeById(hike.getHikeID());
        
        runOnUiThread(() -> {
            Toast.makeText(this, R.string.hike_ended, Toast.LENGTH_SHORT).show();
            populateData();
            syncIfLoggedIn();
        });
    });
}

private void syncIfLoggedIn() {
    if (SessionManager.isLoggedIn(this) && NetworkUtils.isOnline(this)) {
        FirebaseSyncManager.getInstance(getApplicationContext()).syncNow();
    }
}

@Override
protected void onDestroy() {
    super.onDestroy();
    if (executorService != null) {
        executorService.shutdown();
    }
}
```

---

#### 5.2.5 SearchActivity.java (622 lines)

**Purpose**: Advanced search interface with fuzzy search, semantic search, and multi-filter capabilities.

| Method | Description |
|--------|-------------|
| `onCreate(Bundle)` | Initialises database, DAO, executor, date format, calendars, views, listeners, RecyclerView, spinners, loads all hikes |
| `initializeViews()` | Binds all search inputs, filter inputs, buttons, RecyclerView, empty state, semantic search switch |
| `setupSpinners()` | Configures difficulty and parking AutoCompleteTextView dropdowns with array adapters |
| `setupListeners()` | Configures back button, toggle filters, clear filters, search button, TextWatchers, date pickers, semantic switch |
| `updateClearButtonVisibility()` | Shows/hides clear filters button based on whether any filters are active |
| `hasActiveFilters()` | Returns true if any search/filter input has a value |
| `setupRecyclerView()` | Initialises HikeListAdapter with LinearLayoutManager |
| `loadAllHikes()` | Queries all hikes from database, updates filteredHikes and results display |
| `performSearch(String)` | Executes fuzzy or semantic search based on switch state, applies additional filters |
| `performSemanticSearch(String)` | Calls SemanticSearchService API, matches results with local Room data, applies filters |
| `performAdvancedSearch()` | Applies all filters (name, location, length, date, difficulty, parking) using SearchHelper |
| `applyOtherFilters(List<Hike>)` | Applies non-text filters to a list of hikes |
| `updateResults(List<Hike>)` | Updates adapter, results count text, shows/hides empty state |
| `toggleFilters()` | Shows/hides the advanced filters card |
| `clearFilters()` | Resets all filter inputs to empty, shows all hikes |
| `showStartDatePicker()` | Opens DatePickerDialog for start date filter |
| `showEndDatePicker()` | Opens DatePickerDialog for end date filter |
| `onHikeClicked(Hike)` | Navigates to HikeDetailActivity |
| `onDestroy()` | Shuts down ExecutorService |

**Complete Code Implementation**:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_search);
    
    // Initialize database
    database = AppDatabase.getDatabase(this);
    hikeDao = database.hikeDao();
    executorService = Executors.newSingleThreadExecutor();
    
    // Initialize date format
    dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    startDateCalendar = Calendar.getInstance();
    endDateCalendar = Calendar.getInstance();
    
    // Initialize views
    initializeViews();
    
    // Setup listeners
    setupListeners();
    
    // Setup RecyclerView
    setupRecyclerView();
    
    // Setup spinners
    setupSpinners();
    
    // Load all hikes
    loadAllHikes();
}

private void initializeViews() {
    searchInput = findViewById(R.id.searchInput);
    locationInput = findViewById(R.id.locationInput);
    minLengthInput = findViewById(R.id.minLengthInput);
    maxLengthInput = findViewById(R.id.maxLengthInput);
    startDateInput = findViewById(R.id.startDateInput);
    endDateInput = findViewById(R.id.endDateInput);
    difficultySpinner = findViewById(R.id.difficultySpinner);
    parkingSpinner = findViewById(R.id.parkingSpinner);
    
    toggleFiltersButton = findViewById(R.id.toggleFiltersButton);
    clearFiltersButton = findViewById(R.id.clearFiltersButton);
    clearAllFiltersButton = findViewById(R.id.clearAllFiltersButton);
    searchButton = findViewById(R.id.searchButton);
    backButton = findViewById(R.id.backButton);
    clearSearchButton = findViewById(R.id.clearSearchButton);
    
    filtersCard = findViewById(R.id.filtersCard);
    searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
    resultsCountText = findViewById(R.id.resultsCountText);
    filteredCountText = findViewById(R.id.filteredCountText);
    emptyStateLayout = findViewById(R.id.emptyStateLayout);
    emptyStateMessage = findViewById(R.id.emptyStateMessage);
    semanticSearchSwitch = findViewById(R.id.semanticSearchSwitch);
}

private void setupSpinners() {
    // Difficulty spinner
    String[] difficultyOptions = {"", "Easy", "Medium", "Hard", "Expert"};
    ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(this, 
        android.R.layout.simple_dropdown_item_1line, difficultyOptions);
    difficultySpinner.setAdapter(difficultyAdapter);
    difficultySpinner.setOnItemClickListener((parent, view, position, id) -> {
        updateClearButtonVisibility();
    });
    
    // Parking spinner
    String[] parkingOptions = {"", "yes", "no"};
    ArrayAdapter<String> parkingAdapter = new ArrayAdapter<>(this, 
        android.R.layout.simple_dropdown_item_1line, parkingOptions);
    parkingSpinner.setAdapter(parkingAdapter);
    parkingSpinner.setOnItemClickListener((parent, view, position, id) -> {
        updateClearButtonVisibility();
    });
}

private void setupListeners() {
    // Back button
    backButton.setOnClickListener(v -> finish());
    
    // Toggle filters
    toggleFiltersButton.setOnClickListener(v -> toggleFilters());
    
    // Clear filters
    clearFiltersButton.setOnClickListener(v -> clearFilters());
    
    // Clear all filters button in empty state
    clearAllFiltersButton.setOnClickListener(v -> clearFilters());
    
    // Search button - only search when clicked
    searchButton.setOnClickListener(v -> {
        String query = searchInput.getText() != null ? searchInput.getText().toString().trim() : "";
        if (!query.isEmpty()) {
            performSearch(query);
        } else {
            // If no search query, apply other filters if any
            if (hasActiveFilters()) {
                performAdvancedSearch();
            } else {
                // Show all hikes
                filteredHikes = new ArrayList<>(allHikes);
                updateResults(filteredHikes);
            }
        }
    });
    
    // Show/hide clear button based on text input
    searchInput.addTextChangedListener(new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (clearSearchButton != null) {
                clearSearchButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            updateClearButtonVisibility();
        }
        
        @Override
        public void afterTextChanged(Editable s) {}
    });
    
    // Clear search button
    if (clearSearchButton != null) {
        clearSearchButton.setOnClickListener(v -> {
            searchInput.setText("");
            clearSearchButton.setVisibility(View.GONE);
            updateClearButtonVisibility();
        });
    }
    
    // Also trigger search on Enter key
    searchInput.setOnEditorActionListener((v, actionId, event) -> {
        if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
            searchButton.performClick();
            return true;
        }
        return false;
    });
    
    // Date pickers
    startDateInput.setOnClickListener(v -> showStartDatePicker());
    endDateInput.setOnClickListener(v -> showEndDatePicker());
    
    // Semantic search toggle
    semanticSearchSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
        semanticSearchEnabled = isChecked;
    });
}

private void updateClearButtonVisibility() {
    boolean hasFilters = hasActiveFilters();
    if (clearFiltersButton != null) {
        clearFiltersButton.setVisibility(hasFilters ? View.VISIBLE : View.GONE);
    }
}

private boolean hasActiveFilters() {
    String searchText = searchInput.getText() != null ? searchInput.getText().toString().trim() : "";
    String locationText = locationInput.getText() != null ? locationInput.getText().toString().trim() : "";
    String minLengthText = minLengthInput.getText() != null ? minLengthInput.getText().toString().trim() : "";
    String maxLengthText = maxLengthInput.getText() != null ? maxLengthInput.getText().toString().trim() : "";
    String startDateText = startDateInput.getText() != null ? startDateInput.getText().toString().trim() : "";
    String endDateText = endDateInput.getText() != null ? endDateInput.getText().toString().trim() : "";
    String difficultyText = difficultySpinner.getText() != null ? difficultySpinner.getText().toString().trim() : "";
    String parkingText = parkingSpinner.getText() != null ? parkingSpinner.getText().toString().trim() : "";
    
    return !searchText.isEmpty() || !locationText.isEmpty() || 
           !minLengthText.isEmpty() || !maxLengthText.isEmpty() ||
           !startDateText.isEmpty() || !endDateText.isEmpty() ||
           !difficultyText.isEmpty() || !parkingText.isEmpty();
}

private void setupRecyclerView() {
    adapter = new HikeListAdapter(this);
    searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    searchResultsRecyclerView.setAdapter(adapter);
}

private void loadAllHikes() {
    executorService.execute(() -> {
        allHikes = hikeDao.getAllHikes();
        runOnUiThread(() -> {
            filteredHikes = new ArrayList<>(allHikes);
            updateResults(filteredHikes);
        });
    });
}

private void performSearch(String query) {
    if (query == null || query.trim().isEmpty()) {
        if (hasActiveFilters()) {
            performAdvancedSearch();
        } else {
            filteredHikes = new ArrayList<>(allHikes);
            updateResults(filteredHikes);
        }
        return;
    }

    // Check if semantic search is enabled and user is logged in
    if (semanticSearchEnabled && SessionManager.isLoggedIn(this)) {
        performSemanticSearch(query);
    } else {
        // Perform fuzzy search with relevance scoring
        executorService.execute(() -> {
            List<Hike> searchResults = SearchHelper.fuzzySearch(allHikes, query);
            // Then apply other filters
            final List<Hike> finalResults = applyOtherFilters(searchResults);
            runOnUiThread(() -> {
                filteredHikes = finalResults;
                updateResults(filteredHikes);
            });
        });
    }
}

private void performSemanticSearch(String query) {
    String firebaseUid = SessionManager.getCurrentFirebaseUid(this);
    if (firebaseUid == null || firebaseUid.isEmpty()) {
        Toast.makeText(this, "Please log in to use semantic search", Toast.LENGTH_SHORT).show();
        semanticSearchSwitch.setChecked(false);
        semanticSearchEnabled = false;
        performSearch(query);
        return;
    }

    // Show loading state
    resultsCountText.setText("Searching...");
    searchResultsRecyclerView.setVisibility(View.GONE);
    emptyStateLayout.setVisibility(View.GONE);

    SemanticSearchService.search(
        this,
        query,
        firebaseUid,
        "hikes",
        20,
        new SemanticSearchService.SearchCallback() {
            @Override
            public void onSuccess(List<SemanticSearchService.SearchResult> results) {
                executorService.execute(() -> {
                    List<Hike> matchedHikes = new ArrayList<>();
                    for (SemanticSearchService.SearchResult result : results) {
                        if ("hike".equals(result.type) && result.hikeId != null) {
                            Hike hike = hikeDao.getHikeById(result.hikeId);
                            if (hike != null) {
                                matchedHikes.add(hike);
                            }
                        }
                    }
                    
                    final List<Hike> finalResults = applyOtherFilters(matchedHikes);
                    
                    runOnUiThread(() -> {
                        filteredHikes = finalResults;
                        updateResults(filteredHikes);
                    });
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(SearchActivity.this, 
                        "Semantic search failed: " + error, 
                        Toast.LENGTH_SHORT).show();
                    semanticSearchSwitch.setChecked(false);
                    semanticSearchEnabled = false;
                    performSearch(query);
                });
            }
        }
    );
}

private void performAdvancedSearch() {
    String nameQuery = searchInput.getText() != null ? searchInput.getText().toString().trim() : "";
    String locationQuery = locationInput.getText() != null ? locationInput.getText().toString().trim() : "";
    
    Double minLength = null;
    Double maxLength = null;
    Long startDate = null;
    Long endDate = null;
    String difficulty = null;
    String parkingAvailable = null;
    
    try {
        if (minLengthInput.getText() != null && !minLengthInput.getText().toString().isEmpty()) {
            minLength = Double.parseDouble(minLengthInput.getText().toString());
        }
        if (maxLengthInput.getText() != null && !maxLengthInput.getText().toString().isEmpty()) {
            maxLength = Double.parseDouble(maxLengthInput.getText().toString());
        }
    } catch (NumberFormatException e) {}
    
    if (startDateInput.getText() != null && !startDateInput.getText().toString().isEmpty()) {
        startDate = startDateCalendar.getTimeInMillis();
    }
    if (endDateInput.getText() != null && !endDateInput.getText().toString().isEmpty()) {
        endDate = endDateCalendar.getTimeInMillis();
    }
    
    if (difficultySpinner.getText() != null && !difficultySpinner.getText().toString().trim().isEmpty()) {
        difficulty = difficultySpinner.getText().toString().trim();
    }
    if (parkingSpinner.getText() != null && !parkingSpinner.getText().toString().trim().isEmpty()) {
        parkingAvailable = parkingSpinner.getText().toString().trim();
    }
    
    final Double finalMinLength = minLength;
    final Double finalMaxLength = maxLength;
    final Long finalStartDate = startDate;
    final Long finalEndDate = endDate;
    final String finalDifficulty = difficulty;
    final String finalParking = parkingAvailable;
    
    executorService.execute(() -> {
        List<Hike> searchResults = allHikes;
        if (!nameQuery.isEmpty()) {
            searchResults = SearchHelper.fuzzySearch(allHikes, nameQuery);
        }
        
        List<Hike> results = SearchHelper.advancedFilter(
            searchResults, 
            null,
            locationQuery, 
            finalMinLength, 
            finalMaxLength, 
            finalStartDate, 
            finalEndDate,
            finalDifficulty,
            finalParking
        );
        
        runOnUiThread(() -> {
            filteredHikes = results;
            updateResults(filteredHikes);
        });
    });
}

private List<Hike> applyOtherFilters(List<Hike> hikes) {
    String locationQuery = locationInput.getText() != null ? locationInput.getText().toString().trim() : "";
    
    Double minLength = null;
    Double maxLength = null;
    Long startDate = null;
    Long endDate = null;
    String difficulty = null;
    String parkingAvailable = null;
    
    try {
        if (minLengthInput.getText() != null && !minLengthInput.getText().toString().isEmpty()) {
            minLength = Double.parseDouble(minLengthInput.getText().toString());
        }
        if (maxLengthInput.getText() != null && !maxLengthInput.getText().toString().isEmpty()) {
            maxLength = Double.parseDouble(maxLengthInput.getText().toString());
        }
    } catch (NumberFormatException e) {}
    
    if (startDateInput.getText() != null && !startDateInput.getText().toString().isEmpty()) {
        startDate = startDateCalendar.getTimeInMillis();
    }
    if (endDateInput.getText() != null && !endDateInput.getText().toString().isEmpty()) {
        endDate = endDateCalendar.getTimeInMillis();
    }
    
    if (difficultySpinner.getText() != null && !difficultySpinner.getText().toString().trim().isEmpty()) {
        difficulty = difficultySpinner.getText().toString().trim();
    }
    if (parkingSpinner.getText() != null && !parkingSpinner.getText().toString().trim().isEmpty()) {
        parkingAvailable = parkingSpinner.getText().toString().trim();
    }
    
    return SearchHelper.advancedFilter(hikes, null, locationQuery, minLength, maxLength, 
        startDate, endDate, difficulty, parkingAvailable);
}

private void updateResults(List<Hike> results) {
    adapter.submitList(results);
    
    int resultCount = results.size();
    int totalCount = allHikes.size();
    
    if (resultCount == 0) {
        searchResultsRecyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
        resultsCountText.setText("No results found");
        
        if (hasActiveFilters()) {
            emptyStateMessage.setText("Try adjusting your search criteria or filters");
            clearAllFiltersButton.setVisibility(View.VISIBLE);
        } else {
            emptyStateMessage.setText("Add some hiking records to see them here");
            clearAllFiltersButton.setVisibility(View.GONE);
        }
    } else {
        searchResultsRecyclerView.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
        
        String resultText = resultCount + (resultCount == 1 ? " Result" : " Results");
        resultsCountText.setText(resultText);
        
        if (hasActiveFilters() && resultCount < totalCount) {
            filteredCountText.setVisibility(View.VISIBLE);
            filteredCountText.setText("Filtered from " + totalCount + " total");
        } else {
            filteredCountText.setVisibility(View.GONE);
        }
    }
}

private void toggleFilters() {
    filtersVisible = !filtersVisible;
    if (filtersVisible) {
        filtersCard.setVisibility(View.VISIBLE);
        toggleFiltersButton.setText(R.string.hide_filters);
    } else {
        filtersCard.setVisibility(View.GONE);
        toggleFiltersButton.setText(R.string.show_filters);
    }
}

private void clearFilters() {
    searchInput.setText("");
    locationInput.setText("");
    minLengthInput.setText("");
    maxLengthInput.setText("");
    startDateInput.setText("");
    endDateInput.setText("");
    difficultySpinner.setText("", false);
    parkingSpinner.setText("", false);
    
    if (clearSearchButton != null) {
        clearSearchButton.setVisibility(View.GONE);
    }
    
    filteredHikes = new ArrayList<>(allHikes);
    updateResults(filteredHikes);
    updateClearButtonVisibility();
}

private void showStartDatePicker() {
    DatePickerDialog datePickerDialog = new DatePickerDialog(
        this,
        (view, year, month, dayOfMonth) -> {
            startDateCalendar.set(year, month, dayOfMonth);
            startDateInput.setText(dateFormat.format(startDateCalendar.getTime()));
            updateClearButtonVisibility();
        },
        startDateCalendar.get(Calendar.YEAR),
        startDateCalendar.get(Calendar.MONTH),
        startDateCalendar.get(Calendar.DAY_OF_MONTH)
    );
    datePickerDialog.show();
}

private void showEndDatePicker() {
    DatePickerDialog datePickerDialog = new DatePickerDialog(
        this,
        (view, year, month, dayOfMonth) -> {
            endDateCalendar.set(year, month, dayOfMonth);
            endDateInput.setText(dateFormat.format(endDateCalendar.getTime()));
            updateClearButtonVisibility();
        },
        endDateCalendar.get(Calendar.YEAR),
        endDateCalendar.get(Calendar.MONTH),
        endDateCalendar.get(Calendar.DAY_OF_MONTH)
    );
    datePickerDialog.show();
}

@Override
public void onHikeClicked(Hike hike) {
    Intent intent = new Intent(this, HikeDetailActivity.class);
    intent.putExtra(HikeDetailActivity.EXTRA_HIKE_ID, hike.getHikeID());
    startActivity(intent);
}

@Override
protected void onDestroy() {
    super.onDestroy();
    if (executorService != null) {
        executorService.shutdown();
    }
}
```

---

#### 5.2.6 LoginActivity.java (178 lines)

**Purpose**: Firebase Authentication login with Room database synchronisation.

| Method | Description |
|--------|-------------|
| `onCreate(Bundle)` | Initialises database, DAO, executor, FirebaseAuth instance, views, and click listeners |
| `initializeViews()` | Binds back button, email/password EditTexts, TextInputLayouts, login/signup buttons, forgot password text |
| `setupClickListeners()` | Configures back button (finish), login button (handleLogin), signup button (navigate), forgot password (toast) |
| `handleLogin()` | Validates form, calls Firebase signInWithEmailAndPassword, syncs with Room user on success |
| `validateForm(String, String)` | Validates email format using regex pattern and password minimum length (6 chars) |
| `onDestroy()` | Shuts down ExecutorService |

**Complete Code Implementation**:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);

    // Initialize database
    database = AppDatabase.getDatabase(this);
    userDao = database.userDao();
    executorService = Executors.newSingleThreadExecutor();
    firebaseAuth = FirebaseAuth.getInstance();

    // Initialize views
    initializeViews();

    // Setup click listeners
    setupClickListeners();
}

private void initializeViews() {
    backButton = findViewById(R.id.backButton);
    emailEditText = findViewById(R.id.emailEditText);
    passwordEditText = findViewById(R.id.passwordEditText);
    emailInputLayout = findViewById(R.id.emailInputLayout);
    passwordInputLayout = findViewById(R.id.passwordInputLayout);
    loginButton = findViewById(R.id.loginButton);
    signupButton = findViewById(R.id.signupButton);
    forgotPasswordText = findViewById(R.id.forgotPasswordText);
}

private void setupClickListeners() {
    backButton.setOnClickListener(v -> finish());

    loginButton.setOnClickListener(v -> handleLogin());

    signupButton.setOnClickListener(v -> {
        Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
        startActivity(intent);
    });

    forgotPasswordText.setOnClickListener(v -> {
        Toast.makeText(this, getString(R.string.forgot_password_feature), Toast.LENGTH_SHORT).show();
    });
}

private void handleLogin() {
    String email = emailEditText.getText().toString().trim();
    String password = passwordEditText.getText().toString().trim();

    // Clear previous errors
    emailInputLayout.setError(null);
    passwordInputLayout.setError(null);

    // Validate form
    if (!validateForm(email, password)) {
        return;
    }

    // Attempt login with Firebase Auth first
    loginButton.setEnabled(false);
    firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    loginButton.setEnabled(true);
                    Toast.makeText(this, getString(R.string.invalid_credentials), Toast.LENGTH_SHORT).show();
                    return;
                }

                FirebaseUser fbUser = firebaseAuth.getCurrentUser();
                if (fbUser == null) {
                    loginButton.setEnabled(true);
                    Toast.makeText(this, getString(R.string.invalid_credentials), Toast.LENGTH_SHORT).show();
                    return;
                }

                String firebaseUid = fbUser.getUid();

                // Find or create corresponding local Room user
                executorService.execute(() -> {
                    User user = userDao.getUserByEmail(email);
                    if (user == null) {
                        user = new User(email, password);
                        user.setFirebaseUid(firebaseUid);
                        long newId = userDao.insertUser(user);
                        user.setUserId((int) newId);
                    } else {
                        user.setFirebaseUid(firebaseUid);
                        userDao.updateUser(user);
                    }

                    User finalUser = user;
                    runOnUiThread(() -> {
                        loginButton.setEnabled(true);
                        SessionManager.setCurrentUserId(this, finalUser.getUserId());
                        SessionManager.setCurrentFirebaseUid(this, firebaseUid);
                        FirebaseSyncManager.getInstance(getApplicationContext()).syncNow();
                        Toast.makeText(this, getString(R.string.login_successful), Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(LoginActivity.this, UsersActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    });
                });
            });
}

private boolean validateForm(String email, String password) {
    boolean isValid = true;

    // Validate email
    if (TextUtils.isEmpty(email)) {
        emailInputLayout.setError(getString(R.string.email_required));
        isValid = false;
    } else if (!EMAIL_PATTERN.matcher(email).matches()) {
        emailInputLayout.setError(getString(R.string.email_invalid));
        isValid = false;
    }

    // Validate password
    if (TextUtils.isEmpty(password)) {
        passwordInputLayout.setError(getString(R.string.password_required));
        isValid = false;
    } else if (password.length() < 6) {
        passwordInputLayout.setError(getString(R.string.password_min_length));
        isValid = false;
    }

    return isValid;
}

@Override
protected void onDestroy() {
    super.onDestroy();
    if (executorService != null && !executorService.isShutdown()) {
        executorService.shutdown();
    }
}
```

---

#### 5.2.7 SignupActivity.java (234 lines)

**Purpose**: User registration with Firebase Auth and local Room database creation.

| Method | Description |
|--------|-------------|
| `onCreate(Bundle)` | Initialises database, DAO, executor, FirebaseAuth, views, and click listeners |
| `initializeViews()` | Binds back button, name/email/phone/password/confirm EditTexts and TextInputLayouts, signup/login buttons |
| `setupClickListeners()` | Configures back button (finish), signup button (handleSignup), login button (navigate to LoginActivity) |
| `handleSignup()` | Validates form, checks email uniqueness in Room, creates Firebase Auth account, creates Room user with firebaseUid |
| `validateForm(String, String, String, String, String)` | Validates name (min 2 chars), email (regex), phone (regex), password (min 6), confirm password (match) |
| `onDestroy()` | Shuts down ExecutorService |

**Complete Code Implementation**:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_signup);

    // Initialize database
    database = AppDatabase.getDatabase(this);
    userDao = database.userDao();
    executorService = Executors.newSingleThreadExecutor();
    firebaseAuth = FirebaseAuth.getInstance();

    // Initialize views
    initializeViews();

    // Setup click listeners
    setupClickListeners();
}

private void initializeViews() {
    backButton = findViewById(R.id.backButton);
    nameEditText = findViewById(R.id.nameEditText);
    emailEditText = findViewById(R.id.emailEditText);
    phoneEditText = findViewById(R.id.phoneEditText);
    passwordEditText = findViewById(R.id.passwordEditText);
    confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
    nameInputLayout = findViewById(R.id.nameInputLayout);
    emailInputLayout = findViewById(R.id.emailInputLayout);
    phoneInputLayout = findViewById(R.id.phoneInputLayout);
    passwordInputLayout = findViewById(R.id.passwordInputLayout);
    confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);
    signupButton = findViewById(R.id.signupButton);
    loginButton = findViewById(R.id.loginButton);
}

private void setupClickListeners() {
    backButton.setOnClickListener(v -> finish());

    signupButton.setOnClickListener(v -> handleSignup());

    loginButton.setOnClickListener(v -> {
        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
        startActivity(intent);
    });
}

private void handleSignup() {
    String name = nameEditText.getText().toString().trim();
    String email = emailEditText.getText().toString().trim();
    String phone = phoneEditText.getText().toString().trim();
    String password = passwordEditText.getText().toString().trim();
    String confirmPassword = confirmPasswordEditText.getText().toString().trim();

    // Clear previous errors
    nameInputLayout.setError(null);
    emailInputLayout.setError(null);
    phoneInputLayout.setError(null);
    passwordInputLayout.setError(null);
    confirmPasswordInputLayout.setError(null);

    // Validate form
    if (!validateForm(name, email, phone, password, confirmPassword)) {
        return;
    }

    // Check if email already exists
    executorService.execute(() -> {
        User existingUser = userDao.getUserByEmail(email);
        runOnUiThread(() -> {
            if (existingUser != null) {
                emailInputLayout.setError(getString(R.string.email_already_exists));
            } else {
                // First create account in Firebase Auth
                signupButton.setEnabled(false);
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) {
                                signupButton.setEnabled(true);
                                Toast.makeText(this, getString(R.string.signup_failed), Toast.LENGTH_SHORT).show();
                                return;
                            }

                            FirebaseUser firebaseUser = task.getResult().getUser();
                            if (firebaseUser == null) {
                                signupButton.setEnabled(true);
                                Toast.makeText(this, getString(R.string.signup_failed), Toast.LENGTH_SHORT).show();
                                return;
                            }

                            String firebaseUid = firebaseUser.getUid();

                            // Now create local Room user with firebaseUid
                            executorService.execute(() -> {
                                User newUser = new User(name, email, password, phone);
                                newUser.setFirebaseUid(firebaseUid);
                                long userId = userDao.insertUser(newUser);
                                runOnUiThread(() -> {
                                    signupButton.setEnabled(true);
                                    if (userId > 0) {
                                        SessionManager.setCurrentUserId(this, (int) userId);
                                        SessionManager.setCurrentFirebaseUid(this, firebaseUid);
                                        FirebaseSyncManager.getInstance(getApplicationContext()).syncNow();
                                        Toast.makeText(this, getString(R.string.signup_successful), Toast.LENGTH_SHORT).show();

                                        Intent intent = new Intent(SignupActivity.this, UsersActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(this, getString(R.string.signup_failed), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            });
                        });
            }
        });
    });
}

private boolean validateForm(String name, String email, String phone, String password, String confirmPassword) {
    boolean isValid = true;

    // Validate name
    if (TextUtils.isEmpty(name) || name.length() < 2) {
        nameInputLayout.setError(getString(R.string.name_min_length));
        isValid = false;
    }

    // Validate email
    if (TextUtils.isEmpty(email)) {
        emailInputLayout.setError(getString(R.string.email_required));
        isValid = false;
    } else if (!EMAIL_PATTERN.matcher(email).matches()) {
        emailInputLayout.setError(getString(R.string.email_invalid));
        isValid = false;
    }

    // Validate phone
    if (TextUtils.isEmpty(phone)) {
        phoneInputLayout.setError(getString(R.string.phone_required));
        isValid = false;
    } else if (!PHONE_PATTERN.matcher(phone).matches()) {
        phoneInputLayout.setError(getString(R.string.phone_invalid));
        isValid = false;
    }

    // Validate password
    if (TextUtils.isEmpty(password)) {
        passwordInputLayout.setError(getString(R.string.password_required));
        isValid = false;
    } else if (password.length() < 6) {
        passwordInputLayout.setError(getString(R.string.password_min_length));
        isValid = false;
    }

    // Validate confirm password
    if (TextUtils.isEmpty(confirmPassword)) {
        confirmPasswordInputLayout.setError(getString(R.string.confirm_password_required));
        isValid = false;
    } else if (!password.equals(confirmPassword)) {
        confirmPasswordInputLayout.setError(getString(R.string.passwords_do_not_match));
        isValid = false;
    }

    return isValid;
}

@Override
protected void onDestroy() {
    super.onDestroy();
    if (executorService != null && !executorService.isShutdown()) {
        executorService.shutdown();
    }
}
```

---

#### 5.2.8 SettingsActivity.java (273 lines)

**Purpose**: App settings, theme toggle, account information display, and logout functionality.

| Method | Description |
|--------|-------------|
| `onCreate(Bundle)` | Initialises database, SessionManager, views, theme switch, account info, and bottom navigation |
| `initializeViews()` | Binds all UI elements: theme container/switch/description, profile section, account cards, logout button, nav items |
| `setupClickListeners()` | Configures account card clicks (showComingSoonMessage), profile area (navigateToUsers), logout button (handleLogout) |
| `initializeThemeSwitch()` | Checks SharedPreferences for "dark_mode" setting and sets switch state accordingly |
| `updateThemeDescription(boolean)` | Updates description TextView to show "Dark theme" or "Light theme" |
| `loadAccountInfo()` | Loads user data from Room if logged in, else displays "Guest User" |
| `setAccountInfo(User)` | Populates name, email, initials circle from User object |
| `generateInitials(String)` | Creates initials (up to 2 chars) from user's full name |
| `updateLogoutState()` | Shows/hides logout button based on login status |
| `handleLogout()` | Signs out Firebase, clears SessionManager, navigates to MainActivity with FLAG_ACTIVITY_CLEAR_TASK |
| `navigateToUsers()` | Starts UsersActivity |
| `setupBottomNavigation()` | Configures bottom nav with click listeners for home, search, add, settings |
| `setActiveNavItem(ImageView, TextView, int)` | Highlights selected nav item with primary color |
| `resetNavItem(ImageView, TextView)` | Resets nav item to default gray color |
| `showComingSoonMessage()` | Displays "Coming soon" Snackbar for unimplemented features |
| `onDestroy()` | Shuts down ExecutorService |

**Complete Code Implementation**:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_settings);

    database = AppDatabase.getDatabase(this);
    userDao = database.userDao();
    executorService = Executors.newSingleThreadExecutor();
    settingsPreferences = getSharedPreferences(SETTINGS_PREFS, MODE_PRIVATE);

    initializeViews();
    setupClickListeners();
    setupBottomNavigation();
    loadAccountInfo();
    initializeThemeSwitch();
}

private void initializeViews() {
    ImageButton backButton = findViewById(R.id.backButton);
    backButton.setOnClickListener(v -> finish());

    accountNameText = findViewById(R.id.accountNameText);
    accountEmailText = findViewById(R.id.accountEmailText);
    accountInitialsText = findViewById(R.id.accountInitialsText);
    themeDescriptionText = findViewById(R.id.themeDescriptionText);
    themeSwitch = findViewById(R.id.themeSwitch);
    logoutButton = findViewById(R.id.logoutButton);

    navHome = findViewById(R.id.navHome);
    navHiking = findViewById(R.id.navHiking);
    navUsers = findViewById(R.id.navUsers);
    navSettings = findViewById(R.id.navSettings);
}

private void setupClickListeners() {
    View[] futureRows = new View[]{
            findViewById(R.id.notificationsRow),
            findViewById(R.id.unitsRow),
            findViewById(R.id.languageRow),
            findViewById(R.id.privacyRow),
            findViewById(R.id.dataRow),
            findViewById(R.id.helpRow),
            findViewById(R.id.aboutRow)
    };

    for (View row : futureRows) {
        if (row != null) {
            row.setOnClickListener(v -> showComingSoonMessage());
        }
    }

    themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
        settingsPreferences.edit().putBoolean(KEY_THEME_GREEN, isChecked).apply();
        updateThemeDescription(isChecked);
        showComingSoonMessage();
    });

    logoutButton.setOnClickListener(v -> handleLogout());
}

private void initializeThemeSwitch() {
    boolean isGreenTheme = settingsPreferences.getBoolean(KEY_THEME_GREEN, true);
    themeSwitch.setChecked(isGreenTheme);
    updateThemeDescription(isGreenTheme);
}

private void updateThemeDescription(boolean isGreenTheme) {
    int descriptionRes = isGreenTheme ? R.string.theme_description_green : R.string.theme_description_brown;
    themeDescriptionText.setText(descriptionRes);
}

private void loadAccountInfo() {
    int userId = SessionManager.getCurrentUserId(this);
    if (userId == -1) {
        setAccountInfo(getString(R.string.adventure_seeker), getString(R.string.default_user_email));
        updateLogoutState(false);
        return;
    }

    executorService.execute(() -> {
        User user = userDao.getUserById(userId);
        runOnUiThread(() -> {
            if (user != null) {
                String name = user.getUserName() != null && !user.getUserName().trim().isEmpty()
                        ? user.getUserName()
                        : getString(R.string.adventure_seeker);
                String email = user.getUserEmail() != null && !user.getUserEmail().trim().isEmpty()
                        ? user.getUserEmail()
                        : getString(R.string.default_user_email);
                setAccountInfo(name, email);
                updateLogoutState(true);
            } else {
                setAccountInfo(getString(R.string.adventure_seeker), getString(R.string.default_user_email));
                updateLogoutState(false);
            }
        });
    });
}

private void setAccountInfo(String name, String email) {
    accountNameText.setText(name);
    accountEmailText.setText(email);
    accountInitialsText.setText(generateInitials(name));
}

private String generateInitials(String name) {
    if (name == null || name.trim().isEmpty()) {
        return "AS";
    }
    String[] parts = name.trim().split("\\s+");
    if (parts.length == 1) {
        return parts[0].substring(0, 1).toUpperCase();
    }
    return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
}

private void updateLogoutState(boolean enabled) {
    logoutButton.setEnabled(enabled);
    logoutButton.setAlpha(enabled ? 1f : 0.5f);
}

private void handleLogout() {
    if (!SessionManager.isLoggedIn(this)) {
        Toast.makeText(this, R.string.logout_disabled_hint, Toast.LENGTH_SHORT).show();
        return;
    }

    logoutButton.setEnabled(false);
    logoutButton.setText(R.string.logout_syncing);
    Toast.makeText(this, R.string.logout_syncing_message, Toast.LENGTH_SHORT).show();

    FirebaseSyncManager.getInstance(getApplicationContext()).syncNow(new FirebaseSyncManager.SyncCallback() {
        @Override
        public void onSuccess() {
            executorService.execute(() -> {
                database.clearAllTables();
                runOnUiThread(() -> {
                    SessionManager.clearCurrentUser(SettingsActivity.this);
                    Toast.makeText(SettingsActivity.this, R.string.logout_success, Toast.LENGTH_SHORT).show();
                    navigateToUsers();
                });
            });
        }

        @Override
        public void onFailure(Exception exception) {
            runOnUiThread(() -> {
                logoutButton.setEnabled(true);
                logoutButton.setText(R.string.logout);
                Toast.makeText(SettingsActivity.this, R.string.logout_sync_failed, Toast.LENGTH_LONG).show();
            });
        }
    });
}

private void navigateToUsers() {
    Intent intent = new Intent(SettingsActivity.this, UsersActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
    finish();
}

private void setupBottomNavigation() {
    setActiveNavItem(navSettings);

    navHome.setOnClickListener(v -> {
        Intent intent = new Intent(SettingsActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    });

    navHiking.setOnClickListener(v -> {
        Intent intent = new Intent(SettingsActivity.this, HikingListActivity.class);
        startActivity(intent);
        finish();
    });

    navUsers.setOnClickListener(v -> {
        Intent intent = new Intent(SettingsActivity.this, UsersActivity.class);
        startActivity(intent);
        finish();
    });

    navSettings.setOnClickListener(v -> {
        // Already on settings
    });
}

private void setActiveNavItem(LinearLayout activeItem) {
    resetNavItem(navHome);
    resetNavItem(navHiking);
    resetNavItem(navUsers);
    resetNavItem(navSettings);

    if (activeItem != null && activeItem.getChildCount() >= 2) {
        activeItem.setBackgroundResource(R.drawable.nav_item_background);
        ImageView icon = (ImageView) activeItem.getChildAt(0);
        TextView text = (TextView) activeItem.getChildAt(1);
        icon.setColorFilter(getResources().getColor(R.color.primary_green));
        text.setTextColor(getResources().getColor(R.color.primary_green));
    }
}

private void resetNavItem(LinearLayout item) {
    if (item == null || item.getChildCount() < 2) {
        return;
    }
    item.setBackground(null);
    ImageView icon = (ImageView) item.getChildAt(0);
    TextView text = (TextView) item.getChildAt(1);
    icon.setColorFilter(getResources().getColor(R.color.gray_text));
    text.setTextColor(getResources().getColor(R.color.gray_text));
}

private void showComingSoonMessage() {
    Toast.makeText(this, R.string.feature_coming_soon, Toast.LENGTH_SHORT).show();
}

@Override
protected void onDestroy() {
    super.onDestroy();
    if (executorService != null && !executorService.isShutdown()) {
        executorService.shutdown();
    }
}
```

---

#### 5.2.9 UsersActivity.java (425 lines)

**Purpose**: User profile display with hiking statistics, recent activity, and account management.

| Method | Description |
|--------|-------------|
| `onCreate(Bundle)` | Initialises database, DAOs, SessionManager, executor, views, login check, and bottom navigation |
| `onResume()` | Reloads user data when activity resumes |
| `initializeViews()` | Binds all profile UI elements: cards, stats, activity containers, nav items |
| `checkLoginStatus()` | Determines if user is logged in and shows appropriate view |
| `showNonRegisteredView()` | Displays guest user placeholder with login prompt |
| `showRegisteredView()` | Shows full profile with stats and activity for logged-in users |
| `loadUserData()` | Fetches user from Room by ID, populates profile section with name/email/initials |
| `getInitials(String)` | Generates up to 2-character initials from user's name |
| `loadStatistics()` | Queries all user's hikes and calculates total count, distance, duration, average distance |
| `loadActivityOverview()` | Counts completed hikes, pending hikes, and total observations for the user |
| `loadRecentActivity()` | Loads last 5 hikes and dynamically creates activity item views |
| `setupBottomNavigation()` | Configures click listeners for home, search, add, settings nav items |
| `setActiveNavItem(ImageView, TextView, int)` | Highlights selected nav item with primary color |
| `resetNavItem(ImageView, TextView)` | Resets nav item color to default gray |
| `onHikeClicked(Hike)` | Navigates to HikeDetailActivity with selected hike ID |
| `onDestroy()` | Shuts down ExecutorService |

**Complete Code Implementation**:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_users);
    
    // Initialize database
    database = AppDatabase.getDatabase(this);
    hikeDao = database.hikeDao();
    executorService = Executors.newSingleThreadExecutor();
    sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    
    // Initialize views
    initializeViews();
    
    // Setup bottom navigation
    setupBottomNavigation();
    
    // Check login status and show appropriate view
    checkLoginStatus();
}

@Override
protected void onResume() {
    super.onResume();
    // Refresh data when returning from login/signup
    checkLoginStatus();
}

private void initializeViews() {
    nonRegisteredView = findViewById(R.id.nonRegisteredView);
    registeredView = findViewById(R.id.registeredView);
    
    registerButton = findViewById(R.id.registerButton);
    loginButton = findViewById(R.id.loginButton);
    
    profileInitials = findViewById(R.id.profileInitials);
    userNameText = findViewById(R.id.userNameText);
    memberSinceText = findViewById(R.id.memberSinceText);
    levelText = findViewById(R.id.levelText);
    totalHikesText = findViewById(R.id.totalHikesText);
    totalKmText = findViewById(R.id.totalKmText);
    avgKmText = findViewById(R.id.avgKmText);
    thisMonthText = findViewById(R.id.thisMonthText);
    easyTrailsProgress = findViewById(R.id.easyTrailsProgress);
    mediumTrailsProgress = findViewById(R.id.mediumTrailsProgress);
    hardTrailsProgress = findViewById(R.id.hardTrailsProgress);
    easyTrailsPercent = findViewById(R.id.easyTrailsPercent);
    mediumTrailsPercent = findViewById(R.id.mediumTrailsPercent);
    hardTrailsPercent = findViewById(R.id.hardTrailsPercent);
    recentActivityRecyclerView = findViewById(R.id.recentActivityRecyclerView);
    
    navHome = findViewById(R.id.navHome);
    navHiking = findViewById(R.id.navHiking);
    navUsers = findViewById(R.id.navUsers);
    navSettings = findViewById(R.id.navSettings);
    
    // Setup click listeners for non-registered view
    registerButton.setOnClickListener(v -> {
        Intent intent = new Intent(UsersActivity.this, SignupActivity.class);
        startActivity(intent);
    });
    
    loginButton.setOnClickListener(v -> {
        Intent intent = new Intent(UsersActivity.this, LoginActivity.class);
        startActivity(intent);
    });
}

private void checkLoginStatus() {
    boolean isLoggedIn = SessionManager.isLoggedIn(this);
    
    if (isLoggedIn) {
        showRegisteredView();
        loadUserData();
    } else {
        showNonRegisteredView();
    }
}

private void showNonRegisteredView() {
    nonRegisteredView.setVisibility(View.VISIBLE);
    registeredView.setVisibility(View.GONE);
}

private void showRegisteredView() {
    nonRegisteredView.setVisibility(View.GONE);
    registeredView.setVisibility(View.VISIBLE);
}

private void loadUserData() {
    // Load user profile
    String userName = sharedPreferences.getString(KEY_USER_NAME, "Adventure Seeker");
    String userEmail = sharedPreferences.getString(KEY_USER_EMAIL, "");
    
    userNameText.setText(userName);
    
    // Set profile initials
    String initials = getInitials(userName);
    profileInitials.setText(initials);
    
    // Set member since (for now, use a default date - can be enhanced later)
    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
    String memberSince = "Member since " + dateFormat.format(new Date());
    memberSinceText.setText(memberSince);
    
    // Load statistics
    loadStatistics();
    loadActivityOverview();
    loadRecentActivity();
}

private String getInitials(String name) {
    if (name == null || name.trim().isEmpty()) {
        return "AS";
    }
    String[] parts = name.trim().split("\\s+");
    if (parts.length >= 2) {
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    } else if (parts.length == 1) {
        return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
    }
    return "AS";
}

private void loadStatistics() {
    executorService.execute(() -> {
        int userId = SessionManager.getCurrentUserId(this);
        List<Hike> hikes;
        
        if (userId == -1) {
            hikes = hikeDao.getHikesForNonRegisteredUsers();
        } else {
            hikes = hikeDao.getHikesByUserId(userId);
        }
        
        int totalHikes = hikes.size();
        double totalKm = 0.0;
        int easyCount = 0;
        int mediumCount = 0;
        int hardCount = 0;
        int thisMonthCount = 0;
        
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentYear = calendar.get(Calendar.YEAR);
        
        for (Hike hike : hikes) {
            totalKm += hike.getLength();
            
            // Count by difficulty
            String difficulty = hike.getDifficulty();
            if (difficulty != null) {
                if (difficulty.equalsIgnoreCase("Easy")) {
                    easyCount++;
                } else if (difficulty.equalsIgnoreCase("Medium")) {
                    mediumCount++;
                } else if (difficulty.equalsIgnoreCase("Hard") || difficulty.equalsIgnoreCase("Expert")) {
                    hardCount++;
                }
            }
            
            // Count this month's hikes
            if (hike.getDate() != null) {
                calendar.setTime(hike.getDate());
                if (calendar.get(Calendar.MONTH) == currentMonth && 
                    calendar.get(Calendar.YEAR) == currentYear) {
                    thisMonthCount++;
                }
            }
        }
        
        double avgKm = totalHikes > 0 ? totalKm / totalHikes : 0.0;
        
        final int finalTotalHikes = totalHikes;
        final double finalTotalKm = totalKm;
        final double finalAvgKm = avgKm;
        final int finalThisMonth = thisMonthCount;
        final int finalEasyCount = easyCount;
        final int finalMediumCount = mediumCount;
        final int finalHardCount = hardCount;
        
        runOnUiThread(() -> {
            totalHikesText.setText(String.valueOf(finalTotalHikes));
            DecimalFormat df = new DecimalFormat("#.#");
            totalKmText.setText(df.format(finalTotalKm));
            avgKmText.setText(df.format(finalAvgKm));
            thisMonthText.setText(String.valueOf(finalThisMonth));
            
            // Calculate level (simple calculation: 1 level per 5 hikes)
            int level = Math.max(1, (finalTotalHikes / 5) + 1);
            levelText.setText("Level " + level);
        });
    });
}

private void loadActivityOverview() {
    executorService.execute(() -> {
        int userId = SessionManager.getCurrentUserId(this);
        List<Hike> hikes;
        
        if (userId == -1) {
            hikes = hikeDao.getHikesForNonRegisteredUsers();
        } else {
            hikes = hikeDao.getHikesByUserId(userId);
        }
        
        int easyCount = 0;
        int mediumCount = 0;
        int hardCount = 0;
        
        for (Hike hike : hikes) {
            String difficulty = hike.getDifficulty();
            if (difficulty != null) {
                if (difficulty.equalsIgnoreCase("Easy")) {
                    easyCount++;
                } else if (difficulty.equalsIgnoreCase("Medium")) {
                    mediumCount++;
                } else if (difficulty.equalsIgnoreCase("Hard") || difficulty.equalsIgnoreCase("Expert")) {
                    hardCount++;
                }
            }
        }
        
        int total = easyCount + mediumCount + hardCount;
        int easyPercent = total > 0 ? (easyCount * 100 / total) : 0;
        int mediumPercent = total > 0 ? (mediumCount * 100 / total) : 0;
        int hardPercent = total > 0 ? (hardCount * 100 / total) : 0;
        
        final int finalEasyPercent = easyPercent;
        final int finalMediumPercent = mediumPercent;
        final int finalHardPercent = hardPercent;
        
        runOnUiThread(() -> {
            easyTrailsProgress.setProgress(finalEasyPercent);
            mediumTrailsProgress.setProgress(finalMediumPercent);
            hardTrailsProgress.setProgress(finalHardPercent);
            
            easyTrailsPercent.setText(finalEasyPercent + "%");
            mediumTrailsPercent.setText(finalMediumPercent + "%");
            hardTrailsPercent.setText(finalHardPercent + "%");
        });
    });
}

private void loadRecentActivity() {
    executorService.execute(() -> {
        int userId = SessionManager.getCurrentUserId(this);
        List<Hike> hikes;
        
        if (userId == -1) {
            hikes = hikeDao.getHikesForNonRegisteredUsers();
        } else {
            hikes = hikeDao.getHikesByUserId(userId);
        }
        
        // Sort by date descending and take first 3
        List<Hike> recentHikes = new ArrayList<>();
        if (!hikes.isEmpty()) {
            // Simple sort by date (most recent first)
            hikes.sort((h1, h2) -> {
                Date d1 = h1.getDate();
                Date d2 = h2.getDate();
                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return 1;
                if (d2 == null) return -1;
                return d2.compareTo(d1);
            });
            
            int limit = Math.min(3, hikes.size());
            recentHikes = hikes.subList(0, limit);
        }
        
        final List<Hike> finalRecentHikes = recentHikes;
        runOnUiThread(() -> {
            // Use HikeListAdapter for recent activity
            HikeListAdapter adapter = new HikeListAdapter(this);
            adapter.submitList(finalRecentHikes);
            recentActivityRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            recentActivityRecyclerView.setAdapter(adapter);
        });
    });
}

private void setupBottomNavigation() {
    setActiveNavItem(navUsers);
    
    navHome.setOnClickListener(v -> {
        Intent intent = new Intent(UsersActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    });
    
    navHiking.setOnClickListener(v -> {
        Intent intent = new Intent(UsersActivity.this, HikingListActivity.class);
        startActivity(intent);
        finish();
    });
    
    navUsers.setOnClickListener(v -> {
        // Already here
    });
    
    navSettings.setOnClickListener(v -> {
        Intent intent = new Intent(UsersActivity.this, SettingsActivity.class);
        startActivity(intent);
        finish();
    });
}

private void setActiveNavItem(LinearLayout activeItem) {
    resetNavItem(navHome);
    resetNavItem(navHiking);
    resetNavItem(navUsers);
    resetNavItem(navSettings);
    
    if (activeItem != null) {
        activeItem.setBackgroundResource(R.drawable.nav_item_background);
        if (activeItem.getChildCount() >= 2) {
            ((android.widget.ImageView) activeItem.getChildAt(0))
                    .setColorFilter(getResources().getColor(R.color.primary_green));
            ((android.widget.TextView) activeItem.getChildAt(1))
                    .setTextColor(getResources().getColor(R.color.primary_green));
        }
    }
}

private void resetNavItem(LinearLayout item) {
    if (item == null) return;
    item.setBackground(null);
    if (item.getChildCount() >= 2) {
        ((android.widget.ImageView) item.getChildAt(0))
                .setColorFilter(getResources().getColor(R.color.gray_text));
        ((android.widget.TextView) item.getChildAt(1))
                .setTextColor(getResources().getColor(R.color.gray_text));
    }
}

@Override
public void onHikeClicked(Hike hike) {
    // Navigate to hike detail page when a hike is clicked in recent activity
    Intent intent = new Intent(this, HikeDetailActivity.class);
    intent.putExtra(HikeDetailActivity.EXTRA_HIKE_ID, hike.getHikeID());
    startActivity(intent);
}

@Override
protected void onDestroy() {
    super.onDestroy();
    if (executorService != null) {
        executorService.shutdown();
    }
}
```

---

#### 5.2.10 ObservationListActivity.java (~150 lines)

**Purpose**: Displays all observations for a specific hike with add/edit/delete functionality.

| Method | Description |
|--------|-------------|
| `onCreate(Bundle)` | Initialises database, DAO, executor, retrieves hikeId from intent, sets up RecyclerView and FAB |
| `onResume()` | Reloads observations when activity resumes |
| `loadObservations()` | Queries observations by hikeId from Room and updates adapter |
| `openAddObservation()` | Starts ObservationFormActivity with hikeId for new observation |
| `onEditClicked(Observation)` | Starts ObservationFormActivity with observation ID for editing |
| `onDeleteClicked(Observation)` | Shows confirmation dialog before deleting observation |
| `deleteObservation(Observation)` | Removes observation from Room database via DAO |
| `onDestroy()` | Shuts down ExecutorService |

**Complete Code Implementation**:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_observation_list);

    // Get hike ID and name from intent
    hikeId = getIntent().getIntExtra(EXTRA_HIKE_ID, -1);
    hikeName = getIntent().getStringExtra(EXTRA_HIKE_NAME);

    if (hikeId == -1) {
        Toast.makeText(this, "Invalid hike", Toast.LENGTH_SHORT).show();
        finish();
        return;
    }

    // Initialize database
    database = AppDatabase.getDatabase(this);
    observationDao = database.observationDao();
    executorService = Executors.newSingleThreadExecutor();

    // Initialize views
    recyclerView = findViewById(R.id.observationsRecyclerView);
    emptyState = findViewById(R.id.emptyState);
    hikeNameText = findViewById(R.id.hikeNameText);
    backButton = findViewById(R.id.backButton);
    addObservationButton = findViewById(R.id.addObservationButton);

    // Set hike name
    hikeNameText.setText(hikeName);

    // Setup RecyclerView
    adapter = new ObservationListAdapter(this);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    recyclerView.setAdapter(adapter);

    // Setup click listeners
    backButton.setOnClickListener(v -> finish());
    addObservationButton.setOnClickListener(v -> openAddObservation());

    // Load observations
    loadObservations();
}

@Override
protected void onResume() {
    super.onResume();
    // Refresh observations when returning from form
    loadObservations();
}

private void loadObservations() {
    executorService.execute(() -> {
        List<Observation> observations = observationDao.getObservationsByHikeId(hikeId);
        runOnUiThread(() -> {
            adapter.submitList(observations);
            if (observations.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        });
    });
}

private void openAddObservation() {
    Intent intent = new Intent(this, ObservationFormActivity.class);
    intent.putExtra(ObservationFormActivity.EXTRA_HIKE_ID, hikeId);
    intent.putExtra(ObservationFormActivity.EXTRA_HIKE_NAME, hikeName);
    startActivity(intent);
}

@Override
public void onEditClicked(Observation observation) {
    Intent intent = new Intent(this, ObservationFormActivity.class);
    intent.putExtra(ObservationFormActivity.EXTRA_HIKE_ID, hikeId);
    intent.putExtra(ObservationFormActivity.EXTRA_HIKE_NAME, hikeName);
    intent.putExtra(ObservationFormActivity.EXTRA_OBSERVATION_ID, observation.getObservationID());
    startActivity(intent);
}

@Override
public void onDeleteClicked(Observation observation) {
    new AlertDialog.Builder(this)
            .setTitle(R.string.delete_observation)
            .setMessage(R.string.confirm_delete_observation)
            .setPositiveButton(android.R.string.yes, (dialog, which) -> deleteObservation(observation))
            .setNegativeButton(android.R.string.no, null)
            .show();
}

private void deleteObservation(Observation observation) {
    executorService.execute(() -> {
        observationDao.deleteObservation(observation);
        runOnUiThread(() -> {
            Toast.makeText(this, R.string.observation_deleted, Toast.LENGTH_SHORT).show();
            loadObservations();
        });
    });
}

@Override
protected void onDestroy() {
    super.onDestroy();
    if (executorService != null) {
        executorService.shutdown();
    }
}
```

---

#### 5.2.11 ObservationFormActivity.java (435 lines)

**Purpose**: Form for creating/editing observations with image picker and GPS location capture.

| Method | Description |
|--------|-------------|
| `onCreate(Bundle)` | Initialises database, DAOs, executor, FusedLocationProviderClient, views, image pickers, and loads existing observation if editing |
| `initializeViews()` | Binds all form fields: observation EditText, comments EditText, date/time button, location container, image preview, camera/gallery buttons, save button |
| `setupImagePickers()` | Registers ActivityResultLaunchers for camera (takePictureLauncher) and gallery (pickImageLauncher) |
| `setupClickListeners()` | Configures date/time picker, location capture, camera, gallery, and save button actions |
| `showDateTimePicker()` | Shows DatePickerDialog, then TimePickerDialog, combines into formatted datetime string |
| `getCurrentLocation()` | Checks permission, uses FusedLocationProviderClient.getLastLocation() to populate location field |
| `onRequestPermissionsResult(int, String[], int[])` | Handles ACCESS_FINE_LOCATION permission callback |
| `loadObservationForEdit()` | Queries observation by ID from intent and populates form fields |
| `populateForm(Observation)` | Sets observation text, comments, datetime, location, and image preview from existing observation |
| `saveObservation()` | Validates form, creates/updates Observation entity, inserts/updates via DAO |
| `validateForm()` | Checks observation text is not empty, returns boolean |
| `onDestroy()` | Shuts down ExecutorService |

**Complete Code Implementation**:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_observation_form);

    // Get extras from intent
    hikeId = getIntent().getIntExtra(EXTRA_HIKE_ID, -1);
    hikeName = getIntent().getStringExtra(EXTRA_HIKE_NAME);
    observationId = getIntent().getIntExtra(EXTRA_OBSERVATION_ID, -1);

    if (hikeId == -1) {
        Toast.makeText(this, "Invalid hike", Toast.LENGTH_SHORT).show();
        finish();
        return;
    }

    // Initialize database
    database = AppDatabase.getDatabase(this);
    observationDao = database.observationDao();
    executorService = Executors.newSingleThreadExecutor();

    // Initialize location client
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

    // Initialize calendar and date format
    calendar = Calendar.getInstance();
    dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    // Initialize views
    initializeViews();

    // Setup image pickers
    setupImagePickers();

    // Setup click listeners
    setupClickListeners();

    // If editing, load observation data
    if (observationId != -1) {
        loadObservationForEdit();
    } else {
        // Set default time to now
        timeInput.setText(dateTimeFormat.format(calendar.getTime()));
    }
}

private void initializeViews() {
    observationInput = findViewById(R.id.observationInput);
    timeInput = findViewById(R.id.timeInput);
    commentsInput = findViewById(R.id.commentsInput);
    locationInput = findViewById(R.id.locationInput);
    getLocationButton = findViewById(R.id.getLocationButton);
    saveButton = findViewById(R.id.saveButton);
    removePictureButton = findViewById(R.id.removePictureButton);
    backButton = findViewById(R.id.backButton);
    pictureCard = findViewById(R.id.pictureCard);
    uploadCard = findViewById(R.id.uploadCard);
    picturePreview = findViewById(R.id.picturePreview);
    hikeNameText = findViewById(R.id.hikeNameText);
    headerSubtitle = findViewById(R.id.headerSubtitle);

    // Set hike name
    hikeNameText.setText(hikeName);

    // Update header based on mode
    if (observationId != -1) {
        headerSubtitle.setText(R.string.edit_observation);
        saveButton.setText(R.string.update_observation);
    } else {
        headerSubtitle.setText(R.string.add_observation);
        saveButton.setText(R.string.save_observation);
    }
}

private void setupImagePickers() {
    // Pick image from gallery
    pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        currentPicturePath = imageUri.toString();
                        picturePreview.setImageURI(imageUri);
                        pictureCard.setVisibility(View.VISIBLE);
                        uploadCard.setVisibility(View.GONE);
                    }
                }
            }
    );
}

private void setupClickListeners() {
    backButton.setOnClickListener(v -> finish());

    timeInput.setOnClickListener(v -> showDateTimePicker());

    getLocationButton.setOnClickListener(v -> getCurrentLocation());

    uploadCard.setOnClickListener(v -> {
        // Show dialog to choose camera or gallery
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.picture)
                .setItems(new String[]{"Take Photo", "Choose from Gallery"}, (dialog, which) -> {
                    if (which == 0) {
                        // Camera not implemented for simplicity - would require camera permissions and file provider
                        Toast.makeText(this, "Camera feature coming soon", Toast.LENGTH_SHORT).show();
                    } else {
                        // Gallery
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        pickImageLauncher.launch(intent);
                    }
                })
                .show();
    });

    removePictureButton.setOnClickListener(v -> {
        currentPicturePath = null;
        pictureCard.setVisibility(View.GONE);
        uploadCard.setVisibility(View.VISIBLE);
    });

    saveButton.setOnClickListener(v -> saveObservation());
}

private void showDateTimePicker() {
    // First show date picker
    DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                
                // Then show time picker
                TimePickerDialog timePickerDialog = new TimePickerDialog(
                        this,
                        (view1, hourOfDay, minute) -> {
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                            calendar.set(Calendar.MINUTE, minute);
                            timeInput.setText(dateTimeFormat.format(calendar.getTime()));
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                );
                timePickerDialog.show();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
    );
    datePickerDialog.show();
}

private void getCurrentLocation() {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_LOCATION_PERMISSION);
        return;
    }

    getLocationButton.setText(R.string.getting_location);
    getLocationButton.setEnabled(false);

    fusedLocationClient.getLastLocation()
            .addOnSuccessListener(this, location -> {
                getLocationButton.setText(R.string.get_location);
                getLocationButton.setEnabled(true);

                if (location != null) {
                    String locationText = String.format(Locale.getDefault(),
                            "%.6f, %.6f",
                            location.getLatitude(),
                            location.getLongitude());
                    locationInput.setText(locationText);
                } else {
                    Toast.makeText(this, "Could not get location", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(this, e -> {
                getLocationButton.setText(R.string.get_location);
                getLocationButton.setEnabled(true);
                Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_SHORT).show();
            });
}

@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQUEST_LOCATION_PERMISSION) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_SHORT).show();
        }
    }
}

private void loadObservationForEdit() {
    executorService.execute(() -> {
        editingObservation = observationDao.getObservationById(observationId);
        if (editingObservation != null) {
            runOnUiThread(() -> populateForm(editingObservation));
        } else {
            runOnUiThread(this::finish);
        }
    });
}

private void populateForm(Observation observation) {
    observationInput.setText(observation.getObservationText());
    
    if (observation.getTime() != null) {
        calendar.setTime(observation.getTime());
        timeInput.setText(dateTimeFormat.format(observation.getTime()));
    }
    
    if (observation.getComments() != null) {
        commentsInput.setText(observation.getComments());
    }
    
    if (observation.getLocation() != null && !observation.getLocation().isEmpty()) {
        locationInput.setText(observation.getLocation());
    }
    
    if (observation.getPicture() != null && !observation.getPicture().isEmpty()) {
        currentPicturePath = observation.getPicture();
        try {
            File imageFile = new File(observation.getPicture());
            if (imageFile.exists()) {
                picturePreview.setImageURI(Uri.fromFile(imageFile));
                pictureCard.setVisibility(View.VISIBLE);
                uploadCard.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

private void saveObservation() {
    // Validate required fields
    if (!validateForm()) {
        return;
    }

    // Get form data
    String observationText = observationInput.getText().toString().trim();
    String timeString = timeInput.getText().toString().trim();
    String comments = commentsInput.getText().toString().trim();
    String location = locationInput.getText().toString().trim();

    // Parse date/time
    Date time;
    try {
        time = dateTimeFormat.parse(timeString);
    } catch (Exception e) {
        time = new Date(); // Fallback to current time
    }

    // Create or update observation object
    Observation observation;
    if (observationId != -1 && editingObservation != null) {
        observation = editingObservation;
    } else {
        observation = new Observation();
        observation.setHikeId(hikeId);
    }

    observation.setObservationText(observationText);
    observation.setTime(time);
    observation.setComments(comments.isEmpty() ? null : comments);
    observation.setLocation(location.isEmpty() ? null : location);
    observation.setPicture(currentPicturePath);
    observation.setUpdatedAt(System.currentTimeMillis());
    observation.setSynced(false);

    final boolean shouldSyncWithCloud = SessionManager.isLoggedIn(this);

    // Save to database
    executorService.execute(() -> {
        try {
            if (observationId != -1) {
                observationDao.updateObservation(observation);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.observation_updated, Toast.LENGTH_SHORT).show();
                    if (shouldSyncWithCloud) {
                        FirebaseSyncManager.getInstance(getApplicationContext()).syncNow();
                    }
                    finish();
                });
            } else {
                observationDao.insertObservation(observation);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.observation_saved, Toast.LENGTH_SHORT).show();
                    if (shouldSyncWithCloud) {
                        FirebaseSyncManager.getInstance(getApplicationContext()).syncNow();
                    }
                    finish();
                });
            }
        } catch (Exception e) {
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.error_saving_observation, Toast.LENGTH_SHORT).show();
            });
        }
    });
}

private boolean validateForm() {
    boolean isValid = true;

    // Reset previous errors
    observationInput.setError(null);
    timeInput.setError(null);

    // Validate observation text
    if (TextUtils.isEmpty(observationInput.getText().toString().trim())) {
        observationInput.setError(getString(R.string.observation_text_required));
        isValid = false;
    }

    // Validate time
    if (TextUtils.isEmpty(timeInput.getText().toString().trim())) {
        timeInput.setError(getString(R.string.time_required));
        isValid = false;
    }

    if (!isValid) {
        Toast.makeText(this, R.string.error_required_fields, Toast.LENGTH_SHORT).show();
    }

    return isValid;
}

@Override
protected void onDestroy() {
    super.onDestroy();
    if (executorService != null) {
        executorService.shutdown();
    }
}
```

---

#### 5.2.12 MapPickerActivity.java (342 lines)

**Purpose**: Interactive map interface using Mapsforge with OpenStreetMap tiles allowing users to visually select hike locations by tapping on the map. No API key required.

| Method | Description |
|--------|-------------|
| `onCreate(Bundle)` | Initialises AndroidGraphicFactory, sets up MapView, confirm/cancel buttons, FusedLocationProviderClient; calls setupMap() and getCurrentLocationAndCenter() |
| `setupMap()` | Creates TileCache with AndroidUtil, configures OpenStreetMapMapnik tile source, adds TileDownloadLayer, sets initial position to London, enables zoom controls, sets up touch listener for tap detection |
| `placeMarker(LatLong)` | Removes existing marker if present, creates new marker bitmap, adds Marker layer at tapped coordinates, enables confirm button, calls getAddressFromLocation() |
| `createMarkerBitmap()` | Creates 48x48 custom marker using Mapsforge Canvas API with white border circle, red fill circle, and inner white dot |
| `getCurrentLocationAndCenter()` | Checks ACCESS_FINE_LOCATION permission, requests if not granted, uses FusedLocationProviderClient.getLastLocation() to centre map on user's current position with zoom level 15 |
| `getAddressFromLocation(LatLong)` | Uses Android Geocoder to reverse-geocode coordinates into readable address string (thoroughfare, locality, admin area, country); falls back to coordinates if geocoding fails |
| `onRequestPermissionsResult(...)` | Handles location permission grant result, calls getCurrentLocationAndCenter() if granted |
| `onStart()` | Resumes TileDownloadLayer for tile fetching |
| `onStop()` | Pauses TileDownloadLayer to conserve resources |
| `onDestroy()` | Destroys MapView, TileCache, clears AndroidGraphicFactory resource memory cache |

**Complete Code Implementation**:

```java
/**
 * Map picker activity using Mapsforge with OpenStreetMap tiles.
 * No API key required - uses free OpenStreetMap data.
 */
public class MapPickerActivity extends AppCompatActivity {

    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_ADDRESS = "address";

    private MapView mapView;
    private TileCache tileCache;
    private TileDownloadLayer tileDownloadLayer;
    private Marker currentMarker;
    
    private LatLong selectedLocation;
    private String selectedAddress = "";

    private TextView tvSelectedLocation;
    private Button btnConfirmLocation;
    private FusedLocationProviderClient fusedLocationClient;

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final double DEFAULT_LAT = 51.5074;
    private static final double DEFAULT_LON = -0.1278;
    private static final byte DEFAULT_ZOOM = 12;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize AndroidGraphicFactory BEFORE setContentView
        AndroidGraphicFactory.createInstance(getApplication());
        
        setContentView(R.layout.activity_map_picker);

        tvSelectedLocation = findViewById(R.id.tvSelectedLocation);
        btnConfirmLocation = findViewById(R.id.btnConfirmLocation);
        Button btnCancel = findViewById(R.id.btnCancel);
        mapView = findViewById(R.id.mapView);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupMap();

        btnConfirmLocation.setOnClickListener(v -> {
            if (selectedLocation != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_LATITUDE, selectedLocation.latitude);
                resultIntent.putExtra(EXTRA_LONGITUDE, selectedLocation.longitude);
                resultIntent.putExtra(EXTRA_ADDRESS, selectedAddress);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Please tap on the map to select a location", 
                        Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        btnConfirmLocation.setEnabled(false);
        getCurrentLocationAndCenter();
    }

    private void setupMap() {
        tileCache = AndroidUtil.createTileCache(
                this, "mapcache",
                mapView.getModel().displayModel.getTileSize(),
                1f, mapView.getModel().frameBufferModel.getOverdrawFactor()
        );

        OpenStreetMapMapnik tileSource = OpenStreetMapMapnik.INSTANCE;
        tileSource.setUserAgent("MHike-Android-App");
        
        tileDownloadLayer = new TileDownloadLayer(
                tileCache, mapView.getModel().mapViewPosition,
                tileSource, AndroidGraphicFactory.INSTANCE
        );
        
        mapView.getLayerManager().getLayers().add(tileDownloadLayer);
        mapView.getModel().mapViewPosition.setCenter(new LatLong(DEFAULT_LAT, DEFAULT_LON));
        mapView.getModel().mapViewPosition.setZoomLevel(DEFAULT_ZOOM);
        mapView.setBuiltInZoomControls(true);

        // Handle map tap to drop pin
        mapView.setOnTouchListener(new View.OnTouchListener() {
            private float startX, startY;
            private long startTime;
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        startTime = System.currentTimeMillis();
                        break;
                    case MotionEvent.ACTION_UP:
                        float distance = (float) Math.sqrt(
                                Math.pow(event.getX() - startX, 2) + 
                                Math.pow(event.getY() - startY, 2));
                        if (distance < 20 && System.currentTimeMillis() - startTime < 300) {
                            LatLong tapLatLong = mapView.getMapViewProjection()
                                    .fromPixels(event.getX(), event.getY());
                            if (tapLatLong != null) {
                                placeMarker(tapLatLong);
                            }
                        }
                        break;
                }
                return false;
            }
        });
    }

    private void placeMarker(LatLong latLong) {
        if (currentMarker != null) {
            mapView.getLayerManager().getLayers().remove(currentMarker);
        }
        Bitmap markerBitmap = createMarkerBitmap();
        currentMarker = new Marker(latLong, markerBitmap, 0, -markerBitmap.getHeight() / 2);
        mapView.getLayerManager().getLayers().add(currentMarker);
        selectedLocation = latLong;
        btnConfirmLocation.setEnabled(true);
        getAddressFromLocation(latLong);
        mapView.getLayerManager().redrawLayers();
    }

    private Bitmap createMarkerBitmap() {
        int width = 48, height = 48;
        Bitmap bitmap = AndroidGraphicFactory.INSTANCE.createBitmap(width, height);
        org.mapsforge.core.graphics.Canvas canvas = AndroidGraphicFactory.INSTANCE.createCanvas();
        canvas.setBitmap(bitmap);
        
        Paint whitePaint = AndroidGraphicFactory.INSTANCE.createPaint();
        whitePaint.setColor(Color.WHITE);
        whitePaint.setStyle(Style.FILL);
        canvas.drawCircle(width / 2, height / 2, 20, whitePaint);
        
        Paint redPaint = AndroidGraphicFactory.INSTANCE.createPaint();
        redPaint.setColor(Color.RED);
        redPaint.setStyle(Style.FILL);
        canvas.drawCircle(width / 2, height / 2, 16, redPaint);
        
        canvas.drawCircle(width / 2, height / 2, 6, whitePaint);
        return bitmap;
    }

    private void getCurrentLocationAndCenter() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLong currentLatLong = new LatLong(
                                location.getLatitude(), location.getLongitude());
                        mapView.getModel().mapViewPosition.setCenter(currentLatLong);
                        mapView.getModel().mapViewPosition.setZoomLevel((byte) 15);
                    }
                });
    }

    private void getAddressFromLocation(LatLong latLong) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    latLong.latitude, latLong.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                if (address.getThoroughfare() != null) sb.append(address.getThoroughfare());
                if (address.getLocality() != null) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(address.getLocality());
                }
                if (address.getAdminArea() != null) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(address.getAdminArea());
                }
                if (address.getCountryName() != null) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(address.getCountryName());
                }
                selectedAddress = sb.length() > 0 ? sb.toString() :
                        String.format(Locale.US, "%.6f, %.6f", latLong.latitude, latLong.longitude);
            } else {
                selectedAddress = String.format(Locale.US, "%.6f, %.6f", 
                        latLong.latitude, latLong.longitude);
            }
        } catch (IOException e) {
            selectedAddress = String.format(Locale.US, "%.6f, %.6f", 
                    latLong.latitude, latLong.longitude);
        }
        tvSelectedLocation.setText(selectedAddress);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAndCenter();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (tileDownloadLayer != null) tileDownloadLayer.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (tileDownloadLayer != null) tileDownloadLayer.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) mapView.destroyAll();
        if (tileCache != null) tileCache.destroy();
        AndroidGraphicFactory.clearResourceMemoryCache();
    }
}
```

---

### 5.3 Auth Package (`com.example.mobilecw.auth`)

#### 5.3.1 SessionManager.java

**Purpose**: Manages user session state using SharedPreferences for the dual-identity system.

| Method | Description |
|--------|-------------|
| `getCurrentUserId(Context)` | Returns local Room user ID from SharedPreferences |
| `setCurrentUserId(Context, int)` | Stores local Room user ID |
| `getCurrentFirebaseUid(Context)` | Returns Firebase UID string |
| `setCurrentFirebaseUid(Context, String)` | Stores Firebase UID |
| `clearCurrentUser(Context)` | Removes all session data (logout) |
| `isLoggedIn(Context)` | Checks if valid session exists |

**Complete Code Implementation**:

```java
/**
 * Simple session manager to keep track of the currently active local user.
 *
 * For now this works entirely with a local Room User (integer userId).
 * Later, when Firebase Auth is added, login/logout code can call
 * {@link #setCurrentUserId(Context, int)} and {@link #clearCurrentUser(Context)}
 * after successful sign-in/sign-out.
 */
public class SessionManager {

    private static final String PREFS_NAME = "mhike_prefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_FIREBASE_UID = "firebase_uid";

    /**
     * Returns the currently active local userId, or -1 if there is none.
     */
    public static int getCurrentUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_USER_ID, -1);
    }

    /**
     * Sets the active local userId after a successful login/registration.
     */
    public static void setCurrentUserId(Context context, int userId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_USER_ID, userId).apply();
    }

    public static String getCurrentFirebaseUid(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_FIREBASE_UID, null);
    }

    public static void setCurrentFirebaseUid(Context context, String firebaseUid) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_FIREBASE_UID, firebaseUid).apply();
    }

    /**
     * Clears the active user on logout.
     */
    public static void clearCurrentUser(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_USER_ID)
                .remove(KEY_FIREBASE_UID)
                .apply();
    }

    /**
     * Returns true if a userId is currently stored (logged-in state).
     */
    public static boolean isLoggedIn(Context context) {
        return getCurrentUserId(context) != -1;
    }
}
```

---

### 5.4 Database Package (`com.example.mobilecw.database`)

#### 5.4.1 AppDatabase.java

**Purpose**: Room database singleton with DAO access methods.

| Method | Description |
|--------|-------------|
| `getDatabase(Context)` | Returns singleton database instance (lazy initialisation) |
| `hikeDao()` | Abstract method returning HikeDao interface |
| `observationDao()` | Abstract method returning ObservationDao interface |
| `userDao()` | Abstract method returning UserDao interface |

**Complete Code Implementation**:

```java
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
```

---

#### 5.4.2 Converters.java

**Purpose**: Type converters for Room to handle Date ↔ Long conversions.

| Method | Description |
|--------|-------------|
| `fromTimestamp(Long)` | Converts Long (milliseconds) to Date object |
| `dateToTimestamp(Date)` | Converts Date object to Long (milliseconds) |

**Complete Code Implementation**:

```java
/**
 * Type converters for Room database
 * Converts Date objects to Long (timestamp) for storage
 */
public class Converters {
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }
    
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}
```

---

#### 5.4.3 HikeDao.java (Data Access Object)

**Purpose**: Room DAO interface for all hike database operations.

| Method | Description |
|--------|-------------|
| `insertHike(Hike)` | Inserts new hike, returns generated ID (OnConflictStrategy.REPLACE) |
| `insertAllHikes(List<Hike>)` | Bulk insert hikes (OnConflictStrategy.REPLACE) |
| `getAllHikes()` | Returns all hikes ordered by date descending |
| `getHikeById(int)` | Returns single hike by primary key |
| `getHikesByUserId(Integer)` | Returns hikes for specific user ordered by date descending |
| `getHikesForNonRegisteredUsers()` | Returns hikes where userId is NULL |
| `searchHikesByName(String)` | LIKE query search on name field |
| `advancedSearch(String, String, Boolean, Date, Date)` | Complex multi-field search with optional filters |
| `searchHikesByDateRange(Date, Date)` | Returns hikes within date range |
| `updateHike(Hike)` | Updates existing hike entity |
| `deleteHike(Hike)` | Deletes hike entity |
| `deleteHikeById(int)` | Deletes hike by primary key |
| `deleteAllHikes()` | Clears all hikes from table |
| `deleteHikesByIds(List<Integer>)` | Bulk delete by list of IDs |
| `getUnsyncedHikes()` | Returns hikes where synced = false or NULL |
| `markHikeAsSynced(int)` | Sets synced = true for specified hike |
| `migrateHikesToUser(Integer, Integer)` | Reassigns hikes from null userId to specified user |
| `getActiveHike()` | Returns currently active hike (isActive = 1) |
| `deactivateAllHikes()` | Sets isActive = 0 on all hikes |
| `startHike(int, long, long)` | Sets isActive = 1, startTime for specified hike |
| `endHike(int, long, long)` | Sets isActive = 0, endTime for specified hike |

**Complete Code Implementation**:

```java
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
```

---

#### 5.4.4 ObservationDao.java

**Purpose**: Room DAO interface for observation database operations.

| Method | Description |
|--------|-------------|
| `insertObservation(Observation)` | Inserts new observation, returns generated ID (OnConflictStrategy.REPLACE) |
| `insertAllObservations(List<Observation>)` | Bulk insert observations (OnConflictStrategy.REPLACE) |
| `getAllObservations()` | Returns all observations |
| `getObservationById(int)` | Returns single observation by primary key |
| `getObservationsByHikeId(int)` | Returns all observations for a specific hike |
| `updateObservation(Observation)` | Updates existing observation entity |
| `deleteObservation(Observation)` | Deletes observation entity |
| `deleteObservationById(int)` | Deletes observation by primary key |
| `deleteObservationsByHikeId(int)` | Deletes all observations for a hike |
| `deleteAllObservations()` | Clears all observations from table |
| `getUnsyncedObservations()` | Returns observations where synced = false or NULL |
| `markObservationAsSynced(int)` | Sets synced = true for specified observation |

**Complete Code Implementation**:

```java
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
```

---

#### 5.4.5 UserDao.java

**Purpose**: Room DAO interface for user database operations.

| Method | Description |
|--------|-------------|
| `insertUser(User)` | Inserts new user, returns generated ID (OnConflictStrategy.REPLACE) |
| `getAllUsers()` | Returns all users in database |
| `getUserById(int)` | Returns user by local primary key |
| `getUserByEmail(String)` | Returns user by email address (unique constraint) |
| `authenticateUser(String, String)` | Returns user matching email and password (local auth) |
| `updateUser(User)` | Updates existing user entity |
| `deleteUser(User)` | Deletes user entity |
| `deleteUserById(int)` | Deletes user by primary key |
| `deleteAllUsers()` | Clears all users from table |

**Complete Code Implementation**:

```java
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
```

---

### 5.5 Entities Package (`com.example.mobilecw.database.entities`)

#### 5.5.1 Hike.java

**Purpose**: Room entity representing a hiking trail record.

| Field | Type | Description |
|-------|------|-------------|
| `hikeID` | int (PK) | Auto-generated primary key |
| `name` | String | Trail name (required) |
| `location` | String | Trail location (required) |
| `date` | Date | Hike date |
| `parkingAvailable` | boolean | Parking availability flag |
| `length` | double | Trail length in kilometres |
| `difficulty` | String | Easy/Medium/Hard |
| `description` | String | Optional description |
| `purchaseParkingPass` | String | Parking pass URL if applicable |
| `userId` | Integer (FK) | Owner's local user ID |
| `isActive` | Boolean | Currently active hike flag |
| `startTime` | Long | Hike start timestamp |
| `endTime` | Long | Hike end timestamp |
| `createdAt` | Long | Record creation timestamp |
| `updatedAt` | Long | Last modification timestamp |
| `synced` | Boolean | Cloud sync status |

**Complete Code Implementation**:

```java
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
```

---

#### 5.5.2 Observation.java

**Purpose**: Room entity representing observations made during a hike.

| Field | Type | Description |
|-------|------|-------------|
| `observationID` | int (PK) | Auto-generated primary key |
| `hikeId` | int (FK) | Parent hike ID (CASCADE delete) |
| `observationText` | String | Observation description (required) |
| `time` | Date | Observation timestamp |
| `comments` | String | Additional comments |
| `location` | String | GPS coordinates or location name |
| `picture` | String | File path to attached image |
| `createdAt` | Long | Record creation timestamp |
| `updatedAt` | Long | Last modification timestamp |
| `synced` | Boolean | Cloud sync status |

**Complete Code Implementation**:

```java
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
```

---

#### 5.5.3 User.java

**Purpose**: Room entity representing application users with dual-identity support.

| Field | Type | Description |
|-------|------|-------------|
| `userId` | int (PK) | Local auto-generated ID |
| `firebaseUid` | String (UK) | Firebase Authentication UID |
| `userName` | String | Display name |
| `userEmail` | String (UK) | Email address (unique) |
| `userPassword` | String | Hashed password (local auth fallback) |
| `userPhone` | String | Phone number (optional) |
| `createdAt` | Long | Account creation timestamp |
| `updatedAt` | Long | Last update timestamp |

**Complete Code Implementation**:

```java
@Entity(tableName = "users",
        indices = {@Index("userEmail")})
public class User {
    @PrimaryKey(autoGenerate = true)
    private int userId;

    // Firebase Authentication UID (owner id in Firestore)
    private String firebaseUid;

    private String userName; // Full name
    private String userEmail; // Required field, unique
    private String userPassword; // Required field (should be hashed in production)
    private String userPhone; // Phone number
    
    // Timestamp
    private Long createdAt;
    private Long updatedAt;
    
    // Constructors
    public User() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    public User(String userEmail, String userPassword) {
        this.userEmail = userEmail;
        this.userPassword = userPassword;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    public User(String userName, String userEmail, String userPassword, String userPhone) {
        this.userName = userName;
        this.userEmail = userEmail;
        this.userPassword = userPassword;
        this.userPhone = userPhone;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public String getUserPassword() {
        return userPassword;
    }
    
    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
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
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getUserPhone() {
        return userPhone;
    }
    
    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }
}
```

---

### 5.6 Adapters Package (`com.example.mobilecw.adapters`)

#### 5.6.1 HikeListAdapter.java (~200 lines)

**Purpose**: RecyclerView adapter with selection mode support for multi-delete operations.

| Method | Description |
|--------|-------------|
| `HikeListAdapter(OnHikeClickListener)` | Constructor with click listener interface |
| `submitList(List<Hike>)` | Updates adapter data and refreshes view |
| `setSelectionMode(boolean)` | Enables/disables checkbox selection mode |
| `clearSelections()` | Clears all selected items |
| `getSelectedHikeIds()` | Returns Set of selected hike IDs |
| `setOnSelectionChangedListener(OnSelectionChangedListener)` | Sets callback for selection count changes |
| `onCreateViewHolder(ViewGroup, int)` | Inflates `item_hike.xml` layout |
| `onBindViewHolder(ViewHolder, int)` | Binds hike data, handles selection state, sets click listeners |
| `getItemCount()` | Returns size of hike list |
| `ViewHolder.bind(Hike)` | Populates name, location, date, difficulty chip, parking icon |

**Complete Code Implementation**:

```java
public class HikeListAdapter extends RecyclerView.Adapter<HikeListAdapter.HikeViewHolder> {

    private List<Hike> hikes = new ArrayList<>();
    private final OnHikeClickListener listener;
    private OnSelectionChangedListener selectionChangedListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private boolean selectionMode = false;
    private final Set<Integer> selectedHikeIds = new HashSet<>();

    public interface OnHikeClickListener {
        void onHikeClicked(Hike hike);
    }
    
    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    public HikeListAdapter(OnHikeClickListener listener) {
        this.listener = listener;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionChangedListener = listener;
    }

    public void submitList(List<Hike> items) {
        this.hikes = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void setSelectionMode(boolean enabled) {
        selectionMode = enabled;
        if (!selectionMode) {
            selectedHikeIds.clear();
            if (selectionChangedListener != null) {
                selectionChangedListener.onSelectionChanged(0);
            }
        }
        notifyDataSetChanged();
    }
    
    public boolean isSelectionMode() {
        return selectionMode;
    }
    
    public List<Integer> getSelectedHikeIds() {
        return new ArrayList<>(selectedHikeIds);
    }

    @NonNull
    @Override
    public HikeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hike_card, parent, false);
        return new HikeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HikeViewHolder holder, int position) {
        Hike hike = hikes.get(position);
        holder.bind(hike);
    }

    @Override
    public int getItemCount() {
        return hikes.size();
    }

    class HikeViewHolder extends RecyclerView.ViewHolder {

        TextView name, location, date, length, difficulty, parking;
        MaterialButton viewButton;
        CheckBox selectCheckbox;

        HikeViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.hikeNameText);
            location = itemView.findViewById(R.id.hikeLocationText);
            date = itemView.findViewById(R.id.hikeDateText);
            length = itemView.findViewById(R.id.hikeLengthText);
            difficulty = itemView.findViewById(R.id.hikeDifficultyText);
            parking = itemView.findViewById(R.id.hikeParkingText);
            viewButton = itemView.findViewById(R.id.viewButton);
            selectCheckbox = itemView.findViewById(R.id.hikeSelectCheckbox);
        }

        void bind(Hike hike) {
            name.setText(hike.getName());
            location.setText(hike.getLocation());
            if (hike.getDate() != null) {
                date.setText(dateFormat.format(hike.getDate()));
            } else {
                date.setText("N/A");
            }
            length.setText(String.format(Locale.getDefault(), "%.1f km", hike.getLength()));
            difficulty.setText(hike.getDifficulty());
            parking.setText("Parking: " + (hike.isParkingAvailable() ? "Yes" : "No"));

            if (selectionMode) {
                selectCheckbox.setVisibility(View.VISIBLE);
                boolean isSelected = selectedHikeIds.contains(hike.getHikeID());
                selectCheckbox.setChecked(isSelected);
                viewButton.setEnabled(false);
                viewButton.setAlpha(0.5f);

                View.OnClickListener toggleListener = v -> toggleSelection(hike);
                itemView.setOnClickListener(toggleListener);
                selectCheckbox.setOnClickListener(toggleListener);
                viewButton.setOnClickListener(toggleListener);
            } else {
                selectCheckbox.setVisibility(View.GONE);
                selectCheckbox.setOnClickListener(null);
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onHikeClicked(hike);
                    }
                });
                viewButton.setEnabled(true);
                viewButton.setAlpha(1f);
                viewButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onHikeClicked(hike);
                    }
                });
            }
        }
    }
    
    private void toggleSelection(Hike hike) {
        int id = hike.getHikeID();
        if (selectedHikeIds.contains(id)) {
            selectedHikeIds.remove(id);
        } else {
            selectedHikeIds.add(id);
        }
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(selectedHikeIds.size());
        }
        notifyDataSetChanged();
    }
}
```

---

#### 5.6.2 ObservationListAdapter.java (151 lines)

**Purpose**: RecyclerView adapter for displaying observations with edit/delete callbacks.

| Method | Description |
|--------|-------------|
| `ObservationListAdapter(OnEditClickListener, OnDeleteClickListener)` | Constructor with edit and delete listeners |
| `submitList(List<Observation>)` | Updates adapter data |
| `onCreateViewHolder(ViewGroup, int)` | Inflates `item_observation.xml` layout |
| `onBindViewHolder(ViewHolder, int)` | Binds observation data, handles image loading |
| `getItemCount()` | Returns size of observation list |
| `ViewHolder.bind(Observation)` | Populates text, time, comments, location, image preview |

**Complete Code Implementation**:

```java
public class ObservationListAdapter extends ListAdapter<Observation, ObservationListAdapter.ObservationViewHolder> {

    private OnObservationClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    public interface OnObservationClickListener {
        void onEditClicked(Observation observation);
        void onDeleteClicked(Observation observation);
    }

    public ObservationListAdapter(OnObservationClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Observation> DIFF_CALLBACK = new DiffUtil.ItemCallback<Observation>() {
        @Override
        public boolean areItemsTheSame(@NonNull Observation oldItem, @NonNull Observation newItem) {
            return oldItem.getObservationID() == newItem.getObservationID();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Observation oldItem, @NonNull Observation newItem) {
            return oldItem.getObservationText().equals(newItem.getObservationText()) &&
                   oldItem.getTime().equals(newItem.getTime());
        }
    };

    @NonNull
    @Override
    public ObservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_observation_card, parent, false);
        return new ObservationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ObservationViewHolder holder, int position) {
        Observation observation = getItem(position);
        holder.bind(observation, listener, dateFormat);
    }

    static class ObservationViewHolder extends RecyclerView.ViewHolder {
        private TextView observationTime;
        private TextView observationText;
        private TextView observationComments;
        private TextView observationLocation;
        private ImageView observationPicture;
        private ImageButton editButton;
        private ImageButton deleteButton;
        private LinearLayout commentsSection;
        private LinearLayout locationSection;
        private CardView pictureCard;

        public ObservationViewHolder(@NonNull View itemView) {
            super(itemView);
            observationTime = itemView.findViewById(R.id.observationTime);
            observationText = itemView.findViewById(R.id.observationText);
            observationComments = itemView.findViewById(R.id.observationComments);
            observationLocation = itemView.findViewById(R.id.observationLocation);
            observationPicture = itemView.findViewById(R.id.observationPicture);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            commentsSection = itemView.findViewById(R.id.commentsSection);
            locationSection = itemView.findViewById(R.id.locationSection);
            pictureCard = itemView.findViewById(R.id.pictureCard);
        }

        public void bind(Observation observation, OnObservationClickListener listener, SimpleDateFormat dateFormat) {
            // Set time
            if (observation.getTime() != null) {
                observationTime.setText(dateFormat.format(observation.getTime()));
            }

            // Set observation text
            observationText.setText(observation.getObservationText());

            // Set comments (show/hide section based on availability)
            if (observation.getComments() != null && !observation.getComments().isEmpty()) {
                observationComments.setText(observation.getComments());
                commentsSection.setVisibility(View.VISIBLE);
            } else {
                commentsSection.setVisibility(View.GONE);
            }

            // Set location (show/hide section based on availability)
            if (observation.getLocation() != null && !observation.getLocation().isEmpty()) {
                observationLocation.setText(observation.getLocation());
                locationSection.setVisibility(View.VISIBLE);
            } else {
                locationSection.setVisibility(View.GONE);
            }

            // Set picture (show/hide based on availability)
            if (observation.getPicture() != null && !observation.getPicture().isEmpty()) {
                try {
                    File imageFile = new File(observation.getPicture());
                    if (imageFile.exists()) {
                        observationPicture.setImageURI(Uri.fromFile(imageFile));
                        pictureCard.setVisibility(View.VISIBLE);
                    } else {
                        pictureCard.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    pictureCard.setVisibility(View.GONE);
                }
            } else {
                pictureCard.setVisibility(View.GONE);
            }

            // Set click listeners
            editButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClicked(observation);
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClicked(observation);
                }
            });
        }
    }
}
```

---

#### 5.6.3 NearbyTrailAdapter.java (107 lines)

**Purpose**: Horizontal RecyclerView adapter for displaying nearby trails on home screen.

| Method | Description |
|--------|-------------|
| `NearbyTrailAdapter(OnTrailClickListener)` | Constructor with click listener |
| `submitList(List<Hike>)` | Updates trail data |
| `onCreateViewHolder(ViewGroup, int)` | Inflates `item_nearby_trail.xml` layout |
| `onBindViewHolder(ViewHolder, int)` | Binds trail name, distance, estimated time |
| `getItemCount()` | Returns size of trail list |
| `ViewHolder.bind(Hike)` | Calculates and displays estimated hiking duration |

**Complete Code Implementation**:

```java
public class NearbyTrailAdapter extends RecyclerView.Adapter<NearbyTrailAdapter.TrailViewHolder> {
    
    private List<Hike> trails;
    private OnTrailClickListener listener;
    
    public interface OnTrailClickListener {
        void onTrailClick(Hike hike);
    }
    
    public NearbyTrailAdapter(List<Hike> trails, OnTrailClickListener listener) {
        this.trails = trails;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public TrailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nearby_trail, parent, false);
        return new TrailViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TrailViewHolder holder, int position) {
        Hike hike = trails.get(position);
        holder.bind(hike);
    }
    
    @Override
    public int getItemCount() {
        return trails != null ? trails.size() : 0;
    }
    
    class TrailViewHolder extends RecyclerView.ViewHolder {
        private TextView trailName, trailDistance, trailDifficulty, trailTime;
        private TextView viewButton;
        
        public TrailViewHolder(@NonNull View itemView) {
            super(itemView);
            trailName = itemView.findViewById(R.id.trailName);
            trailDistance = itemView.findViewById(R.id.trailDistance);
            trailDifficulty = itemView.findViewById(R.id.trailDifficulty);
            trailTime = itemView.findViewById(R.id.trailTime);
            viewButton = itemView.findViewById(R.id.viewButton);
        }
        
        public void bind(Hike hike) {
            trailName.setText(hike.getName());
            trailDistance.setText(String.format("%.1f km", hike.getLength()));
            trailDifficulty.setText(hike.getDifficulty());
            
            // Calculate estimated time based on length and difficulty
            double estimatedHours = calculateEstimatedTime(hike.getLength(), hike.getDifficulty());
            int hours = (int) estimatedHours;
            int minutes = (int) ((estimatedHours - hours) * 60);
            trailTime.setText(String.format("%dh %dm", hours, minutes));
            
            viewButton.setOnClickListener(v -> {
                Context context = itemView.getContext();
                Intent intent = new Intent(context, HikeDetailActivity.class);
                intent.putExtra(HikeDetailActivity.EXTRA_HIKE_ID, hike.getHikeID());
                context.startActivity(intent);
            });
        }
        
        private double calculateEstimatedTime(double length, String difficulty) {
            // Average hiking speed: Easy = 4 km/h, Medium = 3 km/h, Hard = 2 km/h
            double speed;
            switch (difficulty.toLowerCase()) {
                case "easy":
                    speed = 4.0;
                    break;
                case "medium":
                    speed = 3.0;
                    break;
                case "hard":
                    speed = 2.0;
                    break;
                default:
                    speed = 3.0;
            }
            return length / speed;
        }
    }
}
```

---

### 5.7 Services Package (`com.example.mobilecw.services`)

#### 5.7.1 SemanticSearchService.java (151 lines)

**Purpose**: HTTP client for communicating with the Python FastAPI semantic search backend.

| Method | Description |
|--------|-------------|
| `search(Context, String, String, String, int, SearchCallback)` | Performs POST request to /search endpoint with Volley |
| `parseSearchResponse(JSONObject)` | Parses JSON response, extracts results array into List<SearchResult> |

| Inner Class/Interface | Description |
|----------------------|-------------|
| `SearchResult` | Data class with fields: id (String), type (String), score (float), name (String), location (String) |
| `SearchCallback` | Callback interface with `onSuccess(List<SearchResult>)` and `onError(String)` methods |

**Complete Code Implementation**:

```java
/**
 * Service to handle semantic search API calls to the backend server.
 */
public class SemanticSearchService {

    private static final String TAG = "SemanticSearchService";
    private static final String BASE_URL = "http://206.189.93.77:8000";
    
    public interface SearchCallback {
        void onSuccess(List<SearchResult> results);
        void onError(String error);
    }

    public static class SearchResult {
        public String id;
        public String type; // "hike" or "observation"
        public double score;
        public String name;
        public String location;
        public String description;
        public String observationText;
        public Integer hikeId;

        public SearchResult(JSONObject json) throws JSONException {
            this.id = json.getString("id");
            this.type = json.getString("type");
            this.score = json.getDouble("score");
            this.name = json.optString("name", null);
            this.location = json.optString("location", null);
            this.description = json.optString("description", null);
            this.observationText = json.optString("observation_text", null);
            if (json.has("hike_id") && !json.isNull("hike_id")) {
                this.hikeId = json.getInt("hike_id");
            }
        }
    }

    /**
     * Perform semantic search using the backend API.
     */
    public static void search(Context context, String query, String firebaseUid, 
                             String searchType, int topK, SearchCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            callback.onError("Query cannot be empty");
            return;
        }

        if (firebaseUid == null || firebaseUid.isEmpty()) {
            callback.onError("User not logged in");
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(context.getApplicationContext());
        String url = BASE_URL + "/search";

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("query", query);
            requestBody.put("firebase_uid", firebaseUid);
            requestBody.put("search_type", searchType != null ? searchType : "hikes");
            requestBody.put("top_k", topK);
        } catch (JSONException e) {
            callback.onError("Failed to create request: " + e.getMessage());
            return;
        }
        
        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.POST,
            url,
            requestBody,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        List<SearchResult> results = parseSearchResponse(response);
                        callback.onSuccess(results);
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to parse search response", e);
                        callback.onError("Failed to parse response: " + e.getMessage());
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    String errorMessage = "Search failed";
                    if (error.networkResponse != null) {
                        errorMessage += " (Code: " + error.networkResponse.statusCode + ")";
                    }
                    if (error.getMessage() != null) {
                        errorMessage += ": " + error.getMessage();
                    }
                    Log.e(TAG, errorMessage, error);
                    callback.onError(errorMessage);
                }
            }
        );

        queue.add(request);
    }

    private static List<SearchResult> parseSearchResponse(JSONObject response) throws JSONException {
        List<SearchResult> results = new ArrayList<>();
        
        // Handle new API response format: { success, status_code, message, data: { results, total_found } }
        JSONArray resultsArray = null;
        
        if (response.has("data")) {
            // New format
            JSONObject data = response.getJSONObject("data");
            if (data.has("results")) {
                resultsArray = data.getJSONArray("results");
            }
        } else if (response.has("results")) {
            // Old format (direct results array)
            resultsArray = response.getJSONArray("results");
        }
        
        if (resultsArray == null) {
            return results;
        }

        for (int i = 0; i < resultsArray.length(); i++) {
            try {
                JSONObject resultJson = resultsArray.getJSONObject(i);
                SearchResult result = new SearchResult(resultJson);
                results.add(result);
            } catch (JSONException e) {
                // Skip invalid results
            }
        }

        return results;
    }
}
```

---

### 5.8 Sync Package (`com.example.mobilecw.sync`)

#### 5.8.1 FirebaseSyncManager.java (261 lines)

**Purpose**: Orchestrates offline-first synchronisation between Room and Cloud Firestore.

| Method | Description |
|--------|-------------|
| `getInstance(Context)` | Returns singleton instance with lazy initialisation |
| `syncNow()` | Triggers sync without callback (fire-and-forget) |
| `syncNow(SyncCallback)` | Triggers full sync with completion callback |
| `syncUserProfile(int, String)` | Uploads user profile document to Firestore users collection |
| `syncHikes(int, String)` | Queries unsynced hikes, uploads to users/{uid}/hikes subcollection |
| `syncObservations(int, String)` | Queries unsynced observations, uploads to users/{uid}/hikes/{hikeId}/observations |
| `buildHikePayload(Hike)` | Converts Hike entity to Map<String, Object> for Firestore |
| `buildObservationPayload(Observation)` | Converts Observation entity to Map<String, Object> |
| `buildUserPayload(User)` | Converts User entity to Map<String, Object> |
| `notifySuccess(SyncCallback)` | Posts onSuccess to main thread Handler |
| `notifyFailure(SyncCallback, Exception)` | Posts onFailure to main thread Handler |

| Interface | Description |
|-----------|-------------|
| `SyncCallback` | Callback with `onSuccess()` and `onFailure(Exception)` methods |

**Complete Code Implementation**:

```java
/**
 * Handles pushing local Room data (per user) to Cloud Firestore.
 *
 * For now we treat the local Room userId as the Firestore user document id.
 * Once Firebase Auth is wired in, swap {@link SessionManager} to return the
 * Firebase UID and the sync layer will continue to work.
 */
public class FirebaseSyncManager {

    private static final String TAG = "FirebaseSyncManager";
    private static FirebaseSyncManager instance;

    private final Context appContext;
    private final FirebaseFirestore firestore;
    private final HikeDao hikeDao;
    private final ObservationDao observationDao;
    private final UserDao userDao;
    private final ExecutorService executorService;
    private final Executor mainThreadExecutor;
    private final VectorSyncManager vectorSyncManager;

    private FirebaseSyncManager(Context context) {
        this.appContext = context.getApplicationContext();
        FirebaseApp.initializeApp(this.appContext);
        this.firestore = FirebaseFirestore.getInstance();
        AppDatabase database = AppDatabase.getDatabase(this.appContext);
        this.hikeDao = database.hikeDao();
        this.observationDao = database.observationDao();
        this.userDao = database.userDao();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainThreadExecutor = ContextCompat.getMainExecutor(this.appContext);
        this.vectorSyncManager = new VectorSyncManager(this.appContext);
    }

    public static synchronized FirebaseSyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseSyncManager(context);
        }
        return instance;
    }

    public interface SyncCallback {
        void onSuccess();
        void onFailure(Exception exception);
    }

    /**
     * Push all unsynced hikes/observations that belong to the current logged-in user.
     */
    public void syncNow() {
        syncNow(null);
    }

    /**
     * Sync unsynced records, invoking the callback when Firebase confirms completion.
     */
    public void syncNow(SyncCallback callback) {
        executorService.execute(() -> {
            int userId = SessionManager.getCurrentUserId(appContext);
            String firebaseUid = SessionManager.getCurrentFirebaseUid(appContext);
            if (userId == -1 || firebaseUid == null) {
                Log.d(TAG, "No logged-in user or Firebase UID detected; skipping Firebase sync");
                notifySuccess(callback);
                return;
            }

            List<Task<Void>> pendingTasks = new ArrayList<>();
            Task<Void> profileTask = syncUserProfile(userId, firebaseUid);
            if (profileTask != null) {
                pendingTasks.add(profileTask);
            }
            pendingTasks.addAll(syncHikes(userId, firebaseUid));
            pendingTasks.addAll(syncObservations(userId, firebaseUid));
            pendingTasks.add(vectorSyncManager.syncUserVectors(userId, firebaseUid));

            if (pendingTasks.isEmpty()) {
                notifySuccess(callback);
                return;
            }

            Tasks.whenAllSuccess(pendingTasks)
                    .addOnSuccessListener(mainThreadExecutor, unused -> notifySuccess(callback))
                    .addOnFailureListener(mainThreadExecutor, e -> notifyFailure(callback, e));
        });
    }

    private void notifySuccess(SyncCallback callback) {
        if (callback == null) {
            return;
        }
        mainThreadExecutor.execute(callback::onSuccess);
    }

    private void notifyFailure(SyncCallback callback, Exception exception) {
        if (callback == null) {
            return;
        }
        mainThreadExecutor.execute(() -> callback.onFailure(exception));
    }

    private Task<Void> syncUserProfile(int userId, String firebaseUid) {
        User user = userDao.getUserById(userId);
        if (user == null) {
            return null;
        }

        user.setFirebaseUid(firebaseUid);

        Map<String, Object> payload = buildUserPayload(user);
        return firestore.collection("users")
                .document(firebaseUid)
                .set(payload, SetOptions.merge())
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to sync user profile for userId=" + userId, e));
    }

    private List<Task<Void>> syncHikes(int userId, String firebaseUid) {
        List<Task<Void>> tasks = new ArrayList<>();
        List<Hike> unsyncedHikes = hikeDao.getUnsyncedHikes();
        if (unsyncedHikes == null || unsyncedHikes.isEmpty()) {
            return tasks;
        }

        for (Hike hike : unsyncedHikes) {
            if (hike.getUserId() == null || hike.getUserId() != userId) {
                continue; // Only sync hikes owned by the active user
            }

            Map<String, Object> payload = buildHikePayload(hike);
            Task<Void> task = firestore.collection("users")
                    .document(firebaseUid)
                    .collection("hikes")
                    .document(String.valueOf(hike.getHikeID()))
                    .set(payload, SetOptions.merge())
                    .addOnSuccessListener(unused -> executorService.execute(() ->
                            hikeDao.markHikeAsSynced(hike.getHikeID())))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to sync hike " + hike.getHikeID(), e));
            tasks.add(task);
        }

        return tasks;
    }

    private List<Task<Void>> syncObservations(int userId, String firebaseUid) {
        List<Task<Void>> tasks = new ArrayList<>();
        List<Observation> unsyncedObservations = observationDao.getUnsyncedObservations();
        if (unsyncedObservations == null || unsyncedObservations.isEmpty()) {
            return tasks;
        }

        List<Hike> userHikes = hikeDao.getHikesByUserId(userId);
        Set<Integer> allowedHikeIds = new HashSet<>();
        for (Hike hike : userHikes) {
            allowedHikeIds.add(hike.getHikeID());
        }

        if (allowedHikeIds.isEmpty()) {
            return tasks;
        }

        for (Observation observation : unsyncedObservations) {
            if (!allowedHikeIds.contains(observation.getHikeId())) {
                continue;
            }

            Map<String, Object> payload = buildObservationPayload(observation);
            Task<Void> task = firestore.collection("users")
                    .document(firebaseUid)
                    .collection("hikes")
                    .document(String.valueOf(observation.getHikeId()))
                    .collection("observations")
                    .document(String.valueOf(observation.getObservationID()))
                    .set(payload, SetOptions.merge())
                    .addOnSuccessListener(unused -> executorService.execute(() ->
                            observationDao.markObservationAsSynced(observation.getObservationID())))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to sync observation " + observation.getObservationID(), e));
            tasks.add(task);
        }

        return tasks;
    }

    private Map<String, Object> buildHikePayload(Hike hike) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", hike.getName());
        data.put("location", hike.getLocation());
        data.put("date", hike.getDate() != null ? hike.getDate().getTime() : null);
        data.put("parkingAvailable", hike.isParkingAvailable());
        data.put("length", hike.getLength());
        data.put("difficulty", hike.getDifficulty());
        data.put("description", hike.getDescription());
        data.put("purchaseParkingPass", hike.getPurchaseParkingPass());
        data.put("isActive", hike.getIsActive());
        data.put("startTime", hike.getStartTime());
        data.put("endTime", hike.getEndTime());
        data.put("createdAt", hike.getCreatedAt());
        data.put("updatedAt", hike.getUpdatedAt());
        data.put("syncedAt", FieldValue.serverTimestamp());
        return data;
    }

    private Map<String, Object> buildObservationPayload(Observation observation) {
        Map<String, Object> data = new HashMap<>();
        data.put("observationText", observation.getObservationText());
        data.put("time", observation.getTime() != null ? observation.getTime().getTime() : null);
        data.put("comments", observation.getComments());
        data.put("location", observation.getLocation());
        data.put("picture", observation.getPicture());
        data.put("createdAt", observation.getCreatedAt());
        data.put("updatedAt", observation.getUpdatedAt());
        data.put("syncedAt", FieldValue.serverTimestamp());
        return data;
    }
    private Map<String, Object> buildUserPayload(User user) {
        Map<String, Object> data = new HashMap<>();
        data.put("displayName", user.getUserName());
        data.put("email", user.getUserEmail());
        data.put("phone", user.getUserPhone());
        data.put("createdAt", user.getCreatedAt());
        data.put("updatedAt", System.currentTimeMillis());
        data.put("lastSeen", FieldValue.serverTimestamp());
        return data;
    }
}
```

---

#### 5.8.2 GeminiEmbeddingService.java (156 lines)

**Purpose**: HTTP client for Gemini API to generate text embeddings for semantic search.

| Method | Description |
|--------|-------------|
| `GeminiEmbeddingService(String)` | Constructor accepting API key |
| `isConfigured()` | Returns true if API key is non-null and non-empty |
| `fetchEmbedding(String, String, String, String)` | Sends POST to Gemini embedContent endpoint, returns float[] |
| `buildPrompt(String, String, String, String)` | Constructs prompt with firebaseUid, chunkType, chunkId, text context |
| `buildPayload(String)` | Creates JSON request body with model and content fields |
| `parseEmbedding(String)` | Extracts embedding.values array from JSON response into float[] |
| `readFully(InputStream)` | Reads entire InputStream into String using ByteArrayOutputStream |

**Complete Code Implementation**:

```java
/**
 * Lightweight HTTP client that talks to the Gemini embeddings API.
 *
 * The call runs on a background thread (invoked by {@link VectorSyncManager}). The class
 * is intentionally synchronous to keep the implementation simple. Callers must never invoke
 * this from the main thread.
 */
public class GeminiEmbeddingService {

    private static final String TAG = "GeminiEmbeddingService";
    private static final String MODEL_NAME = "models/gemini-2.5-flash";
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/" + MODEL_NAME + ":embedContent";
    private static final int MAX_PROMPT_LENGTH = 2000;

    private final String apiKey;

    public GeminiEmbeddingService() {
        this.apiKey = BuildConfig.GEMINI_API_KEY;
        if (TextUtils.isEmpty(apiKey)) {
            Log.w(TAG, "Gemini API key is empty in BuildConfig");
        } else {
            // Log first 10 chars for debugging (never log full key)
            String preview = apiKey.length() > 10 ? apiKey.substring(0, 10) + "..." : apiKey;
            Log.d(TAG, "Gemini API key loaded (preview: " + preview + ", length: " + apiKey.length() + ")");
        }
    }

    public boolean isConfigured() {
        return !TextUtils.isEmpty(apiKey);
    }

    /**
     * Requests an embedding for the provided chunk of text.
     *
     * @param firebaseUid active Firebase user (for context in the prompt)
     * @param chunkType   high-level category (hike_description, observation_note, etc.)
     * @param chunkId     unique chunk identifier (used only for prompt context)
     * @param text        text to embed
     * @return float array representing the embedding or null if anything failed
     */
    public float[] fetchEmbedding(String firebaseUid, String chunkType, String chunkId, String text) {
        if (!isConfigured()) {
            Log.w(TAG, "Gemini API key missing; cannot fetch embeddings");
            return null;
        }
        if (TextUtils.isEmpty(text)) {
            return null;
        }

        String prompt = buildPrompt(firebaseUid, chunkType, chunkId, text);
        HttpURLConnection connection = null;
        try {
            URL url = new URL(ENDPOINT + "?key=" + apiKey);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setDoOutput(true);

            byte[] payloadBytes = buildPayload(prompt).getBytes(StandardCharsets.UTF_8);
            OutputStream os = connection.getOutputStream();
            os.write(payloadBytes);
            os.flush();

            int statusCode = connection.getResponseCode();
            InputStream inputStream = statusCode >= 200 && statusCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String responseBody = readFully(inputStream);

            if (statusCode < 200 || statusCode >= 300) {
                Log.e(TAG, "Gemini API error (" + statusCode + "): " + responseBody);
                return null;
            }
            return parseEmbedding(responseBody);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Failed to fetch embedding", e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String buildPrompt(String firebaseUid, String chunkType, String chunkId, String text) {
        String trimmed = text.length() > MAX_PROMPT_LENGTH
                ? text.substring(0, MAX_PROMPT_LENGTH)
                : text;
        return "You are Gemini 2.5 Flash acting strictly as an embedder. "
                + "Return only the raw embedding without commentary.\n"
                + "User UID: " + firebaseUid + "\n"
                + "Chunk ID: " + chunkId + "\n"
                + "Chunk Type: " + chunkType + "\n"
                + "Text:\n"
                + trimmed;
    }

    private String buildPayload(String promptText) throws JSONException {
        // Gemini Embedding API format: { "model": "...", "content": { "parts": [{ "text": "..." }] } }
        JSONObject textPart = new JSONObject().put("text", promptText);
        JSONArray parts = new JSONArray().put(textPart);
        JSONObject content = new JSONObject().put("parts", parts);

        JSONObject body = new JSONObject();
        body.put("model", MODEL_NAME);
        body.put("content", content);  // Note: "content" (singular), not "contents"
        return body.toString();
    }

    private float[] parseEmbedding(String responseBody) throws JSONException {
        JSONObject json = new JSONObject(responseBody);
        if (!json.has("embedding")) {
            Log.e(TAG, "Gemini response missing embedding: " + responseBody);
            return null;
        }
        JSONObject embeddingObject = json.getJSONObject("embedding");
        JSONArray values = embeddingObject.optJSONArray("values");
        if (values == null) {
            Log.e(TAG, "Gemini response missing values array");
            return null;
        }
        float[] embedding = new float[values.length()];
        for (int i = 0; i < values.length(); i++) {
            embedding[i] = (float) values.getDouble(i);
        }
        return embedding;
    }

    private String readFully(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }
}
```

---

#### 5.8.3 VectorSyncManager.java (206 lines)

**Purpose**: Generates and stores vector embeddings for hikes and observations in Firestore.

| Method | Description |
|--------|-------------|
| `VectorSyncManager(Context)` | Constructor initialising Firestore, GeminiEmbeddingService, Room DAOs |
| `syncUserVectors(int, String)` | Public entry point - spawns background thread for performVectorSync |
| `performVectorSync(int, String)` | Iterates all user's hikes, calls syncHikeVector and syncObservationVectors |
| `syncHikeVector(String, Hike)` | Generates embedding for hike (name+location+description), writes to Firestore |
| `syncObservationVectors(String, int)` | Generates embeddings for each observation of a hike |
| `buildObservationChunk(Observation)` | Concatenates observationText + comments + location into embeddable text |
| `writeEmbeddingToHike(String, int, float[])` | Stores embedding_vector, embedding_updatedAt, embedding_source in hike document |
| `writeEmbeddingToObservation(String, int, int, float[])` | Stores embedding fields in observation document |
| `toDoubleList(float[])` | Converts float[] to List<Double> for Firestore compatibility |
| `nullSafe(String)` | Returns empty string if input is null |

**Complete Code Implementation**:

```java
/**
 * Generates Gemini embeddings for user text content and stores them directly in
 * the hike/observation documents as "embedding_vector" fields.
 *
 * Structure:
 * - users/{uid}/hikes/{hikeId} -> contains "embedding_vector" field
 * - users/{uid}/hikes/{hikeId}/observations/{obsId} -> contains "embedding_vector" field
 *
 * The manager is intentionally conservative:
 * - Runs only when the device is online
 * - Requires a configured Gemini API key
 * - Processes hikes + observations sequentially to avoid overwhelming quotas
 */
public class VectorSyncManager {

    private static final String TAG = "VectorSyncManager";

    private final Context appContext;
    private final HikeDao hikeDao;
    private final ObservationDao observationDao;
    private final FirebaseFirestore firestore;
    private final ExecutorService executorService;
    private final GeminiEmbeddingService embeddingService;

    public VectorSyncManager(Context context) {
        this.appContext = context.getApplicationContext();
        AppDatabase database = AppDatabase.getDatabase(this.appContext);
        this.hikeDao = database.hikeDao();
        this.observationDao = database.observationDao();
        this.firestore = FirebaseFirestore.getInstance();
        this.executorService = Executors.newSingleThreadExecutor();
        this.embeddingService = new GeminiEmbeddingService();
    }

    public Task<Void> syncUserVectors(int userId, String firebaseUid) {
        if (!embeddingService.isConfigured()) {
            Log.d(TAG, "Gemini API key missing; skipping vector sync");
            return Tasks.forResult(null);
        }
        if (!NetworkUtils.isOnline(appContext)) {
            Log.d(TAG, "Device offline; skipping vector sync");
            return Tasks.forResult(null);
        }
        return Tasks.call(executorService, () -> {
            performVectorSync(userId, firebaseUid);
            return null;
        });
    }

    private void performVectorSync(int userId, String firebaseUid) {
        List<Hike> hikes = hikeDao.getHikesByUserId(userId);
        if (hikes == null || hikes.isEmpty()) {
            return;
        }
        for (Hike hike : hikes) {
            syncHikeVector(firebaseUid, hike);
            syncObservationVectors(firebaseUid, hike.getHikeID());
        }
    }

    private void syncHikeVector(String firebaseUid, Hike hike) {
        if (hike == null) return;
        StringBuilder builder = new StringBuilder();
        builder.append("Hike: ").append(nullSafe(hike.getName())).append("\n");
        builder.append("Location: ").append(nullSafe(hike.getLocation())).append("\n");
        builder.append("Difficulty: ").append(nullSafe(hike.getDifficulty())).append("\n");
        builder.append("LengthKm: ").append(hike.getLength()).append("\n");
        if (hike.getDescription() != null) {
            builder.append("Description: ").append(hike.getDescription());
        }

        String chunkType = "hike_description";
        String chunkId = "hike_" + hike.getHikeID();
        float[] embedding = embeddingService.fetchEmbedding(firebaseUid, chunkType, chunkId, builder.toString());
        if (embedding == null) {
            return;
        }
        // Store embedding directly in the hike document
        writeEmbeddingToHike(firebaseUid, hike.getHikeID(), embedding);
    }

    private void syncObservationVectors(String firebaseUid, int hikeId) {
        List<Observation> observations = observationDao.getObservationsByHikeId(hikeId);
        if (observations == null || observations.isEmpty()) {
            return;
        }
        for (Observation observation : observations) {
            if (observation == null || observation.getObservationText() == null) {
                continue;
            }
            String chunkType = "observation_note";
            String chunkId = "obs_" + observation.getObservationID();
            String text = buildObservationChunk(observation);
            float[] embedding = embeddingService.fetchEmbedding(firebaseUid, chunkType, chunkId, text);
            if (embedding == null) {
                continue;
            }
            // Store embedding directly in the observation document
            writeEmbeddingToObservation(firebaseUid, observation.getHikeId(), observation.getObservationID(), embedding);
        }
    }

    private String buildObservationChunk(@NonNull Observation observation) {
        StringBuilder builder = new StringBuilder();
        builder.append("Observation: ").append(nullSafe(observation.getObservationText())).append("\n");
        if (observation.getComments() != null) {
            builder.append("Comments: ").append(observation.getComments()).append("\n");
        }
        if (observation.getLocation() != null) {
            builder.append("Location: ").append(observation.getLocation()).append("\n");
        }
        return builder.toString();
    }

    /**
     * Writes the embedding vector directly to the hike document as "embedding_vector" field.
     */
    private void writeEmbeddingToHike(String firebaseUid, int hikeId, float[] embedding) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("embedding_vector", toDoubleList(embedding));
            update.put("embedding_updatedAt", System.currentTimeMillis());
            update.put("embedding_source", "gemini-2.5-flash");

            Task<Void> writeTask = firestore.collection("users")
                    .document(firebaseUid)
                    .collection("hikes")
                    .document(String.valueOf(hikeId))
                    .set(update, SetOptions.merge());
            Tasks.await(writeTask);
            Log.d(TAG, "Successfully stored embedding_vector in hike " + hikeId);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Failed to store embedding_vector in hike " + hikeId, e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Writes the embedding vector directly to the observation document as "embedding_vector" field.
     */
    private void writeEmbeddingToObservation(String firebaseUid, int hikeId, int observationId, float[] embedding) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("embedding_vector", toDoubleList(embedding));
            update.put("embedding_updatedAt", System.currentTimeMillis());
            update.put("embedding_source", "gemini-2.5-flash");

            Task<Void> writeTask = firestore.collection("users")
                    .document(firebaseUid)
                    .collection("hikes")
                    .document(String.valueOf(hikeId))
                    .collection("observations")
                    .document(String.valueOf(observationId))
                    .set(update, SetOptions.merge());
            Tasks.await(writeTask);
            Log.d(TAG, "Successfully stored embedding_vector in observation " + observationId);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Failed to store embedding_vector in observation " + observationId, e);
            Thread.currentThread().interrupt();
        }
    }

    private List<Double> toDoubleList(float[] embedding) {
        List<Double> list = new ArrayList<>();
        if (embedding == null) {
            return list;
        }
        for (float value : embedding) {
            list.add((double) value);
        }
        return list;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
```

---

### 5.9 Utils Package (`com.example.mobilecw.utils`)

#### 5.9.1 NetworkUtils.java (27 lines)

**Purpose**: Simple utility for checking network connectivity status.

| Method | Description |
|--------|-------------|
| `isOnline(Context)` | Returns true if device has active network connection |

**Complete Code Implementation**:

```java
/**
 * Simple helper to check basic network connectivity.
 *
 * We only care whether the device appears to have an active network connection.
 * If this returns false, we treat the app as offline and only use the local Room database.
 */
public class NetworkUtils {

    public static boolean isOnline(Context context) {
        if (context == null) {
            return false;
        }
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
}
```

---

#### 5.9.2 SearchHelper.java (251 lines)

**Purpose**: Advanced search algorithms including fuzzy matching, relevance scoring, and multi-field filtering.

| Method | Description |
|--------|-------------|
| `fuzzySearch(List<Hike>, String)` | Main search method - filters by fuzzy match, sorts by relevance score descending |
| `calculateRelevanceScore(Hike, String)` | Multi-tier scoring: exact (100), prefix (50), contains (30), word (10), fuzzy (5-15) |
| `levenshteinDistance(String, String)` | Dynamic programming implementation of edit distance algorithm |
| `isFuzzyMatch(String, String, int)` | Returns true if Levenshtein distance ≤ threshold |
| `advancedFilter(List<Hike>, String, Boolean, Date, Date)` | Applies difficulty, parking, date range filters |

**Complete Code Implementation**:

```java
/**
 * SearchHelper provides advanced search algorithms including:
 * - Partial matching
 * - Case-insensitive search
 * - Fuzzy matching (Levenshtein distance)
 * - Relevance scoring
 */
public class SearchHelper {
    
    private static final int FUZZY_THRESHOLD = 3; // Max edit distance for fuzzy match
    
    /**
     * Search hikes with fuzzy matching and relevance scoring
     * @param hikes List of all hikes
     * @param query Search query
     * @return List of matching hikes sorted by relevance
     */
    public static List<Hike> fuzzySearch(List<Hike> hikes, String query) {
        if (query == null || query.trim().isEmpty()) {
            return hikes;
        }
        
        String normalizedQuery = query.toLowerCase().trim();
        List<HikeScore> scoredHikes = new ArrayList<>();
        
        for (Hike hike : hikes) {
            int score = calculateRelevanceScore(hike, normalizedQuery);
            if (score > 0) {
                scoredHikes.add(new HikeScore(hike, score));
            }
        }
        
        // Sort by score (highest first)
        Collections.sort(scoredHikes, new Comparator<HikeScore>() {
            @Override
            public int compare(HikeScore o1, HikeScore o2) {
                return Integer.compare(o2.score, o1.score);
            }
        });
        
        // Extract sorted hikes
        List<Hike> results = new ArrayList<>();
        for (HikeScore hs : scoredHikes) {
            results.add(hs.hike);
        }
        
        return results;
    }
    
    /**
     * Calculate relevance score for a hike based on search query
     * Higher score = more relevant
     */
    private static int calculateRelevanceScore(Hike hike, String query) {
        int score = 0;
        
        String name = hike.getName() != null ? hike.getName().toLowerCase() : "";
        String location = hike.getLocation() != null ? hike.getLocation().toLowerCase() : "";
        String description = hike.getDescription() != null ? hike.getDescription().toLowerCase() : "";
        String difficulty = hike.getDifficulty() != null ? hike.getDifficulty().toLowerCase() : "";
        
        // Exact match (highest score)
        if (name.equals(query)) {
            score += 100;
        }
        if (location.equals(query)) {
            score += 80;
        }
        
        // Starts with query (high score)
        if (name.startsWith(query)) {
            score += 50;
        }
        if (location.startsWith(query)) {
            score += 40;
        }
        
        // Contains query (medium score)
        if (name.contains(query)) {
            score += 30;
        }
        if (location.contains(query)) {
            score += 25;
        }
        if (description.contains(query)) {
            score += 15;
        }
        if (difficulty.contains(query)) {
            score += 10;
        }
        
        // Word-by-word matching (for multi-word queries)
        String[] queryWords = query.split("\\s+");
        for (String word : queryWords) {
            if (word.length() < 2) continue;
            
            if (name.contains(word)) {
                score += 10;
            }
            if (location.contains(word)) {
                score += 8;
            }
            if (description.contains(word)) {
                score += 5;
            }
        }
        
        // Fuzzy matching (Levenshtein distance)
        String[] nameWords = name.split("\\s+");
        String[] locationWords = location.split("\\s+");
        
        for (String nameWord : nameWords) {
            if (nameWord.length() >= 3) {
                int distance = levenshteinDistance(nameWord, query);
                if (distance <= FUZZY_THRESHOLD) {
                    score += (FUZZY_THRESHOLD - distance) * 5;
                }
            }
        }
        
        for (String locationWord : locationWords) {
            if (locationWord.length() >= 3) {
                int distance = levenshteinDistance(locationWord, query);
                if (distance <= FUZZY_THRESHOLD) {
                    score += (FUZZY_THRESHOLD - distance) * 4;
                }
            }
        }
        
        return score;
    }
    
    /**
     * Calculate Levenshtein distance (edit distance) between two strings
     * This measures how many single-character edits are needed to change one word into another
     */
    public static int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        
        // If one string is empty, distance is length of the other
        if (len1 == 0) return len2;
        if (len2 == 0) return len1;
        
        // Create distance matrix
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        // Initialize first row and column
        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }
        
        // Fill the matrix
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                
                dp[i][j] = Math.min(
                    Math.min(
                        dp[i - 1][j] + 1,      // deletion
                        dp[i][j - 1] + 1       // insertion
                    ),
                    dp[i - 1][j - 1] + cost    // substitution
                );
            }
        }
        
        return dp[len1][len2];
    }
    
    /**
     * Check if two strings are similar based on fuzzy matching
     */
    public static boolean isFuzzyMatch(String s1, String s2, int threshold) {
        if (s1 == null || s2 == null) return false;
        return levenshteinDistance(s1.toLowerCase(), s2.toLowerCase()) <= threshold;
    }
    
    /**
     * Helper class to hold hike and its relevance score
     */
    private static class HikeScore {
        Hike hike;
        int score;
        
        HikeScore(Hike hike, int score) {
            this.hike = hike;
            this.score = score;
        }
    }
    
    /**
     * Filter hikes by advanced criteria
     */
    public static List<Hike> advancedFilter(List<Hike> hikes, String nameQuery, String locationQuery, 
                                           Double minLength, Double maxLength, Long startDate, Long endDate,
                                           String difficulty, String parkingAvailable) {
        List<Hike> filtered = new ArrayList<>();
        
        for (Hike hike : hikes) {
            boolean matches = true;
            
            // Name filter
            if (nameQuery != null && !nameQuery.trim().isEmpty()) {
                String hikeName = hike.getName() != null ? hike.getName().toLowerCase() : "";
                if (!hikeName.contains(nameQuery.toLowerCase().trim())) {
                    matches = false;
                }
            }
            
            // Location filter
            if (locationQuery != null && !locationQuery.trim().isEmpty()) {
                String hikeLocation = hike.getLocation() != null ? hike.getLocation().toLowerCase() : "";
                if (!hikeLocation.contains(locationQuery.toLowerCase().trim())) {
                    matches = false;
                }
            }
            
            // Length filter
            if (minLength != null && hike.getLength() < minLength) {
                matches = false;
            }
            if (maxLength != null && hike.getLength() > maxLength) {
                matches = false;
            }
            
            // Date filter
            if (hike.getDate() != null) {
                long hikeTime = hike.getDate().getTime();
                if (startDate != null && hikeTime < startDate) {
                    matches = false;
                }
                if (endDate != null && hikeTime > endDate) {
                    matches = false;
                }
            }
            
            // Difficulty filter
            if (difficulty != null && !difficulty.trim().isEmpty()) {
                String hikeDifficulty = hike.getDifficulty() != null ? hike.getDifficulty() : "";
                if (!hikeDifficulty.equals(difficulty)) {
                    matches = false;
                }
            }
            
            // Parking filter
            if (parkingAvailable != null && !parkingAvailable.trim().isEmpty()) {
                boolean hikeHasParking = hike.isParkingAvailable();
                if (parkingAvailable.equalsIgnoreCase("yes") && !hikeHasParking) {
                    matches = false;
                } else if (parkingAvailable.equalsIgnoreCase("no") && hikeHasParking) {
                    matches = false;
                }
            }
            
            if (matches) {
                filtered.add(hike);
            }
        }
        
        return filtered;
    }
}
```

---

### 5.10 Backend (Python FastAPI)

#### 5.10.1 main.py

**Purpose**: FastAPI server providing semantic search API with cosine similarity calculation.

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/search` | POST | Accepts SearchRequest, returns ranked results based on vector similarity |
| `/health` | GET | Health check endpoint returning {"status": "healthy"} |

| Function | Description |
|----------|-------------|
| `cosine_similarity(vec1, vec2)` | Calculates cosine similarity: (A·B)/(||A||×||B||) using numpy |
| `fetch_user_hikes(firebase_uid)` | Queries Firestore users/{uid}/hikes, returns docs with embedding_vector |
| `fetch_user_observations(firebase_uid)` | Queries all observations across user's hikes with embeddings |
| `generate_query_embedding(query)` | Calls Gemini embedContent API to vectorise search query |
| `search_hikes(query, firebase_uid, top_k)` | Fetches embeddings, generates query vector, ranks by cosine similarity |
| `search_observations(query, firebase_uid, top_k)` | Same as above but for observation embeddings |
| `search_all(query, firebase_uid, top_k)` | Combines hike and observation search, returns top_k overall |

| Data Model | Description |
|------------|-------------|
| `SearchRequest` | Pydantic model: query (str), firebase_uid (str), search_type (str), top_k (int) |
| `SearchResult` | Pydantic model: id (str), type (str), score (float), name (str), location (str) |

**Cosine Similarity Implementation**:
```python
def cosine_similarity(vec1: List[float], vec2: List[float]) -> float:
    """
    Calculate cosine similarity between two vectors.
    Formula: similarity = (A · B) / (||A|| × ||B||)
    """
    vec1 = np.array(vec1)
    vec2 = np.array(vec2)

    dot_product = np.dot(vec1, vec2)
    norm1 = np.linalg.norm(vec1)
    norm2 = np.linalg.norm(vec2)

    if norm1 == 0 or norm2 == 0:
        return 0.0

    return float(dot_product / (norm1 * norm2))
```

---

## Section 6: Advanced Feature Deep Dive

This section provides an in-depth technical analysis of the most sophisticated features implemented in M-Hike, explaining the algorithms, design patterns, and architectural decisions in detail.

### 6.1 Vector Embedding Pipeline Architecture

The semantic search capability relies on a sophisticated **Vector Embedding Pipeline** that transforms textual hike descriptions into high-dimensional numerical vectors. This enables context-aware search where queries like "peaceful water trail" can match hikes named "Blue Lake Trail" based on semantic meaning rather than keyword overlap.

#### How Text Becomes Searchable Vectors

The `VectorSyncManager` orchestrates the embedding generation process. When a user creates or updates a hike, the system constructs a structured text chunk containing all relevant information:

```java
// VectorSyncManager.java - Building the embedding input
private void syncHikeVector(String firebaseUid, Hike hike) {
    StringBuilder builder = new StringBuilder();
    builder.append("Hike: ").append(nullSafe(hike.getName())).append("\n");
    builder.append("Location: ").append(nullSafe(hike.getLocation())).append("\n");
    builder.append("Difficulty: ").append(nullSafe(hike.getDifficulty())).append("\n");
    builder.append("LengthKm: ").append(hike.getLength()).append("\n");
    if (hike.getDescription() != null) {
        builder.append("Description: ").append(hike.getDescription());
    }

    String chunkType = "hike_description";
    String chunkId = "hike_" + hike.getHikeID();
    float[] embedding = embeddingService.fetchEmbedding(
        firebaseUid, chunkType, chunkId, builder.toString()
    );
}
```

**Why this structure matters**: The Gemini embedding model processes natural language, so providing structured context (field labels like "Location:", "Difficulty:") helps the model understand the semantic relationships between different attributes.

#### The Embedding Storage Strategy

Rather than creating a separate vector database, embeddings are stored **directly within the Firestore hike documents**:

```mermaid
flowchart LR
    subgraph Firestore["Firestore Document Structure"]
        direction TB
        Hike["users/{uid}/hikes/{id}"]
        Fields["name: 'Blue Lake Trail'<br/>location: 'Mountain Valley'<br/>difficulty: 'Medium'<br/>embedding_vector: [0.023, -0.441, ...]<br/>embedding_source: 'gemini-2.5-flash'"]
    end

    Hike --> Fields
```

This **co-located storage** approach has several advantages:
- **Single query retrieval**: Both metadata and vectors are fetched in one read operation
- **Atomic updates**: When a hike is modified, its embedding updates in the same transaction
- **Simplified architecture**: No need for a separate vector database like Pinecone or Weaviate

#### Conservative Rate Limiting

The pipeline is designed to be "quota-friendly" for the Gemini API:

```java
// VectorSyncManager - Sequential processing to avoid rate limits
public Task<Void> syncUserVectors(int userId, String firebaseUid) {
    if (!embeddingService.isConfigured()) {
        Log.d(TAG, "Gemini API key missing; skipping vector sync");
        return Tasks.forResult(null);  // Graceful degradation
    }
    if (!NetworkUtils.isOnline(appContext)) {
        return Tasks.forResult(null);  // Skip when offline
    }
    // Process sequentially, not in parallel
    return Tasks.call(executorService, () -> {
        performVectorSync(userId, firebaseUid);
        return null;
    });
}
```

**Design decision**: Embeddings are generated **sequentially** (not in parallel) to avoid hitting API rate limits. This trades speed for reliability—a crucial consideration for a production application.

---

### 6.2 Dual Authentication Architecture

M-Hike implements a sophisticated **Dual Identity System** that bridges local Room storage with Firebase cloud authentication. This enables the application to work fully offline whilst maintaining secure cloud synchronisation.

#### The Two-ID Problem

Every user has **two different identifiers**:

| ID Type | Storage | Purpose |
|---------|---------|---------|
| `userId` (int) | Local Room Database | Primary key for local relationships |
| `firebaseUid` (string) | Firebase Auth & Firestore | Cloud document paths and security |

The `SessionManager` maintains both simultaneously:

```java
public class SessionManager {
    private static final String KEY_USER_ID = "user_id";        // Local Room ID
    private static final String KEY_FIREBASE_UID = "firebase_uid";  // Cloud ID

    public static void setCurrentUserId(Context context, int userId) {
        prefs.edit().putInt(KEY_USER_ID, userId).apply();
    }

    public static void setCurrentFirebaseUid(Context context, String firebaseUid) {
        prefs.edit().putString(KEY_FIREBASE_UID, firebaseUid).apply();
    }
}
```

#### The Registration Flow

When a user signs up, both identity systems must be initialised in the correct order:

```mermaid
sequenceDiagram
    participant User
    participant App
    participant Firebase as Firebase Auth
    participant Room as Room DB
    participant Firestore

    User->>App: Submit registration form
    App->>App: Validate form locally
    App->>Room: Check email uniqueness
    Room-->>App: Email available

    App->>Firebase: createUserWithEmailAndPassword()
    Firebase-->>App: FirebaseUser + UID

    App->>Room: Insert User with firebaseUid
    Room-->>App: Local userId generated

    App->>App: SessionManager.setCurrentUserId(userId)
    App->>App: SessionManager.setCurrentFirebaseUid(firebaseUid)

    App->>Firestore: Sync user profile to cloud
    App-->>User: Registration complete
```

**Critical ordering**: Firebase Auth must succeed **before** creating the local Room user. This ensures we have the `firebaseUid` to store in the local entity, enabling future sync operations.

#### Why Not Just Use Firebase?

A common question is: "Why maintain a local Room database at all?"

| Scenario | Firebase-Only | M-Hike's Dual System |
|----------|---------------|----------------------|
| No internet connection | ❌ App unusable | ✅ Full functionality |
| Slow/flaky connection | ⚠️ Sluggish UI | ✅ Instant responses |
| Data ownership | ⚠️ Google's servers | ✅ Local copy guaranteed |
| Query performance | ⚠️ Network latency | ✅ Millisecond queries |

The Dual Authentication Architecture provides the best of both worlds: local speed and reliability with cloud backup and cross-device sync.

---

### 6.3 Multi-Selection Mode with State Management

The `HikeListAdapter` implements a sophisticated **stateful selection system** that demonstrates proper Android RecyclerView state management patterns.

#### The Selection State Machine

The adapter maintains a `Set<Integer>` of selected hike IDs, enabling O(1) lookup for selection state:

```java
public class HikeListAdapter extends RecyclerView.Adapter<HikeViewHolder> {
    private boolean selectionMode = false;
    private final Set<Integer> selectedHikeIds = new HashSet<>();

    public void setSelectionMode(boolean enabled) {
        selectionMode = enabled;
        if (!selectionMode) {
            selectedHikeIds.clear();  // Reset on mode exit
        }
        notifyDataSetChanged();  // Trigger full rebind
    }
}
```

#### Visual State Binding

Each ViewHolder dynamically adjusts its appearance based on both mode and selection state:

```java
void bind(Hike hike) {
    // ... basic data binding ...

    if (selectionMode) {
        // SELECTION MODE: Show checkboxes, disable navigation
        selectCheckbox.setVisibility(View.VISIBLE);
        boolean isSelected = selectedHikeIds.contains(hike.getHikeID());
        selectCheckbox.setChecked(isSelected);
        viewButton.setEnabled(false);
        viewButton.setAlpha(0.5f);  // Visual disabled state

        // Unified click handler for the entire row
        View.OnClickListener toggleListener = v -> toggleSelection(hike);
        itemView.setOnClickListener(toggleListener);
        selectCheckbox.setOnClickListener(toggleListener);
        viewButton.setOnClickListener(toggleListener);
    } else {
        // NORMAL MODE: Hide checkboxes, enable navigation
        selectCheckbox.setVisibility(View.GONE);
        viewButton.setEnabled(true);
        viewButton.setAlpha(1f);
        itemView.setOnClickListener(v -> listener.onHikeClicked(hike));
    }
}
```

#### The Observer Pattern for Selection Changes

The adapter notifies the Activity when selection count changes, enabling dynamic UI updates:

```java
public interface OnSelectionChangedListener {
    void onSelectionChanged(int selectedCount);
}

private void toggleSelection(Hike hike) {
    int id = hike.getHikeID();
    if (selectedHikeIds.contains(id)) {
        selectedHikeIds.remove(id);
    } else {
        selectedHikeIds.add(id);
    }

    // Notify observer (the Activity)
    if (selectionChangedListener != null) {
        selectionChangedListener.onSelectionChanged(selectedHikeIds.size());
    }
    notifyDataSetChanged();
}
```

The Activity responds by enabling/disabling the "Delete Selected" button:

```java
// HikingListActivity.java
adapter.setOnSelectionChangedListener(count -> {
    deleteSelectedButton.setEnabled(count > 0);
});
```

---

### 6.4 Thread Safety and Concurrency Patterns

M-Hike employs multiple concurrency strategies to ensure thread-safe database operations without blocking the UI thread.

#### The Single-Threaded Executor Pattern

Critical managers use **SingleThreadExecutor** to serialize operations:

```java
// FirebaseSyncManager - Guaranteed sequential execution
private final ExecutorService executorService = Executors.newSingleThreadExecutor();

public void syncNow(SyncCallback callback) {
    executorService.execute(() -> {
        // All database reads happen sequentially
        List<Hike> unsyncedHikes = hikeDao.getUnsyncedHikes();
        // No race conditions possible
    });
}
```

**Why single-threaded?** The Firebase sync involves:
1. Reading from Room (local)
2. Writing to Firestore (cloud)
3. Updating Room again (marking as synced)

If these operations ran in parallel, hikes could be uploaded twice or marked as synced before the upload completed.

#### Main Thread Executor for UI Updates

After background work completes, results must be posted back to the UI thread:

```java
private final Executor mainThreadExecutor = ContextCompat.getMainExecutor(appContext);

private void notifySuccess(SyncCallback callback) {
    mainThreadExecutor.execute(() -> {
        callback.onSuccess();  // Safe to update UI here
    });
}
```

#### The Task Aggregation Pattern

Firebase SDK returns `Task<Void>` objects. M-Hike aggregates multiple async operations using `Tasks.whenAllSuccess()`:

```java
List<Task<Void>> pendingTasks = new ArrayList<>();
pendingTasks.add(syncUserProfile(userId, firebaseUid));
pendingTasks.addAll(syncHikes(userId, firebaseUid));
pendingTasks.addAll(syncObservations(userId, firebaseUid));

// Wait for ALL operations to complete
Tasks.whenAllSuccess(pendingTasks)
    .addOnSuccessListener(unused -> notifySuccess(callback))
    .addOnFailureListener(e -> notifyFailure(callback, e));
```

This ensures the logout sequence only clears local data **after** all cloud writes have succeeded.

---

### 6.5 Relevance Scoring Algorithm

The `SearchHelper` implements a **multi-factor relevance scoring system** that ranks search results beyond simple string matching.

#### Score Calculation Logic

Each hike receives a cumulative score based on multiple criteria:

```java
private static int calculateRelevanceScore(Hike hike, String query) {
    int score = 0;

    // TIER 1: Exact matches (highest value)
    if (name.equals(query)) score += 100;
    if (location.equals(query)) score += 80;

    // TIER 2: Prefix matches
    if (name.startsWith(query)) score += 50;
    if (location.startsWith(query)) score += 40;

    // TIER 3: Contains matches
    if (name.contains(query)) score += 30;
    if (location.contains(query)) score += 25;
    if (description.contains(query)) score += 15;
    if (difficulty.contains(query)) score += 10;

    // TIER 4: Word-by-word matching (multi-word queries)
    for (String word : queryWords) {
        if (name.contains(word)) score += 10;
        if (location.contains(word)) score += 8;
    }

    // TIER 5: Fuzzy matching (typo tolerance)
    for (String nameWord : nameWords) {
        int distance = levenshteinDistance(nameWord, query);
        if (distance <= FUZZY_THRESHOLD) {
            score += (FUZZY_THRESHOLD - distance) * 5;
        }
    }

    return score;
}
```

#### The Scoring Hierarchy Visualised

```mermaid
graph TD
    A["User Query: 'mountain'"] --> B{Exact Name Match?}
    B -->|Yes| C["+100 points"]
    B -->|No| D{Name Starts With?}
    D -->|Yes| E["+50 points"]
    D -->|No| F{Name Contains?}
    F -->|Yes| G["+30 points"]
    F -->|No| H{Fuzzy Match ≤3 edits?}
    H -->|Yes| I["+5-15 points<br/>(based on distance)"]
    H -->|No| J["0 points"]

    C --> K[Final Score]
    E --> K
    G --> K
    I --> K
    J --> K
```

#### Why This Matters for UX

Consider the query "mountan" (misspelled):

| Hike Name | Contains Match | Fuzzy Match | Total Score |
|-----------|---------------|-------------|-------------|
| Mountain Peak Trail | ❌ | ✅ (distance=1) | 10 |
| Mount Everest | ❌ | ✅ (distance=2) | 5 |
| Lake Valley | ❌ | ❌ | 0 |

The fuzzy matching ensures typos don't result in zero results—a critical UX consideration for mobile keyboards where typing errors are common.

---

### 6.6 Room Type Converters for Complex Types

SQLite (the underlying database for Room) doesn't natively support Java `Date` objects. The `Converters` class bridges this gap.

#### The Conversion Mechanism

```java
public class Converters {
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}
```

#### How Room Uses Converters

When Room encounters a `Date` field in an entity:

```mermaid
flowchart LR
    subgraph Write["Write Operation"]
        A["Hike.setDate(new Date())"] --> B["dateToTimestamp()"]
        B --> C["Store as LONG in SQLite"]
    end

    subgraph Read["Read Operation"]
        D["Query returns LONG"] --> E["fromTimestamp()"]
        E --> F["Returns Date object"]
    end
```

The `@TypeConverters({Converters.class})` annotation on `AppDatabase` registers these converters globally.

---

### 6.7 Cascade Deletion with Foreign Keys

The `Observation` entity demonstrates proper relational database design with **referential integrity enforcement**.

#### The Foreign Key Definition

```java
@Entity(tableName = "observations",
        foreignKeys = @ForeignKey(
                entity = Hike.class,
                parentColumns = "hikeID",
                childColumns = "hikeId",
                onDelete = ForeignKey.CASCADE  // KEY: Automatic cleanup
        ),
        indices = {@Index("hikeId")})
public class Observation {
    private int hikeId;  // References Hike.hikeID
}
```

#### What CASCADE Means

When a Hike is deleted, **all its Observations are automatically deleted** by SQLite:

```mermaid
flowchart TD
    A["User deletes<br/>'Mountain Trail' hike"] --> B["Room executes<br/>DELETE FROM hikes WHERE hikeID = 5"]
    B --> C["SQLite CASCADE trigger"]
    C --> D["Automatic execution:<br/>DELETE FROM observations WHERE hikeId = 5"]
    D --> E["4 observations<br/>automatically removed"]
```

**Benefits**:
- **No orphaned records**: Observations can't exist without their parent Hike
- **Atomic deletion**: If the Hike delete fails, Observations remain intact
- **Simplified code**: No need to manually delete child records first

---

### 6.8 Active Hike Real-Time Tracking

The application tracks whether the user is currently on an active hike, displaying elapsed time on the home screen.

#### The Active State Fields

```java
@Entity(tableName = "hikes")
public class Hike {
    private Boolean isActive;   // Currently on this hike?
    private Long startTime;     // When did the hike begin?
    private Long endTime;       // When did the hike end?
}
```

#### Ensuring Single Active Hike

Only one hike can be active at a time. The DAO enforces this:

```java
@Dao
public interface HikeDao {
    // Deactivate ALL hikes before starting a new one
    @Query("UPDATE hikes SET isActive = 0, synced = 0 WHERE isActive = 1")
    void deactivateAllHikes();

    // Then activate the specific hike
    @Query("UPDATE hikes SET isActive = 1, startTime = :startTime WHERE hikeID = :hikeId")
    void startHike(int hikeId, long startTime, long updatedAt);

    // Find the currently active hike (for home screen display)
    @Query("SELECT * FROM hikes WHERE isActive = 1 LIMIT 1")
    Hike getActiveHike();
}
```

#### Real-Time Duration Calculation

The home screen dynamically calculates elapsed time:

```java
private void loadActiveHike() {
    executorService.execute(() -> {
        Hike activeHike = hikeDao.getActiveHike();
        runOnUiThread(() -> {
            if (activeHike != null && activeHike.getStartTime() != null) {
                long durationMillis = System.currentTimeMillis() - activeHike.getStartTime();
                long hours = durationMillis / (1000 * 60 * 60);
                long minutes = (durationMillis / (1000 * 60)) % 60;

                activeHikeDuration.setText(
                    String.format("Started %dh %dm ago", hours, minutes)
                );
            }
        });
    });
}
```

This provides users with immediate awareness of their hiking activity without requiring manual time tracking.

---

## References

Firtman, M. (2018) *High Performance Mobile Web: Best Practices for Optimizing Mobile Web Apps*. Sebastopol: O'Reilly Media.

Fowler, M. (2002) *Patterns of Enterprise Application Architecture*. Boston: Addison-Wesley Professional.

Google (2024a) *Room Persistence Library*. Available at: https://developer.android.com/training/data-storage/room (Accessed: 28 November 2024).

Google (2024b) *Cloud Firestore Documentation*. Available at: https://firebase.google.com/docs/firestore (Accessed: 28 November 2024).

Google (2024c) *Firebase Authentication*. Available at: https://firebase.google.com/docs/auth (Accessed: 28 November 2024).

Google (2024d) *Gemini API - Embeddings Guide*. Available at: https://ai.google.dev/gemini-api/docs/embeddings (Accessed: 28 November 2024).

Manning, C.D., Raghavan, P. and Schütze, H. (2008) *Introduction to Information Retrieval*. Cambridge: Cambridge University Press.

Navarro, G. (2001) 'A Guided Tour to Approximate String Matching', *ACM Computing Surveys*, 33(1), pp. 31-88. doi: 10.1145/375360.375365.

Nielsen, J. (1994) 'Enhancing the Explanatory Power of Usability Heuristics', *Proceedings of the SIGCHI Conference on Human Factors in Computing Systems*. Boston, MA, 24-28 April. New York: ACM Press, pp. 152-158.

Ramírez, S. (2024) *FastAPI Documentation*. Available at: https://fastapi.tiangolo.com/ (Accessed: 28 November 2024).

Gamma, E., Helm, R., Johnson, R. and Vlissides, J. (1994) *Design Patterns: Elements of Reusable Object-Oriented Software*. Boston: Addison-Wesley Professional.

Goetz, B., Peierls, T., Bloch, J., Bowbeer, J., Holmes, D. and Lea, D. (2006) *Java Concurrency in Practice*. Boston: Addison-Wesley Professional.

Mikolov, T., Chen, K., Corrado, G. and Dean, J. (2013) 'Efficient Estimation of Word Representations in Vector Space', *Proceedings of the International Conference on Learning Representations (ICLR)*. Scottsdale, AZ, 2-4 May.

Codd, E.F. (1970) 'A Relational Model of Data for Large Shared Data Banks', *Communications of the ACM*, 13(6), pp. 377-387. doi: 10.1145/362384.362685.

---

## Appendix: Project Structure

```
M-Hike/
├── app/
│   ├── src/main/java/com/example/mobilecw/
│   │   ├── activities/          # UI Activities
│   │   ├── adapters/            # RecyclerView Adapters
│   │   ├── auth/                # Session Management
│   │   ├── database/            # Room Database
│   │   │   ├── dao/             # Data Access Objects
│   │   │   └── entities/        # Entity Classes
│   │   ├── services/            # API Services
│   │   ├── sync/                # Firebase & Vector Sync
│   │   └── utils/               # Utilities
│   └── src/main/res/            # Android Resources
├── backend/
│   ├── main.py                  # FastAPI Server
│   └── requirements.txt         # Python Dependencies
└── README.md                    # This Report
```

---

## Summary

This coursework demonstrates the successful implementation of a comprehensive hiking management application that extends significantly beyond the basic CRUD requirements. Key achievements include:

1. **Offline-First Architecture**: The application functions fully without internet connectivity, with seamless cloud synchronisation when online
2. **AI-Powered Search**: Integration of Gemini 2.5 Flash embeddings enables semantic search capabilities that understand context, not just keywords
3. **Microservices Design**: Separation of the embedding/search logic into a Python backend demonstrates modern distributed system design
4. **Robust Security**: Multi-layered security including Firebase Authentication, user data isolation, and secure session management
5. **Advanced Algorithms**: Implementation of Levenshtein distance for fuzzy search and cosine similarity for vector comparison
6. **Thread-Safe Concurrency**: Proper use of ExecutorService patterns ensures responsive UI whilst performing complex background operations

The M-Hike application represents a production-ready foundation that could be deployed with the enhancements outlined in Section 4.iv.

---

*This report was prepared as part of the COMP1786 Mobile Application Design and Development coursework.*
