package com.example.floodalert;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmergencyContactsViewModel extends ViewModel {

    private final MutableLiveData<UiState> _uiState = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public LiveData<UiState> getUiState() {
        return _uiState;
    }

    public void fetchEmergencyContacts() {
        _uiState.setValue(new UiState.Loading("Fetching contacts..."));
        executorService.execute(() -> {
            try {
                Thread.sleep(1500); // Simulate network delay

                // --- FIX: Create a list of mock emergency contacts ---
                ArrayList<EmergencyContact> contacts = new ArrayList<>();
                contacts.add(new EmergencyContact("National Emergency", "112", R.drawable.ic_baseline_emergency_24));
                contacts.add(new EmergencyContact("Police", "100", R.drawable.ic_baseline_local_police_24));
                contacts.add(new EmergencyContact("Fire Brigade", "101", R.drawable.ic_baseline_fire_truck_24));
                contacts.add(new EmergencyContact("Ambulance", "102", R.drawable.ic_baseline_ambulance_24));
                contacts.add(new EmergencyContact("Disaster Management", "108", R.drawable.ic_baseline_house_siding_24));

                _uiState.postValue(new UiState.Success<>(contacts));
            } catch (Exception e) {
                _uiState.postValue(new UiState.Error("Failed to load contacts."));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}