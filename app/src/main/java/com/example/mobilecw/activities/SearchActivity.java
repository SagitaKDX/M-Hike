package com.example.mobilecw.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilecw.R;
import com.example.mobilecw.adapters.HikeListAdapter;
import com.example.mobilecw.auth.SessionManager;
import com.example.mobilecw.database.AppDatabase;
import com.example.mobilecw.database.dao.HikeDao;
import com.example.mobilecw.database.entities.Hike;
import com.example.mobilecw.services.SemanticSearchService;
import com.example.mobilecw.utils.SearchHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.widget.SwitchCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchActivity extends AppCompatActivity implements HikeListAdapter.OnHikeClickListener {
    
    private static final String TAG = "SearchActivity";
    
    private EditText searchInput;
    private TextInputEditText locationInput, minLengthInput, maxLengthInput;
    private TextInputEditText startDateInput, endDateInput;
    private AutoCompleteTextView difficultySpinner, parkingSpinner;
    private MaterialButton toggleFiltersButton, clearFiltersButton, clearAllFiltersButton, searchButton;
    private ImageButton backButton, clearSearchButton;
    private LinearLayout filtersCard;
    private RecyclerView searchResultsRecyclerView;
    private TextView resultsCountText, filteredCountText, emptyStateMessage;
    private LinearLayout emptyStateLayout;
    private SwitchCompat semanticSearchSwitch;
    
    private HikeListAdapter adapter;
    private AppDatabase database;
    private HikeDao hikeDao;
    private ExecutorService executorService;
    
    private List<Hike> allHikes = new ArrayList<>();
    private List<Hike> filteredHikes = new ArrayList<>();
    
    private Calendar startDateCalendar;
    private Calendar endDateCalendar;
    private SimpleDateFormat dateFormat;
    
    private boolean filtersVisible = false;
    private boolean semanticSearchEnabled = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        
        // Initialize database
        database = AppDatabase.getDatabase(this);
        hikeDao = database.hikeDao();
        executorService = Executors.newSingleThreadExecutor();
        
        // Initialize date format
        dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        startDateCalendar = Calendar.getInstance();
        endDateCalendar = Calendar.getInstance();
        
        // Initialize views
        initializeViews();
        
        // Setup listeners
        setupListeners();
        
        // Setup RecyclerView
        setupRecyclerView();
        
        // Setup spinners
        setupSpinners();
        
        // Load all hikes
        loadAllHikes();
    }
    
    private void initializeViews() {
        searchInput = findViewById(R.id.searchInput);
        locationInput = findViewById(R.id.locationInput);
        minLengthInput = findViewById(R.id.minLengthInput);
        maxLengthInput = findViewById(R.id.maxLengthInput);
        startDateInput = findViewById(R.id.startDateInput);
        endDateInput = findViewById(R.id.endDateInput);
        difficultySpinner = findViewById(R.id.difficultySpinner);
        parkingSpinner = findViewById(R.id.parkingSpinner);
        
        toggleFiltersButton = findViewById(R.id.toggleFiltersButton);
        clearFiltersButton = findViewById(R.id.clearFiltersButton);
        clearAllFiltersButton = findViewById(R.id.clearAllFiltersButton);
        searchButton = findViewById(R.id.searchButton);
        backButton = findViewById(R.id.backButton);
        clearSearchButton = findViewById(R.id.clearSearchButton);
        
        filtersCard = findViewById(R.id.filtersCard);
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
        resultsCountText = findViewById(R.id.resultsCountText);
        filteredCountText = findViewById(R.id.filteredCountText);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        emptyStateMessage = findViewById(R.id.emptyStateMessage);
        semanticSearchSwitch = findViewById(R.id.semanticSearchSwitch);
    }
    
    private void setupSpinners() {
        // Difficulty spinner
        String[] difficultyOptions = {"", "Easy", "Medium", "Hard", "Expert"};
        ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_dropdown_item_1line, difficultyOptions);
        difficultySpinner.setAdapter(difficultyAdapter);
        difficultySpinner.setOnItemClickListener((parent, view, position, id) -> {
            updateClearButtonVisibility();
            // Don't auto-trigger search
        });
        
        // Parking spinner
        String[] parkingOptions = {"", "yes", "no"};
        ArrayAdapter<String> parkingAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_dropdown_item_1line, parkingOptions);
        parkingSpinner.setAdapter(parkingAdapter);
        parkingSpinner.setOnItemClickListener((parent, view, position, id) -> {
            updateClearButtonVisibility();
            // Don't auto-trigger search
        });
    }
    
    private void setupListeners() {
        // Back button
        backButton.setOnClickListener(v -> finish());
        
        // Toggle filters
        toggleFiltersButton.setOnClickListener(v -> toggleFilters());
        
        // Clear filters
        clearFiltersButton.setOnClickListener(v -> clearFilters());
        
        // Clear all filters button in empty state
        clearAllFiltersButton.setOnClickListener(v -> clearFilters());
        
        // Search button - only search when clicked
        searchButton.setOnClickListener(v -> {
            String query = searchInput.getText() != null ? searchInput.getText().toString().trim() : "";
            if (!query.isEmpty()) {
                performSearch(query);
            } else {
                // If no search query, apply other filters if any
                if (hasActiveFilters()) {
                    performAdvancedSearch();
                } else {
                    // Show all hikes
                    filteredHikes = new ArrayList<>(allHikes);
                    updateResults(filteredHikes);
                }
            }
        });
        
        // Show/hide clear button based on text input
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Show/hide clear button only, don't perform search
                if (clearSearchButton != null) {
                    clearSearchButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                }
                updateClearButtonVisibility();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Clear search button
        if (clearSearchButton != null) {
            clearSearchButton.setOnClickListener(v -> {
                searchInput.setText("");
                clearSearchButton.setVisibility(View.GONE);
                updateClearButtonVisibility();
            });
        }
        
        // Also trigger search on Enter key
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                searchButton.performClick();
                return true;
            }
            return false;
        });
        
        // Date pickers
        startDateInput.setOnClickListener(v -> showStartDatePicker());
        endDateInput.setOnClickListener(v -> showEndDatePicker());
        
        // Semantic search toggle - don't auto-trigger search
        semanticSearchSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            semanticSearchEnabled = isChecked;
            // Don't auto-trigger search, user must click search button
        });

        // Filter inputs - don't trigger search automatically
        locationInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateClearButtonVisibility();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        minLengthInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateClearButtonVisibility();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        maxLengthInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateClearButtonVisibility();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void updateClearButtonVisibility() {
        boolean hasFilters = hasActiveFilters();
        if (clearFiltersButton != null) {
            clearFiltersButton.setVisibility(hasFilters ? View.VISIBLE : View.GONE);
        }
    }
    
    private boolean hasActiveFilters() {
        String searchText = searchInput.getText() != null ? searchInput.getText().toString().trim() : "";
        String locationText = locationInput.getText() != null ? locationInput.getText().toString().trim() : "";
        String minLengthText = minLengthInput.getText() != null ? minLengthInput.getText().toString().trim() : "";
        String maxLengthText = maxLengthInput.getText() != null ? maxLengthInput.getText().toString().trim() : "";
        String startDateText = startDateInput.getText() != null ? startDateInput.getText().toString().trim() : "";
        String endDateText = endDateInput.getText() != null ? endDateInput.getText().toString().trim() : "";
        String difficultyText = difficultySpinner.getText() != null ? difficultySpinner.getText().toString().trim() : "";
        String parkingText = parkingSpinner.getText() != null ? parkingSpinner.getText().toString().trim() : "";
        
        return !searchText.isEmpty() || !locationText.isEmpty() || 
               !minLengthText.isEmpty() || !maxLengthText.isEmpty() ||
               !startDateText.isEmpty() || !endDateText.isEmpty() ||
               !difficultyText.isEmpty() || !parkingText.isEmpty();
    }
    
    private void setupRecyclerView() {
        adapter = new HikeListAdapter(this);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchResultsRecyclerView.setAdapter(adapter);
    }
    
    private void loadAllHikes() {
        executorService.execute(() -> {
            allHikes = hikeDao.getAllHikes();
            runOnUiThread(() -> {
                filteredHikes = new ArrayList<>(allHikes);
                updateResults(filteredHikes);
            });
        });
    }
    
    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            // If search is empty, apply other filters if any
            if (hasActiveFilters()) {
                performAdvancedSearch();
            } else {
                filteredHikes = new ArrayList<>(allHikes);
                updateResults(filteredHikes);
            }
            return;
        }

        // Check if semantic search is enabled and user is logged in
        if (semanticSearchEnabled && SessionManager.isLoggedIn(this)) {
            performSemanticSearch(query);
        } else {
            // Perform fuzzy search with relevance scoring
            executorService.execute(() -> {
                List<Hike> searchResults = SearchHelper.fuzzySearch(allHikes, query);
                // Then apply other filters
                final List<Hike> finalResults = applyOtherFilters(searchResults);
                runOnUiThread(() -> {
                    filteredHikes = finalResults;
                    updateResults(filteredHikes);
                });
            });
        }
    }

    private void performSemanticSearch(String query) {
        String firebaseUid = SessionManager.getCurrentFirebaseUid(this);
        if (firebaseUid == null || firebaseUid.isEmpty()) {
            Toast.makeText(this, "Please log in to use semantic search", Toast.LENGTH_SHORT).show();
            semanticSearchSwitch.setChecked(false);
            semanticSearchEnabled = false;
            // Fall back to regular search
            performSearch(query);
            return;
        }

        // Show loading state
        resultsCountText.setText("Searching...");
        searchResultsRecyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);

        SemanticSearchService.search(
            this,
            query,
            firebaseUid,
            "hikes", // Search only hikes
            20, // Top 20 results
            new SemanticSearchService.SearchCallback() {
                @Override
                public void onSuccess(List<SemanticSearchService.SearchResult> results) {
                    // Convert semantic search results to Hike objects
                    executorService.execute(() -> {
                        List<Hike> matchedHikes = new ArrayList<>();
                        for (SemanticSearchService.SearchResult result : results) {
                            if ("hike".equals(result.type) && result.hikeId != null) {
                                Hike hike = hikeDao.getHikeById(result.hikeId);
                                if (hike != null) {
                                    matchedHikes.add(hike);
                                }
                            }
                        }
                        
                        // Apply other filters (location, date, etc.)
                        final List<Hike> finalResults = applyOtherFilters(matchedHikes);
                        
                        runOnUiThread(() -> {
                            filteredHikes = finalResults;
                            updateResults(filteredHikes);
                        });
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(SearchActivity.this, 
                            "Semantic search failed: " + error, 
                            Toast.LENGTH_SHORT).show();
                        // Fall back to regular search
                        semanticSearchSwitch.setChecked(false);
                        semanticSearchEnabled = false;
                        performSearch(query);
                    });
                }
            }
        );
    }
    
    private void performAdvancedSearch() {
        String nameQuery = searchInput.getText() != null ? searchInput.getText().toString().trim() : "";
        String locationQuery = locationInput.getText() != null ? locationInput.getText().toString().trim() : "";
        
        Double minLength = null;
        Double maxLength = null;
        Long startDate = null;
        Long endDate = null;
        String difficulty = null;
        String parkingAvailable = null;
        
        // Parse length values
        try {
            if (minLengthInput.getText() != null && !minLengthInput.getText().toString().isEmpty()) {
                minLength = Double.parseDouble(minLengthInput.getText().toString());
            }
            if (maxLengthInput.getText() != null && !maxLengthInput.getText().toString().isEmpty()) {
                maxLength = Double.parseDouble(maxLengthInput.getText().toString());
            }
        } catch (NumberFormatException e) {
            // Ignore invalid numbers
        }
        
        // Get date values
        if (startDateInput.getText() != null && !startDateInput.getText().toString().isEmpty()) {
            startDate = startDateCalendar.getTimeInMillis();
        }
        if (endDateInput.getText() != null && !endDateInput.getText().toString().isEmpty()) {
            endDate = endDateCalendar.getTimeInMillis();
        }
        
        // Get difficulty and parking
        if (difficultySpinner.getText() != null && !difficultySpinner.getText().toString().trim().isEmpty()) {
            difficulty = difficultySpinner.getText().toString().trim();
        }
        if (parkingSpinner.getText() != null && !parkingSpinner.getText().toString().trim().isEmpty()) {
            parkingAvailable = parkingSpinner.getText().toString().trim();
        }
        
        final Double finalMinLength = minLength;
        final Double finalMaxLength = maxLength;
        final Long finalStartDate = startDate;
        final Long finalEndDate = endDate;
        final String finalDifficulty = difficulty;
        final String finalParking = parkingAvailable;
        
        executorService.execute(() -> {
            // First apply fuzzy search if name query exists
            List<Hike> searchResults = allHikes;
            if (!nameQuery.isEmpty()) {
                searchResults = SearchHelper.fuzzySearch(allHikes, nameQuery);
            }
            
            // Then apply advanced filters
            List<Hike> results = SearchHelper.advancedFilter(
                searchResults, 
                null, // Already searched by name
                locationQuery, 
                finalMinLength, 
                finalMaxLength, 
                finalStartDate, 
                finalEndDate,
                finalDifficulty,
                finalParking
            );
            
            runOnUiThread(() -> {
                filteredHikes = results;
                updateResults(filteredHikes);
            });
        });
    }
    
    private List<Hike> applyOtherFilters(List<Hike> hikes) {
        String locationQuery = locationInput.getText() != null ? locationInput.getText().toString().trim() : "";
        
        Double minLength = null;
        Double maxLength = null;
        Long startDate = null;
        Long endDate = null;
        String difficulty = null;
        String parkingAvailable = null;
        
        try {
            if (minLengthInput.getText() != null && !minLengthInput.getText().toString().isEmpty()) {
                minLength = Double.parseDouble(minLengthInput.getText().toString());
            }
            if (maxLengthInput.getText() != null && !maxLengthInput.getText().toString().isEmpty()) {
                maxLength = Double.parseDouble(maxLengthInput.getText().toString());
            }
        } catch (NumberFormatException e) {}
        
        if (startDateInput.getText() != null && !startDateInput.getText().toString().isEmpty()) {
            startDate = startDateCalendar.getTimeInMillis();
        }
        if (endDateInput.getText() != null && !endDateInput.getText().toString().isEmpty()) {
            endDate = endDateCalendar.getTimeInMillis();
        }
        
        if (difficultySpinner.getText() != null && !difficultySpinner.getText().toString().trim().isEmpty()) {
            difficulty = difficultySpinner.getText().toString().trim();
        }
        if (parkingSpinner.getText() != null && !parkingSpinner.getText().toString().trim().isEmpty()) {
            parkingAvailable = parkingSpinner.getText().toString().trim();
        }
        
        return SearchHelper.advancedFilter(hikes, null, locationQuery, minLength, maxLength, 
            startDate, endDate, difficulty, parkingAvailable);
    }
    
    private void updateResults(List<Hike> results) {
        adapter.submitList(results);
        
        // Update results count
        int resultCount = results.size();
        int totalCount = allHikes.size();
        
        if (resultCount == 0) {
            searchResultsRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
            resultsCountText.setText("No results found");
            
            // Update empty state message
            if (hasActiveFilters()) {
                emptyStateMessage.setText("Try adjusting your search criteria or filters");
                clearAllFiltersButton.setVisibility(View.VISIBLE);
            } else {
                emptyStateMessage.setText("Add some hiking records to see them here");
                clearAllFiltersButton.setVisibility(View.GONE);
            }
        } else {
            searchResultsRecyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            
            String resultText = resultCount + (resultCount == 1 ? " Result" : " Results");
            resultsCountText.setText(resultText);
            
            // Show filtered count if filters are active
            if (hasActiveFilters() && resultCount < totalCount) {
                filteredCountText.setVisibility(View.VISIBLE);
                filteredCountText.setText("Filtered from " + totalCount + " total");
            } else {
                filteredCountText.setVisibility(View.GONE);
            }
        }
    }
    
    private void toggleFilters() {
        filtersVisible = !filtersVisible;
        if (filtersVisible) {
            filtersCard.setVisibility(View.VISIBLE);
            toggleFiltersButton.setText(R.string.hide_filters);
        } else {
            filtersCard.setVisibility(View.GONE);
            toggleFiltersButton.setText(R.string.show_filters);
        }
    }
    
    private void clearFilters() {
        // Clear all filter inputs
        searchInput.setText("");
        locationInput.setText("");
        minLengthInput.setText("");
        maxLengthInput.setText("");
        startDateInput.setText("");
        endDateInput.setText("");
        difficultySpinner.setText("", false);
        parkingSpinner.setText("", false);
        
        if (clearSearchButton != null) {
            clearSearchButton.setVisibility(View.GONE);
        }
        
        // Reset results to all hikes
        filteredHikes = new ArrayList<>(allHikes);
        updateResults(filteredHikes);
        updateClearButtonVisibility();
    }
    
    private void showStartDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                startDateCalendar.set(year, month, dayOfMonth);
                startDateInput.setText(dateFormat.format(startDateCalendar.getTime()));
                updateClearButtonVisibility();
                // Don't auto-trigger search
            },
            startDateCalendar.get(Calendar.YEAR),
            startDateCalendar.get(Calendar.MONTH),
            startDateCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
    
    private void showEndDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                endDateCalendar.set(year, month, dayOfMonth);
                endDateInput.setText(dateFormat.format(endDateCalendar.getTime()));
                updateClearButtonVisibility();
                // Don't auto-trigger search
            },
            endDateCalendar.get(Calendar.YEAR),
            endDateCalendar.get(Calendar.MONTH),
            endDateCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
    
    @Override
    public void onHikeClicked(Hike hike) {
        Intent intent = new Intent(this, HikeDetailActivity.class);
        intent.putExtra(HikeDetailActivity.EXTRA_HIKE_ID, hike.getHikeID());
        startActivity(intent);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
