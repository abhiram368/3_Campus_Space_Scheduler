package com.example.hod.staff;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LabDataRecoveryActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView tvStatus, tvTitle, tvSubtitle;
    private MaterialButton btnStart;
    private DatabaseReference schedulesRef;
    private String currentLabId;
    private String currentLabName;
    private boolean isRecovering = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ssl_recovery);

        currentLabId = getIntent().getStringExtra("labId");
        currentLabName = getIntent().getStringExtra("labName");

        if (currentLabId == null) {
            Toast.makeText(this, "Error: Missing Lab ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar = findViewById(R.id.progressBarRecovery);
        tvStatus = findViewById(R.id.tvStatus);
        btnStart = findViewById(R.id.btnStartRecovery);
        
        tvTitle = findViewById(R.id.header_title);
        tvSubtitle = findViewById(R.id.header_subtitle);
        View btnBack = findViewById(R.id.btnBack);

        if (tvTitle != null) tvTitle.setText(currentLabName != null ? currentLabName + " Reset" : "Lab Reset Utility");
        if (tvSubtitle != null) tvSubtitle.setText("Year 2026 Cleanup");
        
        if (btnBack != null) btnBack.setOnClickListener(v -> {
            if (isRecovering) {
                Toast.makeText(this, "Reset in progress...", Toast.LENGTH_SHORT).show();
            } else {
                finish();
            }
        });

        schedulesRef = FirebaseDatabase.getInstance().getReference("schedules");
        btnStart.setOnClickListener(v -> showConfirmationDialog());
    }

    private void showConfirmationDialog() {
        String msg = "Are you sure? This will delete ALL slots for " + 
                     (currentLabName != null ? currentLabName : "this lab") + 
                     " for the entire year 2026 and regenerate them in the new standard format (08:00 - 23:00).";
        
        new AlertDialog.Builder(this)
                .setTitle("Database Reset")
                .setMessage(msg)
                .setPositiveButton("WIPE & RESET", (d, w) -> startYearlyReset())
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void startYearlyReset() {
        isRecovering = true;
        btnStart.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setVisibility(View.VISIBLE);
        
        new Thread(() -> {
            Calendar cal = Calendar.getInstance();
            cal.set(2026, 0, 1); // Jan 1, 2026
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            int totalDays = 365;
            
            for (int i = 0; i < totalDays; i++) {
                final String dateStr = sdf.format(cal.getTime());
                final int progress = i + 1;
                final String currentStatusStr = "Syncing: " + dateStr;

                // 1. Wipe Fragmented Keys
                String directKey = currentLabId + "_" + dateStr;
                schedulesRef.child(directKey).removeValue();
                
                // 2. Generate Clean Slots (08:00 AM - 11:00 PM)
                Map<String, Object> scheduleNode = new HashMap<>();
                scheduleNode.put("spaceId", currentLabId);
                scheduleNode.put("date", dateStr);
                
                Map<String, Object> slots = new HashMap<>();
                for (int h = 8; h < 23; h++) {
                    addSlot(slots, h, 0);
                    addSlot(slots, h, 30);
                }
                scheduleNode.put("slots", slots);
                
                // Write clean node with error tracking
                schedulesRef.child(directKey).setValue(scheduleNode)
                    .addOnFailureListener(e -> Log.e("ResetTool", "Failed to sync " + dateStr + ": " + e.getMessage()));

                new Handler(Looper.getMainLooper()).post(() -> {
                    progressBar.setProgress(progress);
                    tvStatus.setText(currentStatusStr);
                });

                try { Thread.sleep(80); } catch (InterruptedException e) {}
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                isRecovering = false;
                tvStatus.setText((currentLabName != null ? currentLabName : "Lab") + " 2026 Reset Complete!");
                Toast.makeText(this, "Database Synchronized Successfully", Toast.LENGTH_LONG).show();
                btnStart.setText("DONE");
                btnStart.setEnabled(true);
                btnStart.setOnClickListener(v -> finish());
            });
        }).start();
    }

    private void addSlot(Map<String, Object> slots, int hour, int minute) {
        String startStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
        
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.add(Calendar.MINUTE, 30);
        
        String endStr = String.format(Locale.getDefault(), "%02d:%02d", 
                next.get(Calendar.HOUR_OF_DAY), next.get(Calendar.MINUTE));
        
        String slotLabel = startStr + " - " + endStr;
        
        Map<String, Object> slotData = new HashMap<>();
        slotData.put("start", startStr);
        slotData.put("end", endStr);
        slotData.put("status", "AVAILABLE");
        
        slots.put(slotLabel, slotData);
    }
}
