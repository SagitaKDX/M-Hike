<!-- 2c79ad7b-0d76-4b1e-915a-59eb38effa65 d81423ab-2b04-4728-ac14-0ee5e655ad37 -->
# M-Hike: Hiker Management App - Development Plan

## Tech Stack Overview

### Native Android (Java) - Features a-d

- **Language**: Java 11
- **UI Framework**: Android Views with Material Design Components
- **Database**: SQLite with Room Persistence Library (offline-first store)
- **Architecture**: MVC/MVP pattern
- **Dependencies**: 
  - Material Design Components
  - Room Database (or SQLiteOpenHelper)
  - RecyclerView for lists
  - Date/Time pickers
  - Volley/Retrofit for semantic-search HTTP calls (future)
  - SharedPreferences for lightweight settings
  - Firebase Auth + Cloud Firestore (registered-user sync)

### Xamarin/MAUI - Features e-f

- **Framework**: .NET MAUI (Multi-platform App UI)
- **Language**: C#
- **Database**: SQLite.NET or Entity Framework Core
- **UI**: XAML with MAUI controls
- **Platforms**: Android, iOS (if needed)
- **Cloud**: Firebase SDK or REST API client

### Additional Features (g)

- Camera integration for photos
- Location services (GPS)
- Maps integration (Google Maps)
- External API integration (Semantic Search API) — planned for a later phase while keeping the app single-user/offline-first today.
- Firebase cloud sync for registered users (Auth, Firestore mirroring)

## Implementation Plan

### Phase 1: Native Android Core Features (50%)

#### 1.1 Database Schema Design

- **Files**: `app/src/main/java/com/example/mobilecw/database/HikeDatabase.java`, `HikeContract.java`
- Create SQLite database schema:
  - `hikes` table: id, name, location, date, parking, length, difficulty, description, weather, duration, created_at
  - `observations` table: id, hike_id, observation, time, comments, created_at
- Use Room Persistence Library or SQLiteOpenHelper

#### 1.2 Feature a: Enter Hike Details (10%)

- **Files**: 
  - `EnterHikeActivity.java` - Main entry form
  - `layout/activity_enter_hike.xml` - Form layout
- **Fields**:
  - Name (EditText, required)
  - Location (EditText, required)
  - Date (DatePicker, required)
  - Parking (RadioGroup: Yes/No, required)
  - Length (EditText with number input, required)
  - Difficulty (Spinner: Easy/Medium/Hard, required)
  - Description (Multi-line EditText, optional)
  - Weather (Spinner, custom field)
  - Estimated Duration (TimePicker, custom field)
- **Validation**: Check all required fields before submission
- **Confirmation Screen**: Display entered details for review before saving

#### 1.3 Feature b: Store, View, Edit, Delete Hikes (15%)

- **Files**:
  - `HikeListActivity.java` - List all hikes
  - `HikeDetailActivity.java` - View/edit hike details
  - `layout/activity_hike_list.xml` - RecyclerView layout
  - `layout/activity_hike_detail.xml` - Detail/edit layout
  - `HikeAdapter.java` - RecyclerView adapter
- **Features**:
  - List all hikes in RecyclerView
  - Click to view details
  - Edit mode in detail screen
  - Delete individual hike (with confirmation)
  - Reset database option (menu item with confirmation)

#### 1.4 Feature c: Add Observations (15%)

- **Files**:
  - `ObservationActivity.java` - Add/view observations
  - `layout/activity_observation.xml` - Observation form
  - `ObservationListActivity.java` - List observations for a hike
  - `layout/activity_observation_list.xml` - Observations list
- **Features**:
  - Select hike from list
  - Add observation with:
    - Observation text (required)
    - Time (defaults to current time, required)
    - Comments (optional)
  - View all observations for selected hike
  - Edit/delete individual observations

#### 1.5 Feature d: Search Functionality (10%)

- **Files**:
  - `SearchActivity.java` - Search interface
  - `layout/activity_search.xml` - Search form
  - `SearchResultsActivity.java` - Display search results
- **Features**:
  - Simple search: Name (partial match, shows all matches)
  - Advanced search: Name, Location, Length range, Date range
  - Display results in RecyclerView
  - Click result to view full details

#### 1.6 Feature g: Firebase Cloud Sync for Registered Users (10%)

- **Files**:
  - `CloudSyncManager.java` (helper)
  - `AuthActivity.java` / `LoginFragment` (or integrate into existing flow)
  - Updates to `EnterHikeActivity`, `HikingListActivity`, `ObservationListActivity`, DAOs
- **Steps**:
  - Configure Firebase project, add `google-services.json`, enable Email/Password auth
  - Add Firebase Auth sign-up/login flow; persist the Firebase UID in `SharedPreferences`
  - Model Firestore structure: `users/{uid}/hikes/{hikeId}` and subcollection `observations`
  - On every local create/update/delete, mirror the change to Firestore when a UID exists
  - On registration/login run a one-time upload of unsynced local hikes/observations, then optionally pull down cloud data and merge (using `updatedAt` timestamps)
  - Keep Room as the UI data source; Firestore is treated as remote backup for registered users

