package com.example.campus_space_scheduler.lab_admin_incharge.models;

public class NotificationModel {
    private String id;
    private String title;
    private String message;
    private long timestamp;
    private boolean read;
    private String type;
    private String bookingId;

    public NotificationModel() {}

    public NotificationModel(String id, String title, String message, long timestamp, String type, String bookingId) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.read = false;
        this.type = type;
        this.bookingId = bookingId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
}
