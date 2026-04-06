package com.example.hod.models;

/**
 * Matches the Firebase Realtime Database users/{uid} schema exactly.
 */
public class User {

    public String uid;
    public String name;
    public String emailId;
    public String phoneNumber;
    public String role;
    public String rollNo;
    public String inchargeToSpace;
    public String department;
    public boolean passwordChanged;
    public Boolean blocked;
    public String blockReason;
    public String blockedBy;

    public User() {
        super();
    }

    public User(String uid, String name, String emailId, String role) {
        super();
        this.uid     = uid;
        this.name    = name;
        this.emailId = emailId;
        this.role    = role;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    
    public String getUserId() { return uid; }
    public void setUserId(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return emailId; }
    public String getEmailId() { return emailId; }
    public void setEmail(String emailId) { this.emailId = emailId; }

    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getBlockReason() { return blockReason; }
    public void setBlockReason(String blockReason) { this.blockReason = blockReason; }

    public String getBlockedBy() { return blockedBy; }
    public void setBlockedBy(String blockedBy) { this.blockedBy = blockedBy; }

    /** Returns a display label for the role. */
    public String getRoleLabel() {
        if (role == null || role.equalsIgnoreCase("user") || role.equalsIgnoreCase("student")) return "Student";
        switch (role.toLowerCase()) {
            case "staff":
            case "staffincharge": return "Staff Incharge";
            case "faculty":
            case "facultyincharge": return "Faculty Incharge";
            case "hod": return "Head of Department";
            case "labadmin": return "Lab Admin";
            default: return role;
        }
    }
}
