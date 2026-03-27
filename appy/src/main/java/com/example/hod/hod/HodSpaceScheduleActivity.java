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

import com.campussync.appy.R;
import com.example.hod.adapters.HodScheduleAdapter;
import com.example.hod.models.Booking;
import com.example.hod.models.LiveStatusData;
import com.example.hod.repository.FirebaseRepository;
import com.example.hod.utils.Result;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HodSpaceScheduleActivity extends AppCompatActivity {

    private String spaceId;
    private String spaceName;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hod_space_schedule);

        spaceId = getIntent().getStringExtra("labId");
        spaceName = getIntent().getStringExtra("roomName");

        if (spaceId == null) {
            Toast.makeText(this, "Missing Space ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Header Setup
        updateHeader(spaceName != null ? spaceName : "Space Schedule", "Loading...");

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
        adapter = new HodScheduleAdapter(this, slotList, spaceName);
        rvSchedule.setAdapter(adapter);

        repo = new FirebaseRepository();
        selectedCalendar = Calendar.getInstance();

        setupChips();
        setupCalendar();

        // Initial Load (Today)
        loadScheduleForDate(selectedCalendar);
    }

    private void setupChips() {
        dateChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            
            int checkedId = checkedIds.get(0);
            calendarCard.setVisibility(View.GONE);
            
            if (checkedId == R.id.chipToday) {
                selectedCalendar = Calendar.getInstance();
                loadScheduleForDate(selectedCalendar);
                rvSchedule.setVisibility(View.VISIBLE);
                tvSelectedDate.setVisibility(View.VISIBLE);
                tvCalendarNote.setVisibility(View.GONE);
                tvSelectedDateBottom.setVisibility(View.GONE);
            } else if (checkedId == R.id.chipTomorrow) {
                selectedCalendar = Calendar.getInstance();
                selectedCalendar.add(Calendar.DAY_OF_YEAR, 1);
                loadScheduleForDate(selectedCalendar);
                rvSchedule.setVisibility(View.VISIBLE);
                tvSelectedDate.setVisibility(View.VISIBLE);
                tvCalendarNote.setVisibility(View.GONE);
                tvSelectedDateBottom.setVisibility(View.GONE);
            } else if (checkedId == R.id.chipPickDate) {
                calendarCard.setVisibility(View.VISIBLE);
                rvSchedule.setVisibility(View.GONE);
                tvSelectedDate.setVisibility(View.GONE);
                tvCalendarNote.setVisibility(View.VISIBLE);
                tvSelectedDateBottom.setVisibility(View.GONE);
            }
        });
    }

    private void setupCalendar() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedCalendar.set(year, month, dayOfMonth);
            loadScheduleForDate(selectedCalendar);
            calendarCard.setVisibility(View.GONE);
            rvSchedule.setVisibility(View.VISIBLE);
            tvSelectedDate.setVisibility(View.GONE); // Keep top header hidden
            tvCalendarNote.setVisibility(View.VISIBLE);
            tvSelectedDateBottom.setVisibility(View.VISIBLE);
            updateSubtitle(selectedCalendar);
        });
    }

    private void loadScheduleForDate(Calendar cal) {
        String dbDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        updateSubtitle(cal);
        
        slotList.clear();
        adapter.notifyDataSetChanged();
        progressBar.setVisibility(View.VISIBLE);
        noDataTextView.setVisibility(View.GONE);

        repo.getSchedulesForLab(spaceId, dbDate, result -> {
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (result instanceof Result.Success) {
                    DataSnapshot snapshot = ((Result.Success<DataSnapshot>) result).data;
                    if (snapshot != null && snapshot.exists()) {
                        List<LiveStatusData> tempSlots = new ArrayList<>();
                        for (DataSnapshot slotSnap : snapshot.getChildren()) {
                            String slotKey = slotSnap.getKey();
                            String status = slotSnap.child("status").getValue(String.class);
                            String start = slotSnap.child("start").getValue(String.class);
                            String end = slotSnap.child("end").getValue(String.class);
                            
                            LiveStatusData data = new LiveStatusData(status != null ? status : "AVAILABLE", null, slotKey, dbDate);
                            data.startTime = start;
                            data.endTime = end;
                            tempSlots.add(data);
                            
                            if ("BOOKED".equalsIgnoreCase(status)) {
                                fetchBookingDetails(data, slotKey, dbDate);
                            }
                        }
                        slotList.clear();
                        slotList.addAll(tempSlots);
                        adapter.notifyDataSetChanged();
                        noDataTextView.setVisibility(tempSlots.isEmpty() ? View.VISIBLE : View.GONE);
                    } else {
                        noDataTextView.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(this, "Error fetching schedule", Toast.LENGTH_SHORT).show();
                    noDataTextView.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void fetchBookingDetails(LiveStatusData data, String slotKey, String date) {
        repo.getBookingForSlot(spaceId, date, slotKey, result -> {
            if (result instanceof Result.Success) {
                Booking b = ((Result.Success<Booking>) result).data;
                if (b != null) {
                    data.booking = b;
                    runOnUiThread(() -> adapter.notifyDataSetChanged());
                }
            }
        });
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
}

