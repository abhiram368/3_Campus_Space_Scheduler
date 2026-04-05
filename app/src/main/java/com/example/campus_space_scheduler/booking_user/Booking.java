package com.example.campus_space_scheduler.booking_user;

import java.util.Map;

public class Booking {
    private String bookingId;
    private String bookedBy;
    private Map<String, String> bookedTime;
    private String purpose;
    private String description;
    private String lorUpload;
    private String lorDownload;
    private String scheduleId;
    private String status;
    private String spaceName;
    private String spaceType; // New field
    private String date;
    private String timeSlot;
    private String remark;
    private String actionBy;
    private String slotStart; // Format: 1030 for 10:30 AM
    private String approvedBy; // stores UID of authority

    public Booking() {
        // Required for Firebase
    }

    public Booking(String date, String timeSlot, String purpose, String status) {
        this.date = date;
        this.timeSlot = timeSlot;
        this.purpose = purpose;
        this.status = status;
    }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getBookedBy() { return bookedBy; }
    public void setBookedBy(String bookedBy) { this.bookedBy = bookedBy; }

    public Map<String, String> getBookedTime() { return bookedTime; }
    public void setBookedTime(Map<String, String> bookedTime) { this.bookedTime = bookedTime; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLorUpload() { return lorUpload; }
    public void setLorUpload(String lorUpload) { this.lorUpload = lorUpload; }

    public String getLorDownload() { return lorDownload; }
    public void setLorDownload(String lorDownload) { this.lorDownload = lorDownload; }

    public String getScheduleId() { return scheduleId; }
    public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSpaceName() { return spaceName; }
    public void setSpaceName(String spaceName) { this.spaceName = spaceName; }

    public String getSpaceType() { return spaceType; }
    public void setSpaceType(String spaceType) { this.spaceType = spaceType; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getActionBy() { return actionBy; }
    public void setActionBy(String actionBy) { this.actionBy = actionBy; }

    public String getSlotStart() { return slotStart; }
    public void setSlotStart(String slotStart) { this.slotStart = slotStart; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
}
