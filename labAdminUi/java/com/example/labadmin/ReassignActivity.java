package com.example.labadmin;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

public class ReassignActivity extends AppCompatActivity {

    private FirestoreRepository repository;
    private String currentAdminId;
    private String bookingId;
    private EditText edtReason;
    private MaterialButton btnConfirmReassign;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reassign);

        repository = new FirestoreRepository();
        
        bookingId = getIntent().getStringExtra("bookingId");
        if (bookingId == null) {
            Toast.makeText(this, "No booking ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentAdminId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            currentAdminId = "admin_001";
        }

        // Initialize Views
        edtReason = findViewById(R.id.edtReason);
        btnConfirmReassign = findViewById(R.id.btnConfirmReassign);

        btnConfirmReassign.setOnClickListener(v -> {
            String reasonText = edtReason.getText().toString().trim();

            if (reasonText.isEmpty()) {
                Toast.makeText(this, "Please enter a reason for reassignment", Toast.LENGTH_SHORT).show();
            } else {
                handleReassignment(reasonText);
            }
        });
    }

    private void handleReassignment(String reason) {
        // Log the request and update the main booking
        Map<String, Object> reassignData = new HashMap<>();
        reassignData.put("bookingId", bookingId);
        reassignData.put("oldAdminId", currentAdminId);
        reassignData.put("reason", reason);
        reassignData.put("timestamp", System.currentTimeMillis());

        // For this task, we will just update the booking directly in this mock
        // but typically you would use a 'reassignments' collection as well.
        repository.reassignAdmin(bookingId, "REASSIGN_PENDING", new FirestoreRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(ReassignActivity.this, "Reassignment request sent to faculty!", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ReassignActivity.this, "Failed to request reassignment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
