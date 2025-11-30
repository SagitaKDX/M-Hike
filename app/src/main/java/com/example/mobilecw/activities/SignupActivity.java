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

public class SignupActivity extends AppCompatActivity {

    private ImageButton backButton;
    private TextInputEditText nameEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText phoneEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText confirmPasswordEditText;
    private TextInputLayout nameInputLayout;
    private TextInputLayout emailInputLayout;
    private TextInputLayout phoneInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputLayout confirmPasswordInputLayout;
    private MaterialButton signupButton;
    private MaterialButton loginButton;

    private AppDatabase database;
    private UserDao userDao;
    private ExecutorService executorService;
    private FirebaseAuth firebaseAuth;

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    // Phone validation pattern (allows various formats)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[+]?[\\d\\s\\-()]{10,}$"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

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
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        nameInputLayout = findViewById(R.id.nameInputLayout);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        phoneInputLayout = findViewById(R.id.phoneInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);
        signupButton = findViewById(R.id.signupButton);
        loginButton = findViewById(R.id.loginButton);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        signupButton.setOnClickListener(v -> handleSignup());

        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    private void handleSignup() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // Clear previous errors
        nameInputLayout.setError(null);
        emailInputLayout.setError(null);
        phoneInputLayout.setError(null);
        passwordInputLayout.setError(null);
        confirmPasswordInputLayout.setError(null);

        // Validate form
        if (!validateForm(name, email, phone, password, confirmPassword)) {
            return;
        }

        // Check if email already exists
        executorService.execute(() -> {
            User existingUser = userDao.getUserByEmail(email);
            runOnUiThread(() -> {
                if (existingUser != null) {
                    emailInputLayout.setError(getString(R.string.email_already_exists));
                } else {
                    // First create account in Firebase Auth
                    signupButton.setEnabled(false);
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(task -> {
                                if (!task.isSuccessful()) {
                                    signupButton.setEnabled(true);
                                    Toast.makeText(this, getString(R.string.signup_failed), Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                FirebaseUser firebaseUser = task.getResult().getUser();
                                if (firebaseUser == null) {
                                    signupButton.setEnabled(true);
                                    Toast.makeText(this, getString(R.string.signup_failed), Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                String firebaseUid = firebaseUser.getUid();

                                // Now create local Room user with firebaseUid
                                executorService.execute(() -> {
                                    User newUser = new User(name, email, password, phone);
                                    newUser.setFirebaseUid(firebaseUid);
                                    long userId = userDao.insertUser(newUser);
                                    runOnUiThread(() -> {
                                        signupButton.setEnabled(true);
                                        if (userId > 0) {
                                            SessionManager.setCurrentUserId(this, (int) userId);
                                            SessionManager.setCurrentFirebaseUid(this, firebaseUid);
                                            FirebaseSyncManager.getInstance(getApplicationContext()).syncNow();
                                            Toast.makeText(this, getString(R.string.signup_successful), Toast.LENGTH_SHORT).show();

                                            Intent intent = new Intent(SignupActivity.this, UsersActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Toast.makeText(this, getString(R.string.signup_failed), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                });
                            });
                }
            });
        });
    }

    private boolean validateForm(String name, String email, String phone, String password, String confirmPassword) {
        boolean isValid = true;

        // Validate name
        if (TextUtils.isEmpty(name) || name.length() < 2) {
            nameInputLayout.setError(getString(R.string.name_min_length));
            isValid = false;
        }

        // Validate email
        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError(getString(R.string.email_required));
            isValid = false;
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            emailInputLayout.setError(getString(R.string.email_invalid));
            isValid = false;
        }

        // Validate phone
        if (TextUtils.isEmpty(phone)) {
            phoneInputLayout.setError(getString(R.string.phone_required));
            isValid = false;
        } else if (!PHONE_PATTERN.matcher(phone).matches()) {
            phoneInputLayout.setError(getString(R.string.phone_invalid));
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

        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInputLayout.setError(getString(R.string.confirm_password_required));
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordInputLayout.setError(getString(R.string.passwords_do_not_match));
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

