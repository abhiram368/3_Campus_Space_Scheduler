package com.example.labmanagementui.models;

public class User {
    public String uid;
    public String name;
    public String email;
    public String role; // staff, faculty, hod
    public String department;

    public User() {}

    public User(String uid, String name, String email, String role, String department) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.department = department;
    }
}
