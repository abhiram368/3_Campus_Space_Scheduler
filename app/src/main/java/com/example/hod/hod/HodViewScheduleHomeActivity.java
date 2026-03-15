package com.example.hod.hod;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;
import com.google.android.material.card.MaterialCardView;

public class HodViewScheduleHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hod_view_schedule_home);

        // Labs
        MaterialCardView btnSSL = findViewById(R.id.btnSSL);
        MaterialCardView btnNSL = findViewById(R.id.btnNSL);

        // Halls
        MaterialCardView hall1 = findViewById(R.id.hall1);
        MaterialCardView hall2 = findViewById(R.id.hall2);

        btnSSL.setOnClickListener(v -> {
            Intent intent = new Intent(this, LabScheduleActivity.class);
            intent.putExtra("labName", "SSL");
            startActivity(intent);
        });

        btnNSL.setOnClickListener(v -> {
            Intent intent = new Intent(this, LabScheduleActivity.class);
            intent.putExtra("labName", "NSL");
            startActivity(intent);
        });

        hall1.setOnClickListener(v -> {
            Intent intent = new Intent(this, HallScheduleActivity.class);
            intent.putExtra("hallName", "Main Auditorium");
            startActivity(intent);
        });

        hall2.setOnClickListener(v -> {
            Intent intent = new Intent(this, HallScheduleActivity.class);
            intent.putExtra("hallName", "Seminar Hall");
            startActivity(intent);
        });
    }
}