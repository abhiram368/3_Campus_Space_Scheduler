package com.example.labmanagementui;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.LinearLayout;

public class LiveStatusActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_status);

        TextView tvStatus = findViewById(R.id.tvStatus);
        TextView tvCurrentSlot = findViewById(R.id.tvCurrentSlot);
        LinearLayout bookedDetailsLayout = findViewById(R.id.bookedDetailsLayout);

        // Dummy logic (UI only)
        boolean isOccupied = false;
        boolean isBooked = true;

        if (isOccupied || isBooked) {
            tvStatus.setText(R.string.status_occupied);
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            tvCurrentSlot.setText(R.string.current_slot_time);
            bookedDetailsLayout.setVisibility(View.VISIBLE);
        } else {
            tvStatus.setText(R.string.status_available);
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            tvCurrentSlot.setText(R.string.current_slot_none);
            bookedDetailsLayout.setVisibility(View.GONE);
        }
    }
}
