package com.example.mobilecw.activities;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.example.mobilecw.R;
import com.example.mobilecw.auth.SessionManager;
import com.example.mobilecw.database.AppDatabase;
import com.example.mobilecw.database.dao.ObservationDao;
import com.example.mobilecw.database.entities.Observation;
import com.example.mobilecw.sync.FirebaseSyncManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ObservationFormActivity extends AppCompatActivity {

    public static final String EXTRA_HIKE_ID = "hike_id";
    public static final String EXTRA_HIKE_NAME = "hike_name";
    public static final String EXTRA_OBSERVATION_ID = "observation_id";

    private static final int REQUEST_LOCATION_PERMISSION = 1001;

    private EditText observationInput;
    private EditText timeInput;
    private EditText commentsInput;
    private EditText locationInput;
    private MaterialButton getLocationButton;
    private MaterialButton saveButton;
    private MaterialButton removePictureButton;
    private ImageButton backButton;
    private CardView pictureCard;
    private CardView uploadCard;
    private ImageView picturePreview;
    private TextView hikeNameText;
    private TextView headerSubtitle;

    private AppDatabase database;
    private ObservationDao observationDao;
    private ExecutorService executorService;

    private int hikeId;
    private String hikeName;
    private int observationId = -1; // -1 means new observation
    private Observation editingObservation;

    private Calendar calendar;
    private SimpleDateFormat dateTimeFormat;

    private FusedLocationProviderClient fusedLocationClient;
    private String currentPicturePath;

    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<Intent> takePictureLauncher;

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
}

