package com.example.floodalert;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button getStartedButton;
    private ImageView iconTsunami;
    private TextView appTitle;
    private TextView appSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Link UI elements
        getStartedButton = findViewById(R.id.get_started_button);
        iconTsunami = findViewById(R.id.icon_tsunami);
        appTitle = findViewById(R.id.app_title);
        appSubtitle = findViewById(R.id.app_subtitle);

        // Set the click listener on the "Get Started" button
        getStartedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Navigate to the next screen (Permissions Request)
                Intent intent = new Intent(MainActivity.this, PermissionsRequestActivity.class);
                startActivity(intent);
            }
        });
    }
}