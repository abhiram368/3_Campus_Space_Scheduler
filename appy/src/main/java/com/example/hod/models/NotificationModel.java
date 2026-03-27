package com.example.hod.models;

public class NotificationModel {
    private String message;
    private long timestamp;
    private boolean read;
    private String id;

    public NotificationModel() {
        // Required for Firebase
    }

    public NotificationModel(String id, String message, long timestamp, boolean read) {
        this.id = id;
        this.message = message;
        this.timestamp = timestamp;
        this.read = read;
    }

    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public boolean isRead() { return read; }
    public String getId() { return id; }
}
