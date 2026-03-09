package com.example.labmanagementui;

import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class ViewScheduleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_schedule);

        CalendarView calendarView = findViewById(R.id.calendarView);
        TextView tvSelectedDate = findViewById(R.id.tvSelectedDate);
        TextView tvSlot1 = findViewById(R.id.tvSlot1);
        TextView tvSlot2 = findViewById(R.id.tvSlot2);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String date = dayOfMonth + "/" + (month + 1) + "/" + year;
            tvSelectedDate.setText("Schedule for: " + date);
            
            // Example logic for slot display
            updateSlot(tvSlot1, true);
            updateSlot(tvSlot2, false);
        });
    }

    private void updateSlot(TextView textView, boolean isBooked) {
        if (isBooked) {
            textView.setText("10:00 - 11:00 : Booked");
            textView.setTextColor(ContextCompat.getColor(this, R.color.status_booked));
        } else {
            textView.setText("11:00 - 12:00 : Available");
            textView.setTextColor(ContextCompat.getColor(this, R.color.status_available));
        }
    }
}
