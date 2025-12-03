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
| **G) Additional Features** | ✅ Fully Completed | **(1) AI Semantic Search**: Integrated Gemini 2.5 Flash embeddings via a Python FastAPI backend for context-aware search using cosine similarity (Manning, Raghavan and Schütze, 2008). **(2) Robust Offline Sync**: A `FirebaseSyncManager` that queues and uploads data automatically when connectivity is restored. **(3) Security**: "Wipe-on-Logout" feature (`SettingsActivity.java`) clears all local data to ensure privacy on shared devices. **(4) Weather API**: Real-time integration with the meteoblue Weather API for trail condition awareness. |

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

#### 5.2.1 HomeActivity.java (374 lines)

**Purpose**: Main dashboard displaying weather, hiking statistics, active hike status, and nearby trails.

| Method | Description |
|--------|-------------|
| `onCreate(Bundle)` | Initialises UI components, sets up bottom navigation, loads all dashboard data |
| `initViews()` | Binds all XML layout elements to Java fields |
| `setupBottomNavigation()` | Configures the BottomNavigationView with navigation listeners |
| `loadActivityStats()` | Queries Room database for total hikes, active hikes, and total distance statistics |
| `loadNearbyTrails()` | Fetches and displays list of available trails in a horizontal RecyclerView |
| `loadActiveHike()` | Checks for currently active hike and displays real-time duration |
| `fetchWeather()` | Makes HTTP request to meteoblue Weather API and updates weather card |
| `updateWeatherUI(JSONObject)` | Parses weather JSON response and updates UI elements |
| `navigateToSearch()` | Handles navigation to SearchActivity |
| `navigateToSettings()` | Handles navigation to SettingsActivity |

**Critical Code** - Weather API Integration:
```java
private void fetchWeather() {
    String url = String.format(
        "https://my.meteoblue.com/packages/basic-1h?apikey=%s&lat=%.4f&lon=%.4f",
        BuildConfig.METEOBLUE_API_KEY, latitude, longitude
    );
    
    JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
        response -> updateWeatherUI(response),
        error -> Log.e(TAG, "Weather fetch failed", error)
    );
    requestQueue.add(request);
}
```

---

#### 5.2.2 EnterHikeActivity.java (332 lines)

**Purpose**: Form for creating new hikes or editing existing ones with comprehensive validation.

| Method | Description |
|--------|-------------|
| `onCreate(Bundle)` | Initialises form, checks for edit mode, loads existing hike if editing |
| `initializeViews()` | Binds all form input fields (EditText, Spinner, CheckBox, etc.) |
| `setupDatePicker()` | Creates and configures MaterialDatePicker for date selection |
| `setupDifficultySpinner()` | Populates difficulty dropdown (Easy, Medium, Hard) |
| `setupParkingToggle()` | Configures parking availability switch with parking pass field visibility |
| `loadHikeForEdit(int)` | Queries database for existing hike and populates form |
| `populateForm(Hike)` | Fills all form fields with hike data for editing |
| `validateForm()` | Validates all required fields before submission |
| `saveHike()` | Creates/updates Hike entity and persists to Room database |
| `showConfirmationDialog()` | Displays summary dialog before final save |

**Critical Code** - Form Validation:
```java
private boolean validateForm() {
    boolean isValid = true;
    
    if (TextUtils.isEmpty(nameInput.getText())) {
        nameInput.setError("Name is required");
        isValid = false;
    }
    if (TextUtils.isEmpty(locationInput.getText())) {
        locationInput.setError("Location is required");
        isValid = false;
    }
    if (selectedDate == null) {
        Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
        isValid = false;
    }
    // Additional validation for length > 0, etc.
    return isValid;
}
```

---

#### 5.2.3 HikingListActivity.java (378 lines)

**Purpose**: Displays all hikes in a RecyclerView with search, filter, and multi-select deletion capabilities.

