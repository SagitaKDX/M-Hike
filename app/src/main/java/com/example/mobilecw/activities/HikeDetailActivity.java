package com.example.mobilecw.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.mobilecw.R;
import com.example.mobilecw.auth.SessionManager;
import com.example.mobilecw.database.AppDatabase;
import com.example.mobilecw.database.dao.HikeDao;
import com.example.mobilecw.database.entities.Hike;
import com.example.mobilecw.sync.FirebaseSyncManager;
import com.example.mobilecw.utils.NetworkUtils;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HikeDetailActivity extends AppCompatActivity {
    
    private TextView hikeNameText, hikeLocationText, hikeIdText, hikeDateText;
    private TextView hikeLengthText, hikeDifficultyText, parkingStatusText, hikeDescriptionText;
    private Button startHikeButton, viewObservationsButton, editButton, backToListButton;
    private ImageButton backButton;
    private CardView descriptionCard;
    
    private Hike hike;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat displayDateFormat;
    
    private AppDatabase database;
    private HikeDao hikeDao;
    private ExecutorService executorService;
    
    public static final String EXTRA_HIKE_ID = "hike_id";
    
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
}

