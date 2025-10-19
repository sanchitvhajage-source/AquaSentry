package com.example.floodalert;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionsRequestActivity extends AppCompatActivity {

    private static final String TAG = "PermissionsActivity";
    private static final int LOCATION_PERMISSION_CODE = 100;
    private static final int NOTIFICATION_PERMISSION_CODE = 101;

    private Button btnLocation;
    private Button btnNotifications;
    private Button btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions_request);

        btnLocation = findViewById(R.id.btn_enable_location);
        btnNotifications = findViewById(R.id.btn_allow_notifications);
        btnContinue = findViewById(R.id.btn_continue);

        btnLocation.setOnClickListener(view -> requestLocationPermissions());
        btnNotifications.setOnClickListener(view -> requestNotificationPermission());
        btnContinue.setOnClickListener(view -> navigateToDashboard());

        // Initial check
        updateUiStatus();
        // FIX: Check on create to auto-proceed if permissions were already granted
        checkAllPermissionsAndProceed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUiStatus();
        // FIX: Re-check permissions and auto-proceed every time user returns from settings
        checkAllPermissionsAndProceed();
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, FloodDashboardActivity.class);
        startActivity(intent);
        finish();
    }

    // ====================================================================
    // CORE PERMISSION AND PROCEED LOGIC
    // ====================================================================

    /**
     * FIX: Unified logic to check all requirements and proceed to dashboard.
     * This method is called in onResume, onCreate, and after permission grants.
     */
    private void checkAllPermissionsAndProceed() {
        boolean locationOk = isLocationPermissionGranted() && isLocationServiceEnabled();
        boolean notificationsOk = isNotificationPermissionGranted();

        // Only navigate if ALL essential permissions and services are active
        if (locationOk && notificationsOk) {
            navigateToDashboard();
        }
        // If not all are met, the user stays on the page to fix the remaining permissions.
    }

    // ====================================================================
    // PERMISSION & SERVICE CHECKERS (Unchanged)
    // ====================================================================

    private boolean isLocationPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isLocationServiceEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return false;
        }
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private boolean isNotificationPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Notifications are granted by default on older OS versions
    }

    // ====================================================================
    // PERMISSION REQUESTERS
    // ====================================================================

    private void requestLocationPermissions() {
        if (isLocationPermissionGranted()) {
            if (isLocationServiceEnabled()) {
                // Both are enabled, proceed automatically (redundant due to onResume, but safe)
                Toast.makeText(this, "Location already enabled! Proceeding...", Toast.LENGTH_SHORT).show();
                checkAllPermissionsAndProceed();
            } else {
                // Guide user to turn on GPS/Location Service
                Toast.makeText(this, "Permission granted, but please turn on Location services in your phone's settings.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            // Request app's location permission
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_CODE);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!isNotificationPermissionGranted()) {
                // Request Android 13+ notification permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            } else {
                // Already granted, proceed automatically
                Toast.makeText(this, "Notifications already allowed! Proceeding...", Toast.LENGTH_SHORT).show();
                checkAllPermissionsAndProceed();
            }
        } else {
            // Older OS: Notifications are automatically allowed, so proceed.
            Toast.makeText(this, "Notifications allowed by OS. Proceeding...", Toast.LENGTH_SHORT).show();
            checkAllPermissionsAndProceed();
        }
    }

    // ====================================================================
    // PERMISSION RESULT HANDLER (Now calls the universal check)
    // ====================================================================

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show();

            // FIX: Auto-proceed after a successful grant if all other conditions are met.
            checkAllPermissionsAndProceed();
        } else {
            Toast.makeText(this, "Permission Denied. Features requiring this permission will not work.", Toast.LENGTH_LONG).show();
        }

        updateUiStatus();
    }

    /**
     * --- MODIFIED: The UI status checker (Unchanged) ---
     */
    private void updateUiStatus() {
        if (btnLocation != null) {
            // The button will only say "ENABLED" if BOTH permission AND GPS service are ON.
            if (isLocationPermissionGranted() && isLocationServiceEnabled()) {
                btnLocation.setText("Location ENABLED");
                btnLocation.setEnabled(false);
            } else {
                btnLocation.setText("Enable Location");
                btnLocation.setEnabled(true);
            }
        }
        if (btnNotifications != null) {
            if (isNotificationPermissionGranted()) {
                btnNotifications.setText("Notifications ALLOWED");
            } else {
                btnNotifications.setText("Allow Notifications");
            }
        }

        // Enable or disable the Continue button based on the checks (optional visual indicator)
        boolean allReady = isLocationPermissionGranted() && isLocationServiceEnabled() && isNotificationPermissionGranted();
        if (btnContinue != null) {
            btnContinue.setEnabled(allReady);
            // Optional: change color/visibility if you want visual feedback on the button
        }
    }
}