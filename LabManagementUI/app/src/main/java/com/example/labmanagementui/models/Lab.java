package com.example.labmanagementui.models;

public class Lab {
    public String name;
    public String id;
    public String capacity;
    public String systems;
    public String location;
    public String admin;

    public Lab() {
        // Default constructor required for calls to DataSnapshot.getValue(Lab.class)
    }

    public Lab(String name, String id, String capacity, String systems, String location, String admin) {
        this.name = name;
        this.id = id;
        this.capacity = capacity;
        this.systems = systems;
        this.location = location;
        this.admin = admin;
    }
}
