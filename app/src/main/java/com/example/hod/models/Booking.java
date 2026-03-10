package com.example.hod.models;

import com.google.firebase.database.PropertyName;
import java.io.Serializable;

public class Booking implements Serializable {
    private String bookingId;
    private String Date; // Matching capitalized key in Firebase screenshot
    private boolean facultyInchargeApproval;
    private boolean hodApproval;
    private String purpose;
    private String remark;
    private String scheduleId;
    private String slotId;
    private String spaceId;
    private boolean staffApproval;
    private String status;
    private long timestamp;
    private String userId;

    public Booking() {}

    @PropertyName("bookingId")
    public String getBookingId() { return bookingId; }
    @PropertyName("bookingId")
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    @PropertyName("Date")
    public String getDate() { return Date; }
    @PropertyName("Date")
    public void setDate(String Date) { this.Date = Date; }

    public boolean isFacultyInchargeApproval() { return facultyInchargeApproval; }
    public void setFacultyInchargeApproval(boolean facultyInchargeApproval) { this.facultyInchargeApproval = facultyInchargeApproval; }

    public boolean isHodApproval() { return hodApproval; }
    public void setHodApproval(boolean hodApproval) { this.hodApproval = hodApproval; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getScheduleId() { return scheduleId; }
    public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }

    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }

    public boolean isStaffApproval() { return staffApproval; }
    public void setStaffApproval(boolean staffApproval) { this.staffApproval = staffApproval; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
