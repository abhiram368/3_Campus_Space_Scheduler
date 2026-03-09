package com.example.labmanagementui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class WorkspaceStaffInchargeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workspace);

        Button btnPending = findViewById(R.id.btnPendingRequests);
        btnPending.setOnClickListener(v -> {
            Intent intent = new Intent(WorkspaceStaffInchargeActivity.this, PendingRequestsActivity.class);
            startActivity(intent);
        });

        Button btnApprovalHistory = findViewById(R.id.btnApprovalHistory);
        btnApprovalHistory.setOnClickListener(v -> startActivity(new Intent(WorkspaceStaffInchargeActivity.this, ApprovalHistoryActivity.class)));

        Button btnLabDetails = findViewById(R.id.btnLabDetails);
        btnLabDetails.setOnClickListener(v -> startActivity(new Intent(WorkspaceStaffInchargeActivity.this, LabDetailsActivity.class)));

        Button btnLabAdmins = findViewById(R.id.btnLabAdmins);

        btnLabAdmins.setOnClickListener(v -> {
            startActivity(new Intent(
                    WorkspaceStaffInchargeActivity.this,
                    LabAdminsHomeActivity.class
            ));
        });
    }
}
