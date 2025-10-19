package com.example.floodalert;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class EvacuationRouteActivity extends BaseActivity {

    // --- Constants ---
    private static final String TAG = "EvacuationRouteActivity";
    private static final double MINIMUM_DANGER_LEVEL = 10.0; // in m³/s
    private static final double DANGER_INCREASE_PERCENT = 1.5; // Represents a 50% increase
    private static final String OPEN_METEO_FLOOD_API_URL = "https://api.open-meteo.com/v1/flood?latitude=%f&longitude=%f&daily=river_discharge&forecast_days=3";

    // --- Views ---
    private ProgressBar progressBar;
    private TextView textLoading;
    private LinearLayout layoutSafe;
    private LinearLayout layoutDanger;
    private Button btnFindEvacuationRoute;

    // --- Location & Threading ---
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initViews();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationPermissionLauncher();
        checkRiskAtCurrentLocation();

        btnFindEvacuationRoute.setOnClickListener(v -> openMapsForEvacuation());
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        textLoading = findViewById(R.id.textLoading);
        layoutSafe = findViewById(R.id.layoutSafe);
        layoutDanger = findViewById(R.id.layoutDanger);
        btnFindEvacuationRoute = findViewById(R.id.btnFindEvacuationRoute);
    }

    private void openMapsForEvacuation() {
        String searchQuery = "evacuation shelter OR hospital near me";
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(searchQuery));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(this, "Google Maps app not found.", Toast.LENGTH_LONG).show();
        }
    }

    private void setupLocationPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                fetchLocationAndCheckRisk();
            } else {
                Toast.makeText(this, "Location permission is required to assess flood risk. Assuming safe.", Toast.LENGTH_LONG).show();
                showSafeLayout();
            }
        });
    }

    private void checkRiskAtCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndCheckRisk();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void fetchLocationAndCheckRisk() {
        progressBar.setVisibility(View.VISIBLE);
        textLoading.setVisibility(View.VISIBLE);
        textLoading.setText("Getting location...");

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(true)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdateDelayMillis(10000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                fusedLocationClient.removeLocationUpdates(this);
                Location lastLocation = locationResult.getLastLocation();
                if (lastLocation != null) {
                    textLoading.setText("Analyzing risk...");
                    fetchFloodData(lastLocation.getLatitude(), lastLocation.getLongitude());
                } else {
                    Toast.makeText(EvacuationRouteActivity.this, "Could not get your location. Assuming safe.", Toast.LENGTH_LONG).show();
                    showSafeLayout();
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted, cannot request updates.", e);
            Toast.makeText(this, "Location permission not granted. Assuming safe.", Toast.LENGTH_LONG).show();
            showSafeLayout();
        }
    }

    private void fetchFloodData(double lat, double lon) {
        String urlString = String.format(java.util.Locale.US, OPEN_METEO_FLOOD_API_URL, lat, lon);

        executor.execute(() -> {
            String jsonResponse = null;

            try {
                URL url = new URL(urlString);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                int responseCode = conn.getResponseCode();

                if (responseCode != HttpsURLConnection.HTTP_OK) {
                    throw new IOException("HTTP error code: " + responseCode);
                }

                try (InputStream in = conn.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    jsonResponse = response.toString();
                } finally {
                    conn.disconnect();
                }

                final String finalResponse = jsonResponse;

                // Post to UI thread for parsing and updating the view
                handler.post(() -> {
                    if (finalResponse != null && !finalResponse.isEmpty()) {
                        parseFloodData(finalResponse);
                    } else {
                        Log.w(TAG, "API response was empty. Defaulting to safe.");
                        showSafeLayout();
                    }
                });

            } catch (IOException e) {
                // Catches network/IO failures. We LOG the error but DO NOT show a toast.
                Log.e(TAG, "Network Error fetching flood data. Falling back to Safe Layout.", e);
                handler.post(() -> {
                    showSafeLayout();
                });
            } catch (Exception e) {
                // Catches ANY other unexpected exception. We LOG the error but DO NOT show a toast.
                Log.e(TAG, "Unexpected Runtime Exception during flood data fetch. Falling back to Safe Layout.", e);
                handler.post(() -> {
                    showSafeLayout();
                });
            }
        });
    }

    private void parseFloodData(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);

            if (jsonObject.has("daily")) {
                JSONObject dailyData = jsonObject.getJSONObject("daily");
                if (dailyData.has("river_discharge")) {
                    JSONArray dischargeArray = dailyData.getJSONArray("river_discharge");

                    boolean inDanger = isFloodRiskHigh(dischargeArray);
                    updateUI(inDanger);
                    return;
                }
            }

            // Fallback for missing or unusable river data
            Log.w(TAG, "JSON response did not contain expected flood data. Assuming safe.");
            showSafeLayout();

        } catch (JSONException e) {
            // Catches internal parsing failures. We default to safe without a toast.
            Log.e(TAG, "JSON Parsing Error. Assuming safe.", e);
            showSafeLayout();
        }
    }

    private boolean isFloodRiskHigh(@NonNull JSONArray dischargeArray) throws JSONException {
        // Includes null checks to prevent crashes when accessing array elements
        if (dischargeArray.length() >= 3 && !dischargeArray.isNull(0) && !dischargeArray.isNull(2)) {
            double todayLevel = dischargeArray.getDouble(0);
            double futureLevel = dischargeArray.getDouble(2);

            return futureLevel > (todayLevel * DANGER_INCREASE_PERCENT) && futureLevel > MINIMUM_DANGER_LEVEL;
        }
        return false;
    }


    private void updateUI(boolean inDanger) {
        progressBar.setVisibility(View.GONE);
        textLoading.setVisibility(View.GONE);
        if (inDanger) {
            layoutDanger.setVisibility(View.VISIBLE);
            layoutSafe.setVisibility(View.GONE);
            // Show button in danger state
            btnFindEvacuationRoute.setVisibility(View.VISIBLE);
        } else {
            layoutSafe.setVisibility(View.VISIBLE);
            layoutDanger.setVisibility(View.GONE);
            // Hide button in safe state
            btnFindEvacuationRoute.setVisibility(View.GONE);
        }
    }

    private void showSafeLayout() {
        updateUI(false);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_evacuation_route;
    }

    @Override
    protected int getRootViewId() {
        return R.id.main_container;
    }
}