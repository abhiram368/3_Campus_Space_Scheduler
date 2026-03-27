package com.example.hod.hod;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campussync.appy.R;
import com.example.hod.staff.StaffDashboardActivity;
import com.google.android.material.card.MaterialCardView;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        MaterialCardView cardHod = findViewById(R.id.cardHod);
        MaterialCardView cardStaff = findViewById(R.id.cardStaff);

        cardHod.setOnClickListener(v -> {
            startActivity(new Intent(this, HodDashboardActivity.class));
        });

        cardStaff.setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, StaffDashboardActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.error_staff_dashboard_not_found_msg, e.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
