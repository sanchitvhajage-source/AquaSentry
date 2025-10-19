package com.example.floodalert;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color; // CORRECTED: Import for Color
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

// --- ADDED: All necessary imports for Google Play Services Location ---
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.google.android.material.button.MaterialButton;

import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

public class MapRoutesActivity extends AppCompatActivity {

    // --- All features and variables are unchanged ---
    private MapView map;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private GeoPoint currentLocation;
    private TextView statusTitleText, statusDescriptionText;
    private MaterialButton checkSafetyButton;
    private List<BoundingBox> currentFloodZones;
    private List<GeoPoint> safeZones;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE));
        setContentView(R.layout.activity_map_routes);

        map = findViewById(R.id.map);
        statusTitleText = findViewById(R.id.status_title_text);
        statusDescriptionText = findViewById(R.id.status_description_text);
        checkSafetyButton = findViewById(R.id.check_safety_button);
        map.setMultiTouchControls(true);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationPermissionLauncher();
        initializeSimulatedData();

        checkSafetyButton.setOnClickListener(v -> promptForLocationAndCheckStatus());
        findViewById(R.id.fab_my_location).setOnClickListener(v -> centerOnMyLocation());
    }

    // --- All feature logic below is exactly the same as before ---

    private void initializeSimulatedData() {
        currentFloodZones = new ArrayList<>();
        currentFloodZones.add(new BoundingBox(19.025, 72.85, 19.015, 72.84));
        currentFloodZones.add(new BoundingBox(19.045, 72.865, 19.035, 72.855));
        safeZones = new ArrayList<>();
        safeZones.add(new GeoPoint(19.0176, 72.8562));
        safeZones.add(new GeoPoint(18.9432, 72.8228));
        safeZones.add(new GeoPoint(19.1197, 72.8437));
    }

    private void setupLocationPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                getCurrentLocationAndAssessSafety();
            } else {
                Toast.makeText(this, "Location permission is required to assess safety.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void promptForLocationAndCheckStatus() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocationAndAssessSafety();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void getCurrentLocationAndAssessSafety() {
        Toast.makeText(this, "Checking your location...", Toast.LENGTH_SHORT).show();
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                    boolean isInDanger = isUserInFloodedZone(currentLocation);
                    if (isInDanger) {
                        updateUiForDanger(currentLocation);
                    } else {
                        updateUiForSafe(currentLocation);
                    }
                } else {
                    Toast.makeText(this, "Could not get your location. Please ensure GPS is enabled.", Toast.LENGTH_LONG).show();
                }
            });
        } catch (SecurityException e) {
            Toast.makeText(this, "Location permission error.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isUserInFloodedZone(GeoPoint userLocation) {
        for (BoundingBox zone : currentFloodZones) {
            if (zone.contains(userLocation)) {
                return true;
            }
        }
        return false;
    }

    private void updateUiForSafe(GeoPoint userLocation) {
        map.getOverlays().clear();
        map.invalidate();
        statusTitleText.setText("Location Status: Safe");
        statusDescriptionText.setText("Your current location appears to be safe from reported flooding. Stay aware and check back if conditions change.");
        checkSafetyButton.setText("Re-check My Location");
        Marker safeMarker = new Marker(map);
        safeMarker.setPosition(userLocation);
        safeMarker.setTitle("You are here");
        safeMarker.setSubDescription("Location appears safe.");
        // NOTE: Make sure you have 'ic_baseline_check_circle_24.xml' in your res/drawable folder
        safeMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_baseline_check_circle_24));
        map.getOverlays().add(safeMarker);
        centerOnMyLocation();
    }

    private void updateUiForDanger(GeoPoint userLocation) {
        map.getOverlays().clear();
        map.invalidate();
        statusTitleText.setText("Warning: Flood Zone Detected");
        statusDescriptionText.setText("Your location is within a reported flood zone. Calculating the nearest evacuation route to a safe area.");
        checkSafetyButton.setText("Recalculating Route...");
        checkSafetyButton.setEnabled(false);
        GeoPoint nearestSafeZone = findNearestPoint(userLocation, safeZones);
        new UpdateRoadTask().execute(userLocation, nearestSafeZone);
    }

    private GeoPoint findNearestPoint(GeoPoint current, List<GeoPoint> points) {
        GeoPoint nearest = null;
        double minDistance = -1;
        for (GeoPoint p : points) {
            double distance = current.distanceToAsDouble(p);
            if (minDistance == -1 || distance < minDistance) {
                minDistance = distance;
                nearest = p;
            }
        }
        return nearest;
    }

    private void centerOnMyLocation() {
        if (currentLocation != null) {
            map.getController().animateTo(currentLocation, 16.0, 1000L);
        } else {
            promptForLocationAndCheckStatus();
        }
    }

    private class UpdateRoadTask extends AsyncTask<GeoPoint, Void, Road> {
        @Override
        protected Road doInBackground(GeoPoint... params) {
            ArrayList<GeoPoint> waypoints = new ArrayList<>();
            waypoints.add(params[0]);
            waypoints.add(params[1]);
            RoadManager roadManager = new OSRMRoadManager(MapRoutesActivity.this, "FloodAlertApp/1.0");
            return roadManager.getRoad(waypoints);
        }

        @Override
        protected void onPostExecute(Road road) {
            super.onPostExecute(road);
            checkSafetyButton.setText("Check My Safety Status");
            checkSafetyButton.setEnabled(true);
            if (road.mStatus == Road.STATUS_OK) {
                Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
                // CORRECTED: Using a standard color to avoid resource errors.
                roadOverlay.getOutlinePaint().setColor(Color.parseColor("#388BFD"));
                roadOverlay.getOutlinePaint().setStrokeWidth(12);
                map.getOverlays().add(roadOverlay);
                Marker startMarker = new Marker(map);
                startMarker.setPosition(currentLocation);
                startMarker.setTitle("Your Location (Danger Zone)");
                // NOTE: Make sure you have 'ic_baseline_warning_24.xml' in your res/drawable folder
                startMarker.setIcon(ContextCompat.getDrawable(MapRoutesActivity.this, R.drawable.ic_baseline_warning_24));
                map.getOverlays().add(startMarker);
                Marker endMarker = new Marker(map);
                endMarker.setPosition(road.mRouteHigh.get(road.mRouteHigh.size() - 1));
                endMarker.setTitle("Nearest Safe Zone");
                // NOTE: Make sure you have 'ic_baseline_check_circle_24.xml' in your res/drawable folder
                endMarker.setIcon(ContextCompat.getDrawable(MapRoutesActivity.this, R.drawable.ic_baseline_check_circle_24));
                map.getOverlays().add(endMarker);
                BoundingBox boundingBox = road.mBoundingBox;
                map.zoomToBoundingBox(boundingBox, true, 100);
                map.invalidate();
                statusDescriptionText.setText("Evacuation route calculated. Please proceed to the safe zone with caution.");
            } else {
                Toast.makeText(MapRoutesActivity.this, "Error: Could not calculate evacuation route. Status: " + road.mStatus, Toast.LENGTH_LONG).show();
                statusDescriptionText.setText("Could not calculate a route. Please check your internet connection and try again.");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }
}