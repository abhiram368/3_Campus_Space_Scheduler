package com.example.hod.models;

public class LabDetail {
    private String computerCount;
    private String studentCapacity;
    private String labAdminsCount;
    private String address;

    public LabDetail() {
        // Required for Firebase
    }

    public LabDetail(String computerCount, String studentCapacity, String labAdminsCount, String address) {
        this.computerCount = computerCount;
        this.studentCapacity = studentCapacity;
        this.labAdminsCount = labAdminsCount;
        this.address = address;
    }

    public String getComputerCount() {
        return computerCount;
    }

    public void setComputerCount(String computerCount) {
        this.computerCount = computerCount;
    }

    public String getStudentCapacity() {
        return studentCapacity;
    }

    public void setStudentCapacity(String studentCapacity) {
        this.studentCapacity = studentCapacity;
    }

    public String getLabAdminsCount() {
        return labAdminsCount;
    }

    public void setLabAdminsCount(String labAdminsCount) {
        this.labAdminsCount = labAdminsCount;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
