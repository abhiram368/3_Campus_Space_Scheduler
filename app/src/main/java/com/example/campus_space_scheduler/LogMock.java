package com.example.campus_space_scheduler;

public class LogMock {
    public String time;
    public String action;
    public String details;
    public String color; // To show different colors for errors vs info

    public LogMock(String time, String action, String details, String color) {
        this.time = time;
        this.action = action;
        this.details = details;
        this.color = color;
    }
}