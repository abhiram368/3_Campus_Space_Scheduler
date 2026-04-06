package com.example.campus_space_scheduler.lab_admin_incharge;

import android.os.Bundle;
import android.widget.Toast;
import com.example.campus_space_scheduler.databinding.ActivityReportIssueBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class ReportIssueActivity extends BaseInchargeActivity {

    private ActivityReportIssueBinding binding;
    private String bookingId;
    private DatabaseReference issuesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReportIssueBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Sidebar using BaseActivity method
        setupDrawer(binding.toolbar, binding.navView, binding.drawerLayout);

        bookingId = getIntent().getStringExtra("bookingId");
        issuesRef = FirebaseDatabase.getInstance().getReference("issues");

        binding.btnSubmitIssue.setOnClickListener(v -> submitIssue());
    }

    private void submitIssue() {
        String description = binding.etIssueDescription.getText().toString().trim();
        if (description.isEmpty()) {
            Toast.makeText(this, "Please describe the issue", Toast.LENGTH_SHORT).show();
            return;
        }

        String issueId = issuesRef.push().getKey();
        String userId = FirebaseAuth.getInstance().getUid();

        Map<String, Object> issueData = new HashMap<>();
        issueData.put("issueId", issueId);
        issueData.put("bookingId", bookingId);
        issueData.put("description", description);
        issueData.put("reportedBy", userId);
        issueData.put("timestamp", System.currentTimeMillis());
        issueData.put("status", "pending");

        if (issueId != null) {
            issuesRef.child(issueId).setValue(issueData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Issue reported successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to report issue: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        }
    }
}