package com.example.hod.hod;

import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.hod.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LabScheduleActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private LinearLayout slotsLayout;
    private String spaceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab_schedule);

        TextView title = findViewById(R.id.tvLabTitle);
        TextView dateText = findViewById(R.id.tvSelectedDate);
        CalendarView calendarView = findViewById(R.id.calendarView);
        slotsLayout = findViewById(R.id.slotsLayout); // Make sure you have a LinearLayout with this ID in your XML

        spaceId = getIntent().getStringExtra("labName");
        title.setText(spaceId);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String selectedDate = year + "-" + String.format(Locale.getDefault(), "%02d", month + 1) + "-" + String.format(Locale.getDefault(), "%02d", dayOfMonth);
            dateText.setText("Schedule for: " + selectedDate);
            fetchSchedule(selectedDate);
        });

        // Fetch schedule for today's date by default
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = sdf.format(new Date());
        fetchSchedule(currentDate);
        dateText.setText("Schedule for: " + currentDate);

    }

    private void fetchSchedule(String date) {
        // This is a simplified example. You'll likely need to query based on spaceId AND date.
        // As per your screenshot, the key is something like: -Ol1cbQAGbNgCKlDPia8_2026-03-15
        // You'll need a way to find the schedule ID for a given spaceId.
        // For now, I'll assume a direct mapping for demonstration.

        // Let's find the schedule ID for the given spaceId
        mDatabase.child("spaces").orderByChild("roomName").equalTo(spaceId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for (DataSnapshot spaceSnapshot : dataSnapshot.getChildren()) {
                        String scheduleKey = spaceSnapshot.getKey() + "_" + date;

                        mDatabase.child("schedules").child(scheduleKey).child("slots").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                slotsLayout.removeAllViews();
                                if (dataSnapshot.exists()) {
                                    for (DataSnapshot slotSnapshot : dataSnapshot.getChildren()) {
                                        String slotTime = slotSnapshot.getKey();
                                        Boolean isBooked = slotSnapshot.getValue(Boolean.class);

                                        TextView slotView = new TextView(LabScheduleActivity.this);
                                        slotView.setText(slotTime + ": " + (isBooked != null && isBooked ? "Booked" : "Available"));
                                        slotView.setTextSize(16);
                                        slotView.setPadding(0, 8, 0, 8);

                                        if(isBooked != null && isBooked){
                                            slotView.setTextColor(ContextCompat.getColor(LabScheduleActivity.this, R.color.status_booked));
                                        } else {
                                            slotView.setTextColor(ContextCompat.getColor(LabScheduleActivity.this, R.color.status_available));
                                        }

                                        slotsLayout.addView(slotView);
                                    }
                                } else {
                                    TextView noSlotsView = new TextView(LabScheduleActivity.this);
                                    noSlotsView.setText("No schedule found for this date.");
                                    slotsLayout.addView(noSlotsView);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Toast.makeText(LabScheduleActivity.this, "Failed to load schedule.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(LabScheduleActivity.this, "Failed to find space.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
