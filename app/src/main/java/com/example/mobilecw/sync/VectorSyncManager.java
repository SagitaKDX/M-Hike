package com.example.mobilecw.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.mobilecw.database.AppDatabase;
import com.example.mobilecw.database.dao.HikeDao;
import com.example.mobilecw.database.dao.ObservationDao;
import com.example.mobilecw.database.entities.Hike;
import com.example.mobilecw.database.entities.Observation;
import com.example.mobilecw.utils.NetworkUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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


