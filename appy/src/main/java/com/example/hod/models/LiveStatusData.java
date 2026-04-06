package com.example.hod.models;

import java.io.Serializable;

public class LiveStatusData implements Serializable {
    public String status;      // AVAILABLE, BOOKED, BLOCKED, etc.
    public Booking booking;    // Populate if status is BOOKED
    public String slotKey;     // The matched slot range, e.g. "09:00 - 10:00"
    public String date;
    public String spaceId;
    public String spaceName;
    public String startTime;  // Added for better formatting
    public String endTime;    // Added for better formatting
    public int startTimeMinutes; // For sorting and comparison

    public LiveStatusData() {}

    public LiveStatusData(String status, Booking booking, String slotKey, String date) {
        this.status = status;
        this.booking = booking;
        this.slotKey = slotKey;
        this.date = date;
    }
}
