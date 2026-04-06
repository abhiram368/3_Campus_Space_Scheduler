package com.example.hod.staff;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;
import com.example.hod.repository.FirebaseRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DataUtilityActivity extends AppCompatActivity {

    private FirebaseRepository repo;
    private String labId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_utility);

        repo = new FirebaseRepository();
        labId = getIntent().getStringExtra("labId");

        Button btnManualSync = findViewById(R.id.btnManualSync);
        btnManualSync.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                .setTitle("Initialize 365-Day Schedule")
                .setMessage("This will generate a default 08:00 - 23:00 schedule for the next year. Existing bookings will be preserved. Continue?")
                .setPositiveButton("Sync Now", (dialog, which) -> {
                    performManualSync();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        Button btnCleanData = findViewById(R.id.btnCleanData);
        btnCleanData.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Data Cleaning")
                .setMessage("Are you sure you want to clean old data? This action cannot be undone.")
                .setPositiveButton("Clean", (dialog, which) -> {
                    repo.cleanOldData(labId, result -> {
                        if (result instanceof com.example.hod.utils.Result.Success) {
                            Toast.makeText(DataUtilityActivity.this, "Old data cleaned successfully.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(DataUtilityActivity.this, "Failed to clean data.", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    private void performManualSync() {
        if (labId == null || labId.isEmpty()) return;

        android.app.ProgressDialog progress = new android.app.ProgressDialog(this);
        progress.setMessage("Synchronizing 365-day schedule... please wait.");
        progress.setCancelable(false);
        progress.show();

        // 1. Fetch Space Details using the repository (points to correctly-named "spaces" node)
        repo.getSpaceDetails(labId, result -> {
            if (result instanceof com.example.hod.utils.Result.Success) {
                com.example.hod.models.Space space = ((com.example.hod.utils.Result.Success<com.example.hod.models.Space>) result).data;
                String labName = (space != null && space.getRoomName() != null) ? space.getRoomName() : labId;

                // 2. Trigger the optimized 365-day sync
                repo.generateWeeklySchedule(labName, labId, true, syncResult -> {
                    runOnUiThread(() -> {
                        progress.dismiss();
                        if (syncResult instanceof com.example.hod.utils.Result.Success) {
                            new MaterialAlertDialogBuilder(DataUtilityActivity.this)
                                .setTitle("Sync Complete")
                                .setMessage("Schedule for the next 365 days (08:00 - 23:00) has been initialized and synchronized.")
                                .setPositiveButton("OK", null)
                                .show();
                        } else {
                            Toast.makeText(DataUtilityActivity.this, "Sync failed: " + syncResult.toString(), Toast.LENGTH_LONG).show();
                        }
                    });
                });
            } else {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(DataUtilityActivity.this, "Failed to retrieve lab details.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
