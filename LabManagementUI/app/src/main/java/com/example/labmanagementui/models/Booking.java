package com.example.labmanagementui.models;

import com.google.firebase.firestore.DocumentId;

public class Booking {
    @DocumentId
    public String id;
    public String spaceName;
    public String spaceType;
    public String requestedBy;
    public String status; // pending, staff_approved, faculty_approved, approved
    public boolean staffApproval;
    public boolean facultyApproval;
    public boolean hodApproval;
    public long timestamp;

    public Booking() {}

    public Booking(String spaceName, String spaceType, String requestedBy) {
        this.spaceName = spaceName;
        this.spaceType = spaceType;
        this.requestedBy = requestedBy;
        this.status = "pending";
        this.staffApproval = false;
        this.facultyApproval = false;
        this.hodApproval = false;
        this.timestamp = System.currentTimeMillis();
    }
}
