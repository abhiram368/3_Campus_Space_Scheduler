package com.example.campus_space_scheduler.model;

public class BookingModel {

    private String approvedBy;
    private String bookedBy;
    private BookedTime bookedTime;
    private String bookingId;
    private boolean hodApproval;
    private String lorUpload;
    private String purpose;
    private String remark;
    private String scheduleId;
    private String slotStart;
    private String spaceName;
    private String status;

    public BookingModel() {}

    public String getApprovedBy() { return approvedBy; }
    public String getBookedBy() { return bookedBy; }
    public BookedTime getBookedTime() { return bookedTime; }
    public String getBookingId() { return bookingId; }
    public boolean isHodApproval() { return hodApproval; }
    public String getLorUpload() { return lorUpload; }
    public String getPurpose() { return purpose; }
    public String getRemark() { return remark; }
    public String getScheduleId() { return scheduleId; }
    public String getSlotStart() { return slotStart; }
    public String getSpaceName() { return spaceName; }
    public String getStatus() { return status; }

    public static class BookedTime {
        private String date;
        private String time;

        public BookedTime() {}

        public String getDate() { return date; }
        public String getTime() { return time; }
    }
}