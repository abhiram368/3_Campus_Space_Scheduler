package com.example.campus_space_scheduler;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campus_space_scheduler.databinding.ActivityViewLogsBinding;
import com.example.campus_space_scheduler.databinding.ItemLogBinding;
import java.util.ArrayList;
import java.util.List;

public class ViewLogsActivity extends AppCompatActivity {

    private ActivityViewLogsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityViewLogsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Correct way to set the back button listener using Binding
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        setupMockData();
    }

    private void setupMockData() {
        List<LogMock> mockLogs = new ArrayList<>();
        // Formatting data to reflect your NITC project context
        mockLogs.add(new LogMock("10:45 PM", "CSV_UPLOAD", "Added 260 users to NITC database.", "#4CAF50"));
        mockLogs.add(new LogMock("09:12 PM", "USER_EDIT", "Admin updated role for Abhiram Katta to 'App admin'.", "#2196F3"));
        mockLogs.add(new LogMock("08:30 PM", "SPACE_REMOVE", "Room 'SSL Lab' deleted from database.", "#F44336"));
        mockLogs.add(new LogMock("06:15 PM", "SYSTEM", "Automated schedule generated for Feb 10 - Feb 16.", "#FF9800"));
        mockLogs.add(new LogMock("04:00 PM", "LOGIN", "New admin login detected: admin.office@nitc.ac.in", "#9C27B0"));
        mockLogs.add(new LogMock("Yesterday", "CSV_ERROR", "Row 142 failed: Capacity must be a number.", "#F44336"));
        mockLogs.add(new LogMock("03:30 PM", "MANUAL_ASSIGN", "HoD Dr. Rajesh Kumar assigned NLHC 201 to Faculty Incharge despite Lab admin booking.", "#2196F3"));
        mockLogs.add(new LogMock("02:15 PM", "ROLE_CHANGE", "User 'Divya Patel' role updated from 'Student' to 'Lab admin'.", "#9C27B0"));
        mockLogs.add(new LogMock("01:00 PM", "SYSTEM_RESET", "Admin cleared 'spaces' node; re-initialized via spaces.csv (60 entries).", "#F44336"));
        mockLogs.add(new LogMock("11:45 AM", "STAFF_UPDATE", "Staff Incharge 'Neha Reddy' assigned to OS Lab oversight.", "#4CAF50"));
        mockLogs.add(new LogMock("10:20 AM", "MAINTENANCE", "CSED Staff marked SSL Lab as 'Unavailable' for hardware upgrades.", "#FF9800"));
        mockLogs.add(new LogMock("09:00 AM", "CONFLICT", "Auto-resolved overlap in Main Hall: Shifted HoD meeting to 10:30 AM.", "#FF9800"));

        binding.rvLogs.setLayoutManager(new LinearLayoutManager(this));
        binding.rvLogs.setAdapter(new LogAdapter(mockLogs));
    }

    // --- INTERNAL ADAPTER FOR PRESENTATION ---
    private class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {
        private final List<LogMock> logs;
        LogAdapter(List<LogMock> logs) { this.logs = logs; }

        @NonNull
        @Override
        public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemLogBinding b = ItemLogBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new LogViewHolder(b);
        }

        @Override
        public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
            LogMock log = logs.get(position);
            holder.binding.logTime.setText(log.time);
            holder.binding.logAction.setText(log.action);
            holder.binding.logAction.setTextColor(Color.parseColor(log.color));
            holder.binding.logDetails.setText(log.details);
        }

        @Override
        public int getItemCount() { return logs.size(); }

        class LogViewHolder extends RecyclerView.ViewHolder {
            ItemLogBinding binding;
            LogViewHolder(ItemLogBinding b) { super(b.getRoot()); this.binding = b; }
        }
    }
}