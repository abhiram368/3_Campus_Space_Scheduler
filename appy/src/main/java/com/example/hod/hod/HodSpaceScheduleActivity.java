package com.example.hod.hod;

import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hod.R;
import com.example.hod.adapters.HodScheduleAdapter;
import com.example.hod.models.Booking;
import com.example.hod.models.LiveStatusData;
import com.example.hod.repository.FirebaseRepository;
import com.example.hod.utils.Result;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HodSpaceScheduleActivity extends AppCompatActivity {

    private String spaceId, roomName;
    private RecyclerView rvSchedule;
    private ProgressBar progressBar;
    private TextView noDataTextView, tvSelectedDate;
    private HodScheduleAdapter adapter;
    private List<LiveStatusData> slotList;
    private FirebaseRepository repo;
    
    private ChipGroup dateChipGroup;
    private View calendarCard;
    private CalendarView calendarView;
    private Calendar selectedCalendar;
    private TextView tvCalendarNote;
    private TextView tvSelectedDateBottom;
    private boolean isPastDate = false;

    private com.google.firebase.database.ValueEventListener scheduleListener;
    private String currentSelectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hod_space_schedule);

        spaceId = getIntent().getStringExtra("labId");
        roomName = getIntent().getStringExtra("roomName");

        if (spaceId == null) {
            Toast.makeText(this, "Missing Space ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        updateHeader(roomName != null ? roomName : "Space Schedule", "Loading...");

        rvSchedule = findViewById(R.id.rvSchedule);
        progressBar = findViewById(R.id.progressBar);
        noDataTextView = findViewById(R.id.noDataTextView);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        dateChipGroup = findViewById(R.id.dateChipGroup);
        calendarCard = findViewById(R.id.calendarCard);
        calendarView = findViewById(R.id.calendarView);
        tvCalendarNote = findViewById(R.id.tvCalendarNote);
        tvSelectedDateBottom = findViewById(R.id.tvSelectedDateBottom);

        rvSchedule.setLayoutManager(new LinearLayoutManager(this));
        slotList = new ArrayList<>();
        adapter = new HodScheduleAdapter(this, slotList, roomName);
        rvSchedule.setAdapter(adapter);

        repo = new FirebaseRepository();
        selectedCalendar = Calendar.getInstance();

        setupChips();
        setupCalendar();

        loadScheduleForDate(selectedCalendar);
    }

    private void setupChips() {
        dateChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            
            int checkedId = checkedIds.get(0);
            calendarCard.setVisibility(View.GONE);
            noDataTextView.setVisibility(View.GONE); // Hide by default

            if (checkedId == R.id.chipToday) {
                selectedCalendar = Calendar.getInstance();
                loadScheduleForDate(selectedCalendar);
                rvSchedule.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.chipTomorrow) {
                selectedCalendar = Calendar.getInstance();
                selectedCalendar.add(Calendar.DAY_OF_YEAR, 1);
                loadScheduleForDate(selectedCalendar);
                rvSchedule.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.chipPickDate) {
                calendarCard.setVisibility(View.VISIBLE);
                tvCalendarNote.setVisibility(View.VISIBLE);
                rvSchedule.setVisibility(View.GONE);
                noDataTextView.setVisibility(View.VISIBLE); // Show only for Pick Date
            }
        });
    }

    private void setupCalendar() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedCalendar.set(year, month, dayOfMonth);
            loadScheduleForDate(selectedCalendar);
            calendarCard.setVisibility(View.GONE);
            tvCalendarNote.setVisibility(View.GONE);
            rvSchedule.setVisibility(View.VISIBLE);
            tvSelectedDate.setVisibility(View.GONE);
            updateSubtitle(selectedCalendar);
        });
    }

    private void loadScheduleForDate(Calendar cal) {
        String dbDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        checkIfPastDate(dbDate);
        updateSubtitle(cal);

        adapter.setPastDate(isPastDate);
        slotList.clear();
        adapter.notifyDataSetChanged();
        progressBar.setVisibility(View.VISIBLE);
        noDataTextView.setVisibility(View.GONE);

        if (scheduleListener != null && currentSelectedDate != null) {
            repo.removeScheduleListener(spaceId, currentSelectedDate, scheduleListener);
            scheduleListener = null;
        }

        currentSelectedDate = dbDate;
        repo.syncScheduleWithBookings(spaceId, dbDate, r -> {});

        scheduleListener = repo.observeSchedulesForLab(spaceId, dbDate, result -> runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);

            java.util.Map<String, LiveStatusData> deduplicated = new java.util.HashMap<>();
            DataSnapshot snapshot = null;
            if (result instanceof Result.Success) snapshot = ((Result.Success<DataSnapshot>) result).data;

            if (snapshot != null && snapshot.exists()) {
                for (DataSnapshot s : snapshot.getChildren()) {
                    String rawKey = s.getKey();
                    if (rawKey == null) continue;

                    String status = s.child("status").getValue(String.class);
                    if (status == null) {
                        Object val = s.getValue();
                        if (val instanceof Boolean) status = ((Boolean) val) ? "BOOKED" : "AVAILABLE";
                        else status = "AVAILABLE";
                    }

                    // Normalize and Expand Filter
                    status = repo.normalizeSlotStatus(status);
                    if (!"AVAILABLE".equals(status) && !"BOOKED".equals(status) && !"BLOCKED".equals(status) && !"PENDING".equals(status) && !"APPROVED".equals(status)) {
                        continue;
                    }

                    String start = s.child("start").getValue(String.class);
                    String end = s.child("end").getValue(String.class);

                    LiveStatusData item = new LiveStatusData(status, null, rawKey, dbDate);
                    item.startTime = start;
                    item.endTime = end;
                    // FIX: Sort by actual start time instead of rawKey
                    item.startTimeMinutes = repo.parseStartTime(start);

                    // Use normalized keys for deduplication to prevent "Old vs New" duplicates
                    String normKey = repo.normalizeSlotKey(rawKey);
                    if (deduplicated.containsKey(normKey)) {
                        LiveStatusData existing = deduplicated.get(normKey);
                        String existingStatus = (existing != null && existing.status != null) ? existing.status.toUpperCase() : "AVAILABLE";
                        String incomingStatus = status.toUpperCase();

                        boolean replace = false;
                        if ("AVAILABLE".equals(existingStatus) && !"AVAILABLE".equals(incomingStatus)) {
                            replace = true;
                        } else if (("BLOCKED".equals(existingStatus)) && "BOOKED".equals(incomingStatus)) {
                            replace = true;
                        }
                        if (replace) {
                            deduplicated.put(normKey, item);
                        }
                    } else {
                        deduplicated.put(normKey, item);
                    }
                }
            }

            slotList.clear();
            slotList.addAll(deduplicated.values());

            for (LiveStatusData item : slotList) {
                if ("BOOKED".equalsIgnoreCase(item.status)) {
                    repo.getBookingForSlot(spaceId, dbDate, item.slotKey, res -> {
                        if (res instanceof Result.Success) {
                            Booking booking = ((Result.Success<Booking>) res).data;
                            item.booking = booking;
                            if (booking != null && booking.getBookedBy() != null) {
                                repo.getUserName(booking.getBookedBy(), nameRes -> {
                                    if (nameRes instanceof Result.Success) {
                                        booking.setRequesterName(((Result.Success<String>) nameRes).data);
                                        runOnUiThread(() -> adapter.notifyDataSetChanged());
                                    }
                                });
                            }
                            runOnUiThread(() -> adapter.notifyDataSetChanged());
                        }
                    });
                }
            }

            java.util.Collections.sort(slotList, (a, b) -> Integer.compare(a.startTimeMinutes, b.startTimeMinutes));
            adapter.setPastDate(isPastDate);
            adapter.notifyDataSetChanged();
            noDataTextView.setVisibility(slotList.isEmpty() ? View.VISIBLE : View.GONE);
        }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scheduleListener != null && currentSelectedDate != null) {
            repo.removeScheduleListener(spaceId, currentSelectedDate, scheduleListener);
        }
    }

    private void updateHeader(String title, String subtitle) {
        TextView tvTitle = findViewById(R.id.header_title);
        TextView tvSubtitle = findViewById(R.id.header_subtitle);
        View btnBack = findViewById(R.id.btnBack);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(subtitle);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void updateSubtitle(Calendar cal) {
        String dateStr = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.getTime());
        String dayOfWeek = new SimpleDateFormat("EEEE", Locale.getDefault()).format(cal.getTime());
        String display = dateStr + " (" + dayOfWeek + ")";

        TextView tvSubtitle = findViewById(R.id.header_subtitle);
        if (tvSubtitle != null) tvSubtitle.setText(display);
        if (tvSelectedDate != null) tvSelectedDate.setText("Schedule for " + display);
        if (tvSelectedDateBottom != null) tvSelectedDateBottom.setText("Schedule for the date : " + display);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (selectedCalendar != null) {
            loadScheduleForDate(selectedCalendar);
        }
    }

    private void checkIfPastDate(String dateStr) {
        try {
            String todayStr = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
            isPastDate = dateStr.compareTo(todayStr) < 0;
        } catch (Exception e) {
            isPastDate = false;
        }
    }
}
