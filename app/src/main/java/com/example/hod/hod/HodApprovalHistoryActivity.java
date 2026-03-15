package com.example.hod.hod;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;
import com.google.android.material.card.MaterialCardView;

public class HodApprovalHistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hod_approval_history);

        MaterialCardView completedRequest1 = findViewById(R.id.completedRequest1);
        MaterialCardView completedRequest2 = findViewById(R.id.completedRequest2);

        completedRequest1.setOnClickListener(v ->
                startActivity(new Intent(this, CompletedRequestDetailActivity.class)));

        completedRequest2.setOnClickListener(v ->
                startActivity(new Intent(this, CompletedRequestDetailActivity.class)));
    }
}