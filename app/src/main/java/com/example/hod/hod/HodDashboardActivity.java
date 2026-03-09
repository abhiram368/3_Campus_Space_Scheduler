package com.example.hod.hod;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;
import com.google.android.material.card.MaterialCardView;

public class HodDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hod_dashboard);

        MaterialCardView btnEscalated = findViewById(R.id.btnEscalatedRequests);
        MaterialCardView btnLiveStatus = findViewById(R.id.btnLiveStatus);
        MaterialCardView btnViewSchedule = findViewById(R.id.btnViewSchedule);
        MaterialCardView btnHistory = findViewById(R.id.btnApprovalHistory);

        btnEscalated.setOnClickListener(v ->
                startActivity(new Intent(this, HodEscalatedRequestsActivity.class)));

        btnLiveStatus.setOnClickListener(v ->
                startActivity(new Intent(this, LiveStatusActivity.class)));

        btnViewSchedule.setOnClickListener(v ->
                startActivity(new Intent(this, HodViewScheduleHomeActivity.class)));

        btnHistory.setOnClickListener(v ->
                startActivity(new Intent(this, HodApprovalHistoryActivity.class)));
    }
}