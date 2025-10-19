package com.example.floodalert;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.card.MaterialCardView;

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

public class SafetyTipsActivity extends AppCompatActivity {

    private static final String TAG = "SafetyTipsActivity";

    // API 1: River Flood Risk (GloFAS)
    private static final String OPEN_METEO_FLOOD_API_URL = "https://api.open-meteo.com/v1/flood?latitude=%f&longitude=%f&daily=river_discharge&forecast_days=3";
    // API 2: Pluvial Flood Risk (Heavy Rain) - FIX: Added &timezone=auto parameter for reliability
    private static final String OPEN_METEO_WEATHER_API_URL = "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&daily=precipitation_sum&forecast_days=3&timezone=auto";

    // --- River Flood Thresholds (m³/s) ---
    private static final double NORMAL_DISCHARGE = 5.0;
    private static final double CRITICAL_DISCHARGE = 30.0;

    // --- Pluvial Flood Thresholds (mm of daily rain) ---
    private static final double RAIN_RISK_LOW = 50.0;
    private static final double RAIN_RISK_CRITICAL = 150.0;

    private static final float MAX_FEET_IN_VIEW = 5.0f;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private FloodLevelView floodLevelView;
    private LocationCallback locationCallback;
    private ProgressBar progressBar;
    private Button checkLocationButton;

