package com.example.campus_space_scheduler.lab_admin_incharge;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campus_space_scheduler.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LabAdminBookingDetailsActivity extends AppCompatActivity {

    private TextView tvLabPlace, tvBookedByName, tvBookedByRole, tvDate, tvSlot;
    private MaterialButton btnUploadPhoto, btnReportIssue, btnReassign;
    private String bookingId;
    private boolean isToday;
    private DatabaseReference bookingsRef, usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab_admin_booking_details);

        tvLabPlace = findViewById(R.id.tvLabPlace);
        tvBookedByName = findViewById(R.id.tvBookedByName);
        tvBookedByRole = findViewById(R.id.tvBookedByRole);
        tvDate = findViewById(R.id.tvDate);
        tvSlot = findViewById(R.id.tvSlot);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
        btnReportIssue = findViewById(R.id.btnReportIssue);
        btnReassign = findViewById(R.id.btnReassign);

        bookingId = getIntent().getStringExtra("bookingId");
        isToday = getIntent().getBooleanExtra("isToday", false);
        
        // Hide buttons if booking is not for today (Upcoming)
        if (!isToday) {
            btnUploadPhoto.setVisibility(View.GONE);
            btnReportIssue.setVisibility(View.GONE);
            btnReassign.setVisibility(View.GONE);
        }

        bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        if (bookingId != null) {
            loadBookingDetails();
        }

        btnUploadPhoto.setOnClickListener(v -> showUploadLinkDialog());

        btnReportIssue.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReportIssueActivity.class);
            intent.putExtra("bookingId", bookingId);
            startActivity(intent);
        });

        btnReassign.setOnClickListener(v -> {
            Toast.makeText(this, "Reassign feature coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void showUploadLinkDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Upload Photo Proof");
        builder.setMessage("Please paste the Google Drive link for the photo proof.");

        final EditText input = new EditText(this);
        input.setHint("https://drive.google.com/...");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        builder.setView(input);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            String link = input.getText().toString().trim();
            if (!link.isEmpty()) {
                savePhotoProof(link);
            } else {
                Toast.makeText(this, "Link cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void savePhotoProof(String link) {
        bookingsRef.child(bookingId).child("photoProof").setValue(link)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Photo proof link updated!", Toast.LENGTH_SHORT).show();
                    btnUploadPhoto.setText("Proof Uploaded");
                    btnUploadPhoto.setEnabled(false);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadBookingDetails() {
        bookingsRef.child(bookingId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    tvLabPlace.setText(String.valueOf(snapshot.child("spaceName").getValue()));
                    tvDate.setText(String.valueOf(snapshot.child("date").getValue()));
                    tvSlot.setText(String.valueOf(snapshot.child("timeSlot").getValue()));

                    String userId = String.valueOf(snapshot.child("bookedBy").getValue());
                    if (userId != null && !userId.isEmpty()) {
                        loadUserDetails(userId);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LabAdminBookingDetailsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserDetails(String userId) {
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    tvBookedByName.setText(String.valueOf(snapshot.child("name").getValue()));
                    tvBookedByRole.setText(String.valueOf(snapshot.child("role").getValue()));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}