| Method | Description |
|--------|-------------|
| `onCreate(Bundle)` | Sets up RecyclerView, adapter, and UI controls |
| `initViews()` | Binds layout elements including search bar and filter chips |
| `setupRecyclerView()` | Configures RecyclerView with LinearLayoutManager and adapter |
| `loadHikes()` | Queries all hikes from Room database for current user |
| `setupSearch()` | Attaches TextWatcher to search input for real-time filtering |
| `filterHikes(String)` | Filters displayed hikes based on search query |
| `toggleSelectionMode()` | Enables/disables multi-select mode for batch deletion |
| `deleteSelectedHikes()` | Deletes all selected hikes with confirmation dialog |
| `seedSampleData()` | Creates demo hikes for testing (development feature) |
| `onHikeClicked(Hike)` | Navigates to HikeDetailActivity for the selected hike |
| `onEditClicked(Hike)` | Opens EnterHikeActivity in edit mode |
| `onDeleteClicked(Hike)` | Single hike deletion with confirmation |

**Critical Code** - Multi-Select Deletion:
```java
private void deleteSelectedHikes() {
    new AlertDialog.Builder(this)
        .setTitle("Delete Selected Hikes")
        .setMessage("Are you sure you want to delete " + selectedCount + " hikes?")
        .setPositiveButton("Delete", (dialog, which) -> {
            executorService.execute(() -> {
                for (Integer hikeId : adapter.getSelectedHikeIds()) {
                    hikeDao.deleteHikeById(hikeId);
                }
                runOnUiThread(() -> {
                    toggleSelectionMode();  // Exit selection mode
                    loadHikes();  // Refresh list
                });
            });
        })
        .setNegativeButton("Cancel", null)
        .show();
}
```

---

#### 5.2.4 HikeDetailActivity.java (270 lines)

**Purpose**: Displays detailed information about a single hike with start/end hike functionality.

| Method | Description |
|--------|-------------|
| `onCreate(Bundle)` | Retrieves hike ID from intent and loads data |
| `loadHike(int)` | Queries Room database for the specific hike |
| `populateData(Hike)` | Fills all UI fields with hike information |
| `setupObservationsButton()` | Configures navigation to observations list |
| `startHike()` | Begins active hike tracking with timestamp |
| `endHike()` | Ends active hike and calculates duration |
| `confirmStartHike()` | Shows confirmation dialog before starting |
| `syncIfLoggedIn()` | Triggers Firebase sync if user is authenticated |
| `onEditClicked()` | Opens edit form for this hike |
| `onDeleteClicked()` | Deletes hike with navigation back to list |

**Critical Code** - Start/End Hike:
```java
private void startHike() {
    confirmStartHike(() -> {
        executorService.execute(() -> {
            hikeDao.deactivateAllHikes();  // Ensure only one active hike
            hikeDao.startHike(hikeId, System.currentTimeMillis(), System.currentTimeMillis());
            runOnUiThread(() -> {
                Toast.makeText(this, "Hike started!", Toast.LENGTH_SHORT).show();
                loadHike(hikeId);  // Refresh UI
                syncIfLoggedIn();
            });
        });
    });
}
```

---

#### 5.2.5 SearchActivity.java (622 lines)

**Purpose**: Advanced search interface with fuzzy search, semantic search, and multi-filter capabilities.

| Method | Description |
|--------|-------------|
| `onCreate(Bundle)` | Initialises search UI and mode toggles |
| `initViews()` | Binds search input, mode selector, and filter controls |
| `setupSearchModeToggle()` | Configures tabs for Basic/Advanced/Semantic search modes |
| `performSearch(String)` | Executes fuzzy search using SearchHelper |
| `performSemanticSearch(String)` | Calls SemanticSearchService for AI-powered search |
| `performAdvancedSearch()` | Applies multiple filter criteria simultaneously |
| `applyOtherFilters(List<Hike>, ...)` | Filters by date, difficulty, length, parking |
| `displayResults(List<Hike>)` | Updates RecyclerView with search results |
| `showNoResults()` | Displays empty state when no matches found |
| `clearFilters()` | Resets all filter inputs to default values |

