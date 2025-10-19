package com.example.floodalert;

public class EmergencyContact {
    private final String name;
    private final String number;
    private final int iconResId; // To hold a drawable resource like R.drawable.ic_police

    public EmergencyContact(String name, String number, int iconResId) {
        this.name = name;
        this.number = number;
        this.iconResId = iconResId;
    }

    public String getName() {
        return name;
    }

    public String getNumber() {
        return number;
    }

    public int getIconResId() {
        return iconResId;
    }
}