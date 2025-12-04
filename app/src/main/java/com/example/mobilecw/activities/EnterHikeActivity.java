package com.example.mobilecw.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobilecw.R;
import com.example.mobilecw.auth.SessionManager;
import com.example.mobilecw.database.AppDatabase;
import com.example.mobilecw.database.dao.HikeDao;
import com.example.mobilecw.database.entities.Hike;
import com.example.mobilecw.sync.FirebaseSyncManager;
import com.example.mobilecw.utils.NetworkUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EnterHikeActivity extends AppCompatActivity {
    
    public static final String EXTRA_EDIT_MODE = "extra_edit_mode";
    public static final String EXTRA_HIKE_ID = "extra_hike_id";
    
    private EditText nameInput, locationInput, dateInput, lengthInput, descriptionInput;
    private Spinner difficultySpinner;
    private Switch parkingSwitch;
    private Button saveButton, cancelButton;
    private ImageButton backButton, btnPickLocation;
    private TextView headerTitleText;
    
    private AppDatabase database;
    private HikeDao hikeDao;
    private ExecutorService executorService;
    
    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    private boolean isEditMode = false;
    private int editingHikeId = -1;
    private Hike editingHike;
    
    // Store selected coordinates
    private double selectedLatitude = 0;
    private double selectedLongitude = 0;
    
    // Map picker launcher
    private ActivityResultLauncher<Intent> mapPickerLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_hike);
        
        // Initialize database
        database = AppDatabase.getDatabase(this);
        hikeDao = database.hikeDao();
        executorService = Executors.newSingleThreadExecutor();
        
        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        // Initialize map picker launcher
        setupMapPickerLauncher();
        
        // Initialize views
        initializeViews();
        
        // Setup spinner
        setupDifficultySpinner();
        
        // Setup date picker
        setupDatePicker();
        
        // Setup click listeners
        setupClickListeners();
        
        Intent intent = getIntent();
        if (intent != null) {
            isEditMode = intent.getBooleanExtra(EXTRA_EDIT_MODE, false);
            editingHikeId = intent.getIntExtra(EXTRA_HIKE_ID, -1);
            if (isEditMode && editingHikeId != -1) {
                if (headerTitleText != null) {
                    headerTitleText.setText(R.string.edit_hiking);
                }
                saveButton.setText(R.string.update_hiking);
                loadHikeForEdit(editingHikeId);
            }
        }
    }
    
    private void initializeViews() {
        headerTitleText = findViewById(R.id.headerTitleText);
        nameInput = findViewById(R.id.nameInput);
        locationInput = findViewById(R.id.locationInput);
        dateInput = findViewById(R.id.dateInput);
        lengthInput = findViewById(R.id.lengthInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        difficultySpinner = findViewById(R.id.difficultySpinner);
        parkingSwitch = findViewById(R.id.parkingSwitch);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
        backButton = findViewById(R.id.backButton);
        btnPickLocation = findViewById(R.id.btnPickLocation);
        
        // Set default date to today
        dateInput.setText(dateFormat.format(calendar.getTime()));
    }
    
    private void setupMapPickerLauncher() {
        mapPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    selectedLatitude = data.getDoubleExtra(MapPickerActivity.EXTRA_LATITUDE, 0);
                    selectedLongitude = data.getDoubleExtra(MapPickerActivity.EXTRA_LONGITUDE, 0);
                    String address = data.getStringExtra(MapPickerActivity.EXTRA_ADDRESS);
                    
                    if (address != null && !address.isEmpty()) {
                        locationInput.setText(address);
                    } else {
                        // Use coordinates if no address
                        locationInput.setText(String.format(Locale.US, "%.6f, %.6f", 
                                selectedLatitude, selectedLongitude));
                    }
                }
            }
        );
    }
    
    private void setupDifficultySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.difficulty_levels,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        difficultySpinner.setAdapter(adapter);
    }
    
    private void setupDatePicker() {
        dateInput.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        dateInput.setText(dateFormat.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        
        cancelButton.setOnClickListener(v -> finish());
        
        saveButton.setOnClickListener(v -> saveHike());
        
        // Map picker button
        btnPickLocation.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapPickerActivity.class);
            mapPickerLauncher.launch(intent);
        });
    }
    
    private void loadHikeForEdit(int hikeId) {
        executorService.execute(() -> {
            editingHike = hikeDao.getHikeById(hikeId);
            if (editingHike != null) {
                runOnUiThread(() -> populateForm(editingHike));
            } else {
                runOnUiThread(this::finish);
            }
        });
    }
    
    private void populateForm(Hike hike) {
        nameInput.setText(hike.getName());
        locationInput.setText(hike.getLocation());
        if (hike.getDate() != null) {
            calendar.setTime(hike.getDate());
            dateInput.setText(dateFormat.format(hike.getDate()));
        }
        lengthInput.setText(String.format(Locale.getDefault(), "%.1f", hike.getLength()));
        String[] difficulties = getResources().getStringArray(R.array.difficulty_levels);
        for (int i = 0; i < difficulties.length; i++) {
            if (difficulties[i].equalsIgnoreCase(hike.getDifficulty())) {
                difficultySpinner.setSelection(i);
                break;
            }
        }
        parkingSwitch.setChecked(hike.isParkingAvailable());
        if (hike.getDescription() != null) {
            descriptionInput.setText(hike.getDescription());
        }
    }
    
    private void saveHike() {
        // Validate required fields
        if (!validateForm()) {
            return;
        }
        
        // Get form data
        String name = nameInput.getText().toString().trim();
        String location = locationInput.getText().toString().trim();
        String dateString = dateInput.getText().toString().trim();
        String lengthString = lengthInput.getText().toString().trim();
        String difficulty = difficultySpinner.getSelectedItem().toString();
        boolean parkingAvailable = parkingSwitch.isChecked();
        String description = descriptionInput.getText().toString().trim();
        
        // Parse date
        Date date;
        try {
            date = dateFormat.parse(dateString);
        } catch (Exception e) {
            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Parse length
        double length;
        try {
            length = Double.parseDouble(lengthString);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid length value", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create hike object
        Hike hike = new Hike();
        hike.setName(name);
        hike.setLocation(location);
        hike.setDate(date);
        hike.setParkingAvailable(parkingAvailable);
        hike.setLength(length);
        hike.setDifficulty(difficulty);
        hike.setDescription(description.isEmpty() ? null : description);
        hike.setPurchaseParkingPass(null); // Optional field
        
        // Set user ID if registered (backend relationship between user and hikes)
        int userId = SessionManager.getCurrentUserId(this);
        final boolean shouldSyncWithCloud = userId != -1;
        if (shouldSyncWithCloud) {
            hike.setUserId(userId);
        } else {
            // Non-registered / anonymous user on this device
            hike.setUserId(null);
        }
        hike.setSynced(false);
        
        if (isEditMode && editingHike != null && editingHikeId != -1) {
            hike.setHikeID(editingHikeId);
            hike.setCreatedAt(editingHike.getCreatedAt());
            hike.setUpdatedAt(System.currentTimeMillis());
            if (editingHike.getUserId() != null) {
                hike.setUserId(editingHike.getUserId());
            }
            executorService.execute(() -> {
                try {
                    hikeDao.updateHike(hike);
                    runOnUiThread(() -> {
                        Toast.makeText(this, getString(R.string.hike_updated), Toast.LENGTH_SHORT).show();
                        if (shouldSyncWithCloud && NetworkUtils.isOnline(getApplicationContext())) {
                            FirebaseSyncManager.getInstance(getApplicationContext()).syncNow();
                        }
                        finish();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this, getString(R.string.error_saving), Toast.LENGTH_SHORT).show());
                }
            });
        } else {
            executorService.execute(() -> {
                try {
                    hikeDao.insertHike(hike);
                    runOnUiThread(() -> {
                        Toast.makeText(this, getString(R.string.hike_saved), Toast.LENGTH_SHORT).show();
                        if (shouldSyncWithCloud && NetworkUtils.isOnline(getApplicationContext())) {
                            FirebaseSyncManager.getInstance(getApplicationContext()).syncNow();
                        }
                        finish();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this, getString(R.string.error_saving), Toast.LENGTH_SHORT).show());
                }
            });
        }
    }
    
    private boolean validateForm() {
        boolean isValid = true;
        
        // Reset previous errors
        nameInput.setError(null);
        locationInput.setError(null);
        dateInput.setError(null);
        lengthInput.setError(null);
        
        // Validate name
        if (TextUtils.isEmpty(nameInput.getText().toString().trim())) {
            nameInput.setError(getString(R.string.required_field));
            isValid = false;
        }
        
        // Validate location
        if (TextUtils.isEmpty(locationInput.getText().toString().trim())) {
            locationInput.setError(getString(R.string.required_field));
            isValid = false;
        }
        
        // Validate date
        if (TextUtils.isEmpty(dateInput.getText().toString().trim())) {
            dateInput.setError(getString(R.string.required_field));
            isValid = false;
        }
        
        // Validate length
        if (TextUtils.isEmpty(lengthInput.getText().toString().trim())) {
            lengthInput.setError(getString(R.string.required_field));
            isValid = false;
        } else {
            try {
                double length = Double.parseDouble(lengthInput.getText().toString().trim());
                if (length <= 0) {
                    lengthInput.setError("Length must be greater than 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                lengthInput.setError("Invalid number");
                isValid = false;
            }
        }
        
        if (!isValid) {
            Toast.makeText(this, getString(R.string.error_required_fields), Toast.LENGTH_SHORT).show();
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

