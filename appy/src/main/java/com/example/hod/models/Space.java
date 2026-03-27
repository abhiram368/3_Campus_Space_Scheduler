package com.example.hod.models;

public class Space {
    private String capacity;
    private String role;
    private String roomName;
    private String computerCount;
    private String labAdminsCount;
    private String address;
    private String spaceId;

    public Space() {
        // Default constructor required for calls to DataSnapshot.getValue(Space.class)
    }

    public Space(String capacity, String role, String roomName) {
        this.capacity = capacity;
        this.role = role;
        this.roomName = roomName;
    }

    public Space(String capacity, String role, String roomName, String computerCount, String labAdminsCount, String address) {
        this.capacity = capacity;
        this.role = role;
        this.roomName = roomName;
        this.computerCount = computerCount;
        this.labAdminsCount = labAdminsCount;
        this.address = address;
    }

    public String getCapacity() {
        return capacity;
    }

    public void setCapacity(String capacity) {
        this.capacity = capacity;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getComputerCount() {
        return computerCount;
    }

    public void setComputerCount(String computerCount) {
        this.computerCount = computerCount;
    }

    public String getLabAdminsCount() {
        return labAdminsCount;
    }

    public void setLabAdminsCount(String labAdminsCount) {
        this.labAdminsCount = labAdminsCount;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }
}
