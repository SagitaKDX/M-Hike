# Gemini Vector Embedding Setup Guide

This guide explains how to set up and use the Gemini vector embedding system in the M-Hike app.

---

## Overview

The app automatically generates **vector embeddings** for your hikes and observations using Google's Gemini API. These embeddings are stored in Firebase Firestore under each user's account, enabling semantic search capabilities.

**What happens:**
1. When you create/edit a hike or observation, the text is sent to Gemini API
2. Gemini returns a vector (array of numbers) representing the semantic meaning
3. The vector is stored in Firestore: `users/{uid}/vectors/{chunkId}`
4. Later, you can search for similar content using cosine similarity

---

## Step 1: Get a Gemini API Key

1. Go to [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Sign in with your Google account
3. Click **"Create API Key"**
4. Select your Google Cloud project (or create a new one)
5. Copy the API key (it looks like: `AIzaSy...`)

**Important:** Keep this key secret! Never commit it to Git.

---

## Step 2: Add API Key to Your Project

1. In your project root directory (same level as `app/`), create or edit `local.properties`
2. Add this line:
   ```
   gemini.apiKey=YOUR_API_KEY_HERE
   ```
   Replace `YOUR_API_KEY_HERE` with the actual key from Step 1.

3. **Verify `local.properties` is in `.gitignore`:**
   - Open `.gitignore` in the project root
   - Make sure it contains: `local.properties`
   - If not, add it (this prevents accidentally committing your API key)

4. **Sync Gradle:**
   - In Android Studio: **File → Sync Project with Gradle Files**
   - Or click the "Sync Now" banner if it appears

---

## Step 3: Configure Firestore Security Rules

The vectors are stored under `users/{uid}/vectors/`. You need to secure this collection.

1. Open [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to **Firestore Database → Rules**
4. Add this rule to your existing rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Existing rules for users, hikes, observations...
    
    // Vector storage: users can only read/write their own vectors
    match /users/{uid}/vectors/{vectorId} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
```

5. Click **"Publish"** to save the rules

---

## Step 4: How It Works

### Automatic Vector Generation

Vectors are created automatically when:

1. **You log in or sign up** → Existing hikes/observations are synced
2. **You create/edit a hike** → The hike description is embedded
3. **You create/edit an observation** → The observation text is embedded
4. **You return to the Hiking List screen** (if online) → Pending data is synced

### What Gets Embedded

**For each Hike:**
- Name, location, difficulty, length, description
- Stored as: `users/{uid}/vectors/hike_{hikeId}`

**For each Observation:**
- Observation text, comments, location
- Stored as: `users/{uid}/vectors/obs_{observationId}`

### The Embedding Process

1. `VectorSyncManager` collects all hikes/observations for the logged-in user
2. For each item, it builds a text chunk (name, description, etc.)
3. `GeminiEmbeddingService` sends the text to Gemini API with this prompt:
   ```
   You are Gemini 1.5 Flash acting strictly as an embedder.
   Return only the raw embedding without commentary.
   User UID: {firebaseUid}
   Chunk ID: {chunkId}
   Chunk Type: {hike_description|observation_note}
   Text: {actual text content}
   ```
4. Gemini returns a float array (typically 768 dimensions)
5. The vector is stored in Firestore with metadata:
   - `text`: Original text
   - `embedding`: Array of floats (converted to doubles for Firestore)
   - `chunkType`: "hike_description" or "observation_note"
   - `hikeId`: Link to the hike (if applicable)
   - `createdAt`, `updatedAt`: Timestamps
   - `vectorSource`: "gemini_text-embedding-004"

---

## Step 5: Verify It's Working

### Check Logcat

1. Open **Logcat** in Android Studio
2. Filter by: `VectorSyncManager` or `GeminiEmbeddingService`
3. Look for messages like:
   - `"Syncing vectors for user..."` (success)
   - `"Gemini API key missing; skipping vector sync"` (API key not set)
   - `"Device offline; skipping vector sync"` (no internet)

### Check Firestore Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Open **Firestore Database**
3. Navigate to: `users → {your-uid} → vectors`
4. You should see documents like:
   - `hike_1` (for hike ID 1)
   - `obs_5` (for observation ID 5)
5. Click a document to see:
   - `text`: The original text
   - `embedding`: An array of numbers
   - `chunkType`: "hike_description" or "observation_note"

### Test the Flow

1. **Make sure you're logged in** (check Users page)
2. **Create a new hike** with a description
3. **Go back to Hiking List** (triggers sync if online)
4. **Wait a few seconds** (vector generation takes time)
5. **Check Firestore** → `users/{uid}/vectors/hike_{newHikeId}` should appear

---

## Step 6: Troubleshooting

### Problem: "Gemini API key missing"

**Solution:**
- Check `local.properties` exists in project root
- Verify the line: `gemini.apiKey=YOUR_KEY`
- **Sync Gradle** after editing `local.properties`
- Rebuild the app: **Build → Rebuild Project**

### Problem: "Gemini API error (401)" or "403"

**Solution:**
- Your API key is invalid or expired
- Get a new key from [Google AI Studio](https://aistudio.google.com/app/apikey)
- Update `local.properties` and sync Gradle

### Problem: "Device offline; skipping vector sync"

**Solution:**
- This is normal! Vectors only sync when online
- Make sure you have internet connection
- Vectors will sync automatically when you go online and return to Hiking List

### Problem: No vectors in Firestore

**Possible causes:**
1. **Not logged in** → Vectors only sync for logged-in users
2. **No hikes/observations** → Create some first
3. **API key not set** → Check Step 2
4. **Firestore rules blocking writes** → Check Step 3
5. **Sync hasn't run yet** → Go to Hiking List screen (triggers sync)

### Problem: Vectors are being created but I can't see them

**Solution:**
- Check Firestore Console → `users/{uid}/vectors`
- Make sure you're looking at the correct `uid` (check `SessionManager.getCurrentFirebaseUid()`)
- Refresh the Firestore console page

---

## Step 7: Using Vectors for Semantic Search (Future)

Once vectors are stored, you can implement semantic search:

1. **Query embedding:** Send a search query to Gemini to get its embedding
2. **Fetch user vectors:** Load all vectors from `users/{uid}/vectors`
3. **Compute similarity:** Calculate cosine similarity between query and stored vectors
4. **Rank results:** Sort by similarity score and return top matches

**Example cosine similarity (pseudo-code):**
```java
float cosineSimilarity(float[] a, float[] b) {
    float dot = 0, normA = 0, normB = 0;
    for (int i = 0; i < a.length; i++) {
        dot += a[i] * b[i];
        normA += a[i] * a[i];
        normB += b[i] * b[i];
    }
    return dot / (Math.sqrt(normA) * Math.sqrt(normB));
}
```

---

## Architecture Summary

```
User creates Hike/Observation
    ↓
Saved to Room (local database)
    ↓
FirebaseSyncManager.syncNow() called
    ↓
VectorSyncManager.syncUserVectors() called
    ↓
For each hike/observation:
    ↓
GeminiEmbeddingService.fetchEmbedding()
    ↓
HTTP POST to Gemini API
    ↓
Gemini returns float[] embedding
    ↓
Write to Firestore: users/{uid}/vectors/{chunkId}
```

---

## Important Notes

- **API Quotas:** Gemini API has rate limits. The app processes vectors sequentially to avoid overwhelming the API.
- **Offline Support:** Vectors are only generated when online. Offline data will sync when you reconnect.
- **Privacy:** Each user's vectors are isolated under their Firebase UID. Users can only access their own vectors.
- **Cost:** Gemini API has free tier limits. Check [Google AI Studio pricing](https://aistudio.google.com/pricing) for details.

---

## Next Steps

- Implement semantic search UI (search bar that queries vectors)
- Add vector similarity scoring to search results
- Consider caching embeddings to avoid regenerating them
- Add batch processing for large datasets

---

## Support

If you encounter issues:
1. Check Logcat for error messages
2. Verify API key is set correctly
3. Ensure Firestore rules allow writes
4. Make sure you're logged in and online