### Phase 2: Xamarin/MAUI Implementation (20%)

#### 2.1 Feature e: Cross-platform Prototype (10%)

- **Project Structure**: Create separate MAUI project
- **Files**:
  - `MauiProgram.cs` - App initialization
  - `Views/EnterHikePage.xaml` - Hike entry form
  - `ViewModels/EnterHikeViewModel.cs` - MVVM pattern
  - `Models/Hike.cs` - Data model
- **Implementation**: Replicate Feature a) using MAUI framework
- **UI**: XAML with MAUI controls (Entry, DatePicker, Picker, etc.)

#### 2.2 Feature f: MAUI Persistence (10%)

- **Files**:
  - `Services/DatabaseService.cs` - SQLite service
  - `Repositories/HikeRepository.cs` - Data access layer
- **Implementation**: 
  - Use SQLite.NET or Entity Framework Core
  - Implement CRUD operations for hikes
  - Replicate Feature b) functionality

### Phase 3: Additional Features (10%)

#### 3.1 Camera Integration

- **Files**: `CameraActivity.java`, `ImageHelper.java`
- Add photo capture capability to hikes/observations
- Store image paths in database
- Display images in detail views

#### 3.2 Location Services

- **Files**: `LocationHelper.java`
- Auto-populate location using GPS
- Request location permissions
- Use Android Location Services

#### 3.3 Maps Integration

- **Files**: `MapActivity.java`
- Show hike location on Google Maps
- Mark observation locations on map
- Requires Google Maps API key

## File Structure

```
app/src/main/java/com/example/mobilecw/
├── MainActivity.java (Navigation hub)
├── database/
│   ├── HikeDatabase.java
│   ├── HikeDao.java
│   ├── ObservationDao.java
│   └── entities/
│       ├── Hike.java
│       └── Observation.java
├── activities/
│   ├── EnterHikeActivity.java
│   ├── HikeListActivity.java
│   ├── HikeDetailActivity.java
│   ├── ObservationActivity.java
│   ├── ObservationListActivity.java
│   ├── SearchActivity.java
│   └── SearchResultsActivity.java
├── adapters/
│   ├── HikeAdapter.java
│   └── ObservationAdapter.java
├── utils/
│   ├── ValidationHelper.java
│   ├── LocationHelper.java
│   └── ImageHelper.java
└── models/
    ├── Hike.java
    └── Observation.java
```

## Dependencies to Add

### build.gradle.kts additions:

- Room Database: `androidx.room:room-runtime:2.6.1`
- Room Compiler: `androidx.room:room-compiler:2.6.1` (kapt)
- RecyclerView: `androidx.recyclerview:recyclerview:1.3.2`
- Google Maps: `com.google.android.gms:play-services-maps:18.2.0`
- Location Services: `com.google.android.gms:play-services-location:21.0.1`
- Firebase BOM: `com.google.firebase:firebase-bom:33.4.0`
- Firebase Auth: `com.google.firebase:firebase-auth`
- Firebase Firestore: `com.google.firebase:firebase-firestore`

## Key Implementation Notes

1. **Database**: Use Room for type-safe database access and easier maintenance
2. **UI/UX**: Follow Material Design guidelines, use appropriate input controls
3. **Validation**: Implement comprehensive input validation with user-friendly error messages
4. **Navigation**: Use Intent-based navigation between activities
5. **Data Models**: Create proper Java classes for Hike and Observation
6. **Error Handling**: Handle database errors, null values, and edge cases gracefully
7. **Testing**: Test on different screen sizes and Android versions (API 24+)

## Development Order

1. Set up database schema and models
2. Implement Feature a) (Enter Hike Details)
3. Implement Feature b) (CRUD operations)
4. Implement Feature c) (Observations)
5. Implement Feature d) (Search)
6. Create Xamarin/MAUI project structure
7. Implement MAUI features e) and f)
8. Add additional features (camera, location, maps)
9. Polish UI/UX and test thoroughly

### To-dos

- [ ] Set up Room database with Hike and Observation entities, DAOs, and Repository classes
- [ ] Implement Feature a: Enter hike details form with validation, confirmation screen, and database storage
- [ ] Implement Feature b: List all hikes, edit, delete individual hikes, and reset database functionality
- [ ] Implement Feature c: Add observations to hikes with time auto-population, view/edit/delete observations
- [ ] Implement Feature d: Basic and advanced search functionality (name, location, length, date)
- [ ] Create Xamarin/MAUI project and set up project structure with MVVM pattern
- [ ] Implement Feature e: Port Feature a to Xamarin/MAUI with XAML UI and data binding
- [ ] Implement Feature f: Port Feature b to Xamarin/MAUI with SQLite persistence
- [ ] Implement additional features: Camera integration, location services, and server upload functionality