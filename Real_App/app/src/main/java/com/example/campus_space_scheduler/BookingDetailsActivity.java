package com.example.campus_space_scheduler;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class BookingDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_details);

        // Get views
        ImageView buttonBack = findViewById(R.id.buttonBack);
        TextView spaceNameTextView = findViewById(R.id.textViewSpaceName);
        TextView statusTextView = findViewById(R.id.textViewStatus);
        TextView dateTimeTextView = findViewById(R.id.textViewDateTime);
        TextView purposeTextView = findViewById(R.id.textViewPurpose);
        TextView descriptionTextView = findViewById(R.id.textViewDescription);
        TextView bookedByTextView = findViewById(R.id.textViewBookedBy);
        TextView approvedByTextView = findViewById(R.id.textViewApprovedBy);
        MaterialButton buttonViewLor = findViewById(R.id.buttonViewLor);

        // Get data from intent
        String spaceName = getIntent().getStringExtra("SPACE_NAME");
        String bookedBy = getIntent().getStringExtra("BOOKED_BY");
        String date = getIntent().getStringExtra("DATE");
        String timeSlot = getIntent().getStringExtra("TIME_SLOT");
        String purpose = getIntent().getStringExtra("PURPOSE");
        String description = getIntent().getStringExtra("DESCRIPTION");
        String status = getIntent().getStringExtra("STATUS");
        String approvedOrRejectedBy = getIntent().getStringExtra("APPROVED_OR_REJECTED_BY");
        boolean hasLor = getIntent().getBooleanExtra("HAS_LOR", false);

        // Set data to views
        spaceNameTextView.setText(spaceName != null ? spaceName : "N/A");
        statusTextView.setText(status != null ? status : "PENDING");
        dateTimeTextView.setText((date != null ? date : "") + " | " + (timeSlot != null ? timeSlot : ""));
        purposeTextView.setText(purpose != null ? purpose : "N/A");
        descriptionTextView.setText(description != null ? description : "No description provided.");
        bookedByTextView.setText(bookedBy != null ? bookedBy : "N/A");

        // UI styling for status
        updateStatusUI(status, statusTextView);

        // Handle conditional visibility
        if ("Accepted".equalsIgnoreCase(status) || "Approved".equalsIgnoreCase(status)) {
            approvedByTextView.setText("Approved by: " + (approvedOrRejectedBy != null ? approvedOrRejectedBy : "Admin"));
            approvedByTextView.setVisibility(View.VISIBLE);
        } else if ("Rejected".equalsIgnoreCase(status)) {
            approvedByTextView.setText("Rejected by: " + (approvedOrRejectedBy != null ? approvedOrRejectedBy : "Admin"));
            approvedByTextView.setVisibility(View.VISIBLE);
        } else {
            approvedByTextView.setVisibility(View.GONE);
        }

        if (hasLor) {
            buttonViewLor.setVisibility(View.VISIBLE);
            buttonViewLor.setOnClickListener(v -> {
                Toast.makeText(this, "Opening LOR Document...", Toast.LENGTH_SHORT).show();
            });
        } else {
            buttonViewLor.setVisibility(View.GONE);
        }

        buttonBack.setOnClickListener(v -> finish());
    }

    private void updateStatusUI(String status, TextView statusTextView) {
        if (status == null) return;
        
        switch (status.toUpperCase()) {
            case "ACCEPTED":
            case "APPROVED":
                statusTextView.setBackgroundResource(R.drawable.status_accepted_bg);
                break;
            case "REJECTED":
                statusTextView.setBackgroundResource(R.drawable.status_rejected_bg);
                break;
            default:
                statusTextView.setBackgroundResource(R.drawable.status_pending_bg);
                break;
        }
    }
}
