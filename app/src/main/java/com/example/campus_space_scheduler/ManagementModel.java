package com.example.campus_space_scheduler;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class ManagementModel {
    private String name;
    private String identifier;
    private String roomName;
    private String capacity;
    private String role;

    public ManagementModel() {} // Required for Firebase

    // Getters for Users
    public String getName() { return name; }
    public String getIdentifier() { return identifier; }

    // Getters for Spaces
    public String getRoomName() { return roomName; }
    public String getCapacity() { return capacity; }

    // Common Getter
    public String getRole() { return role; }

    // Dynamic Helper for Table Rows
    public String getPrimaryValue(String mode) {
        return "USER".equals(mode) ? name : roomName;
    }

    public String getSecondaryValue(String mode) {
        return "USER".equals(mode) ? identifier : capacity;
    }
}