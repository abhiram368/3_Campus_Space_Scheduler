package com.example.labadmin;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

public class ReportIssueActivity extends AppCompatActivity {

    private FirestoreRepository repository;
    private String currentUserId;
    private String bookingId;
    private EditText edtIssue;
    private MaterialButton btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_issue);

        repository = new FirestoreRepository();
        
        bookingId = getIntent().getStringExtra("bookingId");
        if (bookingId == null) {
            Toast.makeText(this, "No booking ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            currentUserId = "admin_001";
        }

        edtIssue = findViewById(R.id.edtIssue);
        btnSubmit = findViewById(R.id.btnSubmitIssue);

        btnSubmit.setOnClickListener(v -> {
            String issueText = edtIssue.getText().toString().trim();

            if (issueText.isEmpty()) {
                Toast.makeText(this, "Please describe the issue", Toast.LENGTH_SHORT).show();
            } else {
                submitIssue(issueText);
            }
        });
    }

    private void submitIssue(String issueText) {
        Map<String, Object> issueData = new HashMap<>();
        issueData.put("bookingId", bookingId);
        issueData.put("issueText", issueText);
        issueData.put("reportedBy", currentUserId);
        issueData.put("timestamp", System.currentTimeMillis());

        repository.reportIssue(issueData, new FirestoreRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(ReportIssueActivity.this, "Issue reported successfully!", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ReportIssueActivity.this, "Failed to report issue: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
