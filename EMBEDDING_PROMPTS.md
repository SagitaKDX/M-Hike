# Embedding Prompts for Hikes and Observations

This document shows where and how embedding prompts are built for hikes and observations.

## Location of Embedding Prompts

### 1. Prompt Builder Method
**File:** `app/src/main/java/com/example/mobilecw/sync/GeminiEmbeddingService.java`

**Method:** `buildPrompt()` (lines 106-116)

```106:116:app/src/main/java/com/example/mobilecw/sync/GeminiEmbeddingService.java
    private String buildPrompt(String firebaseUid, String chunkType, String chunkId, String text) {
        String trimmed = text.length() > MAX_PROMPT_LENGTH
                ? text.substring(0, MAX_PROMPT_LENGTH)
                : text;
        return "You are Gemini 1.5 Flash acting strictly as an embedder. "
                + "Return only the raw embedding without commentary.\n"
                + "User UID: " + firebaseUid + "\n"
                + "Chunk ID: " + chunkId + "\n"
                + "Chunk Type: " + chunkType + "\n"
                + "Text:\n"
                + trimmed;
    }
```

### 2. Hike Embedding Request
**File:** `app/src/main/java/com/example/mobilecw/sync/VectorSyncManager.java`

**Method:** `syncHikeVector()` (lines 87-106)

**Where it's called:**
- Line 100: `embeddingService.fetchEmbedding(firebaseUid, chunkType, chunkId, builder.toString())`

**Text content for hikes:**
```87:106:app/src/main/java/com/example/mobilecw/sync/VectorSyncManager.java
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
```

**Example prompt for a hike:**
```
You are Gemini 1.5 Flash acting strictly as an embedder. 
Return only the raw embedding without commentary.
User UID: abc123xyz
Chunk ID: hike_1
Chunk Type: hike_description
Text:
Hike: Mountain Peak Trail
Location: Rocky Mountain
Difficulty: Hard
LengthKm: 8.5
Description: A challenging trail with beautiful views
```

### 3. Observation Embedding Request
**File:** `app/src/main/java/com/example/mobilecw/sync/VectorSyncManager.java`

**Method:** `syncObservationVectors()` (lines 108-127)

**Where it's called:**
- Line 120: `embeddingService.fetchEmbedding(firebaseUid, chunkType, chunkId, text)`

**Text content for observations:**
```108:127:app/src/main/java/com/example/mobilecw/sync/VectorSyncManager.java
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
```

**Observation text builder:**
```129:139:app/src/main/java/com/example/mobilecw/sync/VectorSyncManager.java
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
```

**Example prompt for an observation:**
```
You are Gemini 1.5 Flash acting strictly as an embedder. 
Return only the raw embedding without commentary.
User UID: abc123xyz
Chunk ID: obs_5
Chunk Type: observation_note
Text:
Observation: Saw a beautiful eagle flying overhead
Comments: Very majestic bird
Location: Near the summit
```

## Flow Summary

1. **VectorSyncManager.performVectorSync()** (line 76-84)
   - Gets all hikes for the user
   - For each hike: calls `syncHikeVector()`
   - For each hike: calls `syncObservationVectors()`

2. **For Hikes:**
   - `syncHikeVector()` builds text from hike fields (name, location, difficulty, length, description)
   - Calls `embeddingService.fetchEmbedding()` with `chunkType = "hike_description"`
   - Stores embedding in Firestore at `users/{uid}/hikes/{hikeId}`

3. **For Observations:**
   - `syncObservationVectors()` gets all observations for a hike
   - For each observation: `buildObservationChunk()` builds text from observation fields
   - Calls `embeddingService.fetchEmbedding()` with `chunkType = "observation_note"`
   - Stores embedding in Firestore at `users/{uid}/hikes/{hikeId}/observations/{obsId}`

4. **GeminiEmbeddingService.fetchEmbedding()** (line 62-104)
   - Builds the prompt using `buildPrompt()`
   - Makes HTTP POST request to Gemini Embedding API
   - Parses the response and returns float array

## When Embeddings Are Generated

Embeddings are generated during Firebase sync:
- Called from `FirebaseSyncManager.syncNow()` (line 106)
- Only runs when:
  - User is logged in
  - Device is online
  - Gemini API key is configured
- Processes all hikes and observations for the logged-in user

