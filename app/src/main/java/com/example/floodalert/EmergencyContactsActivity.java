package com.example.floodalert;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EmergencyContactsActivity extends AppCompatActivity {

    private EmergencyContactsViewModel viewModel;
    private ProgressBar progressBar;
    private TextView messageTextView;
    private RecyclerView contactsRecyclerView;
    private EmergencyContactsAdapter contactsAdapter; // --- Add adapter member

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contacts);

        viewModel = new ViewModelProvider(this).get(EmergencyContactsViewModel.class);
        setupViews(); // --- Setup views
        observeUiState();
        viewModel.fetchEmergencyContacts();
    }

    private void setupViews() {
        progressBar = findViewById(R.id.progress_bar);
        messageTextView = findViewById(R.id.message_text_view);
        contactsRecyclerView = findViewById(R.id.contacts_recycler_view);

        // --- FIX: Setup RecyclerView and Adapter ---
        contactsAdapter = new EmergencyContactsAdapter();
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        contactsRecyclerView.setAdapter(contactsAdapter);
    }

    private void observeUiState() {
        viewModel.getUiState().observe(this, uiState -> {
            progressBar.setVisibility(View.GONE);
            messageTextView.setVisibility(View.GONE);
            contactsRecyclerView.setVisibility(View.GONE);

            if (uiState instanceof UiState.Loading) {
                UiState.Loading loadingState = (UiState.Loading) uiState;
                progressBar.setVisibility(View.VISIBLE);
                messageTextView.setVisibility(View.VISIBLE);
                messageTextView.setText(loadingState.message);
            } else if (uiState instanceof UiState.Success) {
                UiState.Success<?> successState = (UiState.Success<?>) uiState;
                contactsRecyclerView.setVisibility(View.VISIBLE);
                // --- FIX: Submit the list to the adapter ---
                contactsAdapter.setContacts((List<EmergencyContact>) successState.data);
            } else if (uiState instanceof UiState.Error) {
                UiState.Error errorState = (UiState.Error) uiState;
                messageTextView.setVisibility(View.VISIBLE);
                messageTextView.setText(errorState.message);
            }
        });
    }
}