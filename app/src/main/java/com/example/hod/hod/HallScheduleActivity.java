package com.example.hod.hod;

import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.hod.R;
import com.example.hod.repository.FirebaseRepository;
import com.example.hod.utils.Result;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class HallScheduleActivity extends AppCompatActivity {

    private LinearLayout slotsLayout;
    private String spaceId;
    private FirebaseRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hall_schedule);

        TextView title = findViewById(R.id.tvHallTitle);
        TextView dateText = findViewById(R.id.tvSelectedDate);
        CalendarView calendarView = findViewById(R.id.calendarView);
        slotsLayout = findViewById(R.id.slotsLayout);

        spaceId = getIntent().getStringExtra("hallName");
        title.setText(spaceId);

        repository = new FirebaseRepository();

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String selectedDate = year + "-" + String.format(Locale.getDefault(), "%02d", month + 1) + "-" + String.format(Locale.getDefault(), "%02d", dayOfMonth);
            dateText.setText("Schedule for: " + selectedDate);
            fetchSchedule(selectedDate);
        });

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = sdf.format(new Date());
        fetchSchedule(currentDate);
        dateText.setText("Schedule for: " + currentDate);
    }

    private void fetchSchedule(String date) {
        repository.getSlotsForHallAndDate(spaceId, date, result -> runOnUiThread(() -> {
            slotsLayout.removeAllViews();

            if (result instanceof Result.Success) {
                Map<String, Boolean> slots = ((Result.Success<Map<String, Boolean>>) result).data;
                if (slots == null || slots.isEmpty()) {
                    TextView noSlotsView = new TextView(HallScheduleActivity.this);
                    noSlotsView.setText("No schedule found for this date.");
                    slotsLayout.addView(noSlotsView);
                    return;
                }

                for (Map.Entry<String, Boolean> entry : slots.entrySet()) {
                    String slotTime = entry.getKey();
                    boolean isBooked = entry.getValue() != null && entry.getValue();

                    TextView slotView = new TextView(HallScheduleActivity.this);
                    slotView.setText(slotTime + ": " + (isBooked ? "Booked" : "Available"));
                    slotView.setTextSize(16);
                    slotView.setPadding(0, 8, 0, 8);

                    if (isBooked) {
                        slotView.setTextColor(ContextCompat.getColor(HallScheduleActivity.this, R.color.status_booked));
                    } else {
                        slotView.setTextColor(ContextCompat.getColor(HallScheduleActivity.this, R.color.status_available));
                    }

                    slotsLayout.addView(slotView);
                }
            } else if (result instanceof Result.Error) {
                Toast.makeText(HallScheduleActivity.this, "Failed to load schedule.", Toast.LENGTH_SHORT).show();
            }
        }));
    }
}

