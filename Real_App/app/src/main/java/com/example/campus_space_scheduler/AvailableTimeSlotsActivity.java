package com.example.campus_space_scheduler;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AvailableTimeSlotsActivity extends AppCompatActivity {

    private static final String TAG = "AvailableTimeSlots";
    private String spaceId, spaceName, date, role, spaceType;
    private LinearLayout slotsContainer;
    private ProgressBar progressBar;
    private TextView noSlotsTextView;
    private DatabaseReference schedulesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_time_slots);

        spaceId = getIntent().getStringExtra("SPACE_ID");
        spaceName = getIntent().getStringExtra("SPACE_NAME");
        date = getIntent().getStringExtra("DATE");
        role = getIntent().getStringExtra("ROLE");
        spaceType = getIntent().getStringExtra("SPACE_TYPE");

        TextView spaceNameTextView = findViewById(R.id.textViewSpaceName);
        TextView dateTextView = findViewById(R.id.textViewDate);
        slotsContainer = findViewById(R.id.linearLayoutSlotsContainer);
        progressBar = findViewById(R.id.progressBarSlots);
        noSlotsTextView = findViewById(R.id.textViewNoSlots);

        spaceNameTextView.setText(spaceName);
        dateTextView.setText(date);

        schedulesRef = FirebaseDatabase.getInstance().getReference("schedules");

        fetchAvailableSlots();
    }

    private void fetchAvailableSlots() {
        progressBar.setVisibility(View.VISIBLE);
        noSlotsTextView.setVisibility(View.GONE);
        slotsContainer.removeAllViews();

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
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                } else {
                    handleScheduleSnapshot(snapshot);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void handleScheduleSnapshot(DataSnapshot snapshot) {
        progressBar.setVisibility(View.GONE);
        boolean foundSchedule = false;

        for (DataSnapshot scheduleSnapshot : snapshot.getChildren()) {
            String scheduleDate = scheduleSnapshot.child("date").getValue(String.class);
            if (date.equals(scheduleDate)) {
                foundSchedule = true;
                processSlots(scheduleSnapshot.child("slots"), scheduleSnapshot.getKey());
                break;
            }
        }

        if (!foundSchedule) {
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
                
                if (date.equals(todayDate)) {
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

        if (!hasAvailableSlots) {
            noSlotsTextView.setVisibility(View.VISIBLE);
            noSlotsTextView.setText("No available slots for the selected time.");
        }
    }

    private void addSlotButton(String start, String end, String scheduleId) {
        String timeLabel = start + " - " + end;
        
        MaterialButton slotButton = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonStyle);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 12, 0, 12);
        slotButton.setLayoutParams(params);
        slotButton.setText(timeLabel);
        slotButton.setAllCaps(false);
        slotButton.setCornerRadius(24);
        slotButton.setBackgroundColor(getResources().getColor(R.color.surface_dark));
        slotButton.setStrokeColorResource(R.color.primary_blue);
        slotButton.setStrokeWidth(2);
        slotButton.setTextColor(getResources().getColor(R.color.text_primary));
        
        slotButton.setOnClickListener(v -> {
            Intent intent = new Intent(AvailableTimeSlotsActivity.this, BookingFormActivity.class);
            intent.putExtra("SPACE_ID", spaceId);
            intent.putExtra("SPACE_NAME", spaceName);
            intent.putExtra("DATE", date);
            intent.putExtra("TIME_SLOT", timeLabel);
            intent.putExtra("ROLE", role);
            intent.putExtra("SPACE_TYPE", spaceType);
            intent.putExtra("SCHEDULE_ID", scheduleId);
            startActivity(intent);
        });

        slotsContainer.addView(slotButton);
    }
}