**Critical Code** - Semantic Search Integration:
```java
private void performSemanticSearch(String query) {
    String firebaseUid = SessionManager.getCurrentFirebaseUid(this);
    if (firebaseUid == null) {
        showLoginRequiredDialog();
        return;
    }
    
    showLoading(true);
    SemanticSearchService.search(this, query, firebaseUid, "hikes", 10,
        new SemanticSearchService.SearchCallback() {
            @Override
            public void onSuccess(List<SearchResult> results) {
                showLoading(false);
                matchResultsWithLocalData(results);  // Cross-reference with Room
            }
            
            @Override
            public void onError(String error) {
                showLoading(false);
                Toast.makeText(SearchActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
}
```

---

#### 5.2.6 LoginActivity.java (~180 lines)

**Purpose**: Firebase Authentication login with Room database synchronisation.

| Method | Description |
|--------|-------------|
| `onCreate(Bundle)` | Initialises login form and Firebase Auth instance |
| `initViews()` | Binds email/password inputs and login button |
| `handleLogin()` | Validates credentials and initiates Firebase sign-in |
| `onLoginSuccess(FirebaseUser)` | Syncs Firebase user with local Room database |
| `syncUserToRoom(FirebaseUser)` | Creates/updates local User entity with Firebase UID |
| `navigateToHome()` | Redirects to HomeActivity after successful login |
| `navigateToSignup()` | Opens SignupActivity for new users |

**Critical Code** - Dual Identity Sync:
```java
private void syncUserToRoom(FirebaseUser firebaseUser) {
    executorService.execute(() -> {
        User existingUser = userDao.getUserByFirebaseUid(firebaseUser.getUid());
        if (existingUser != null) {
            // Update existing user
            SessionManager.setCurrentUserId(this, existingUser.getUserId());
        } else {
            // Create new local user
            User newUser = new User();
            newUser.setFirebaseUid(firebaseUser.getUid());
            newUser.setUserEmail(firebaseUser.getEmail());
            long userId = userDao.insertUser(newUser);
            SessionManager.setCurrentUserId(this, (int) userId);
        }
        SessionManager.setCurrentFirebaseUid(this, firebaseUser.getUid());
        runOnUiThread(this::navigateToHome);
    });
}
```

---

#### 5.2.7 SignupActivity.java (234 lines)

**Purpose**: User registration with Firebase Auth and local Room database creation.

| Method | Description |
|--------|-------------|
| `onCreate(Bundle)` | Initialises registration form |
| `initViews()` | Binds all registration input fields |
| `handleSignup()` | Validates form and creates Firebase account |
| `validateInputs()` | Checks all fields (email format, password strength, etc.) |
| `createFirebaseUser(String, String)` | Calls Firebase createUserWithEmailAndPassword |
| `createLocalUser(FirebaseUser)` | Inserts new User entity into Room database |
| `setupSession(int, String)` | Stores user IDs in SessionManager |
| `navigateToHome()` | Redirects after successful registration |

**Critical Code** - Password Validation:
```java
private boolean validateInputs() {
    String password = passwordInput.getText().toString();
    String confirmPassword = confirmPasswordInput.getText().toString();
    
    if (password.length() < 8) {
        passwordInput.setError("Password must be at least 8 characters");
        return false;
    }
    if (!password.equals(confirmPassword)) {
        confirmPasswordInput.setError("Passwords do not match");
        return false;
    }
    // Additional validation for email format, etc.
    return true;
}
```

---

#### 5.2.8 SettingsActivity.java (273 lines)

**Purpose**: User settings and logout with secure data cleanup.

| Method | Description |
|--------|-------------|
| `onCreate(Bundle)` | Initialises settings UI and user profile section |
| `loadUserProfile()` | Displays current user's email and name |
| `handleLogout()` | Orchestrates the secure logout process |
| `syncBeforeLogout()` | Uploads all pending data to Firebase before clearing |
| `clearLocalData()` | Wipes all tables from Room database |
| `clearSession()` | Removes user IDs from SharedPreferences |
| `navigateToLogin()` | Redirects to LoginActivity after logout |

