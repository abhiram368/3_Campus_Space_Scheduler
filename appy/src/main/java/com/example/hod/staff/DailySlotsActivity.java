package com.example.hod.staff;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.appy.R;
import com.example.hod.adapters.StaffScheduleAdapter;
import com.example.hod.models.Booking;
import com.example.hod.repository.FirebaseRepository;
import com.example.hod.utils.Result;
import com.google.firebase.database.DataSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DailySlotsActivity extends AppCompatActivity {

    private String labId;
    private String date;
    private RecyclerView rvScheduleSlots;
    private ProgressBar progressBar;
    private View emptyStateLayout;
    private FirebaseRepository repo;
    private List<StaffScheduleAdapter.SlotItem> slotList;
    private StaffScheduleAdapter adapter;

    private View selectionBar;
    private TextView tvSelectionCount;
    private boolean isPastDate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_slots);

        labId = getIntent().getStringExtra("labId");
        date = getIntent().getStringExtra("date");

        if (labId == null || date == null) {
            Toast.makeText(this, "Error: Invalid data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        checkIfPastDate();

        // Header
        View headerView = findViewById(R.id.header_layout);
        if (headerView != null) {
            TextView title = headerView.findViewById(R.id.header_title);
            TextView subtitle = headerView.findViewById(R.id.header_subtitle);
            View btnBack = headerView.findViewById(R.id.btnBack);
            
            if (title != null) title.setText("Loading...");
            if (subtitle != null) subtitle.setText(formatDisplayDate(date));
            if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        }

        repo = new FirebaseRepository();

        // Fetch Space Name for Header
        repo.getSpaceDetails(labId, result -> {
            if (result instanceof Result.Success) {
                com.example.hod.models.Space space = ((Result.Success<com.example.hod.models.Space>) result).data;
                if (space != null && space.getRoomName() != null) {
                    runOnUiThread(() -> {
                        TextView title = findViewById(R.id.header_title);
                        if (title != null) title.setText(space.getRoomName());
                    });
                } else {
                    runOnUiThread(() -> {
                        TextView title = findViewById(R.id.header_title);
                        if (title != null) title.setText("Lab Schedule");
                    });
                }
            }
        });

        rvScheduleSlots = findViewById(R.id.rvScheduleSlots);
        progressBar = findViewById(R.id.progressBar);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        selectionBar = findViewById(R.id.selectionBar);
        tvSelectionCount = findViewById(R.id.tvSelectionCount);

        View btnBulkBlock = findViewById(R.id.btnBulkBlock);
        View btnBulkUnblock = findViewById(R.id.btnBulkUnblock);
        View btnCancelSelection = findViewById(R.id.btnCancelSelection);
        slotList = new ArrayList<>();
        adapter = new StaffScheduleAdapter(this, slotList);
        adapter.setPastDate(isPastDate);
        rvScheduleSlots.setLayoutManager(new LinearLayoutManager(this));
        rvScheduleSlots.setAdapter(adapter);

        adapter.setSelectionListener(new StaffScheduleAdapter.SelectionListener() {
            @Override
            public void onSelectionModeChanged(boolean enabled) {
                if (isPastDate && enabled) {
                    Toast.makeText(DailySlotsActivity.this, "Cannot modify past schedules", Toast.LENGTH_SHORT).show();
                    adapter.exitSelectionMode();
                    return;
                }
                selectionBar.setVisibility(enabled ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onSelectionCountChanged(int count) {
                tvSelectionCount.setText(count + " slots selected");
            }
        });

        btnCancelSelection.setOnClickListener(v -> adapter.exitSelectionMode());
        btnBulkBlock.setOnClickListener(v -> {
            if (isPastDate) return;
            showBulkBlockDialog();
        });
        btnBulkUnblock.setOnClickListener(v -> {
            if (isPastDate) return;
            executeBulkUnblock();
        });

        fetchScheduleForDate(date);
    }

    private void checkIfPastDate() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            
            Calendar selected = Calendar.getInstance();
            selected.setTime(sdf.parse(date));
            
            isPastDate = selected.before(today);
        } catch (Exception e) {
            isPastDate = false;
        }
    }

    private String formatDisplayDate(String dateStr) {
        try {
            SimpleDateFormat from = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat to = new SimpleDateFormat("dd MMM yyyy (EEEE)", Locale.getDefault());
            return to.format(from.parse(dateStr));
        } catch (Exception e) {
            return dateStr;
        }
    }

    private void fetchScheduleForDate(String date) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        repo.observeSchedulesForLab(labId, date, result -> runOnUiThread(() -> {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            DataSnapshot snapshot = null;
            if (result instanceof Result.Success) snapshot = ((Result.Success<DataSnapshot>) result).data;

            List<StaffScheduleAdapter.SlotItem> tempSlots = new ArrayList<>();
            if (snapshot != null && snapshot.exists()) {
                for (DataSnapshot slotSnap : snapshot.getChildren()) {
                    String slotKey = slotSnap.getKey();
                    String status = slotSnap.child("status").getValue(String.class);
                    String start = slotSnap.child("start").getValue(String.class);
                    String end = slotSnap.child("end").getValue(String.class);
                    if (status == null || (!status.equalsIgnoreCase("BOOKED") && !status.equalsIgnoreCase("BLOCKED"))) {
                        status = "AVAILABLE";
                    }

                    StaffScheduleAdapter.SlotItem item = new StaffScheduleAdapter.SlotItem();
                    item.timeRange = (start != null && end != null) ? start + " – " + end : slotKey;
                    item.status = status;
                    item.spaceId = labId;
                    item.date = date;
                    item.slotKey = slotKey;
                    tempSlots.add(item);
                }
            }
            slotList.clear();
            slotList.addAll(tempSlots);
            adapter.notifyDataSetChanged();

            if (slotList.isEmpty()) {
                emptyStateLayout.setVisibility(View.VISIBLE);
                rvScheduleSlots.setVisibility(View.GONE);
            } else {
                emptyStateLayout.setVisibility(View.GONE);
                rvScheduleSlots.setVisibility(View.VISIBLE);
            }

            for (StaffScheduleAdapter.SlotItem item : slotList) {
                if ("BOOKED".equalsIgnoreCase(item.status)) {
                    repo.getBookingForSlot(labId, date, item.slotKey, res -> {
                        if (res instanceof Result.Success) {
                            item.booking = ((Result.Success<Booking>) res).data;
                            runOnUiThread(() -> adapter.notifyDataSetChanged());
                        }
                    });
                }
            }
        }));
    }

    private void showBulkBlockDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Reason (e.g., Maintenance)");
        
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Block Selected Slots")
            .setMessage("Enter a reason for blocking " + adapter.getSelectedSlots().size() + " slots.")
            .setView(input)
            .setPositiveButton("Block All", (dialog, which) -> {
                String reason = input.getText().toString().trim();
                if (reason.isEmpty()) {
                    Toast.makeText(this, "Reason is required", Toast.LENGTH_SHORT).show();
                } else {
                    executeBulkBlock(reason);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void executeBulkBlock(String reason) {
        java.util.Set<StaffScheduleAdapter.SlotItem> selected = new java.util.HashSet<>(adapter.getSelectedSlots());
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        int alreadyBlocked = 0;
        java.util.List<StaffScheduleAdapter.SlotItem> toBlock = new java.util.ArrayList<>();
        for (StaffScheduleAdapter.SlotItem item : selected) {
            if ("BLOCKED".equalsIgnoreCase(item.status)) alreadyBlocked++;
            else toBlock.add(item);
        }

        if (toBlock.isEmpty()) {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "All selected slots are already blocked", Toast.LENGTH_SHORT).show();
            adapter.exitSelectionMode();
            return;
        }

        final int numToBlock = toBlock.size();
        final int finalAlreadyBlocked = alreadyBlocked;
        final int[] completed = {0};

        for (StaffScheduleAdapter.SlotItem item : toBlock) {
            repo.blockSlot(item.spaceId, item.date, item.slotKey, reason, result -> {
                completed[0]++;
                if (completed[0] == numToBlock) {
                    runOnUiThread(() -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        String msg = numToBlock + " slots blocked successfully";
                        if (finalAlreadyBlocked > 0) msg += ", " + finalAlreadyBlocked + " were already blocked";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        adapter.exitSelectionMode();
                    });
                }
            });
        }
    }

    private void executeBulkUnblock() {
        java.util.Set<StaffScheduleAdapter.SlotItem> selected = new java.util.HashSet<>(adapter.getSelectedSlots());
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        int alreadyAvailable = 0;
        java.util.List<StaffScheduleAdapter.SlotItem> toUnblock = new java.util.ArrayList<>();
        for (StaffScheduleAdapter.SlotItem item : selected) {
            if ("AVAILABLE".equalsIgnoreCase(item.status)) alreadyAvailable++;
            else toUnblock.add(item);
        }

        if (toUnblock.isEmpty()) {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "All selected slots are already available", Toast.LENGTH_SHORT).show();
            adapter.exitSelectionMode();
            return;
        }

        final int numToUnblock = toUnblock.size();
        final int finalAlreadyAvailable = alreadyAvailable;
        final int[] completed = {0};

        for (StaffScheduleAdapter.SlotItem item : toUnblock) {
            repo.unblockSlot(item.spaceId, item.date, item.slotKey, result -> {
                completed[0]++;
                if (completed[0] == numToUnblock) {
                    runOnUiThread(() -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        String msg = numToUnblock + " slots unblocked successfully";
                        if (finalAlreadyAvailable > 0) msg += ", " + finalAlreadyAvailable + " were already available";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        adapter.exitSelectionMode();
                    });
                }
            });
        }
    }
}
