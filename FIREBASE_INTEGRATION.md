## Firebase Integration Overview

This document explains how Firebase is used in the **M‑Hike** Android app and how the data flows between:
- Firebase **Authentication**
- Cloud **Firestore**
- Local **Room** database

---

## 1. Identity and configuration

- The app is registered in Firebase with:
  - **Package name**: `com.example.mobilecw`
  - Android app entry in Firebase Console (Project settings → General → Android apps).
- The Android project contains `app/google-services.json`, downloaded from that page.
- The Gradle plugin `com.google.gms.google-services` reads `google-services.json` and injects the Firebase config (project ID, API key, app ID) into the app at build time.
- The debug keystore’s **SHA‑1** and **SHA‑256** fingerprints are added in Firebase so that Firebase Auth can validate requests from this app instance.

Result: when the app runs, `FirebaseApp.initializeApp(context)` connects to the correct Firebase project automatically.

---

## 2. Authentication model

### Firebase Auth

- The app uses **Email/Password** authentication via `FirebaseAuth`:
  - `createUserWithEmailAndPassword(email, password)` for signup.
  - `signInWithEmailAndPassword(email, password)` for login.
- On success, Firebase returns a **UID**:
  - `String uid = firebaseUser.getUid();`
  - This UID is the **global identity** for the user across Firebase (Auth + Firestore).

### Local session (`SessionManager`)

- `SessionManager` stores:
  - `userId` (int) – local Room primary key.
  - `firebaseUid` (String) – UID from Firebase Auth.
- On login/signup:
  - `SessionManager.setCurrentUserId(context, userId);`
  - `SessionManager.setCurrentFirebaseUid(context, firebaseUid);`
- On logout:
  - `SessionManager.clearCurrentUser(context);` clears both values.

This provides a bridge between the local database and Firebase.

---

## 3. Local data model (Room)

Room database (`AppDatabase`) holds three entities:

- `User`
- `Hike`
- `Observation`

Key details:

- `User`:
  - `userId` – `@PrimaryKey(autoGenerate = true)` (local integer ID).
  - `firebaseUid` – Firebase Auth UID string.
  - `userName`, `userEmail`, `userPassword`, `userPhone`, timestamps, etc.
- `Hike` / `Observation`:
  - Reference `userId` (local) and `hikeId` for relationships.
  - Include a `synced` flag to indicate if the record has been pushed to Firestore.

The database version is currently **6**, and `fallbackToDestructiveMigration()` is enabled for development (schema changes wipe and recreate the database).

---

## 4. Cloud data model (Firestore)

The app uses **pattern 1: per‑user subcollections**:

```text
users (collection)
  └─ {uid} (document)              ← Firebase Auth user UID
       ├─ profile fields (name, email, phone, timestamps)
       ├─ hikes (subcollection)
       │    └─ {hikeId} (document with hike fields)
       └─ hikes/{hikeId}/observations (subcollection)
            └─ {observationId} (document with observation fields)
```

- `{uid}` matches `firebaseUid` from Firebase Auth.
- `{hikeId}` and `{observationId}` are based on the local Room IDs.

This structure makes it clear which hikes/observations belong to which authenticated user.

---

## 5. Sync engine (`FirebaseSyncManager`)

`FirebaseSyncManager` is responsible for pushing local Room data to Firestore.

### Initialization

- In the singleton constructor:
  - `FirebaseApp.initializeApp(appContext);`
  - `firestore = FirebaseFirestore.getInstance();`
  - `AppDatabase.getDatabase(appContext)` to obtain DAOs.

### Public API

- `syncNow()` – convenience method that calls `syncNow(null)`.
- `syncNow(SyncCallback callback)` – syncs all unsynced data for the currently logged‑in user, then invokes:
  - `onSuccess()` if all writes succeeded or nothing needed syncing.
  - `onFailure(Exception e)` if any write failed.

### What `syncNow` does

1. Reads session:
   - `userId = SessionManager.getCurrentUserId(appContext);`
   - `firebaseUid = SessionManager.getCurrentFirebaseUid(appContext);`
2. If either is missing → logs and returns.
3. Creates a list of Firebase Tasks:
   - `syncUserProfile(userId, firebaseUid)`:
     - Reads `User` from Room.
     - Writes to `users/{uid}` with profile fields and timestamps.
   - `syncHikes(userId, firebaseUid)`:
     - Reads all unsynced hikes from Room.
     - Filters to hikes belonging to `userId`.
     - For each, writes a document under `users/{uid}/hikes/{hikeId}`.
     - On success, marks the hike as `synced` locally.
   - `syncObservations(userId, firebaseUid)`:
     - Reads all unsynced observations.
     - Only keeps observations whose `hikeId` belongs to the current user.
     - Writes documents under `users/{uid}/hikes/{hikeId}/observations/{observationId}`.
     - On success, marks the observation as `synced`.
4. Uses `Tasks.whenAllSuccess(pendingTasks)` to wait for all Firebase writes.

---

## 6. Signup / login flows with Firebase

### Signup

