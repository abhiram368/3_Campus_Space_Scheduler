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
}