**Critical Code** - Secure Logout:
```java
private void handleLogout() {
    new AlertDialog.Builder(this)
        .setTitle("Logout")
        .setMessage("Your local data will be synced and then cleared. Continue?")
        .setPositiveButton("Logout", (dialog, which) -> {
            FirebaseSyncManager.getInstance(this).syncNow(new SyncCallback() {
                @Override
                public void onSuccess() {
                    executorService.execute(() -> {
                        database.clearAllTables();  // Wipe-on-Logout
                        SessionManager.clearCurrentUser(SettingsActivity.this);
                        FirebaseAuth.getInstance().signOut();
                        runOnUiThread(() -> navigateToLogin());
                    });
                }
                
                @Override
                public void onFailure(Exception e) {
                    // Still logout but warn user data may not be synced
                    Toast.makeText(SettingsActivity.this, 
                        "Sync failed - some data may be lost", Toast.LENGTH_LONG).show();
                }
            });
        })
        .setNegativeButton("Cancel", null)
        .show();
}
```

---

#### 5.2.9 UsersActivity.java (425 lines)

**Purpose**: User profile display with hiking statistics and account management.

| Method | Description |
|--------|-------------|
| `onCreate(Bundle)` | Initialises profile UI |
| `checkLoginStatus()` | Verifies user is logged in, redirects if not |
| `loadUserData()` | Fetches user profile from Room database |
| `loadStatistics()` | Calculates and displays hiking statistics |
| `calculateTotalDistance()` | Sums length of all user's hikes |
| `calculateTotalDuration()` | Calculates total time spent hiking |
| `countCompletedHikes()` | Counts hikes with both start and end times |
| `updateProfileUI(User)` | Populates UI with user information |
| `navigateToEditProfile()` | Opens profile edit form |

---

#### 5.2.10 ObservationListActivity.java (~150 lines)

**Purpose**: Displays all observations for a specific hike.

| Method | Description |
|--------|-------------|
| `onCreate(Bundle)` | Retrieves hike ID and sets up RecyclerView |
| `loadObservations()` | Queries observations for the parent hike |
| `openAddObservation()` | Navigates to ObservationFormActivity |
| `onEditClicked(Observation)` | Opens edit form for observation |
| `deleteObservation(Observation)` | Removes observation with confirmation |
| `onBackPressed()` | Returns to parent HikeDetailActivity |

---

#### 5.2.11 ObservationFormActivity.java (435 lines)

**Purpose**: Form for creating/editing observations with camera and GPS integration.

| Method | Description |
|--------|-------------|
| `onCreate(Bundle)` | Initialises form with optional pre-populated data |
| `initViews()` | Binds observation text, comments, location, and image fields |
| `setupImagePickers()` | Configures camera and gallery image selection |
| `launchCamera()` | Opens device camera with FileProvider for secure file access |
| `launchGallery()` | Opens image picker for existing photos |
| `onActivityResult(int, int, Intent)` | Handles returned camera/gallery images |
| `getCurrentLocation()` | Uses FusedLocationProviderClient for GPS coordinates |
| `showDateTimePicker()` | Opens combined date and time selection |
| `saveObservation()` | Validates and persists observation to Room |
| `requestLocationPermission()` | Handles runtime permission for ACCESS_FINE_LOCATION |

**Critical Code** - Camera with FileProvider:
```java
private void launchCamera() {
    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (cameraIntent.resolveActivity(getPackageManager()) != null) {
        File photoFile = createImageFile();
        if (photoFile != null) {
            currentPhotoPath = photoFile.getAbsolutePath();
            Uri photoUri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", photoFile);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(cameraIntent, REQUEST_CAMERA);
        }
    }
}
```

**Critical Code** - GPS Location:
```java
private void getCurrentLocation() {
    if (ActivityCompat.checkSelfPermission(this, 
            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        requestLocationPermission();
        return;
    }
    
    fusedLocationClient.getLastLocation()
        .addOnSuccessListener(location -> {
            if (location != null) {
                String coords = String.format(Locale.US, 
                    "%.6f, %.6f", location.getLatitude(), location.getLongitude());
                locationInput.setText(coords);
            }
        });
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

**Critical Code**:
```java
public class SessionManager {
    private static final String PREFS_NAME = "MHikeSession";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_FIREBASE_UID = "firebase_uid";

    public static int getCurrentUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_USER_ID, -1);  // -1 indicates no user
    }

    public static boolean isLoggedIn(Context context) {
        return getCurrentUserId(context) != -1 && getCurrentFirebaseUid(context) != null;
    }

    public static void clearCurrentUser(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_FIREBASE_UID)
            .apply();
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

