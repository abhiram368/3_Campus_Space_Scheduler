package com.example.campus_space_scheduler;

import com.google.firebase.database.FirebaseDatabase;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class LogHelper {

    public static void log(String action, String details) {

        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

        HashMap<String,Object> log = new HashMap<>();
        log.put("time", time);
        log.put("action", action);
        log.put("details", details);

        FirebaseDatabase.getInstance()
                .getReference("logs")
                .push()
                .setValue(log);
    }
}