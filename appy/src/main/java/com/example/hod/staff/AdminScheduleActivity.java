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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hod.R;
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

        Log.d("LabAdminSchedule", "onCreate: labId=" + spaceId + ", roomName=" + labName);

        if (spaceId == null) {
            Toast.makeText(this, "Missing Lab ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        repo = new FirebaseRepository();
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        CalendarView calendarView = findViewById(R.id.calendarView);
        rvAdminSchedule = findViewById(R.id.rvAdminSchedule);

        rvAdminSchedule.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminSlotAdapter(new ArrayList<>());
        rvAdminSchedule.setAdapter(adapter);

        if (labName == null || labName.isEmpty()) {
            repo.getSpaceDetails(spaceId, result -> {
                if (result instanceof Result.Success) {
                    com.example.hod.models.Space space = ((Result.Success<com.example.hod.models.Space>) result).data;
                    if (space != null) {
                        labName = space.getRoomName();
                        Log.d("LabAdminSchedule", "Fetched labName: " + labName);
                        updateHeader();
                        initSchedule(calendarView);
                    } else {
                        Log.e("LabAdminSchedule", "Space not found for ID: " + spaceId);
                        Toast.makeText(this, "Lab not found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("LabAdminSchedule", "Failed to fetch space details");
                    Toast.makeText(this, "Error fetching lab details", Toast.LENGTH_SHORT).show();
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
        Calendar cal = Calendar.getInstance();
        updateScheduleForDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            updateScheduleForDate(year, month, dayOfMonth);
        });
    }

    private void updateScheduleForDate(int year, int month, int dayOfMonth) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, dayOfMonth);
        
        String dateStr = String.format(Locale.getDefault(), "%d/%d/%d", dayOfMonth, month + 1, year);
        String dayOfWeek = new SimpleDateFormat("EEEE", Locale.ENGLISH).format(cal.getTime());
        
        tvSelectedDate.setText(String.format(Locale.getDefault(), "Schedule for: %s (%s)", dateStr, dayOfWeek));
        
        loadWeeklyTemplate(dayOfWeek);
    }

    private java.util.Set<String> generatedDays = new java.util.HashSet<>();

    private void loadWeeklyTemplate(String dayOfWeek) {
        if (spaceId == null || spaceId.isEmpty()) {
            Log.e("LabAdminSchedule", "Cannot load schedule: spaceId is null");
            return;
        }

        Log.d("LabAdminSchedule", "loadWeeklyTemplate: spaceId=" + spaceId + " day=" + dayOfWeek);
        
        // FIX: Query using spaceId, matching the new backend logic
        repo.getLabAdminWeeklySchedule(spaceId, dayOfWeek, result -> {
            if (result instanceof Result.Success) {
                List<Map<String, String>> slots = ((Result.Success<List<Map<String, String>>>) result).data;
                if (slots == null || slots.isEmpty()) {
                    if (!generatedDays.contains(dayOfWeek)) {
                        generatedDays.add(dayOfWeek);
                        Log.d("LabAdminSchedule", "No slots found. Attempting generation for " + labName + " " + dayOfWeek);
                        Toast.makeText(AdminScheduleActivity.this, "Generating schedule for " + labName + "... Please wait.", Toast.LENGTH_LONG).show();
                        
                        repo.generateWeeklySchedule(labName, spaceId, true, regenResult -> {
                            runOnUiThread(() -> {
                                if (regenResult instanceof Result.Success) {
                                    Log.d("LabAdminSchedule", "Regeneration complete. Retrying fetch.");
                                    loadWeeklyTemplate(dayOfWeek);
                                } else {
                                    Log.e("LabAdminSchedule", "Regeneration failed");
                                    Toast.makeText(AdminScheduleActivity.this, "No admins assigned to " + labName, Toast.LENGTH_SHORT).show();
                                    adapter.updateData(new ArrayList<>());
                                }
                            });
                        });
                    } else {
                        Log.d("LabAdminSchedule", "Generation already attempted for " + dayOfWeek);
                        adapter.updateData(new ArrayList<>());
                    }
                } else {
                    // STALE DATA DETECTION: Check if weekday has morning slots (08:00)
                    boolean isWeekend = dayOfWeek.equalsIgnoreCase("Saturday") || dayOfWeek.equalsIgnoreCase("Sunday");
                    boolean hasLegacySlots = false;
                    if (!isWeekend) {
                        for (Map<String, String> s : slots) {
                            int startMin = getTimeInMinutesFromSlot(s.get("slot"));
                            if (startMin >= 0 && startMin < (17 * 60)) {
                                hasLegacySlots = true;
                                break;
                            }
                        }
                    }

                    if (hasLegacySlots && !generatedDays.contains(dayOfWeek)) {
                        generatedDays.add(dayOfWeek);
                        Log.d("LabAdminSchedule", "Stale data detected. Attempting repair.");
                        repo.generateWeeklySchedule(labName, spaceId, true, regenResult -> {
                            runOnUiThread(() -> {
                                if (regenResult instanceof Result.Success) {
                                    loadWeeklyTemplate(dayOfWeek);
                                }
                            });
                        });
                    } else {
                        Log.d("LabAdminSchedule", "Found " + slots.size() + " slots");
                        java.util.Collections.sort(slots, (a, b) -> {
                            int t1 = getTimeInMinutesFromSlot(a.get("slot"));
                            int t2 = getTimeInMinutesFromSlot(b.get("slot"));
                            return Integer.compare(t1, t2);
                        });
                        adapter.updateData(slots);
                    }
                }
            } else {
                Exception e = ((Result.Error) result).exception;
                Log.e("LabAdminSchedule", "Failed: " + (e != null ? e.getMessage() : "Unknown error"));
                Toast.makeText(this, "Error loading schedule.", Toast.LENGTH_SHORT).show();
                adapter.updateData(new ArrayList<>());
            }
        });
    }

    private int getTimeInMinutesFromSlot(String slotKey) {
        return repo.parseStartTime(slotKey);
    }

    private static class AdminSlotAdapter extends RecyclerView.Adapter<AdminSlotAdapter.ViewHolder> {
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

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView tvSlotTime;
            final TextView tvAdminName;
            final TextView tvAdminRoll;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvSlotTime = itemView.findViewById(R.id.tvSlotTime);
                tvAdminName = itemView.findViewById(R.id.tvAdminName);
                tvAdminRoll = itemView.findViewById(R.id.tvAdminRoll);
            }
        }
    }
}
