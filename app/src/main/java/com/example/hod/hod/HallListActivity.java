package com.example.hod.hod;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;
import com.google.android.material.card.MaterialCardView;

public class HallListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hall_list);

        MaterialCardView hall1 = findViewById(R.id.hall1);
        MaterialCardView hall2 = findViewById(R.id.hall2);

        hall1.setOnClickListener(v -> {
            Intent intent = new Intent(HallListActivity.this, HallScheduleActivity.class);
            intent.putExtra("hallName", "Main Auditorium");
            startActivity(intent);
        });

        hall2.setOnClickListener(v -> {
            Intent intent = new Intent(HallListActivity.this, HallScheduleActivity.class);
            intent.putExtra("hallName", "Seminar Hall");
            startActivity(intent);
        });
    }
}