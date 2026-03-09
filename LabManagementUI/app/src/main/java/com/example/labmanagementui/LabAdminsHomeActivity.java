package com.example.labmanagementui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class LabAdminsHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab_admins_home);

        Button btnViewAdmins = findViewById(R.id.btnViewAdmins);
        Button btnViewAdminSchedule = findViewById(R.id.btnViewAdminSchedule);
        MaterialCardView adminCard1 = findViewById(R.id.adminCard1);

        // Connect the "View Admins" button to ViewAdminsActivity
        if (btnViewAdmins != null) {
            btnViewAdmins.setOnClickListener(v -> {
                startActivity(new Intent(
                        LabAdminsHomeActivity.this,
                        ViewAdminsActivity.class
                ));
            });
        }

        // Connect the "View Admin Schedule" button to AdminScheduleActivity
        if (btnViewAdminSchedule != null) {
            btnViewAdminSchedule.setOnClickListener(v -> {
                startActivity(new Intent(
                        LabAdminsHomeActivity.this,
                        AdminScheduleActivity.class
                ));
            });
        }

        // Optional: Connect the sample admin card to AdminDetailsActivity
        if (adminCard1 != null) {
            adminCard1.setOnClickListener(v -> {
                Intent intent = new Intent(LabAdminsHomeActivity.this, AdminDetailsActivity.class);
                intent.putExtra("name", "John Doe");
                intent.putExtra("roll", "SYSADM001");
                intent.putExtra("phone", "9876543210");
                intent.putExtra("email", "johndoe@nitc.ac.in");
                startActivity(intent);
            });
        }
    }
}
