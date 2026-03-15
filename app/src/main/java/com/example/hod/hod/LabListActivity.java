package com.example.hod.hod;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;
import com.google.android.material.card.MaterialCardView;

public class LabListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab_list);

        MaterialCardView btnSSL = findViewById(R.id.btnSSL);
        MaterialCardView btnNSL = findViewById(R.id.btnNSL);

        btnSSL.setOnClickListener(v -> {
            Intent intent = new Intent(LabListActivity.this, LabScheduleActivity.class);
            intent.putExtra("labName", "SSL");
            startActivity(intent);
        });

        btnNSL.setOnClickListener(v -> {
            Intent intent = new Intent(LabListActivity.this, LabScheduleActivity.class);
            intent.putExtra("labName", "NSL");
            startActivity(intent);
        });
    }
}