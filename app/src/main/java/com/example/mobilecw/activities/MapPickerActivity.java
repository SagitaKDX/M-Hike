package com.example.mobilecw.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.mobilecw.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.overlay.Marker;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Map picker activity using Mapsforge with OpenStreetMap tiles.
 * No API key required - uses free OpenStreetMap data.
 */
public class MapPickerActivity extends AppCompatActivity {

    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_ADDRESS = "address";

    private MapView mapView;
    private TileCache tileCache;
    private TileDownloadLayer tileDownloadLayer;
    private Marker currentMarker;
    
    private LatLong selectedLocation;
    private String selectedAddress = "";

    private TextView tvSelectedLocation;
    private Button btnConfirmLocation;
    private FusedLocationProviderClient fusedLocationClient;

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    
    // Default location (London) - change as needed
    private static final double DEFAULT_LAT = 51.5074;
    private static final double DEFAULT_LON = -0.1278;
    private static final byte DEFAULT_ZOOM = 12;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize AndroidGraphicFactory BEFORE setContentView
        AndroidGraphicFactory.createInstance(getApplication());
        
        setContentView(R.layout.activity_map_picker);

        // Initialize views
        tvSelectedLocation = findViewById(R.id.tvSelectedLocation);
        btnConfirmLocation = findViewById(R.id.btnConfirmLocation);
        Button btnCancel = findViewById(R.id.btnCancel);
        mapView = findViewById(R.id.mapView);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Setup map
        setupMap();

        // Confirm button click
        btnConfirmLocation.setOnClickListener(v -> {
            if (selectedLocation != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_LATITUDE, selectedLocation.latitude);
                resultIntent.putExtra(EXTRA_LONGITUDE, selectedLocation.longitude);
                resultIntent.putExtra(EXTRA_ADDRESS, selectedAddress);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Please tap on the map to select a location", Toast.LENGTH_SHORT).show();
            }
        });

        // Cancel button click
        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        // Initially disable confirm button
        btnConfirmLocation.setEnabled(false);
        
        // Try to get current location
        getCurrentLocationAndCenter();
    }

    private void setupMap() {
        // Create tile cache
        tileCache = AndroidUtil.createTileCache(
                this,
                "mapcache",
                mapView.getModel().displayModel.getTileSize(),
                1f,
                mapView.getModel().frameBufferModel.getOverdrawFactor()
        );

        OpenStreetMapMapnik tileSource = OpenStreetMapMapnik.INSTANCE;
        tileSource.setUserAgent("MHike-Android-App");
        
        tileDownloadLayer = new TileDownloadLayer(
                tileCache,
                mapView.getModel().mapViewPosition,
                tileSource,
                AndroidGraphicFactory.INSTANCE
        );
        
        mapView.getLayerManager().getLayers().add(tileDownloadLayer);

        // Set initial position
        mapView.getModel().mapViewPosition.setCenter(new LatLong(DEFAULT_LAT, DEFAULT_LON));
        mapView.getModel().mapViewPosition.setZoomLevel(DEFAULT_ZOOM);

        // Enable built-in zoom controls
        mapView.setBuiltInZoomControls(true);

        // Handle map tap to drop pin
        mapView.setOnTouchListener(new View.OnTouchListener() {
            private float startX, startY;
            private long startTime;
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        startTime = System.currentTimeMillis();
                        break;
                        
                    case MotionEvent.ACTION_UP:
                        float endX = event.getX();
                        float endY = event.getY();
                        long duration = System.currentTimeMillis() - startTime;
                        
                        // Check if it's a tap (not a drag)
                        float distance = (float) Math.sqrt(
                                Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2)
                        );
                        
                        if (distance < 20 && duration < 300) {
                            // It's a tap - place marker
                            LatLong tapLatLong = mapView.getMapViewProjection()
                                    .fromPixels(endX, endY);
                            
                            if (tapLatLong != null) {
                                placeMarker(tapLatLong);
                            }
                        }
                        break;
                }
                return false; // Allow map to handle the event too
            }
        });
    }

    private void placeMarker(LatLong latLong) {
        // Remove existing marker
        if (currentMarker != null) {
            mapView.getLayerManager().getLayers().remove(currentMarker);
        }

        // Create marker bitmap
        Bitmap markerBitmap = createMarkerBitmap();
        
        // Create and add new marker
        currentMarker = new Marker(latLong, markerBitmap, 0, -markerBitmap.getHeight() / 2);
        mapView.getLayerManager().getLayers().add(currentMarker);

        selectedLocation = latLong;
        btnConfirmLocation.setEnabled(true);

        // Get address from coordinates
        getAddressFromLocation(latLong);
        
        // Redraw map
        mapView.getLayerManager().redrawLayers();
    }

    private Bitmap createMarkerBitmap() {
        // Create a simple marker using Mapsforge graphics
        int width = 48;
        int height = 48;
        
        Bitmap bitmap = AndroidGraphicFactory.INSTANCE.createBitmap(width, height);
        
        // Draw a red circle with white border
        org.mapsforge.core.graphics.Canvas canvas = AndroidGraphicFactory.INSTANCE.createCanvas();
        canvas.setBitmap(bitmap);
        
        // White border
        Paint whitePaint = AndroidGraphicFactory.INSTANCE.createPaint();
        whitePaint.setColor(Color.WHITE);
        whitePaint.setStyle(Style.FILL);
        canvas.drawCircle(width / 2, height / 2, 20, whitePaint);
        
        // Red fill
        Paint redPaint = AndroidGraphicFactory.INSTANCE.createPaint();
        redPaint.setColor(Color.RED);
        redPaint.setStyle(Style.FILL);
        canvas.drawCircle(width / 2, height / 2, 16, redPaint);
        
        // Inner white dot
        canvas.drawCircle(width / 2, height / 2, 6, whitePaint);
        
        return bitmap;
    }

    private void getCurrentLocationAndCenter() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLong currentLatLong = new LatLong(
                                location.getLatitude(), 
                                location.getLongitude()
                        );
                        mapView.getModel().mapViewPosition.setCenter(currentLatLong);
                        mapView.getModel().mapViewPosition.setZoomLevel((byte) 15);
                    }
                });
    }

    private void getAddressFromLocation(LatLong latLong) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    latLong.latitude, 
                    latLong.longitude, 
                    1
            );
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();

                // Build address string
                if (address.getThoroughfare() != null) {
                    sb.append(address.getThoroughfare());
                }
                if (address.getLocality() != null) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(address.getLocality());
                }
                if (address.getAdminArea() != null) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(address.getAdminArea());
                }
                if (address.getCountryName() != null) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(address.getCountryName());
                }

                selectedAddress = sb.length() > 0 ? sb.toString() :
                        String.format(Locale.US, "%.6f, %.6f", latLong.latitude, latLong.longitude);
            } else {
                selectedAddress = String.format(Locale.US, "%.6f, %.6f", 
                        latLong.latitude, latLong.longitude);
            }
        } catch (IOException e) {
            selectedAddress = String.format(Locale.US, "%.6f, %.6f", 
                    latLong.latitude, latLong.longitude);
        }

        tvSelectedLocation.setText(selectedAddress);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAndCenter();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (tileDownloadLayer != null) {
            tileDownloadLayer.onResume();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (tileDownloadLayer != null) {
            tileDownloadLayer.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.destroyAll();
        }
        if (tileCache != null) {
            tileCache.destroy();
        }
        AndroidGraphicFactory.clearResourceMemoryCache();
    }
}
