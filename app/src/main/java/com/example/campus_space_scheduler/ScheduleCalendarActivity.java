package com.example.campus_space_scheduler;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.campus_space_scheduler.databinding.ActivityScheduleCalendarBinding;

public class ScheduleCalendarActivity extends AppCompatActivity {

    private ActivityScheduleCalendarBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScheduleCalendarBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Toolbar and Room Name from Intent
        String room = getIntent().getStringExtra("ROOM_NAME");
        binding.toolbar.setTitle("Schedule " + room);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.tvRoomName.setText("Room: " + room);

        // Date selection logic
        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String date = dayOfMonth + "-" + (month + 1) + "-" + year;
            Intent intent = new Intent(this, HourlyGridActivity.class);
            intent.putExtra("ROOM_NAME", room);
            intent.putExtra("SELECTED_DATE", date);
            startActivity(intent);
        });
    }
}