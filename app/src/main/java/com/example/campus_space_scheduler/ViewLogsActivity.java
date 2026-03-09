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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

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

        loadLogs();
    }

    private void loadLogs() {

        FirebaseDatabase.getInstance()
                .getReference("logs")
                .limitToLast(50)
                .get()
                .addOnSuccessListener(snapshot -> {

                    List<LogMock> logs = new ArrayList<>();

                    for (DataSnapshot s : snapshot.getChildren()) {

                        String time = s.child("time").getValue(String.class);
                        String action = s.child("action").getValue(String.class);
                        String details = s.child("details").getValue(String.class);

                        logs.add(new LogMock(time, action, details, "#2196F3"));
                    }

                    binding.rvLogs.setLayoutManager(new LinearLayoutManager(this));
                    binding.rvLogs.setAdapter(new LogAdapter(logs));
                });
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