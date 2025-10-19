package com.example.floodalert; // Make sure this package name matches your project exactly

import android.os.Bundle;
import android.view.View;

// This is the new import you need for the fix
import androidx.core.graphics.Insets;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * This is the BaseActivity. It acts as a template for all other activities in the app.
 *
 * Its primary purpose is to hold the shared code that fixes the problem where the
 * layout is cut off by the system status bar at the top of the screen.
 *
 * Any activity that "extends" this BaseActivity will automatically get this fix.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResId());
        View rootView = findViewById(getRootViewId());

        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
                //
                // --- THIS IS THE CORRECTED CODE ---
                //
                // The getInsets() method returns an "Insets" object, not a "WindowInsetsCompat" object.
                // This line now uses the correct type.
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

                // Now we can correctly access insets.left, insets.top, etc.
                v.setPadding(insets.left, insets.top, insets.right, insets.bottom);

                return WindowInsetsCompat.CONSUMED;
            });
        }
    }

    /**
     * A contract forcing the child activity to provide its layout file.
     */
    @LayoutRes
    protected abstract int getLayoutResId();

    /**
     * A contract forcing the child activity to provide the ID of its root layout.
     */
    protected abstract int getRootViewId();
}