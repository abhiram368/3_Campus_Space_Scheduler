package com.example.campus_space_scheduler;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Data model for a single hour block in the 24-hour scheduling grid.
 */
@IgnoreExtraProperties
public class HourSlot {
    private int hour;
    private boolean open; // Defaulting to 'open' to match the boolean state logic

    // Required empty constructor for Firebase data mapping
    public HourSlot() {
    }

    public HourSlot(int hour, boolean open) {
        this.hour = hour;
        this.open = open;
    }

    // Getters and Setters
    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    // Using 'isOpen' naming convention for better readability in the Adapter
    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }
}