    private boolean isFetchingLocation = false;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safety_tips);

        initViews();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        setupCardExpandListeners();
        setupLocationPermissionLauncher();
        checkLocationButton.setOnClickListener(v -> checkRiskAtCurrentLocation());
    }

    private void initViews() {
        // These IDs must exist in activity_safety_tips.xml to prevent crashes
        floodLevelView = findViewById(R.id.flood_level_view);
        progressBar = findViewById(R.id.progressBar);
        checkLocationButton = findViewById(R.id.check_location_button);
    }

    private void setupLocationPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                fetchLocationAndAnimateFloodLevel();
            } else {
                Toast.makeText(this, "Location permission is required to show local flood risk.", Toast.LENGTH_LONG).show();
                floodLevelView.setFloodLevel(0.0f);
            }
        });
    }

    private void setupCardExpandListeners() {
        setupCardClickListener(R.id.card_before_flood, R.id.tips_layout_before);
        setupCardClickListener(R.id.card_during_flood, R.id.tips_layout_during);
        setupCardClickListener(R.id.card_after_flood, R.id.tips_layout_after);
    }

    private void setupCardClickListener(int cardId, int layoutId) {
        MaterialCardView card = findViewById(cardId);
        View tipsLayout = findViewById(layoutId);
        if (card != null && tipsLayout != null) {
            card.setOnClickListener(v -> {
                tipsLayout.setVisibility(tipsLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            });
        }
    }

    private void checkRiskAtCurrentLocation() {
        if (isFetchingLocation) {
            Toast.makeText(this, "Already checking...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndAnimateFloodLevel();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void fetchLocationAndAnimateFloodLevel() {
        isFetchingLocation = true;
        progressBar.setVisibility(View.VISIBLE);
        checkLocationButton.setEnabled(false);
        Toast.makeText(this, "Fetching your location...", Toast.LENGTH_SHORT).show();

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(true)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                fusedLocationProviderClient.removeLocationUpdates(this);
                Location lastLocation = locationResult.getLastLocation();
                if (lastLocation != null) {
                    // Start the chained fetching process for both river and rain data
                    fetchFloodData(lastLocation);
                } else {
                    Toast.makeText(SafetyTipsActivity.this, "Failed to get location. Defaulting to low risk.", Toast.LENGTH_LONG).show();
                    floodLevelView.setFloodLevel(0.0f);
                    resetState();
                }
            }
        };

        try {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission failed.", e);
            Toast.makeText(this, "Location permission error. Defaulting to low risk.", Toast.LENGTH_LONG).show();
            floodLevelView.setFloodLevel(0.0f);
            resetState();
        }
    }

    private void resetState() {
        isFetchingLocation = false;
        progressBar.setVisibility(View.GONE);
        checkLocationButton.setEnabled(true);
    }

    // New Chained Fetching Method to get both River and Rain data
    private void fetchFloodData(Location location) {
        Toast.makeText(this, "Analyzing combined flood risk...", Toast.LENGTH_SHORT).show();

        String riverUrl = String.format(java.util.Locale.US, OPEN_METEO_FLOOD_API_URL, location.getLatitude(), location.getLongitude());
        String weatherUrl = String.format(java.util.Locale.US, OPEN_METEO_WEATHER_API_URL, location.getLatitude(), location.getLongitude());

        executor.execute(() -> {
            // Step 1: Get River Discharge Data
            String riverJson = fetchUrlData(riverUrl);
            // Step 2: Get Precipitation Data
            String weatherJson = fetchUrlData(weatherUrl);

            // Post to UI thread to process the results
            handler.post(() -> combineAndAnimateFloodData(riverJson, weatherJson));
        });
    }

    /**
     * Helper method to perform the network request and handle basic IO exceptions.
     * Returns null on any failure, posting a network toast to the UI thread.
     */
    private String fetchUrlData(String urlString) {
        String jsonResponse = null;
        try {
            URL url = new URL(urlString);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            int responseCode = conn.getResponseCode();

            if (responseCode != HttpsURLConnection.HTTP_OK) {
                Log.w(TAG, "HTTP error code: " + responseCode + " for URL: " + urlString);
                return null;
            }

            try (InputStream in = conn.getInputStream(); BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                jsonResponse = response.toString();
            } finally {
                conn.disconnect();
            }
        } catch (IOException e) {
            // Log the failure and show an error toast on the UI thread
            Log.e(TAG, "Network Error or IO failure for URL: " + urlString, e);
            handler.post(() -> Toast.makeText(SafetyTipsActivity.this, "Network error during analysis. Check connection.", Toast.LENGTH_LONG).show());
            return null;
        }
        return jsonResponse;
    }


    private void combineAndAnimateFloodData(String riverJson, String weatherJson) {
        float riverRiskLevel = 0.0f;
        float rainRiskLevel = 0.0f;
        String riskSource = "None";
        boolean dataAvailable = false;

        // 1. Parse River Risk
        if (riverJson != null) {
            riverRiskLevel = parseRiverData(riverJson);
            // Check if river data was retrieved successfully, even if risk is zero
            if (riverRiskLevel >= 0.0f) {
                dataAvailable = true;
            }
        }

        // 2. Parse Rain Risk
        if (weatherJson != null) {
            float newRainRisk = parsePluvialData(weatherJson);
            // Check if rain data was retrieved successfully
            if (newRainRisk >= 0.0f) {
                dataAvailable = true;
                rainRiskLevel = newRainRisk;
            }
        }

        // 3. Determine the highest risk and source
        float finalWaterLevel = Math.max(riverRiskLevel, rainRiskLevel);

        if (finalWaterLevel > 0.0f) {
            if (riverRiskLevel > rainRiskLevel) {
                riskSource = "River Discharge";
            } else if (rainRiskLevel > riverRiskLevel) {
                riskSource = "Extreme Rainfall";
            } else {
                riskSource = "Combined Risk"; // If they are equal and > 0
            }

            Toast.makeText(this, String.format("Flood Risk Detected: %.1f ft (Source: %s)",
                    finalWaterLevel, riskSource), Toast.LENGTH_LONG).show();
        } else if (dataAvailable) {
            Toast.makeText(this, "Low immediate flood risk detected (0.0 ft).", Toast.LENGTH_SHORT).show();
        } else {
            // If both APIs failed to return parseable data (and dataAvailable is false)
            Toast.makeText(this, "Could not retrieve comprehensive risk data. Assuming low risk.", Toast.LENGTH_LONG).show();
        }

        floodLevelView.setFloodLevel(finalWaterLevel);
        resetState();
    }

    // --- PARSING METHODS ---

    /** Parses the River Discharge (GloFAS) data and calculates the risk level. */
    private float parseRiverData(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);

            if (jsonObject.has("daily")) {
                JSONObject dailyData = jsonObject.getJSONObject("daily");
                if (dailyData.has("river_discharge")) {
                    JSONArray dischargeArray = dailyData.getJSONArray("river_discharge");
                    double maxDischarge = 0.0;

                    for (int i = 0; i < dischargeArray.length(); i++) {
                        if (!dischargeArray.isNull(i)) {
                            double discharge = dischargeArray.getDouble(i);
                            if (discharge > maxDischarge) {
                                maxDischarge = discharge;
                            }
                        }
                    }
                    return calculateRiverWaterLevel(maxDischarge);
                }
            }
        } catch (JSONException e) {
            // Log the error but return 0.0f to be handled by the calling method
            Log.e(TAG, "River JSON Parsing Error: ", e);
        }
        return -1.0f; // Return a negative value to signal parsing failure
    }

    /** Parses the Precipitation Sum (Pluvial) data and calculates the risk level. */
    private float parsePluvialData(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);

            if (jsonObject.has("daily")) {
                JSONObject dailyData = jsonObject.getJSONObject("daily");
                if (dailyData.has("precipitation_sum")) {
                    JSONArray precipitationArray = dailyData.getJSONArray("precipitation_sum");
                    double maxRainfall = 0.0;

                    for (int i = 0; i < precipitationArray.length(); i++) {
                        if (!precipitationArray.isNull(i)) {
                            double rainfall = precipitationArray.getDouble(i);
                            if (rainfall > maxRainfall) {
                                maxRainfall = rainfall;
                            }
                        }
                    }
                    return calculateRainWaterLevel(maxRainfall);
                }
            }
        } catch (JSONException e) {
            // Log the error but return 0.0f to be handled by the calling method
            Log.e(TAG, "Rain JSON Parsing Error: ", e);
        }
        return -1.0f; // Return a negative value to signal parsing failure
    }

    // --- CALCULATION METHODS ---

    private float calculateRiverWaterLevel(double dischargeValue) {
        if (dischargeValue <= NORMAL_DISCHARGE) {
            return 0.0f;
        }
        if (dischargeValue >= CRITICAL_DISCHARGE) {
            return MAX_FEET_IN_VIEW;
        }
        double percentage = (dischargeValue - NORMAL_DISCHARGE) / (CRITICAL_DISCHARGE - NORMAL_DISCHARGE);
        return (float) (percentage * MAX_FEET_IN_VIEW);
    }

    private float calculateRainWaterLevel(double rainfallMM) {
        if (rainfallMM <= RAIN_RISK_LOW) {
            return 0.0f;
        }
        if (rainfallMM >= RAIN_RISK_CRITICAL) {
            return MAX_FEET_IN_VIEW;
        }

        // Scale the rain risk between 0.0f and MAX_FEET_IN_VIEW
        double percentage = (rainfallMM - RAIN_RISK_LOW) / (RAIN_RISK_CRITICAL - RAIN_RISK_LOW);
        return (float) (percentage * MAX_FEET_IN_VIEW);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (fusedLocationProviderClient != null && locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }
}