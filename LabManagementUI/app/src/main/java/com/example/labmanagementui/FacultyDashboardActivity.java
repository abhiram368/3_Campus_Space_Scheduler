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

public class FacultyDashboardActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_dashboard);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Map the IDs correctly from activity_faculty_dashboard.xml
        CardView btnEscalated = findViewById(R.id.btnEscalated);
        CardView btnLiveStatus = findViewById(R.id.btnLiveStatus);
        CardView btnViewSchedule = findViewById(R.id.btnViewSchedule);
        CardView btnApprovalHistory = findViewById(R.id.btnApprovalHistory);
        CardView btnLabDetails = findViewById(R.id.btnLabDetails);
        CardView btnLabAdmins = findViewById(R.id.btnLabAdmins);
        View btnBookResource = findViewById(R.id.btnBookResource);

        if (btnEscalated != null) {
            btnEscalated.setOnClickListener(v -> startActivity(new Intent(
                    FacultyDashboardActivity.this,
                    PendingRequestsActivity.class
            )));
        }

        if (btnLiveStatus != null) {
            btnLiveStatus.setOnClickListener(v -> startActivity(new Intent(
                    FacultyDashboardActivity.this,
                    LabLiveStatusActivity.class
            )));
        }

        if (btnViewSchedule != null) {
            btnViewSchedule.setOnClickListener(v -> startActivity(new Intent(
                    FacultyDashboardActivity.this,
                    ViewScheduleActivity.class
            )));
        }

        if (btnApprovalHistory != null) {
            btnApprovalHistory.setOnClickListener(v -> startActivity(new Intent(
                    FacultyDashboardActivity.this,
                    ApprovalHistoryActivity.class
            )));
        }

        if (btnLabDetails != null) {
            btnLabDetails.setOnClickListener(v -> startActivity(new Intent(
                    FacultyDashboardActivity.this,
                    LabDetailsActivity.class
            )));
        }

        if (btnLabAdmins != null) {
            btnLabAdmins.setOnClickListener(v -> startActivity(new Intent(
                    FacultyDashboardActivity.this,
                    LabAdminsHomeActivity.class
            )));
        }

        if (btnBookResource != null) {
            btnBookResource.setOnClickListener(v -> {
                // TEST SAVE DATA TO FIRESTORE
                Map<String, Object> booking = new HashMap<>();
                booking.put("spaceName", "Faculty Room");
                booking.put("status", "pending");
                booking.put("timestamp", System.currentTimeMillis());

                db.collection("bookings")
                        .add(booking)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(FacultyDashboardActivity.this, "Booking Saved to Firestore!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(FacultyDashboardActivity.this, "Firestore Save Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });
        }
    }
}
