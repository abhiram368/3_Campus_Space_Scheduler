package com.example.campus_space_scheduler;

public class ManagementModel {

    private String name;
    private String emailId;
    private Object phoneNumber;
    private Object rollNo;
    private String role;
    private String uid;

    private String roomName;
    private Object capacity;

    public ManagementModel() {}

    public String getName() {
        return name == null ? "" : name;
    }

    public String getEmailId() {
        return emailId == null ? "" : emailId;
    }

    public String getPhoneNumber() {
        return phoneNumber == null ? "" : phoneNumber.toString();
    }

    public String getRollNo() {
        return rollNo == null ? "" : rollNo.toString();
    }

    public String getRole() {
        return role == null ? "" : role;
    }

    public String getUid() {
        return uid == null ? "" : uid;
    }

    public String getRoomName() {
        return roomName == null ? "" : roomName;
    }

    public String getCapacity() {
        return capacity == null ? "" : capacity.toString();
    }

    public String getPrimaryValue(String mode) {
        if ("USER".equals(mode)) return getName();
        return getRoomName();
    }

    public String getSecondaryValue(String mode) {
        if ("USER".equals(mode)) return getEmailId();
        return getCapacity();
    }
}