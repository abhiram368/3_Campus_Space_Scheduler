package com.example.labadmin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;

public class AssignedSessionDetailsActivity extends AppCompatActivity {

    private FirestoreRepository repository;
    private String bookingId;
    
    TextView txtVenue, txtBookedBy, txtDateTime, txtSlot, txtUserRole;
    MaterialButton btnUploadProof, btnReportIssue, btnReassign;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assigned_session_details);

        repository = new FirestoreRepository();
        
        bookingId = getIntent().getStringExtra("bookingId");
        if (bookingId == null) {
            Toast.makeText(this, "No booking ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Views
        txtVenue = findViewById(R.id.txtVenue);
        txtBookedBy = findViewById(R.id.txtBookedBy);
        txtDateTime = findViewById(R.id.txtDateTime);
        txtSlot = findViewById(R.id.txtSlot);
        txtUserRole = findViewById(R.id.txtUserRole);

        btnUploadProof = findViewById(R.id.btnUploadProof);
        btnReportIssue = findViewById(R.id.btnReportIssue);
        btnReassign = findViewById(R.id.btnReassign);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        fetchSessionDetails();

        btnUploadProof.setOnClickListener(v -> {
            Intent intent = new Intent(this, UploadPhotoActivity.class);
            intent.putExtra("bookingId", bookingId);
            startActivity(intent);
        });

        btnReportIssue.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReportIssueActivity.class);
            intent.putExtra("bookingId", bookingId);
            startActivity(intent);
        });

        btnReassign.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReassignActivity.class);
            intent.putExtra("bookingId", bookingId);
            startActivity(intent);
        });
    }

    private void fetchSessionDetails() {
        repository.getSessionByBookingId(bookingId, new FirestoreRepository.Callback<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot document) {
                if (document != null && document.exists()) {
                    txtVenue.setText(document.getString("venue"));
                    txtBookedBy.setText(document.getString("bookedBy"));
                    txtDateTime.setText(document.getString("date"));
                    txtSlot.setText(document.getString("slot"));
                    txtUserRole.setText(document.getString("role"));
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(AssignedSessionDetailsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
