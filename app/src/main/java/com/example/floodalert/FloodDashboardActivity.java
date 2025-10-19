package com.example.floodalert; // Your package name

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;

// This class correctly extends BaseActivity
public class FloodDashboardActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // NASA API Readiness Check
        initializeNasaDataSystem();

        // 1. Safety Tips (Unchanged)
        setupDashboardItem(
                R.id.item_safety_tips,
                R.drawable.ic_baseline_verified_user_24,
                R.color.accent_green,
                getString(R.string.title_safety_tips_item),
                getString(R.string.desc_safety_tips_item),
                SafetyTipsActivity.class
        );

        // 2. Current Flood Map (CHANGED)
        // This is your renamed item.
        setupDashboardItem(
                R.id.item_current_flood_map, // <-- ID from new XML
                R.drawable.ic_baseline_map_24,
                R.color.accent_blue,
                "Current Flood Map", // <-- New Title
                "Check the flood risk for your current area.", // <-- New Subtitle
                MapRoutesActivity.class // This still opens your original map
        );

        // 3. Emergency Contacts (Unchanged)
        setupDashboardItem(
                R.id.item_emergency_contact,
                R.drawable.ic_baseline_call_24,
                R.color.accent_purple,
                getString(R.string.title_emergency_contacts),
                getString(R.string.desc_emergency_contacts),
                EmergencyContactsActivity.class
        );

        // 4. Evacuation Routes (NEWLY ADDED)
        // This is your new 4th item.
        setupDashboardItem(
                R.id.item_evacuation_routes, // <-- ID from new XML
                R.drawable.ic_baseline_directions_24, // <-- New Icon (see below)
                R.color.accent_green, // Or any color you like
                "Evacuation Routes", // <-- New Title
                "Find an AI-powered safe route if in danger.", // <-- New Subtitle
                EvacuationRouteActivity.class // This opens the new AI-powered activity
        );
    }

    /**
     * Helper method to configure each dashboard item (icon, text, and click listener).
     * This logic was already good and has been preserved.
     */
    private void setupDashboardItem(int includeId, int iconDrawable, int tintColor, String titleString, String descString, Class<?> targetActivity) {
        View itemContainer = findViewById(includeId);

        // Safety check to prevent crashes if the view isn't found
        if (itemContainer == null) return;

        // Find the specific views inside the included layout
        // NOTE: Make sure your list_item_safety.xml file uses these IDs!
        ImageView icon = itemContainer.findViewById(R.id.icon_safety_item);
        TextView title = itemContainer.findViewById(R.id.text_safety_title);
        TextView description = itemContainer.findViewById(R.id.text_safety_subtitle);

        // Set the content for the views
        if (icon != null) {
            icon.setImageResource(iconDrawable);
            icon.setColorFilter(ContextCompat.getColor(this, tintColor));
        }
        if (title != null) {
            title.setText(titleString);
        }
        if (description != null) {
            description.setText(descString);
        }

        // Set the click listener to navigate to the target activity
        itemContainer.setOnClickListener(v -> {
            Intent intent = new Intent(this, targetActivity);
            startActivity(intent);
        });
    }

    /**
     * Placeholder method to show a startup message.
     */
    private void initializeNasaDataSystem() {
        Toast.makeText(this, "System: NASA Data Modules Initialized.", Toast.LENGTH_SHORT).show();
    }


    // These two methods are required by BaseActivity. They are correct.
    @Override
    protected int getLayoutResId() {
        // This tells BaseActivity which XML layout to use for this screen.
        return R.layout.activity_flood_dashboard;
    }

    @Override
    protected int getRootViewId() {
        // This tells BaseActivity the ID of the main container in that XML file.
        return R.id.main;
    }
}