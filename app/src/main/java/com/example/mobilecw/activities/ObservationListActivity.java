package com.example.mobilecw.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilecw.R;
import com.example.mobilecw.adapters.ObservationListAdapter;
import com.example.mobilecw.auth.SessionManager;
import com.example.mobilecw.database.AppDatabase;
import com.example.mobilecw.database.dao.ObservationDao;
import com.example.mobilecw.database.entities.Observation;
import com.example.mobilecw.sync.FirebaseSyncManager;
import com.example.mobilecw.utils.NetworkUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ObservationListActivity extends AppCompatActivity implements ObservationListAdapter.OnObservationClickListener {

    public static final String EXTRA_HIKE_ID = "hike_id";
    public static final String EXTRA_HIKE_NAME = "hike_name";

    private RecyclerView recyclerView;
    private ObservationListAdapter adapter;
    private LinearLayout emptyState;
    private TextView hikeNameText;
    private ImageButton backButton;
    private ImageButton addObservationButton;

    private AppDatabase database;
    private ObservationDao observationDao;
    private ExecutorService executorService;

    private int hikeId;
    private String hikeName;

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
            long now = System.currentTimeMillis();
            observationDao.softDeleteObservationById(observation.getObservationID(), now, now);
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.observation_deleted, Toast.LENGTH_SHORT).show();
                loadObservations();
                syncIfLoggedIn();
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

    private void syncIfLoggedIn() {
        if (SessionManager.isLoggedIn(this) && NetworkUtils.isOnline(this)) {
            FirebaseSyncManager.getInstance(getApplicationContext()).syncNow();
        }
    }
}

