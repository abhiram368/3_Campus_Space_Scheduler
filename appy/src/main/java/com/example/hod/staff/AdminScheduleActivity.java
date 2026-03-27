package com.example.hod.staff;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.appy.R;
import com.example.hod.repository.FirebaseRepository;
import com.example.hod.utils.Result;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminScheduleActivity extends AppCompatActivity {

    private String labName;
    private String spaceId;
    private RecyclerView rvAdminSchedule;
    private AdminSlotAdapter adapter;
    private FirebaseRepository repo;
    private TextView tvSelectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_schedule);

        labName = getIntent().getStringExtra("roomName");
        spaceId = getIntent().getStringExtra("labId");

        if (spaceId == null) {
            Toast.makeText(this, "Missing Lab ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        repo = new FirebaseRepository();
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        CalendarView calendarView = findViewById(R.id.calendarView);
        rvAdminSchedule = findViewById(R.id.rvAdminSchedule);

        adapter = new AdminSlotAdapter(new ArrayList<>());
        rvAdminSchedule.setAdapter(adapter);

        if (labName == null) {
            repo.getSpaceDetails(spaceId, result -> {
                if (result instanceof Result.Success) {
                    com.example.hod.models.Space space = ((Result.Success<com.example.hod.models.Space>) result).data;
                    if (space != null) {
                        labName = space.getRoomName();
                        updateHeader();
                        initSchedule(calendarView);
                    }
                }
            });
        } else {
            updateHeader();
            initSchedule(calendarView);
        }
    }

    private void updateHeader() {
        TextView tvTitle = findViewById(R.id.header_title);
        TextView tvSubtitle = findViewById(R.id.header_subtitle);
        View btnBack = findViewById(R.id.btnBack);

        if (tvTitle != null) tvTitle.setText(labName != null ? labName : "Schedule");
        if (tvSubtitle != null) tvSubtitle.setText("Weekly Admin Schedule");
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void initSchedule(CalendarView calendarView) {
        // Initial load for today
        Calendar cal = Calendar.getInstance();
        updateScheduleForDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            updateScheduleForDate(year, month, dayOfMonth);
        });
    }

    private void updateScheduleForDate(int year, int month, int dayOfMonth) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, dayOfMonth);
        
        String dateStr = dayOfMonth + "/" + (month + 1) + "/" + year;
        String dayOfWeek = new SimpleDateFormat("EEEE", Locale.ENGLISH).format(cal.getTime());
        
        tvSelectedDate.setText("Schedule for: " + dateStr + " (" + dayOfWeek + ")");
        
        loadWeeklyTemplate(dayOfWeek);
    }

    private java.util.Set<String> generatedDays = new java.util.HashSet<>();

    private void loadWeeklyTemplate(String dayOfWeek) {
        Log.d("LabAdminSchedule", "loadWeeklyTemplate: labName=" + labName + " day=" + dayOfWeek);
        repo.getLabAdminWeeklySchedule(labName, dayOfWeek, result -> {
            if (result instanceof Result.Success) {
                List<Map<String, String>> slots = ((Result.Success<List<Map<String, String>>>) result).data;
                if (slots == null || slots.isEmpty()) {
                    if (!generatedDays.contains(dayOfWeek)) {
                        generatedDays.add(dayOfWeek);
                        Log.d("LabAdminSchedule", "No slots found. Attempting one-time generation for " + labName + " " + dayOfWeek);
                        Toast.makeText(this, "Generating schedule for " + labName + ". Please wait.", Toast.LENGTH_LONG).show();
                        repo.generateWeeklySchedule(labName, spaceId, regenResult -> {
                            if (regenResult instanceof Result.Success) {
                                Log.d("LabAdminSchedule", "Regeneration complete. Retrying schedule fetch.");
                                loadWeeklyTemplate(dayOfWeek);
                            } else {
                                Log.e("LabAdminSchedule", "Regeneration failed");
                                Toast.makeText(this, "Failed to generate schedule. No admins assigned.", Toast.LENGTH_SHORT).show();
                                adapter.updateData(new ArrayList<>());
                            }
                        });
                    } else {
                        Log.d("LabAdminSchedule", "Already attempted generation for " + dayOfWeek + ". Remaining empty.");
                        adapter.updateData(new ArrayList<>());
                    }
                } else {
                    Log.d("LabAdminSchedule", "Found " + slots.size() + " slots");
                    adapter.updateData(slots);
                }
            } else {
                Log.e("LabAdminSchedule", "Failed to load schedule template: " + ((Result.Error) result).exception.getMessage());
                Toast.makeText(this, "Error loading schedule.", Toast.LENGTH_SHORT).show();
                adapter.updateData(new ArrayList<>());
            }
        });
    }

    private class AdminSlotAdapter extends RecyclerView.Adapter<AdminSlotAdapter.ViewHolder> {
        private final List<Map<String, String>> slots;

        public AdminSlotAdapter(List<Map<String, String>> slots) {
            this.slots = slots;
        }

        public void updateData(List<Map<String, String>> newSlots) {
            this.slots.clear();
            this.slots.addAll(newSlots);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_slot, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, String> slot = slots.get(position);
            holder.tvSlotTime.setText(slot.get("slot"));
            holder.tvAdminName.setText(slot.get("name"));
            holder.tvAdminRoll.setText(slot.get("rollNo"));
        }

        @Override
        public int getItemCount() {
            return slots.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSlotTime, tvAdminName, tvAdminRoll;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvSlotTime = itemView.findViewById(R.id.tvSlotTime);
                tvAdminName = itemView.findViewById(R.id.tvAdminName);
                tvAdminRoll = itemView.findViewById(R.id.tvAdminRoll);
            }
        }
    }
}