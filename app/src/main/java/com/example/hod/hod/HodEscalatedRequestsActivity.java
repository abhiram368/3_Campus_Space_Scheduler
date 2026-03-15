package com.example.hod.hod;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;
import com.google.android.material.card.MaterialCardView;

public class HodEscalatedRequestsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hod_escalated_requests);

        MaterialCardView req1 = findViewById(R.id.escalatedRequest1);
        MaterialCardView req2 = findViewById(R.id.escalatedRequest2);

        req1.setOnClickListener(v ->
                startActivity(new Intent(this, HodRequestDetailActivity.class)));

        req2.setOnClickListener(v ->
                startActivity(new Intent(this, HodRequestDetailActivity.class)));

    }
}