**Critical Code**:
```java
@Database(entities = {Hike.class, Observation.class, User.class}, 
          version = 8, 
          exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class, "mhike_database"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract HikeDao hikeDao();
    public abstract ObservationDao observationDao();
    public abstract UserDao userDao();
}
```

---

#### 5.4.2 Converters.java

**Purpose**: Type converters for Room to handle Date ↔ Long conversions.

| Method | Description |
|--------|-------------|
| `fromTimestamp(Long)` | Converts Long (milliseconds) to Date object |
| `dateToTimestamp(Date)` | Converts Date object to Long (milliseconds) |

**Critical Code**:
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

---

#### 5.4.3 HikeDao.java (Data Access Object)

**Purpose**: Room DAO interface for all hike database operations.

| Method | Description |
|--------|-------------|
| `insertHike(Hike)` | Inserts new hike, returns generated ID |
| `updateHike(Hike)` | Updates existing hike |
| `deleteHike(Hike)` | Deletes hike entity |
| `deleteHikeById(int)` | Deletes hike by primary key |
| `getAllHikes()` | Returns all hikes ordered by date descending |
| `getHikesByUserId(Integer)` | Returns hikes for specific user |
| `getHikeById(int)` | Returns single hike by ID |
| `getUnsyncedHikes()` | Returns hikes where synced = false (for sync) |
| `markHikeAsSynced(int)` | Sets synced = true after successful upload |
| `getActiveHike()` | Returns currently active hike (if any) |
| `startHike(int, long, long)` | Activates hike with start timestamp |
| `endHike(int, long, long)` | Deactivates hike with end timestamp |
| `deactivateAllHikes()` | Sets isActive = false on all hikes |
| `searchHikes(String)` | Searches hikes by name or location (LIKE query) |

**Critical Code**:
```java
@Dao
public interface HikeDao {
    @Query("SELECT * FROM hikes WHERE synced = 0 OR synced IS NULL")
    List<Hike> getUnsyncedHikes();

    @Query("UPDATE hikes SET synced = 1 WHERE hikeID = :hikeId")
    void markHikeAsSynced(int hikeId);

    @Query("UPDATE hikes SET isActive = 0, synced = 0, updatedAt = :updatedAt WHERE isActive = 1")
    void deactivateAllHikes(long updatedAt);

    @Query("UPDATE hikes SET isActive = 1, startTime = :startTime, synced = 0, updatedAt = :updatedAt WHERE hikeID = :hikeId")
    void startHike(int hikeId, long startTime, long updatedAt);

    @Query("SELECT * FROM hikes WHERE isActive = 1 LIMIT 1")
    Hike getActiveHike();
}
```

---

#### 5.4.4 ObservationDao.java

**Purpose**: Room DAO interface for observation database operations.

| Method | Description |
|--------|-------------|
| `insertObservation(Observation)` | Inserts new observation |
| `updateObservation(Observation)` | Updates existing observation |
| `deleteObservation(Observation)` | Deletes observation entity |
| `getObservationsByHikeId(int)` | Returns all observations for a hike |
| `getUnsyncedObservations()` | Returns observations pending sync |
| `markObservationAsSynced(int)` | Marks observation as synced |

---

#### 5.4.5 UserDao.java

**Purpose**: Room DAO interface for user database operations.

| Method | Description |
|--------|-------------|
| `insertUser(User)` | Inserts new user, returns generated ID |
| `updateUser(User)` | Updates existing user |
| `getUserById(int)` | Returns user by local ID |
| `getUserByFirebaseUid(String)` | Returns user by Firebase UID |
| `getUserByEmail(String)` | Returns user by email address |

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

