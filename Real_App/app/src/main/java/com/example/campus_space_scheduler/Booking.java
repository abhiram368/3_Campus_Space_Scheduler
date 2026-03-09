package com.example.campus_space_scheduler;

public class Booking {
    private String date;
    private String timeSlot;
    private String purpose;
    private String status;

    public Booking(String date, String timeSlot, String purpose, String status) {
        this.date = date;
        this.timeSlot = timeSlot;
        this.purpose = purpose;
        this.status = status;
    }

    public String getDate() {
        return date;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getStatus() {
        return status;
    }
}
