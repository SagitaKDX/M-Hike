package com.example.mobilecw.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UsersActivity extends AppCompatActivity implements HikeListAdapter.OnHikeClickListener {

    private LinearLayout nonRegisteredView;
    private LinearLayout registeredView;
    
    // Non-registered view
    private com.google.android.material.button.MaterialButton registerButton;
    private com.google.android.material.button.MaterialButton loginButton;
    
    // Registered view
    private TextView profileInitials;
    private TextView userNameText;
    private TextView memberSinceText;
    private TextView levelText;
    private TextView totalHikesText;
    private TextView totalKmText;
    private TextView avgKmText;
    private TextView thisMonthText;
    private ProgressBar easyTrailsProgress;
    private ProgressBar mediumTrailsProgress;
    private ProgressBar hardTrailsProgress;
    private TextView easyTrailsPercent;
    private TextView mediumTrailsPercent;
    private TextView hardTrailsPercent;
    private RecyclerView recentActivityRecyclerView;
    
    private LinearLayout navHome, navHiking, navUsers, navSettings;
    
    private AppDatabase database;
    private HikeDao hikeDao;
    private ExecutorService executorService;
    private SharedPreferences sharedPreferences;
    
    private static final String PREFS_NAME = "mhike_prefs";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);
        
        // Initialize database
        database = AppDatabase.getDatabase(this);
        hikeDao = database.hikeDao();
        executorService = Executors.newSingleThreadExecutor();
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Initialize views
        initializeViews();
        
        // Setup bottom navigation
        setupBottomNavigation();
        
        // Check login status and show appropriate view
        checkLoginStatus();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from login/signup
        checkLoginStatus();
    }
    
    private void initializeViews() {
        nonRegisteredView = findViewById(R.id.nonRegisteredView);
        registeredView = findViewById(R.id.registeredView);
        
        registerButton = findViewById(R.id.registerButton);
        loginButton = findViewById(R.id.loginButton);
        
        profileInitials = findViewById(R.id.profileInitials);
        userNameText = findViewById(R.id.userNameText);
        memberSinceText = findViewById(R.id.memberSinceText);
        levelText = findViewById(R.id.levelText);
        totalHikesText = findViewById(R.id.totalHikesText);
        totalKmText = findViewById(R.id.totalKmText);
        avgKmText = findViewById(R.id.avgKmText);
        thisMonthText = findViewById(R.id.thisMonthText);
        easyTrailsProgress = findViewById(R.id.easyTrailsProgress);
        mediumTrailsProgress = findViewById(R.id.mediumTrailsProgress);
        hardTrailsProgress = findViewById(R.id.hardTrailsProgress);
        easyTrailsPercent = findViewById(R.id.easyTrailsPercent);
        mediumTrailsPercent = findViewById(R.id.mediumTrailsPercent);
        hardTrailsPercent = findViewById(R.id.hardTrailsPercent);
        recentActivityRecyclerView = findViewById(R.id.recentActivityRecyclerView);
        
        navHome = findViewById(R.id.navHome);
        navHiking = findViewById(R.id.navHiking);
        navUsers = findViewById(R.id.navUsers);
        navSettings = findViewById(R.id.navSettings);
        
        // Setup click listeners for non-registered view
        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(UsersActivity.this, SignupActivity.class);
            startActivity(intent);
        });
        
        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(UsersActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }
    
    private void checkLoginStatus() {
        boolean isLoggedIn = SessionManager.isLoggedIn(this);
        
        if (isLoggedIn) {
            showRegisteredView();
            loadUserData();
        } else {
            showNonRegisteredView();
        }
    }
    
    private void showNonRegisteredView() {
        nonRegisteredView.setVisibility(View.VISIBLE);
        registeredView.setVisibility(View.GONE);
    }
    
    private void showRegisteredView() {
        nonRegisteredView.setVisibility(View.GONE);
        registeredView.setVisibility(View.VISIBLE);
    }
    
    private void loadUserData() {
        // Load user profile
        String userName = sharedPreferences.getString(KEY_USER_NAME, "Adventure Seeker");
        String userEmail = sharedPreferences.getString(KEY_USER_EMAIL, "");
        
        userNameText.setText(userName);
        
        // Set profile initials
        String initials = getInitials(userName);
        profileInitials.setText(initials);
        
        // Set member since (for now, use a default date - can be enhanced later)
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
        String memberSince = "Member since " + dateFormat.format(new Date());
        memberSinceText.setText(memberSince);
        
        // Load statistics
        loadStatistics();
        loadActivityOverview();
        loadRecentActivity();
    }
    
    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "AS";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        } else if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return "AS";
    }
    
    private void loadStatistics() {
        executorService.execute(() -> {
            int userId = SessionManager.getCurrentUserId(this);
            List<Hike> hikes;
            
            if (userId == -1) {
                hikes = hikeDao.getHikesForNonRegisteredUsers();
            } else {
                hikes = hikeDao.getHikesByUserId(userId);
            }
            
            int totalHikes = hikes.size();
            double totalKm = 0.0;
            int easyCount = 0;
            int mediumCount = 0;
            int hardCount = 0;
            int thisMonthCount = 0;
            
            Calendar calendar = Calendar.getInstance();
            int currentMonth = calendar.get(Calendar.MONTH);
            int currentYear = calendar.get(Calendar.YEAR);
            
            for (Hike hike : hikes) {
                totalKm += hike.getLength();
                
                // Count by difficulty
                String difficulty = hike.getDifficulty();
                if (difficulty != null) {
                    if (difficulty.equalsIgnoreCase("Easy")) {
                        easyCount++;
                    } else if (difficulty.equalsIgnoreCase("Medium")) {
                        mediumCount++;
                    } else if (difficulty.equalsIgnoreCase("Hard") || difficulty.equalsIgnoreCase("Expert")) {
                        hardCount++;
                    }
                }
                
                // Count this month's hikes
                if (hike.getDate() != null) {
                    calendar.setTime(hike.getDate());
                    if (calendar.get(Calendar.MONTH) == currentMonth && 
                        calendar.get(Calendar.YEAR) == currentYear) {
                        thisMonthCount++;
                    }
                }
            }
            
            double avgKm = totalHikes > 0 ? totalKm / totalHikes : 0.0;
            
            final int finalTotalHikes = totalHikes;
            final double finalTotalKm = totalKm;
            final double finalAvgKm = avgKm;
            final int finalThisMonth = thisMonthCount;
            final int finalEasyCount = easyCount;
            final int finalMediumCount = mediumCount;
            final int finalHardCount = hardCount;
            
            runOnUiThread(() -> {
                totalHikesText.setText(String.valueOf(finalTotalHikes));
                DecimalFormat df = new DecimalFormat("#.#");
                totalKmText.setText(df.format(finalTotalKm));
                avgKmText.setText(df.format(finalAvgKm));
                thisMonthText.setText(String.valueOf(finalThisMonth));
                
                // Calculate level (simple calculation: 1 level per 5 hikes)
                int level = Math.max(1, (finalTotalHikes / 5) + 1);
                levelText.setText("Level " + level);
            });
        });
    }
    
    private void loadActivityOverview() {
        executorService.execute(() -> {
            int userId = SessionManager.getCurrentUserId(this);
            List<Hike> hikes;
            
            if (userId == -1) {
                hikes = hikeDao.getHikesForNonRegisteredUsers();
            } else {
                hikes = hikeDao.getHikesByUserId(userId);
            }
            
            int easyCount = 0;
            int mediumCount = 0;
            int hardCount = 0;
            
            for (Hike hike : hikes) {
                String difficulty = hike.getDifficulty();
                if (difficulty != null) {
                    if (difficulty.equalsIgnoreCase("Easy")) {
                        easyCount++;
                    } else if (difficulty.equalsIgnoreCase("Medium")) {
                        mediumCount++;
                    } else if (difficulty.equalsIgnoreCase("Hard") || difficulty.equalsIgnoreCase("Expert")) {
                        hardCount++;
                    }
                }
            }
            
            int total = easyCount + mediumCount + hardCount;
            int easyPercent = total > 0 ? (easyCount * 100 / total) : 0;
            int mediumPercent = total > 0 ? (mediumCount * 100 / total) : 0;
            int hardPercent = total > 0 ? (hardCount * 100 / total) : 0;
            
            final int finalEasyPercent = easyPercent;
            final int finalMediumPercent = mediumPercent;
            final int finalHardPercent = hardPercent;
            
            runOnUiThread(() -> {
                easyTrailsProgress.setProgress(finalEasyPercent);
                mediumTrailsProgress.setProgress(finalMediumPercent);
                hardTrailsProgress.setProgress(finalHardPercent);
                
                easyTrailsPercent.setText(finalEasyPercent + "%");
                mediumTrailsPercent.setText(finalMediumPercent + "%");
                hardTrailsPercent.setText(finalHardPercent + "%");
            });
        });
    }
    
    private void loadRecentActivity() {
        executorService.execute(() -> {
            int userId = SessionManager.getCurrentUserId(this);
            List<Hike> hikes;
            
            if (userId == -1) {
                hikes = hikeDao.getHikesForNonRegisteredUsers();
            } else {
                hikes = hikeDao.getHikesByUserId(userId);
            }
            
            // Sort by date descending and take first 3
            List<Hike> recentHikes = new ArrayList<>();
            if (!hikes.isEmpty()) {
                // Simple sort by date (most recent first)
                hikes.sort((h1, h2) -> {
                    Date d1 = h1.getDate();
                    Date d2 = h2.getDate();
                    if (d1 == null && d2 == null) return 0;
                    if (d1 == null) return 1;
                    if (d2 == null) return -1;
                    return d2.compareTo(d1);
                });
                
                int limit = Math.min(3, hikes.size());
                recentHikes = hikes.subList(0, limit);
            }
            
            final List<Hike> finalRecentHikes = recentHikes;
            runOnUiThread(() -> {
                // Use HikeListAdapter for recent activity
                HikeListAdapter adapter = new HikeListAdapter(this);
                adapter.submitList(finalRecentHikes);
                recentActivityRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                recentActivityRecyclerView.setAdapter(adapter);
            });
        });
    }
    
    private void setupBottomNavigation() {
        setActiveNavItem(navUsers);
        
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(UsersActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });
        
        navHiking.setOnClickListener(v -> {
            Intent intent = new Intent(UsersActivity.this, HikingListActivity.class);
            startActivity(intent);
            finish();
        });
        
        navUsers.setOnClickListener(v -> {
            // Already here
        });
        
        navSettings.setOnClickListener(v -> {
            Intent intent = new Intent(UsersActivity.this, SettingsActivity.class);
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
    
    @Override
    public void onHikeClicked(Hike hike) {
        // Navigate to hike detail page when a hike is clicked in recent activity
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

