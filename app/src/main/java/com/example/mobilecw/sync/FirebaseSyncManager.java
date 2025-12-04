package com.example.mobilecw.sync;

import android.content.Context;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.example.mobilecw.auth.SessionManager;
import com.example.mobilecw.database.AppDatabase;
import com.example.mobilecw.database.dao.HikeDao;
import com.example.mobilecw.database.dao.ObservationDao;
import com.example.mobilecw.database.dao.UserDao;
import com.example.mobilecw.database.entities.Hike;
import com.example.mobilecw.database.entities.Observation;
import com.example.mobilecw.database.entities.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    /**
     * Download all hikes (and their observations) for the current Firebase user
     * from Firestore into the local Room database. This is intended to be used
     * after a fresh login on a device where local tables were cleared on logout.
     */
    public void downloadUserData(SyncCallback callback) {
        int userId = SessionManager.getCurrentUserId(appContext);
        String firebaseUid = SessionManager.getCurrentFirebaseUid(appContext);

        if (userId == -1 || firebaseUid == null) {
            Log.d(TAG, "downloadUserData: no logged-in user; skipping");
            notifySuccess(callback);
            return;
        }

        // Fetch all hikes for this user from Firestore
        firestore.collection("users")
                .document(firebaseUid)
                .collection("hikes")
                .get()
                .addOnSuccessListener(querySnapshot -> executorService.execute(() -> {
                    try {
                        // Clear local hikes/observations before re-populating
                        hikeDao.deleteAllHikes();
                        observationDao.deleteAllObservations();

                        List<Task<QuerySnapshot>> observationTasks = new ArrayList<>();

                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            if (!doc.exists()) continue;

                            // Parse hike fields defensively
                            Hike hike = new Hike();
                            try {
                                String id = doc.getId();
                                int hikeId = Integer.parseInt(id);
                                hike.setHikeID(hikeId);
                            } catch (NumberFormatException e) {
                                // If ID is not an integer, skip this hike
                                continue;
                            }

                            hike.setUserId(userId);
                            hike.setName(doc.getString("name"));
                            hike.setLocation(doc.getString("location"));

                            Long dateMillis = doc.getLong("date");
                            if (dateMillis != null) {
                                hike.setDate(new java.util.Date(dateMillis));
                            }

                            Boolean parkingAvailable = doc.getBoolean("parkingAvailable");
                            hike.setParkingAvailable(parkingAvailable != null && parkingAvailable);

                            Double length = doc.getDouble("length");
                            if (length != null) {
                                hike.setLength(length);
                            }

                            hike.setDifficulty(doc.getString("difficulty"));
                            hike.setDescription(doc.getString("description"));
                            hike.setPurchaseParkingPass(doc.getString("purchaseParkingPass"));

                            hike.setIsActive(doc.getBoolean("isActive"));
                            Long startTime = doc.getLong("startTime");
                            Long endTime = doc.getLong("endTime");
                            hike.setStartTime(startTime);
                            hike.setEndTime(endTime);

                            Long createdAt = doc.getLong("createdAt");
                            Long updatedAt = doc.getLong("updatedAt");
                            hike.setCreatedAt(createdAt);
                            hike.setUpdatedAt(updatedAt);
                            hike.setSynced(true);

                            // Insert/replace hike locally
                            hikeDao.insertHike(hike);

                            // Also queue observation downloads for this hike
                            Task<QuerySnapshot> obsTask = doc.getReference()
                                    .collection("observations")
                                    .get();
                            observationTasks.add(obsTask);
                        }

                        if (observationTasks.isEmpty()) {
                            notifySuccess(callback);
                            return;
                        }

                        // When all observation sub-collections have been fetched, write them to Room
                        Tasks.whenAllSuccess(observationTasks)
                                .addOnSuccessListener(mainThreadExecutor, unused -> executorService.execute(() -> {
                                    try {
                                        for (Task<QuerySnapshot> task : observationTasks) {
                                            QuerySnapshot snapshot = null;
                                            try {
                                                snapshot = Tasks.await(task);
                                            } catch (Exception e) {
                                                Log.e(TAG, "Failed awaiting observation task", e);
                                            }
                                            if (snapshot == null) continue;

                                            for (DocumentSnapshot obsDoc : snapshot.getDocuments()) {
                                                if (!obsDoc.exists()) continue;

                                                Observation obs = new Observation();
                                                try {
                                                    String obsId = obsDoc.getId();
                                                    obs.setObservationID(Integer.parseInt(obsId));
                                                } catch (NumberFormatException e) {
                                                    continue;
                                                }

                                                // Parent hikeId from path
                                                try {
                                                    String parentId = obsDoc.getReference()
                                                            .getParent() // observations
                                                            .getParent() // hikes/{hikeId}
                                                            .getId();
                                                    obs.setHikeId(Integer.parseInt(parentId));
                                                } catch (Exception e) {
                                                    // If we can't parse hikeId, skip
                                                    continue;
                                                }

                                                obs.setObservationText(obsDoc.getString("observationText"));

                                                Long timeMillis = obsDoc.getLong("time");
                                                if (timeMillis != null) {
                                                    obs.setTime(new java.util.Date(timeMillis));
                                                }

                                                obs.setComments(obsDoc.getString("comments"));
                                                obs.setLocation(obsDoc.getString("location"));
                                                obs.setPicture(obsDoc.getString("picture"));

                                                Long createdAt = obsDoc.getLong("createdAt");
                                                Long updatedAt = obsDoc.getLong("updatedAt");
                                                obs.setCreatedAt(createdAt);
                                                obs.setUpdatedAt(updatedAt);
                                                obs.setSynced(true);

                                                observationDao.insertObservation(obs);
                                            }
                                        }
                                        notifySuccess(callback);
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error while saving downloaded observations", e);
                                        notifyFailure(callback, e);
                                    }
                                }))
                                .addOnFailureListener(mainThreadExecutor, e -> {
                                    Log.e(TAG, "Failed to download observations", e);
                                    notifyFailure(callback, e);
                                });

                    } catch (Exception e) {
                        Log.e(TAG, "downloadUserData: error while processing hikes", e);
                        notifyFailure(callback, e);
                    }
                }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to download hikes for user " + firebaseUid, e);
                    notifyFailure(callback, e);
                });
    }
}