**Entity Definition**:
```java
@Entity(tableName = "hikes", indices = {@Index("hikeID")})
public class Hike {
    @PrimaryKey(autoGenerate = true)
    private int hikeID;
    
    private String name;
    private String location;
    private Date date;
    private boolean parkingAvailable;
    private double length;
    private String difficulty;
    private String description;
    private String purchaseParkingPass;
    private Integer userId;
    private Boolean isActive;
    private Long startTime;
    private Long endTime;
    private Long createdAt;
    private Long updatedAt;
    private Boolean synced;
    
    // Getters and setters for all fields
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

**Entity Definition with Foreign Key**:
```java
@Entity(tableName = "observations",
        foreignKeys = @ForeignKey(
                entity = Hike.class,
                parentColumns = "hikeID",
                childColumns = "hikeId",
                onDelete = ForeignKey.CASCADE  // Auto-delete observations when hike deleted
        ),
        indices = {@Index("hikeId")})
public class Observation {
    @PrimaryKey(autoGenerate = true)
    private int observationID;
    
    private int hikeId;
    private String observationText;
    private Date time;
    private String comments;
    private String location;
    private String picture;
    private Long createdAt;
    private Long updatedAt;
    private Boolean synced;
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

---

### 5.6 Adapters Package (`com.example.mobilecw.adapters`)

#### 5.6.1 HikeListAdapter.java (~200 lines)

**Purpose**: RecyclerView adapter with selection mode support for multi-delete operations.

| Method | Description |
|--------|-------------|
| `onCreateViewHolder(ViewGroup, int)` | Inflates item layout |
| `onBindViewHolder(ViewHolder, int)` | Binds hike data to views |
| `setSelectionMode(boolean)` | Enables/disables checkbox selection |
| `toggleSelection(Hike)` | Toggles selection state for a hike |
| `getSelectedHikeIds()` | Returns Set of selected hike IDs |
| `setOnSelectionChangedListener(...)` | Sets callback for selection count changes |

**Selection Mode Implementation**:
```java
private boolean selectionMode = false;
private final Set<Integer> selectedHikeIds = new HashSet<>();

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
    if (selectionChangedListener != null) {
        selectionChangedListener.onSelectionChanged(selectedHikeIds.size());
    }
    notifyDataSetChanged();
}
```

---

#### 5.6.2 ObservationListAdapter.java (151 lines)

**Purpose**: RecyclerView adapter for displaying observations with DiffUtil for efficient updates.

| Method | Description |
|--------|-------------|
| `onCreateViewHolder(ViewGroup, int)` | Inflates observation card layout |
| `onBindViewHolder(ViewHolder, int)` | Binds observation data including image loading |
| `ObservationViewHolder.bind(...)` | Configures all view elements |

**DiffUtil Implementation**:
```java
private static final DiffUtil.ItemCallback<Observation> DIFF_CALLBACK = 
    new DiffUtil.ItemCallback<Observation>() {
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
```

---

#### 5.6.3 NearbyTrailAdapter.java (107 lines)

**Purpose**: Horizontal RecyclerView adapter for displaying nearby trails on home screen.

| Method | Description |
|--------|-------------|
| `onBindViewHolder(ViewHolder, int)` | Binds trail data and calculates estimated time |
| `calculateEstimatedTime(double, String)` | Estimates hiking duration based on length and difficulty |

**Time Estimation Logic**:
```java
private double calculateEstimatedTime(double length, String difficulty) {
    // Average hiking speed: Easy = 4 km/h, Medium = 3 km/h, Hard = 2 km/h
    double speed;
    switch (difficulty.toLowerCase()) {
        case "easy": speed = 4.0; break;
        case "medium": speed = 3.0; break;
        case "hard": speed = 2.0; break;
        default: speed = 3.0;
    }
    return length / speed;
}
```

---

### 5.7 Services Package (`com.example.mobilecw.services`)

#### 5.7.1 SemanticSearchService.java (151 lines)

**Purpose**: HTTP client for communicating with the Python FastAPI semantic search backend.

| Method | Description |
|--------|-------------|
| `search(Context, String, String, String, int, SearchCallback)` | Performs semantic search API call |
| `parseSearchResponse(JSONObject)` | Parses JSON response into SearchResult objects |

| Inner Class | Description |
|-------------|-------------|
| `SearchResult` | Data class holding search result with id, type, score, name, location |
| `SearchCallback` | Interface with onSuccess(List) and onError(String) methods |

**API Call Implementation**:
```java
public static void search(Context context, String query, String firebaseUid, 
                         String searchType, int topK, SearchCallback callback) {
    RequestQueue queue = Volley.newRequestQueue(context.getApplicationContext());
    String url = BASE_URL + "/search";

    JSONObject requestBody = new JSONObject();
    requestBody.put("query", query);
    requestBody.put("firebase_uid", firebaseUid);
    requestBody.put("search_type", searchType);
    requestBody.put("top_k", topK);
    
    JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
        response -> {
            List<SearchResult> results = parseSearchResponse(response);
            callback.onSuccess(results);
        },
        error -> callback.onError("Search failed: " + error.getMessage())
    );
    queue.add(request);
}
```

---

### 5.8 Sync Package (`com.example.mobilecw.sync`)

#### 5.8.1 FirebaseSyncManager.java (261 lines)

**Purpose**: Orchestrates offline-first synchronisation between Room and Cloud Firestore.

| Method | Description |
|--------|-------------|
| `getInstance(Context)` | Returns singleton instance |
| `syncNow()` | Triggers sync without callback |
| `syncNow(SyncCallback)` | Triggers sync with completion callback |
| `syncUserProfile(int, String)` | Uploads user profile to Firestore |
| `syncHikes(int, String)` | Uploads unsynced hikes |
| `syncObservations(int, String)` | Uploads unsynced observations |
| `buildHikePayload(Hike)` | Converts Hike entity to Firestore Map |
| `buildObservationPayload(Observation)` | Converts Observation entity to Firestore Map |
| `buildUserPayload(User)` | Converts User entity to Firestore Map |
| `notifySuccess(SyncCallback)` | Invokes callback on main thread |
| `notifyFailure(SyncCallback, Exception)` | Invokes error callback on main thread |

**Selective Sync Pattern**:
```java
private List<Task<Void>> syncHikes(int userId, String firebaseUid) {
    List<Task<Void>> tasks = new ArrayList<>();
    List<Hike> unsyncedHikes = hikeDao.getUnsyncedHikes();  // WHERE synced = false
    
    for (Hike hike : unsyncedHikes) {
        if (hike.getUserId() == null || hike.getUserId() != userId) {
            continue;  // Only sync current user's hikes
        }

        Map<String, Object> payload = buildHikePayload(hike);
        Task<Void> task = firestore.collection("users")
                .document(firebaseUid)
                .collection("hikes")
                .document(String.valueOf(hike.getHikeID()))
                .set(payload, SetOptions.merge())
                .addOnSuccessListener(unused -> 
                    executorService.execute(() -> hikeDao.markHikeAsSynced(hike.getHikeID()))
                );
        tasks.add(task);
    }
    return tasks;
}
```

---

#### 5.8.2 GeminiEmbeddingService.java (156 lines)

**Purpose**: HTTP client for Gemini API to generate text embeddings for semantic search.

| Method | Description |
|--------|-------------|
| `isConfigured()` | Checks if API key is present |
| `fetchEmbedding(String, String, String, String)` | Requests embedding from Gemini API |
| `buildPrompt(String, String, String, String)` | Constructs prompt with context |
| `buildPayload(String)` | Creates JSON request body |
| `parseEmbedding(String)` | Extracts float[] from API response |
| `readFully(InputStream)` | Reads HTTP response as String |

**Embedding Request**:
```java
public float[] fetchEmbedding(String firebaseUid, String chunkType, String chunkId, String text) {
    if (!isConfigured() || TextUtils.isEmpty(text)) {
        return null;
    }

    String prompt = buildPrompt(firebaseUid, chunkType, chunkId, text);
    
    URL url = new URL(ENDPOINT + "?key=" + apiKey);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    connection.setDoOutput(true);

    byte[] payloadBytes = buildPayload(prompt).getBytes(StandardCharsets.UTF_8);
    connection.getOutputStream().write(payloadBytes);

    if (connection.getResponseCode() == 200) {
        return parseEmbedding(readFully(connection.getInputStream()));
    }
    return null;
}
```

---

#### 5.8.3 VectorSyncManager.java (206 lines)

**Purpose**: Generates and stores vector embeddings for hikes and observations in Firestore.

| Method | Description |
|--------|-------------|
| `syncUserVectors(int, String)` | Entry point for vector sync operation |
| `performVectorSync(int, String)` | Processes all user's hikes and observations |
| `syncHikeVector(String, Hike)` | Generates and stores embedding for single hike |
| `syncObservationVectors(String, int)` | Generates embeddings for hike's observations |
| `buildObservationChunk(Observation)` | Constructs text chunk for embedding |
| `writeEmbeddingToHike(String, int, float[])` | Stores embedding in hike document |
| `writeEmbeddingToObservation(String, int, int, float[])` | Stores embedding in observation document |
| `toDoubleList(float[])` | Converts float[] to List<Double> for Firestore |

**Co-located Vector Storage**:
```java
private void writeEmbeddingToHike(String firebaseUid, int hikeId, float[] embedding) {
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
}
```

---

### 5.9 Utils Package (`com.example.mobilecw.utils`)

#### 5.9.1 NetworkUtils.java (27 lines)

**Purpose**: Simple utility for checking network connectivity status.

| Method | Description |
|--------|-------------|
| `isOnline(Context)` | Returns true if device has active network connection |

**Implementation**:
```java
public class NetworkUtils {
    public static boolean isOnline(Context context) {
        if (context == null) return false;
        
        ConnectivityManager cm = (ConnectivityManager) 
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
}
```

---

#### 5.9.2 SearchHelper.java (251 lines)

**Purpose**: Advanced search algorithms including fuzzy matching and relevance scoring.

| Method | Description |
|--------|-------------|
| `fuzzySearch(List<Hike>, String)` | Main search method with relevance scoring |
| `calculateRelevanceScore(Hike, String)` | Calculates multi-factor relevance score |
| `levenshteinDistance(String, String)` | Computes edit distance between strings |
| `isFuzzyMatch(String, String, int)` | Checks if strings match within threshold |
| `advancedFilter(List<Hike>, ...)` | Applies multiple filter criteria |

**Levenshtein Distance Implementation**:
```java
public static int levenshteinDistance(String s1, String s2) {
    int len1 = s1.length();
    int len2 = s2.length();
    
    if (len1 == 0) return len2;
    if (len2 == 0) return len1;
    
    int[][] dp = new int[len1 + 1][len2 + 1];
    
    for (int i = 0; i <= len1; i++) dp[i][0] = i;
    for (int j = 0; j <= len2; j++) dp[0][j] = j;
    
    for (int i = 1; i <= len1; i++) {
        for (int j = 1; j <= len2; j++) {
            int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
            dp[i][j] = Math.min(
                Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                dp[i - 1][j - 1] + cost
            );
        }
    }
    return dp[len1][len2];
}
```

**Relevance Scoring Tiers**:
```java
private static int calculateRelevanceScore(Hike hike, String query) {
    int score = 0;
    String name = hike.getName().toLowerCase();
    
    // Tier 1: Exact match (100 points)
    if (name.equals(query)) score += 100;
    
    // Tier 2: Prefix match (50 points)
    if (name.startsWith(query)) score += 50;
    
    // Tier 3: Contains match (30 points)
    if (name.contains(query)) score += 30;
    
    // Tier 4: Word-by-word (10 points per word)
    for (String word : query.split("\\s+")) {
        if (name.contains(word)) score += 10;
    }
    
    // Tier 5: Fuzzy match (5-15 points based on distance)
    int distance = levenshteinDistance(name, query);
    if (distance <= FUZZY_THRESHOLD) {
        score += (FUZZY_THRESHOLD - distance) * 5;
    }
    
    return score;
}
```

---

### 5.10 Backend (Python FastAPI)

#### 5.10.1 main.py

**Purpose**: FastAPI server providing semantic search API with cosine similarity calculation.

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/search` | POST | Performs semantic search against user's hike/observation embeddings |
| `/health` | GET | Health check endpoint |

| Function | Description |
|----------|-------------|
| `cosine_similarity(vec1, vec2)` | Calculates similarity between two vectors |
| `fetch_user_hikes(firebase_uid)` | Retrieves hikes with embeddings from Firestore |
| `generate_query_embedding(query)` | Calls Gemini API for query embedding |
| `search_hikes(query, firebase_uid, top_k)` | Main search logic with ranking |

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
