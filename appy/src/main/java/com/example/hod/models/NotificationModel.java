package com.example.hod.models;

public class NotificationModel {
    private String id;
    private String message;
    private long timestamp;
    private boolean read;
    private String type;
    private String relatedBookingId;
    private String targetStatus;
    private String bookedBy;
    private String roleTarget;

    public NotificationModel() {
        // Required for Firebase
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRelatedBookingId() { return relatedBookingId; }
    public void setRelatedBookingId(String relatedBookingId) { this.relatedBookingId = relatedBookingId; }

    public String getTargetStatus() { return targetStatus; }
    public void setTargetStatus(String targetStatus) { this.targetStatus = targetStatus; }

    public String getBookedBy() { return bookedBy; }
    public void setBookedBy(String bookedBy) { this.bookedBy = bookedBy; }

    public String getRoleTarget() { return roleTarget; }
    public void setRoleTarget(String roleTarget) { this.roleTarget = roleTarget; }
}
