package com.example.campus_space_scheduler;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.campus_space_scheduler.databinding.ActivityScheduleEditorBinding;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

public class ScheduleEditorActivity extends AppCompatActivity {

    private ActivityScheduleEditorBinding binding;
    private String startTime = "09:00"; // Default start
    private String endTime = "10:00";   // Default end

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScheduleEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupDropdowns();
        setupTimePickers();
        updateTimeSlotDisplay(); // Set initial text

        binding.btnSaveSchedule.setOnClickListener(v -> saveSchedule());
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupDropdowns() {
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        binding.dropdownDay.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, days));
    }

    private void setupTimePickers() {
        binding.etStartTime.setOnClickListener(v -> showTimePicker("Select Start Time", true));
        binding.etEndTime.setOnClickListener(v -> showTimePicker("Select End Time", false));
    }

    private void showTimePicker(String title, boolean isStart) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(isStart ? 9 : 10)
                .setMinute(0)
                .setTitleText(title)
                .build();

        picker.show(getSupportFragmentManager(), "TIME_PICKER");

        picker.addOnPositiveButtonClickListener(v -> {
            String formattedTime = String.format("%02d:%02d", picker.getHour(), picker.getMinute());
            if (isStart) {
                startTime = formattedTime;
                binding.etStartTime.setText(startTime);
            } else {
                endTime = formattedTime;
                binding.etEndTime.setText(endTime);
            }
            updateTimeSlotDisplay();
        });
    }

    private void updateTimeSlotDisplay() {
        // Combined display for your frontend presentation
        String slot = startTime + " - " + endTime;
        binding.tvPreviewSlot.setText("Scheduled Slot: " + slot);
    }

    private void saveSchedule() {
        // When saving, you can now push the full range "9:00 - 10:00" string
        String finalSlot = startTime + " - " + endTime;
        Toast.makeText(this, "Saved: " + finalSlot, Toast.LENGTH_SHORT).show();
        finish();
    }
}