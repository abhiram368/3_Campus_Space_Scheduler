package com.example.hod.models;

import java.io.Serializable;
import java.util.Map;

/**
 * Matches the Firebase Realtime Database bookings/{bookingId} schema exactly.
 *
 * bookings/{bookingId}
 *   bookedBy                   – uid of requester
 *   bookedTime/date            – date string when booking was made
 *   bookedTime/time            – time string when booking was made
 *   bookingId                  – same as the node key
 *   date                       – date of the booking/event
 *   description
 *   facultyInchargeApproval    – boolean
 *   hodApproval                – boolean
 *   lorUpload                  – download URL
 *   purpose
 *   scheduleId
 *   slotStart                  – e.g. "09:00"
 *   spaceName                  – human-readable room name
 *   status                     – pending | approved | rejected | forwarded_to_*
 *   timeSlot                   – display string, e.g. "9:00 AM – 10:00 AM"
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
   // UID of the person who made the final decision
    private String requesterName; // resolved from users/{bookedBy}/name at runtime

    public Booking() {}

    // ── bookingId ────────────────────────────────────────────────────────────
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    // ── bookedBy ─────────────────────────────────────────────────────────────
    public String getBookedBy() { return bookedBy; }
    public void setBookedBy(String bookedBy) { this.bookedBy = bookedBy; }

    // ── bookedTime (nested) ───────────────────────────────────────────────────
    public Map<String, String> getBookedTime() { return bookedTime; }
    public void setBookedTime(Map<String, String> bookedTime) { this.bookedTime = bookedTime; }

    /** Convenience: returns "date time" string or null. */
    public String getBookedTimeDisplay() {
        if (bookedTime == null) return null;
        String d = bookedTime.get("date");
        String t = bookedTime.get("time");
        if (d != null && t != null) return d + " " + t;
        return d != null ? d : t;
    }

    // ── date ─────────────────────────────────────────────────────────────────
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    // ── description ──────────────────────────────────────────────────────────
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // ── facultyInchargeApproval ───────────────────────────────────────────────
    public boolean isFacultyInchargeApproval() { return facultyInchargeApproval; }
    public void setFacultyInchargeApproval(boolean facultyInchargeApproval) {
        this.facultyInchargeApproval = facultyInchargeApproval;
    }

    // ── hodApproval ───────────────────────────────────────────────────────────
    public boolean isHodApproval() { return hodApproval; }
    public void setHodApproval(boolean hodApproval) { this.hodApproval = hodApproval; }

    // ── lorUpload ─────────────────────────────────────────────────────────────
    public String getLorUpload() { return lorUpload; }
    public void setLorUpload(String lorUpload) { this.lorUpload = lorUpload; }

    // ── purpose ───────────────────────────────────────────────────────────────
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    // ── scheduleId ────────────────────────────────────────────────────────────
    public String getScheduleId() { return scheduleId; }
    public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }

    // ── slotStart ─────────────────────────────────────────────────────────────
    public String getSlotStart() { return slotStart; }
    public void setSlotStart(String slotStart) { this.slotStart = slotStart; }

    // ── spaceName ─────────────────────────────────────────────────────────────
    public String getSpaceName() { return spaceName; }
    public void setSpaceName(String spaceName) { this.spaceName = spaceName; }

    // ── status ────────────────────────────────────────────────────────────────
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // ── timeSlot ──────────────────────────────────────────────────────────────
    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    // ── remark (optional, written by approvers) ───────────────────────────────
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    // ── decisionBy (UID of the person who made the decision) ───────────────
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    // ── requesterName (runtime, not persisted) ────────────────────────────────
    private String requesterRole; // resolved from users/{bookedBy}/role at runtime
    private String decisionTime; // stored in bookings/{bookingId}/decisionTime

    public String getRequesterRole() { return requesterRole; }
    public void setRequesterRole(String requesterRole) { this.requesterRole = requesterRole; }

    public String getDecisionTime() { return decisionTime; }
    public void setDecisionTime(String decisionTime) { this.decisionTime = decisionTime; }

    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }

    // ── Legacy / compatibility helpers ────────────────────────────────────────

    /**
     * No "staffApproval" field in the DB schema.
     * Staff forwards to faculty; consider staff-approved when status is NOT "pending".
     */
    public boolean isStaffApproval() {
        return status != null && !status.equalsIgnoreCase("pending");
    }

    // Convenience: human-readable display name for a booking list item
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
