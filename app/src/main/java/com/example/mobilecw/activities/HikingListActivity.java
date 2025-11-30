package com.example.mobilecw.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilecw.R;
import com.example.mobilecw.adapters.HikeListAdapter;
import com.example.mobilecw.auth.SessionManager;
import com.example.mobilecw.database.AppDatabase;
import com.example.mobilecw.database.dao.HikeDao;
import com.example.mobilecw.database.entities.Hike;
import com.example.mobilecw.sync.FirebaseSyncManager;
import com.example.mobilecw.utils.NetworkUtils;
import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HikingListActivity extends AppCompatActivity implements HikeListAdapter.OnHikeClickListener {

    private RecyclerView recyclerView;
    private HikeListAdapter adapter;
    private EditText searchInput;
    private MaterialButton deleteSelectedButton;
    private ImageButton addHikeButton, editModeButton, deleteAllButton, searchButton;
    private LinearLayout navHome, navHiking, navUsers, navSettings;

    private AppDatabase database;
    private HikeDao hikeDao;
    private ExecutorService executorService;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private List<Hike> currentHikes = new ArrayList<>();
    private boolean selectionMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hiking_list);

        database = AppDatabase.getDatabase(this);
        hikeDao = database.hikeDao();
        executorService = Executors.newSingleThreadExecutor();

        recyclerView = findViewById(R.id.hikeRecyclerView);
        searchInput = findViewById(R.id.searchInput);
        deleteSelectedButton = findViewById(R.id.deleteSelectedButton);
        searchButton = findViewById(R.id.searchButton);
        addHikeButton = findViewById(R.id.addHikeButton);
        editModeButton = findViewById(R.id.editModeButton);
        deleteAllButton = findViewById(R.id.deleteAllButton);
        navHome = findViewById(R.id.navHome);
        navHiking = findViewById(R.id.navHiking);
        navUsers = findViewById(R.id.navUsers);
        navSettings = findViewById(R.id.navSettings);

        adapter = new HikeListAdapter(this);
        adapter.setOnSelectionChangedListener(count -> {
            if (deleteSelectedButton != null) {
                deleteSelectedButton.setEnabled(count > 0);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Setup add hike button
        addHikeButton.setOnClickListener(v -> {
            Intent intent = new Intent(HikingListActivity.this, EnterHikeActivity.class);
            startActivity(intent);
        });
        
        // Setup search button
        searchButton.setOnClickListener(v -> {
            Intent intent = new Intent(HikingListActivity.this, SearchActivity.class);
            startActivity(intent);
        });
        
        // Setup advanced search button (Filters button in search bar)
        MaterialButton advancedSearchButton = findViewById(R.id.advancedSearchButton);
        if (advancedSearchButton != null) {
            advancedSearchButton.setOnClickListener(v -> {
                Intent intent = new Intent(HikingListActivity.this, SearchActivity.class);
                startActivity(intent);
            });
        }

        loadHikes();
        setupSearch();
        setupBottomNavigation();
        setupManagementButtons();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from other screens
        loadHikes();

        // If user is logged in and now online, push any pending local data to Firebase
        if (SessionManager.isLoggedIn(this) && NetworkUtils.isOnline(this)) {
            FirebaseSyncManager.getInstance(getApplicationContext()).syncNow();
        }
    }

    private void loadHikes() {
        executorService.execute(() -> {
            // Backend rule: each hike belongs to a specific user (or is anonymous).
            // If a user is logged in, only load their hikes. Otherwise, load
            // hikes for non-registered users.
            int currentUserId = SessionManager.getCurrentUserId(this);
            List<Hike> hikes;
            if (currentUserId != -1) {
                hikes = hikeDao.getHikesByUserId(currentUserId);
            } else {
                hikes = hikeDao.getHikesForNonRegisteredUsers();
            }
            if (hikes.isEmpty() && currentUserId == -1) {
                seedSampleData();
                // Re-load after seeding using the same per-user rules
                if (currentUserId != -1) {
                    hikes = hikeDao.getHikesByUserId(currentUserId);
                } else {
                    hikes = hikeDao.getHikesForNonRegisteredUsers();
                }
            }
            currentHikes = hikes;

            List<Hike> finalHikes = hikes;
            runOnUiThread(() -> {
                adapter.submitList(finalHikes);
                if (selectionMode && finalHikes.isEmpty()) {
                    exitSelectionMode();
                }
            });
        });
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterHikes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // no-op
            }
        });
    }
    
    private void setupManagementButtons() {
        deleteSelectedButton.setVisibility(View.GONE);
        deleteSelectedButton.setEnabled(false);
        deleteSelectedButton.setOnClickListener(v -> confirmDeleteSelected());
        editModeButton.setOnClickListener(v -> toggleSelectionMode());
        deleteAllButton.setOnClickListener(v -> confirmDeleteAll());
    }
    
    private void toggleSelectionMode() {
        selectionMode = !selectionMode;
        adapter.setSelectionMode(selectionMode);
        if (selectionMode) {
            deleteSelectedButton.setVisibility(View.VISIBLE);
            deleteSelectedButton.setEnabled(!adapter.getSelectedHikeIds().isEmpty());
            addHikeButton.setEnabled(false);
            editModeButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        } else {
            deleteSelectedButton.setVisibility(View.GONE);
            deleteSelectedButton.setEnabled(false);
            addHikeButton.setEnabled(true);
            editModeButton.setImageResource(android.R.drawable.ic_menu_edit);
        }
    }
    
    private void exitSelectionMode() {
        if (selectionMode) {
            selectionMode = false;
            adapter.setSelectionMode(false);
            deleteSelectedButton.setVisibility(View.GONE);
            deleteSelectedButton.setEnabled(false);
            addHikeButton.setEnabled(true);
            editModeButton.setImageResource(android.R.drawable.ic_menu_edit);
        }
    }
    
    private void confirmDeleteSelected() {
        List<Integer> selectedIds = adapter.getSelectedHikeIds();
        if (selectedIds.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_hikes_selected), Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_selected)
                .setMessage(R.string.confirm_delete_selected)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> deleteSelectedHikes(selectedIds))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
    
    private void deleteSelectedHikes(List<Integer> selectedIds) {
        executorService.execute(() -> {
            hikeDao.deleteHikesByIds(selectedIds);
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.delete_selected, Toast.LENGTH_SHORT).show();
                exitSelectionMode();
                loadHikes();
            });
        });
    }
    
    private void confirmDeleteAll() {
        if (currentHikes.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_hikes), Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_all)
                .setMessage(R.string.confirm_delete_all)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> deleteAllHikes())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
    
    private void deleteAllHikes() {
        executorService.execute(() -> {
            hikeDao.deleteAllHikes();
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.delete_all, Toast.LENGTH_SHORT).show();
                exitSelectionMode();
                loadHikes();
            });
        });
    }

    private void setupBottomNavigation() {
        setActiveNavItem(navHiking);

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(HikingListActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });

        navHiking.setOnClickListener(v -> {
            // Already here
        });

        navUsers.setOnClickListener(v -> {
            Intent intent = new Intent(HikingListActivity.this, UsersActivity.class);
            startActivity(intent);
            finish();
        });

        navSettings.setOnClickListener(v -> {
            Intent intent = new Intent(HikingListActivity.this, SettingsActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void setActiveNavItem(LinearLayout activeItem) {
        resetNavItem(navHome);
        resetNavItem(navHiking);
        resetNavItem(navUsers);
        resetNavItem(navSettings);

        if (activeItem != null) {
            activeItem.setBackgroundResource(R.drawable.nav_item_background);
            if (activeItem.getChildCount() >= 2) {
                ((android.widget.ImageView) activeItem.getChildAt(0))
                        .setColorFilter(getResources().getColor(R.color.primary_green));
                ((android.widget.TextView) activeItem.getChildAt(1))
                        .setTextColor(getResources().getColor(R.color.primary_green));
            }
        }
    }

    private void resetNavItem(LinearLayout item) {
        if (item == null) return;
        item.setBackground(null);
        if (item.getChildCount() >= 2) {
            ((android.widget.ImageView) item.getChildAt(0))
                    .setColorFilter(getResources().getColor(R.color.gray_text));
            ((android.widget.TextView) item.getChildAt(1))
                    .setTextColor(getResources().getColor(R.color.gray_text));
        }
    }

    private void filterHikes(String query) {
        executorService.execute(() -> {
            List<Hike> filtered;
            if (query == null || query.trim().isEmpty()) {
                filtered = currentHikes;
            } else {
                String search = "%" + query + "%";
                filtered = hikeDao.searchHikesByName(search);
            }
            List<Hike> finalFiltered = filtered;
            runOnUiThread(() -> adapter.submitList(finalFiltered));
        });
    }

    private void seedSampleData() {
        List<Hike> sampleHikes = new ArrayList<>();
        sampleHikes.add(createSampleHike("Mountain Peak Trail", "Rocky Mountain", "2025-11-21", true, 8.5, "Hard", "Challenging but rewarding trail"));
        sampleHikes.add(createSampleHike("Forest Loop", "Greenwood Forest", "2025-11-18", false, 4.2, "Easy", "Relaxing walk through lush forest"));
        sampleHikes.add(createSampleHike("River Walk", "Crystal River", "2025-11-15", true, 6.7, "Medium", "Scenic river views and wildlife"));

        for (Hike hike : sampleHikes) {
            hikeDao.insertHike(hike);
        }
    }

    private Hike createSampleHike(String name, String location, String dateStr, boolean parking, double length,
                                  String difficulty, String description) {
        Hike hike = new Hike();
        hike.setName(name);
        hike.setLocation(location);
        try {
            Date date = dateFormat.parse(dateStr);
            hike.setDate(date);
        } catch (ParseException e) {
            hike.setDate(new Date());
        }
        hike.setParkingAvailable(parking);
        hike.setLength(length);
        hike.setDifficulty(difficulty);
        hike.setDescription(description);
        hike.setPurchaseParkingPass("");
        // Seeded hikes are for non-registered (local-only) users
        hike.setUserId(null);
        hike.setSynced(false);
        hike.setCreatedAt(System.currentTimeMillis());
        hike.setUpdatedAt(System.currentTimeMillis());
        return hike;
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

