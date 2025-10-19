package com.example.floodalert;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AddressAutocompleteAdapter extends ArrayAdapter<String> implements Filterable {

    private List<String> suggestions = new ArrayList<>();
    private final Geocoder geocoder;

    public AddressAutocompleteAdapter(@NonNull Context context, int resource) {
        super(context, resource);
        geocoder = new Geocoder(context, Locale.getDefault());
    }

    @Override
    public int getCount() {
        return suggestions.size();
    }

    @Nullable
    @Override
    public String getItem(int position) {
        return suggestions.get(position);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null) {
                    try {
                        // Use Geocoder to get address suggestions from the user's input
                        List<Address> addresses = geocoder.getFromLocationName(constraint.toString(), 5);
                        suggestions.clear();
                        if (addresses != null) {
                            for (Address address : addresses) {
                                // Format the address into a readable string
                                suggestions.add(address.getAddressLine(0));
                            }
                        }
                    } catch (IOException e) {
                        // Handle geocoder exceptions
                    }
                    filterResults.values = suggestions;
                    filterResults.count = suggestions.size();
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, @NonNull FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
    }
}