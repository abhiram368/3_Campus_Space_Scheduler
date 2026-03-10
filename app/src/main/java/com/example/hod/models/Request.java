package com.example.hod.models;

public class Request {
    private String username;
    private String reason;
    private String labName;
    private String slot;
    private String status;

    public Request() {
        // Default constructor for Firebase
    }

    public Request(String username, String reason, String labName, String slot, String status) {
        this.username = username;
        this.reason = reason;
        this.labName = labName;
        this.slot = slot;
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public String getReason() {
        return reason;
    }

    public String getLabName() {
        return labName;
    }

    public String getSlot() {
        return slot;
    }

    public String getStatus() {
        return status;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setLabName(String labName) {
        this.labName = labName;
    }

    public void setSlot(String slot) {
        this.slot = slot;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
