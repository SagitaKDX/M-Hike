package com.example.mobilecw.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.mobilecw.R;
import com.example.mobilecw.auth.SessionManager;
import com.example.mobilecw.database.AppDatabase;
import com.example.mobilecw.database.dao.UserDao;
import com.example.mobilecw.database.entities.User;
import com.example.mobilecw.sync.FirebaseSyncManager;
import com.google.android.material.button.MaterialButton;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    private static final String SETTINGS_PREFS = "settings_prefs";
    private static final String KEY_THEME_GREEN = "theme_green";

    private TextView accountNameText;
    private TextView accountEmailText;
    private TextView accountInitialsText;
    private TextView themeDescriptionText;
    private SwitchCompat themeSwitch;
    private MaterialButton logoutButton;

    private LinearLayout navHome, navHiking, navUsers, navSettings;

    private AppDatabase database;
    private UserDao userDao;
    private ExecutorService executorService;
    private SharedPreferences settingsPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        database = AppDatabase.getDatabase(this);
        userDao = database.userDao();
        executorService = Executors.newSingleThreadExecutor();
        settingsPreferences = getSharedPreferences(SETTINGS_PREFS, MODE_PRIVATE);

        initializeViews();
        setupClickListeners();
        setupBottomNavigation();
        loadAccountInfo();
        initializeThemeSwitch();
    }

    private void initializeViews() {
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        accountNameText = findViewById(R.id.accountNameText);
        accountEmailText = findViewById(R.id.accountEmailText);
        accountInitialsText = findViewById(R.id.accountInitialsText);
        themeDescriptionText = findViewById(R.id.themeDescriptionText);
        themeSwitch = findViewById(R.id.themeSwitch);
        logoutButton = findViewById(R.id.logoutButton);

        navHome = findViewById(R.id.navHome);
        navHiking = findViewById(R.id.navHiking);
        navUsers = findViewById(R.id.navUsers);
        navSettings = findViewById(R.id.navSettings);
    }

    private void setupClickListeners() {
        View[] futureRows = new View[]{
                findViewById(R.id.notificationsRow),
                findViewById(R.id.unitsRow),
                findViewById(R.id.languageRow),
                findViewById(R.id.privacyRow),
                findViewById(R.id.dataRow),
                findViewById(R.id.helpRow),
                findViewById(R.id.aboutRow)
        };

        for (View row : futureRows) {
            if (row != null) {
                row.setOnClickListener(v -> showComingSoonMessage());
            }
        }

        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsPreferences.edit().putBoolean(KEY_THEME_GREEN, isChecked).apply();
            updateThemeDescription(isChecked);
            showComingSoonMessage();
        });

        logoutButton.setOnClickListener(v -> handleLogout());
    }

    private void initializeThemeSwitch() {
        boolean isGreenTheme = settingsPreferences.getBoolean(KEY_THEME_GREEN, true);
        themeSwitch.setChecked(isGreenTheme);
        updateThemeDescription(isGreenTheme);
    }

    private void updateThemeDescription(boolean isGreenTheme) {
        int descriptionRes = isGreenTheme ? R.string.theme_description_green : R.string.theme_description_brown;
        themeDescriptionText.setText(descriptionRes);
    }

    private void loadAccountInfo() {
        int userId = SessionManager.getCurrentUserId(this);
        if (userId == -1) {
            setAccountInfo(getString(R.string.adventure_seeker), getString(R.string.default_user_email));
            updateLogoutState(false);
            return;
        }

        executorService.execute(() -> {
            User user = userDao.getUserById(userId);
            runOnUiThread(() -> {
                if (user != null) {
                    String name = user.getUserName() != null && !user.getUserName().trim().isEmpty()
                            ? user.getUserName()
                            : getString(R.string.adventure_seeker);
                    String email = user.getUserEmail() != null && !user.getUserEmail().trim().isEmpty()
                            ? user.getUserEmail()
                            : getString(R.string.default_user_email);
                    setAccountInfo(name, email);
                    updateLogoutState(true);
                } else {
                    setAccountInfo(getString(R.string.adventure_seeker), getString(R.string.default_user_email));
                    updateLogoutState(false);
                }
            });
        });
    }

    private void setAccountInfo(String name, String email) {
        accountNameText.setText(name);
        accountEmailText.setText(email);
        accountInitialsText.setText(generateInitials(name));
    }

    private String generateInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "AS";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private void updateLogoutState(boolean enabled) {
        logoutButton.setEnabled(enabled);
        logoutButton.setAlpha(enabled ? 1f : 0.5f);
    }

    private void handleLogout() {
        if (!SessionManager.isLoggedIn(this)) {
            Toast.makeText(this, R.string.logout_disabled_hint, Toast.LENGTH_SHORT).show();
            return;
        }

        logoutButton.setEnabled(false);
        logoutButton.setText(R.string.logout_syncing);
        Toast.makeText(this, R.string.logout_syncing_message, Toast.LENGTH_SHORT).show();

        FirebaseSyncManager.getInstance(getApplicationContext()).syncNow(new FirebaseSyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                executorService.execute(() -> {
                    database.clearAllTables();
                    runOnUiThread(() -> {
                        SessionManager.clearCurrentUser(SettingsActivity.this);
                        Toast.makeText(SettingsActivity.this, R.string.logout_success, Toast.LENGTH_SHORT).show();
                        navigateToUsers();
                    });
                });
            }

            @Override
            public void onFailure(Exception exception) {
                runOnUiThread(() -> {
                    logoutButton.setEnabled(true);
                    logoutButton.setText(R.string.logout);
                    Toast.makeText(SettingsActivity.this, R.string.logout_sync_failed, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void navigateToUsers() {
        Intent intent = new Intent(SettingsActivity.this, UsersActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBottomNavigation() {
        setActiveNavItem(navSettings);

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });

        navHiking.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, HikingListActivity.class);
            startActivity(intent);
            finish();
        });

        navUsers.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, UsersActivity.class);
            startActivity(intent);
            finish();
        });

        navSettings.setOnClickListener(v -> {
            // Already on settings
        });
    }

    private void setActiveNavItem(LinearLayout activeItem) {
        resetNavItem(navHome);
        resetNavItem(navHiking);
        resetNavItem(navUsers);
        resetNavItem(navSettings);

        if (activeItem != null && activeItem.getChildCount() >= 2) {
            activeItem.setBackgroundResource(R.drawable.nav_item_background);
            ImageView icon = (ImageView) activeItem.getChildAt(0);
            TextView text = (TextView) activeItem.getChildAt(1);
            icon.setColorFilter(getResources().getColor(R.color.primary_green));
            text.setTextColor(getResources().getColor(R.color.primary_green));
        }
    }

    private void resetNavItem(LinearLayout item) {
        if (item == null || item.getChildCount() < 2) {
            return;
        }
        item.setBackground(null);
        ImageView icon = (ImageView) item.getChildAt(0);
        TextView text = (TextView) item.getChildAt(1);
        icon.setColorFilter(getResources().getColor(R.color.gray_text));
        text.setTextColor(getResources().getColor(R.color.gray_text));
    }

    private void showComingSoonMessage() {
        Toast.makeText(this, R.string.feature_coming_soon, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}

