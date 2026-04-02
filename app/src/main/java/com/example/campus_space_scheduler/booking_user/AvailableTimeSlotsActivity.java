package com.example.campus_space_scheduler.booking_user;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.campus_space_scheduler.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AvailableTimeSlotsActivity extends AppCompatActivity {

    private String spaceId, spaceName, date, role, spaceType;
    private GridLayout gridLayoutSlots;
    private ProgressBar progressBar;
    private TextView noSlotsTextView;
    private DatabaseReference schedulesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.t_activity_available_time_slots);

        spaceId = getIntent().getStringExtra("SPACE_ID");
        spaceName = getIntent().getStringExtra("SPACE_NAME");
        date = getIntent().getStringExtra("DATE");
        role = getIntent().getStringExtra("ROLE");
        spaceType = getIntent().getStringExtra("SPACE_TYPE");

        ImageView buttonBack = findViewById(R.id.buttonBack);
        TextView spaceNameTextView = findViewById(R.id.textViewSpaceName);
        TextView dateTextView = findViewById(R.id.textViewDate);
        gridLayoutSlots = findViewById(R.id.gridLayoutSlots);
        progressBar = findViewById(R.id.progressBarSlots);
        noSlotsTextView = findViewById(R.id.textViewNoSlots);

        if (spaceNameTextView != null) spaceNameTextView.setText(spaceName);
        if (dateTextView != null) dateTextView.setText(date);

        if (buttonBack != null) {
            buttonBack.setOnClickListener(v -> finish());
        }

        schedulesRef = FirebaseDatabase.getInstance().getReference("schedules");

        fetchAvailableSlots();
    }

    private void fetchAvailableSlots() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (noSlotsTextView != null) noSlotsTextView.setVisibility(View.GONE);
        if (gridLayoutSlots != null) gridLayoutSlots.removeAllViews();

        schedulesRef.orderByChild("spaceId").equalTo(spaceId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    schedulesRef.orderByChild("spaceID").equalTo(spaceId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot2) {
                            handleScheduleSnapshot(snapshot2);
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                        }
                    });
                } else {
                    handleScheduleSnapshot(snapshot);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void handleScheduleSnapshot(DataSnapshot snapshot) {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        boolean foundSchedule = false;

        for (DataSnapshot scheduleSnapshot : snapshot.getChildren()) {
            String scheduleDate = scheduleSnapshot.child("date").getValue(String.class);
            if (date != null && date.equals(scheduleDate)) {
                foundSchedule = true;
                processSlots(scheduleSnapshot.child("slots"), scheduleSnapshot.getKey());
                break;
            }
        }

        if (!foundSchedule && noSlotsTextView != null) {
            noSlotsTextView.setVisibility(View.VISIBLE);
            noSlotsTextView.setText("No schedule found for " + date);
        }
    }

    private void processSlots(DataSnapshot slotsSnapshot, String scheduleId) {
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String currentTimeStr = new SimpleDateFormat("HHmm", Locale.getDefault()).format(new Date());
        int currentIntTime = Integer.parseInt(currentTimeStr);

        boolean hasAvailableSlots = false;

        for (DataSnapshot slotSnapshot : slotsSnapshot.getChildren()) {
            String status = slotSnapshot.child("status").getValue(String.class);
            String startStr = slotSnapshot.child("start").getValue(String.class);
            String endStr = slotSnapshot.child("end").getValue(String.class);

            if (status != null && status.equalsIgnoreCase("AVAILABLE") && startStr != null && endStr != null) {
                int startTime = Integer.parseInt(startStr.replace(":", ""));

                if (date != null && date.equals(todayDate)) {
                    if (startTime >= currentIntTime) {
                        addSlotButton(startStr, endStr, scheduleId);
                        hasAvailableSlots = true;
                    }
                } else {
                    addSlotButton(startStr, endStr, scheduleId);
                    hasAvailableSlots = true;
                }
            }
        }

        if (!hasAvailableSlots && noSlotsTextView != null) {
            noSlotsTextView.setVisibility(View.VISIBLE);
            noSlotsTextView.setText("No available slots for the selected time.");
        }
    }

    private void addSlotButton(String start, String end, String scheduleId) {
        String timeLabel = start + " - " + end;
        String slotStartValue = start.replace(":", "");

        MaterialButton slotButton = new MaterialButton(this);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(8, 8, 8, 8);
        slotButton.setLayoutParams(params);
        
        slotButton.setText(timeLabel);
        slotButton.setAllCaps(false);
        slotButton.setCornerRadius(12 * (int) getResources().getDisplayMetrics().density);
        slotButton.setTextSize(14);
        slotButton.setPadding(0, 32, 0, 32);

        // Blue background and White words as requested
        int blueColor = ContextCompat.getColor(this, R.color.primary);
        slotButton.setBackgroundTintList(ColorStateList.valueOf(blueColor));
        slotButton.setTextColor(Color.WHITE);

        slotButton.setOnClickListener(v -> {
            Intent intent = new Intent(AvailableTimeSlotsActivity.this, BookingFormActivity.class);
            intent.putExtra("SPACE_ID", spaceId);
            intent.putExtra("SPACE_NAME", spaceName);
            intent.putExtra("DATE", date);
            intent.putExtra("TIME_SLOT", timeLabel);
            intent.putExtra("SLOT_START", slotStartValue);
            intent.putExtra("ROLE", role);
            intent.putExtra("SPACE_TYPE", spaceType);
            intent.putExtra("SCHEDULE_ID", scheduleId);
            startActivity(intent);
        });

        if (gridLayoutSlots != null) {
            gridLayoutSlots.addView(slotButton);
        }
    }
}
