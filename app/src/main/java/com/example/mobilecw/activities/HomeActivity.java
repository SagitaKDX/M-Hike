package com.example.mobilecw.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.mobilecw.BuildConfig;
import com.example.mobilecw.R;
import com.example.mobilecw.adapters.NearbyTrailAdapter;
import com.example.mobilecw.database.AppDatabase;
import com.example.mobilecw.database.dao.HikeDao;
import com.example.mobilecw.database.entities.Hike;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity {
    
    private TextView welcomeText, userNameText;
    private TextView weatherLocation, weatherTemp, weatherDescription;
    private ImageView weatherImage;
    private TextView hikesCountText, totalKmText;
    private RecyclerView nearbyTrailsRecyclerView;
    private ImageButton addHikeButton, searchButton;
    
    private androidx.cardview.widget.CardView activeHikeCard;
    private TextView activeHikeName, activeHikeLocation, activeHikeDuration;
    private com.google.android.material.button.MaterialButton viewActiveHikeButton;
    
    private LinearLayout navHome, navHiking, navUsers, navSettings;
    
    private AppDatabase database;
    private HikeDao hikeDao;
    private RequestQueue requestQueue;
    private ExecutorService executorService;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int REQUEST_LOCATION_PERMISSION = 2001;
    private static final double DEFAULT_LAT = 10.4963;
    private static final double DEFAULT_LON = 107.169;
    
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "mhike_prefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        
        // Initialize database
        database = AppDatabase.getDatabase(this);
        hikeDao = database.hikeDao();
        
        // Initialize Volley for network requests
        requestQueue = Volley.newRequestQueue(this);
        
        // Initialize executor for database operations
        executorService = Executors.newSingleThreadExecutor();

        // Initialize location provider for weather
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Initialize views
        initializeViews();
        
        // Setup bottom navigation
        setupBottomNavigation();
        
        // Load user data
        loadUserData();
        
        // Load activity statistics
        loadActivityStats();
        
        // Load nearby trails
        loadNearbyTrails();
        
        // Fetch weather
        fetchWeatherWithUserLocation();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from EnterHikeActivity or HikeDetailActivity
        loadActivityStats();
        loadNearbyTrails();
        loadActiveHike();
    }
    
    private void initializeViews() {
        welcomeText = findViewById(R.id.welcomeText);
        userNameText = findViewById(R.id.userNameText);
        weatherLocation = findViewById(R.id.weatherLocation);
        weatherTemp = findViewById(R.id.weatherTemp);
        weatherDescription = findViewById(R.id.weatherDescription);
        weatherImage = findViewById(R.id.weatherImage);
        hikesCountText = findViewById(R.id.hikesCountText);
        totalKmText = findViewById(R.id.totalKmText);
        nearbyTrailsRecyclerView = findViewById(R.id.nearbyTrailsRecyclerView);
        addHikeButton = findViewById(R.id.addHikeButton);
        searchButton = findViewById(R.id.searchButton);
        
        activeHikeCard = findViewById(R.id.activeHikeCard);
        activeHikeName = findViewById(R.id.activeHikeName);
        activeHikeLocation = findViewById(R.id.activeHikeLocation);
        activeHikeDuration = findViewById(R.id.activeHikeDuration);
        viewActiveHikeButton = findViewById(R.id.viewActiveHikeButton);
        
        navHome = findViewById(R.id.navHome);
        navHiking = findViewById(R.id.navHiking);
        navUsers = findViewById(R.id.navUsers);
        navSettings = findViewById(R.id.navSettings);
        
        // Setup add hike button
        addHikeButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, EnterHikeActivity.class);
            startActivity(intent);
        });
        
        // Setup search button
        searchButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SearchActivity.class);
            startActivity(intent);
        });
        
        // Load active hike
        loadActiveHike();
    }
    
    private void setupBottomNavigation() {
        // Set Home as active
        setActiveNavItem(navHome);
        
        navHome.setOnClickListener(v -> {
            // Already on home, do nothing
        });
        
        navHiking.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, HikingListActivity.class);
            startActivity(intent);
        });
        
        navUsers.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, UsersActivity.class);
            startActivity(intent);
        });
        
        navSettings.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
            startActivity(intent);
            finish();
        });
    }
    
    private void setActiveNavItem(LinearLayout activeItem) {
        // Reset all nav items
        resetNavItem(navHome);
        resetNavItem(navHiking);
        resetNavItem(navUsers);
        resetNavItem(navSettings);
        
        // Set active item
        activeItem.setBackgroundResource(R.drawable.nav_item_background);
        ImageView icon = (ImageView) activeItem.getChildAt(0);
        TextView text = (TextView) activeItem.getChildAt(1);
        icon.setColorFilter(getResources().getColor(R.color.primary_green));
        text.setTextColor(getResources().getColor(R.color.primary_green));
    }
    
    private void resetNavItem(LinearLayout item) {
        item.setBackground(null);
        ImageView icon = (ImageView) item.getChildAt(0);
        TextView text = (TextView) item.getChildAt(1);
        icon.setColorFilter(getResources().getColor(R.color.gray_text));
        text.setTextColor(getResources().getColor(R.color.gray_text));
    }
    
    private void loadUserData() {
        String userName = sharedPreferences.getString(KEY_USER_NAME, "Adventure Seeker");
        userNameText.setText(userName);
    }
    
    private void loadActivityStats() {
        executorService.execute(() -> {
            Integer userId = sharedPreferences.getInt(KEY_USER_ID, -1);
            List<Hike> hikes;
            
            if (userId == -1) {
                // Non-registered user
                hikes = hikeDao.getHikesForNonRegisteredUsers();
            } else {
                // Registered user
                hikes = hikeDao.getHikesByUserId(userId);
            }
            
            int hikeCount = hikes.size();
            double totalKm = 0.0;
            for (Hike hike : hikes) {
                totalKm += hike.getLength();
            }
            
            final int finalHikeCount = hikeCount;
            final double finalTotalKm = totalKm;
            
            runOnUiThread(() -> {
                hikesCountText.setText(String.valueOf(finalHikeCount));
                DecimalFormat df = new DecimalFormat("#.#");
                totalKmText.setText(df.format(finalTotalKm));
            });
        });
    }
    
    private void loadNearbyTrails() {
        executorService.execute(() -> {
            Integer userId = sharedPreferences.getInt(KEY_USER_ID, -1);
            List<Hike> hikes;
            
            if (userId == -1) {
                hikes = hikeDao.getHikesForNonRegisteredUsers();
            } else {
                hikes = hikeDao.getHikesByUserId(userId);
            }
            
            // Get all hikes if user has less than 3
            if (hikes.size() < 3) {
                List<Hike> allHikes = hikeDao.getAllHikes();
                // Limit to 10 most recent
                int limit = Math.min(10, allHikes.size());
                hikes = allHikes.subList(0, limit);
            }
            
            final List<Hike> finalHikes = hikes;
            
            runOnUiThread(() -> {
                NearbyTrailAdapter adapter = new NearbyTrailAdapter(finalHikes, hike -> {
                    // Handle trail click - navigate to detail page
                    Toast.makeText(this, "Viewing: " + hike.getName(), Toast.LENGTH_SHORT).show();
                });
                nearbyTrailsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                nearbyTrailsRecyclerView.setAdapter(adapter);
            });
        });
    }
    
    private void fetchWeatherWithUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION
            );
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        fetchWeather(location.getLatitude(), location.getLongitude());
                    } else {
                        fetchWeather(DEFAULT_LAT, DEFAULT_LON);
                    }
                })
                .addOnFailureListener(e -> fetchWeather(DEFAULT_LAT, DEFAULT_LON));
    }

    private void fetchWeather(double latitude, double longitude) {
        // Using meteoblue Free Weather API (Current weather package)
        // API key stored securely in local.properties via BuildConfig
        String apiKey = BuildConfig.METEOBLUE_API_KEY;
        String url = "https://my.meteoblue.com/packages/current?apikey=" + apiKey +
                "&lat=" + latitude +
                "&lon=" + longitude +
                "&asl=8&format=json&tz=GMT&forecast_days=1";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject metadata = response.getJSONObject("metadata");
                            JSONObject units = response.getJSONObject("units");
                            JSONObject dataCurrent = response.getJSONObject("data_current");

                            double metaLat = metadata.optDouble("latitude", latitude);
                            double metaLon = metadata.optDouble("longitude", longitude);
                            double temp = dataCurrent.getDouble("temperature");
                            String tempUnit = units.optString("temperature", "C");

                            String locationLabel = String.format("Lat %.4f, Lon %.4f", metaLat, metaLon);

                            weatherLocation.setText(locationLabel);
                            weatherTemp.setText(String.format("%.0f°%s", temp, tempUnit));
                            weatherDescription.setText(getString(R.string.weather_updated));

                        } catch (Exception e) {
                            e.printStackTrace();
                            weatherDescription.setText(getString(R.string.weather_error));
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        weatherDescription.setText(getString(R.string.weather_error));
                        weatherLocation.setText("Current Location");
                        weatherTemp.setText("22°C");
                        weatherDescription.setText("Partly Cloudy");
                    }
                }
        );

        requestQueue.add(jsonObjectRequest);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchWeatherWithUserLocation();
            } else {
                // Fall back to default coordinates if permission denied
                fetchWeather(DEFAULT_LAT, DEFAULT_LON);
            }
        }
    }
    
    private void loadActiveHike() {
        executorService.execute(() -> {
            Hike activeHike = hikeDao.getActiveHike();
            runOnUiThread(() -> {
                if (activeHike != null) {
                    activeHikeCard.setVisibility(View.VISIBLE);
                    activeHikeName.setText(activeHike.getName());
                    activeHikeLocation.setText(activeHike.getLocation());
                    
                    // Calculate duration
                    if (activeHike.getStartTime() != null) {
                        long durationMillis = System.currentTimeMillis() - activeHike.getStartTime();
                        long hours = durationMillis / (1000 * 60 * 60);
                        long minutes = (durationMillis / (1000 * 60)) % 60;
                        activeHikeDuration.setText(String.format("Started %dh %dm ago", hours, minutes));
                    } else {
                        activeHikeDuration.setText(getString(R.string.started));
                    }
                    
                    // Setup view button
                    viewActiveHikeButton.setOnClickListener(v -> {
                        Intent intent = new Intent(HomeActivity.this, HikeDetailActivity.class);
                        intent.putExtra(HikeDetailActivity.EXTRA_HIKE_ID, activeHike.getHikeID());
                        startActivity(intent);
                    });
                } else {
                    activeHikeCard.setVisibility(View.GONE);
                }
            });
        });
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}