1. Validate the form.
2. Check if email already exists in Room via `UserDao.getUserByEmail(email)`.
3. Call `FirebaseAuth.createUserWithEmailAndPassword(email, password)`.
4. On success:
   - Get `firebaseUid`.
   - Insert `User` into Room with `firebaseUid` set.
   - Store `userId` + `firebaseUid` in `SessionManager`.
   - Call `FirebaseSyncManager.syncNow()` – creates/updates `users/{uid}` in Firestore.
   - Navigate to `UsersActivity`.

### Login

1. Validate form input.
2. Call `FirebaseAuth.signInWithEmailAndPassword(email, password)`.
3. On success:
   - Get `firebaseUid`.
   - In Room:
     - Try `getUserByEmail(email)`.
     - If missing, create a new `User` with that email/password and `firebaseUid`.
     - If present, update its `firebaseUid`.
   - Store `userId` + `firebaseUid` in `SessionManager`.
   - Call `FirebaseSyncManager.syncNow()` to push any local data to Firestore.
   - Navigate to `UsersActivity`.

---

## 7. Logout and single‑user local cache behavior

The app enforces **one local user at a time**:

- In `SettingsActivity.handleLogout()`:
  1. If not logged in → show message and return.
  2. Disable the logout button and show a "Syncing..." state.
  3. Call `FirebaseSyncManager.syncNow(callback)`.
  4. On `onSuccess()`:
     - In a background thread: `database.clearAllTables()` (delete all local users, hikes, observations).
     - On UI thread:
       - `SessionManager.clearCurrentUser(context)` (removes userId + firebaseUid).
       - Show "You have been logged out."
       - Navigate back to `UsersActivity`.
  5. On `onFailure()`:
     - Re‑enable logout button, restore label.
     - Show an error toast.
     - **Local data is not deleted** to avoid data loss.

This guarantees that:
- All of a user’s hikes/observations are synced to Firestore before local wipe.
- After logout, the next signup/login starts from a clean local database.

---

## 8. How to verify data in Firebase

1. Open Firebase Console → **Firestore Database**.
2. In the collections list:
   - You should see a `users` collection.
   - Each document ID is a Firebase Auth UID (e.g. `Qd9...xyz`).
3. Inside a user document:
   - Check the profile fields (displayName, email, phone, etc.).
   - Open the `hikes` subcollection:
     - Documents correspond to local hikes (`hikeId`).
   - For a given hike, open the `observations` subcollection to see synced observations.

If you don’t see data:
- Make sure you are logged in (Firebase Auth + SessionManager have valid UID and userId).
- Perform an action that creates or updates hikes/observations.
- Trigger a sync (it runs on login, signup, and on certain actions; or explicitly from code using `syncNow()`).

---

## 9. Gemini vectors (per-user semantic search)

### 9.1 API key and build config

1. Add your Gemini API key to `local.properties` (never commit this file):
   ```
   gemini.apiKey=YOUR_GEMINI_KEY
   ```
2. Gradle now exposes this as `BuildConfig.GEMINI_API_KEY` via:
   ```kotlin
   buildConfigField("String", "GEMINI_API_KEY", "\"${geminiApiKey}\"")
   ```
3. At runtime, if the key is missing, vector sync silently skips itself.

### 9.2 Vector generation flow

- `VectorSyncManager` gathers user-owned hikes and their observations.
- For each piece of text it builds a chunk (e.g. `"hike_description"`, `"observation_note"`).
- `GeminiEmbeddingService` calls `models/text-embedding-004:embedContent` with a strict prompt:
  - Includes `firebaseUid`, chunk type, chunkId, and truncated text.
  - Returns a float array embedding (handled synchronously on a background thread).
- The embedding plus metadata is stored under `users/{uid}/vectors/{chunkId}`:
  ```json
  {
    "text": "...original chunk...",
    "chunkType": "hike_description",
    "hikeId": 12,
    "embedding": [0.0123, -0.4421, ...],
    "vectorSource": "gemini_text-embedding-004",
    "createdAt": 1732812345123,
    "updatedAt": 1732812345123
  }
  ```

### 9.3 When vector sync runs

- `FirebaseSyncManager.syncNow()` now adds `vectorSyncManager.syncUserVectors(...)` to the same Task bundle.
- Requirements for a run:
  - User logged in (valid `firebaseUid` + `userId`).
  - Device online (`NetworkUtils.isOnline()`).
  - Gemini API key configured.
- Because embeddings can be large, the upload pipeline is sequential with basic throttling.

### 9.4 Querying vectors

- Firestore doesn’t provide ANN search, so the app should:
  1. Query `users/{uid}/vectors` with whatever filters you need (e.g. `chunkType`, `hikeId`).
  2. Download embeddings and compute cosine similarity client-side or inside a Cloud Function.
  3. Use the top-N matches as context for Gemini or to highlight relevant hikes/observations.
- Security rules should ensure `request.auth.uid == uid` for the vectors subcollection.

---

## 10. Notes for future development

- Replace `fallbackToDestructiveMigration()` with explicit Room migrations before release.
- Harden Firestore rules: restrict reads/writes to `request.auth.uid == uid`.
- Store password hashes instead of plain text if you keep them locally.
- Consider enabling Firestore offline persistence for direct cloud reads.
- If you need faster semantic search, replicate vector docs to a managed vector DB (e.g. Vertex AI Vector Search, Pinecone) and store only references in Firestore.

