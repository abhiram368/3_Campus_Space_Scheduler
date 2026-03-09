package com.example.labmanagementui.models;

public class Space {
    public String spaceName;
    public String spaceType;
    public int capacity;
    public boolean availability;

    public Space() {}

    public Space(String spaceName, String spaceType, int capacity, boolean availability) {
        this.spaceName = spaceName;
        this.spaceType = spaceType;
        this.capacity = capacity;
        this.availability = availability;
    }
}
