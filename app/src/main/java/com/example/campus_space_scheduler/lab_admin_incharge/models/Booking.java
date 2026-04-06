package com.example.campus_space_scheduler.lab_admin_incharge.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Booking {
    private String approvedBy;
    private String bookedBy;
    private String bookedTime;
    private String bookingId;
    private String date;
    private String description;
    private Object facultyInchargeApproval;
    private Object hodApproval;
    private String lorUpload;
    private String purpose;
    private String scheduleId;
    private String slotStart;
    private String spaceName;
    private String status;
    private String timeSlot;

    public Booking() {
        // Default constructor required for calls to DataSnapshot.getValue(Booking.class)
    }

    // Getters and Setters
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public String getBookedBy() { return bookedBy; }
    public void setBookedBy(String bookedBy) { this.bookedBy = bookedBy; }

    public String getBookedTime() { return bookedTime; }
    public void setBookedTime(String bookedTime) { this.bookedTime = bookedTime; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Object getFacultyInchargeApproval() { return facultyInchargeApproval; }
    public void setFacultyInchargeApproval(Object facultyInchargeApproval) { this.facultyInchargeApproval = facultyInchargeApproval; }

    public Object getHodApproval() { return hodApproval; }
    public void setHodApproval(Object hodApproval) { this.hodApproval = hodApproval; }

    public String getLorUpload() { return lorUpload; }
    public void setLorUpload(String lorUpload) { this.lorUpload = lorUpload; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getScheduleId() { return scheduleId; }
    public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }

    public String getSlotStart() { return slotStart; }
    public void setSlotStart(String slotStart) { this.slotStart = slotStart; }

    public String getSpaceName() { return spaceName; }
    public void setSpaceName(String spaceName) { this.spaceName = spaceName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
}