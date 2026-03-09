package com.example.labmanagementui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class StaffDashboardActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_dashboard);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Map the IDs correctly from activity_staff_dashboard.xml
        CardView btnEscalated = findViewById(R.id.btnEscalated);
        CardView btnLiveStatus = findViewById(R.id.btnLiveStatus);
        CardView btnViewSchedule = findViewById(R.id.btnViewSchedule);
        CardView btnApprovalHistory = findViewById(R.id.btnApprovalHistory);
        CardView btnLabDetails = findViewById(R.id.btnLabDetails);
        CardView btnLabAdmins = findViewById(R.id.btnLabAdmins);
        View btnBookResource = findViewById(R.id.btnBookResource);

        if (btnEscalated != null) {
            btnEscalated.setOnClickListener(v -> startActivity(new Intent(
                    StaffDashboardActivity.this,
                    PendingRequestsActivity.class
            )));
        }

        if (btnLiveStatus != null) {
            btnLiveStatus.setOnClickListener(v -> startActivity(new Intent(
                    StaffDashboardActivity.this,
                    LabLiveStatusActivity.class
            )));
        }

        if (btnViewSchedule != null) {
            btnViewSchedule.setOnClickListener(v -> startActivity(new Intent(
                    StaffDashboardActivity.this,
                    ViewScheduleActivity.class
            )));
        }

        if (btnApprovalHistory != null) {
            btnApprovalHistory.setOnClickListener(v -> startActivity(new Intent(
                    StaffDashboardActivity.this,
                    ApprovalHistoryActivity.class
            )));
        }

        if (btnLabDetails != null) {
            btnLabDetails.setOnClickListener(v -> startActivity(new Intent(
                    StaffDashboardActivity.this,
                    LabDetailsActivity.class
            )));
        }

        if (btnLabAdmins != null) {
            btnLabAdmins.setOnClickListener(v -> startActivity(new Intent(
                    StaffDashboardActivity.this,
                    LabAdminsHomeActivity.class
            )));
        }

        if (btnBookResource != null) {
            btnBookResource.setOnClickListener(v -> {
                // TEST SAVE DATA TO FIRESTORE
                Map<String, Object> booking = new HashMap<>();
                booking.put("spaceName", "NSL");
                booking.put("status", "pending");
                booking.put("timestamp", System.currentTimeMillis());

                db.collection("bookings")
                        .add(booking)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(StaffDashboardActivity.this, "Booking Saved to Firestore!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(StaffDashboardActivity.this, "Firestore Save Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });
        }
    }
}
