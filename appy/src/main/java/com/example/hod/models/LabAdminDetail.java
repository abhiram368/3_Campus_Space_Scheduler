package com.example.hod.models;

import java.io.Serializable;

public class LabAdminDetail implements Serializable {
    public String labAdminId; // The push() key
    public String uid;        // From users node
    public String name;
    public String labName;
    public String spaceId;
    public String phoneNumber;
    public String emailId;
    public String rollNo;

    public LabAdminDetail() {
        // Required for Firebase
    }

    public LabAdminDetail(String labAdminId, String uid, String name, String labName, String spaceId, String phoneNumber, String emailId, String rollNo) {
        this.labAdminId = labAdminId;
        this.uid = uid;
        this.name = name;
        this.labName = labName;
        this.spaceId = spaceId;
        this.phoneNumber = phoneNumber;
        this.emailId = emailId;
        this.rollNo = rollNo;
    }
}
