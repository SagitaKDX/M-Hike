package com.example.mobilecw.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobilecw.R;
import com.example.mobilecw.auth.SessionManager;
import com.example.mobilecw.database.AppDatabase;
import com.example.mobilecw.database.dao.UserDao;
import com.example.mobilecw.database.entities.User;
import com.example.mobilecw.sync.FirebaseSyncManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    private ImageButton backButton;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private MaterialButton loginButton;
    private MaterialButton signupButton;
    private View forgotPasswordText;

    private AppDatabase database;
    private UserDao userDao;
    private ExecutorService executorService;
    private FirebaseAuth firebaseAuth;

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize database
        database = AppDatabase.getDatabase(this);
        userDao = database.userDao();
        executorService = Executors.newSingleThreadExecutor();
        firebaseAuth = FirebaseAuth.getInstance();

        // Initialize views
        initializeViews();

        // Setup click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        loginButton = findViewById(R.id.loginButton);
        signupButton = findViewById(R.id.signupButton);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        loginButton.setOnClickListener(v -> handleLogin());

        signupButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        forgotPasswordText.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.forgot_password_feature), Toast.LENGTH_SHORT).show();
        });
    }

    private void handleLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Clear previous errors
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);

        // Validate form
        if (!validateForm(email, password)) {
            return;
        }

        // Attempt login with Firebase Auth first
        loginButton.setEnabled(false);
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        loginButton.setEnabled(true);
                        Toast.makeText(this, getString(R.string.invalid_credentials), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseUser fbUser = firebaseAuth.getCurrentUser();
                    if (fbUser == null) {
                        loginButton.setEnabled(true);
                        Toast.makeText(this, getString(R.string.invalid_credentials), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String firebaseUid = fbUser.getUid();

                    // Find or create corresponding local Room user
                    executorService.execute(() -> {
                        User user = null;
                        try {
                            user = userDao.getUserByEmail(email);
                            if (user == null) {
                                user = new User(email, password);
                                user.setFirebaseUid(firebaseUid);
                                long newId = userDao.insertUser(user);
                                user.setUserId((int) newId);
                            } else {
                                user.setFirebaseUid(firebaseUid);
                                userDao.updateUser(user);
                            }
                        } catch (Exception e) {
                            // Database error - create a temporary user object
                            user = new User(email, password);
                            user.setFirebaseUid(firebaseUid);
                            user.setUserId(1); // Temporary ID
                        }

                        User finalUser = user;
                        runOnUiThread(() -> {
                            loginButton.setEnabled(true);
                            SessionManager.setCurrentUserId(this, finalUser.getUserId());
                            SessionManager.setCurrentFirebaseUid(this, firebaseUid);

                            // First push any local unsynced data (if present), then download from cloud
                            FirebaseSyncManager.getInstance(getApplicationContext())
                                    .syncNow(new FirebaseSyncManager.SyncCallback() {
                                        @Override
                                        public void onSuccess() {
                                            FirebaseSyncManager.getInstance(getApplicationContext())
                                                    .downloadUserData(new FirebaseSyncManager.SyncCallback() {
                                                        @Override
                                                        public void onSuccess() {
                                                            Toast.makeText(LoginActivity.this, getString(R.string.login_successful), Toast.LENGTH_SHORT).show();
                                                            Intent intent = new Intent(LoginActivity.this, UsersActivity.class);
                                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                            startActivity(intent);
                                                            finish();
                                                        }

                                                        @Override
                                                        public void onFailure(Exception exception) {
                                                            // Even if download fails, allow login to proceed
                                                            Toast.makeText(LoginActivity.this, "Login succeeded but failed to load cloud data", Toast.LENGTH_LONG).show();
                                                            Intent intent = new Intent(LoginActivity.this, UsersActivity.class);
                                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                            startActivity(intent);
                                                            finish();
                                                        }
                                                    });
                                        }

                                        @Override
                                        public void onFailure(Exception exception) {
                                            // If upload sync fails, still attempt to download cloud data
                                            FirebaseSyncManager.getInstance(getApplicationContext())
                                                    .downloadUserData(new FirebaseSyncManager.SyncCallback() {
                                                        @Override
                                                        public void onSuccess() {
                                                            Toast.makeText(LoginActivity.this, getString(R.string.login_successful), Toast.LENGTH_SHORT).show();
                                                            Intent intent = new Intent(LoginActivity.this, UsersActivity.class);
                                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                            startActivity(intent);
                                                            finish();
                                                        }

                                                        @Override
                                                        public void onFailure(Exception exception) {
                                                            Toast.makeText(LoginActivity.this, "Login succeeded but sync failed", Toast.LENGTH_LONG).show();
                                                            Intent intent = new Intent(LoginActivity.this, UsersActivity.class);
                                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                            startActivity(intent);
                                                            finish();
                                                        }
                                                    });
                                        }
                                    });
                        });
                    });
                });
    }

    private boolean validateForm(String email, String password) {
        boolean isValid = true;

        // Validate email
        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError(getString(R.string.email_required));
            isValid = false;
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            emailInputLayout.setError(getString(R.string.email_invalid));
            isValid = false;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.setError(getString(R.string.password_required));
            isValid = false;
        } else if (password.length() < 6) {
            passwordInputLayout.setError(getString(R.string.password_min_length));
            isValid = false;
        }

        return isValid;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}

