package com.example.hod.models;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Matches the Firebase Realtime Database bookings/{bookingId} schema exactly.
 */
public class Booking implements Serializable {

    private String bookingId;
    private String bookedBy;

    /** Nested object: {date: "...", time: "..."} */
    private Map<String, String> bookedTime;

    private String date;
    private String description;
    private boolean facultyInchargeApproval;
    private boolean hodApproval;
    private String lorUpload;
    private String purpose;
    private String scheduleId;
    private String slotStart;
    private String spaceName;
    private String status;
    private String timeSlot;

    // Extra runtime fields (not stored in DB – set by the repository after fetching)
    private String remark;       // stored in DB on some flows
    private String approvedBy;
    private String requesterName; // resolved from users/{bookedBy}/name at runtime
    private String requesterRole; // resolved from users/{bookedBy}/role at runtime
    private String decisionTime; // stored in bookings/{bookingId}/decisionTime

    public Booking() {}

    // ── Getters and Setters ──────────────────────────────────────────────────
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getBookedBy() { return bookedBy; }
    public void setBookedBy(String bookedBy) { this.bookedBy = bookedBy; }

    public Map<String, String> getBookedTime() { return bookedTime; }
    public void setBookedTime(Map<String, String> bookedTime) { this.bookedTime = bookedTime; }

    public String getBookedTimeDisplay() {
        if (bookedTime == null) return null;
        String d = bookedTime.get("date");
        String t = bookedTime.get("time");
        if (d != null && t != null) return d + " " + t;
        return d != null ? d : t;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isFacultyInchargeApproval() { return facultyInchargeApproval; }
    public void setFacultyInchargeApproval(boolean facultyInchargeApproval) {
        this.facultyInchargeApproval = facultyInchargeApproval;
    }

    public boolean isHodApproval() { return hodApproval; }
    public void setHodApproval(boolean hodApproval) { this.hodApproval = hodApproval; }

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

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public String getRequesterRole() { return requesterRole; }
    public void setRequesterRole(String requesterRole) { this.requesterRole = requesterRole; }

    public String getDecisionTime() { return decisionTime; }
    public void setDecisionTime(String decisionTime) { this.decisionTime = decisionTime; }

    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }

    // ── Business Logic ────────────────────────────────────────────────────────

    public boolean isExpired() {
        if (date == null || date.isEmpty()) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date bookingDate = sdf.parse(date);
            Date now = new Date();
            
            // Normalize today's date (strip time for pure date comparison)
            Date todayOnly = sdf.parse(sdf.format(now));
            
            if (bookingDate == null) return false;
            
            // 1. If date is in the absolute past
            if (bookingDate.before(todayOnly)) return true;
            
            // 2. If date is today, check if the specific time slot has passed
            if (bookingDate.equals(todayOnly)) {
                if (timeSlot != null && timeSlot.contains("-")) {
                    String[] parts = timeSlot.split("-");
                    if (parts.length > 1) {
                        String startTimeStr = parts[0].trim();
                        
                        // Parse current time vs start time in minutes from midnight
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        int currentMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE);
                        int startMinutes = getMinutesFromTime(startTimeStr);
                        
                        // If current clock time is past the slot START time, it's considered expired 
                        // for pending/authority approval purposes.
                        return startMinutes != -1 && currentMinutes >= startMinutes;
                    }
                }
            }
            
            return false;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Helper to convert times like "2:30 PM", "14:30", or "10:00" to minutes since midnight.
     */
    private int getMinutesFromTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return -1;
        try {
            String upper = timeStr.toUpperCase().trim();
            boolean isPM = upper.contains("PM");
            boolean isAM = upper.contains("AM");
            
            String clean = upper.replaceAll("[^0-9:]", "");
            String[] parts = clean.split(":");
            if (parts.length == 0) return -1;
            
            int hours = Integer.parseInt(parts[0]);
            int minutes = (parts.length > 1) ? Integer.parseInt(parts[1]) : 0;
            
            if (isPM && hours < 12) hours += 12;
            if (isAM && hours == 12) hours = 0;
            
            return hours * 60 + minutes;
        } catch (Exception e) {
            return -1;
        }
    }

    public boolean isStaffApproval() {
        return status != null && !status.equalsIgnoreCase("pending");
    }

    public String getDisplayTitle() {
        if (spaceName != null && !spaceName.isEmpty()) return spaceName;
        if (purpose != null && !purpose.isEmpty()) return purpose;
        return bookingId != null ? bookingId : "Booking";
    }

    public String getDisplaySubtitle() {
        StringBuilder sb = new StringBuilder();
        if (date != null) sb.append(date);
        if (timeSlot != null) {
            if (sb.length() > 0) sb.append("  •  ");
            sb.append(timeSlot);
        }
        return sb.length() > 0 ? sb.toString() : (slotStart != null ? slotStart : "—");
    }
}
