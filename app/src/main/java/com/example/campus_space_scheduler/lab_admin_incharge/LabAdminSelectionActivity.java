package com.example.campus_space_scheduler.lab_admin_incharge;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campus_space_scheduler.R;
import com.google.android.material.card.MaterialCardView;

public class LabAdminSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab_admin_selection);

        MaterialCardView cardSSL = findViewById(R.id.cardLabSSL);
        MaterialCardView cardNSL = findViewById(R.id.cardLabNSL);
        MaterialCardView cardBDL = findViewById(R.id.cardLabBDL);

        cardSSL.setOnClickListener(v -> openLabDashboard("SSL"));
        cardNSL.setOnClickListener(v -> openLabDashboard("NSL"));
        cardBDL.setOnClickListener(v -> openLabDashboard("BDL"));
    }

    private void openLabDashboard(String labName) {
        Intent intent = new Intent(this, LabAdminDashboardActivity.class);
        intent.putExtra("labName", labName);
        startActivity(intent);
    }
}