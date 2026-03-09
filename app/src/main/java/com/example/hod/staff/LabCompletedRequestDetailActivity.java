package com.example.hod.staff;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;

public class LabCompletedRequestDetailActivity extends AppCompatActivity {

    boolean isBlocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_completed_request_detail);

        TextView tvBlockStatus = findViewById(R.id.tvBlockStatus);
        EditText remarkBox = findViewById(R.id.remarkBox);
        Button btnBlockStudent = findViewById(R.id.btnBlockStudent);
        Button btnUnblockStudent = findViewById(R.id.btnUnblockStudent);

        // SAFETY CHECK (VERY IMPORTANT)
        if (btnBlockStudent == null) {
            return;
        }

        btnBlockStudent.setOnClickListener(v -> {

            String remark = remarkBox.getText().toString().trim();

            if (remark.isEmpty()) {
                remarkBox.setError("Remark required to block student");
                return;
            }

            new AlertDialog.Builder(LabCompletedRequestDetailActivity.this)
                    .setTitle("Block Student")
                    .setMessage("Are you sure you want to block this student?")
                    .setPositiveButton("Block", (dialog, which) -> {
                        isBlocked = true;

                        tvBlockStatus.setText("Student Status: Blocked");
                        tvBlockStatus.setTextColor(
                                getResources().getColor(android.R.color.holo_red_dark)
                        );

                        btnBlockStudent.setVisibility(View.GONE);
                        btnUnblockStudent.setVisibility(View.VISIBLE);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnUnblockStudent.setOnClickListener(v -> {
            new AlertDialog.Builder(LabCompletedRequestDetailActivity.this)
                    .setTitle("Unblock Student")
                    .setMessage("Unblock this student?")
                    .setPositiveButton("Unblock", (dialog, which) -> {
                        isBlocked = false;

                        tvBlockStatus.setText("Student Status: Active");
                        tvBlockStatus.setTextColor(
                                getResources().getColor(android.R.color.holo_green_dark)
                        );

                        btnUnblockStudent.setVisibility(View.GONE);
                        btnBlockStudent.setVisibility(View.VISIBLE);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }
}
