package com.example.hod.staff;

import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.content.Intent;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ViewScheduleActivity extends AppCompatActivity {

    private com.google.android.material.chip.ChipGroup dateChipGroup;
    private android.view.View calendarCard;
    private TextView tvSelectedDate;
    private RecyclerView rvScheduleSlots;
    private ProgressBar progressBar;
    private FirebaseRepository repo;
    private List<StaffScheduleAdapter.SlotItem> slotList;
    private StaffScheduleAdapter adapter;
    private View emptyStateLayout;
    private String currentSelectedDate;
    private String labId;
    private String roomName = "View Schedule";

    private View selectionBar;
    private TextView tvSelectionCount;

    private boolean isPastDate = false;

    private TextView tvDayStatus;
    private View calendarHint;
    private com.google.firebase.database.ValueEventListener dayStatusListener;
    private View bulkDayOptions;

    private long lastTapTime = 0;
    private String lastTapDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_schedule);

        // Header Configuration
        View headerView = findViewById(R.id.header_layout);
        if (headerView != null) {
            TextView title = headerView.findViewById(R.id.header_title);
            View btnBack = headerView.findViewById(R.id.btnBack);
            if (title != null) title.setText("Loading...");
            if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        }

        CalendarView calendarView = findViewById(R.id.calendarView);
        tvSelectedDate  = findViewById(R.id.tvSelectedDate);
        tvDayStatus     = findViewById(R.id.tvDayStatus);
        progressBar     = findViewById(R.id.progressBar);
        dateChipGroup   = findViewById(R.id.dateChipGroup);
        calendarCard    = findViewById(R.id.calendarCard);

        View btnUnblockFullDay = findViewById(R.id.btnUnblockFullDay);
        View btnBlockFullDay = findViewById(R.id.btnBlockFullDay);

        labId = getIntent().getStringExtra("labId");
        if (labId == null || labId.isEmpty()) {
            Toast.makeText(this, "Error: missing Lab ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        repo = new FirebaseRepository();

        // Fetch Space Name for Header
        repo.getSpaceDetails(labId, result -> {
            if (result instanceof Result.Success) {
                com.example.hod.models.Space space = ((Result.Success<com.example.hod.models.Space>) result).data;
                if (space != null && space.getRoomName() != null) {
                    roomName = space.getRoomName();
                    runOnUiThread(() -> {
                        TextView title = findViewById(R.id.header_title);
                        if (title != null) title.setText(roomName);
                    });
                } else {
                    runOnUiThread(() -> {
                        TextView title = findViewById(R.id.header_title);
                        if (title != null) title.setText("Lab Schedule");
                    });
                }
            }
        });

        btnUnblockFullDay.setOnClickListener(v -> {
            if (isPastDate) {
                Toast.makeText(this, "Cannot modify past schedules", Toast.LENGTH_SHORT).show();
                return;
            }
            executeBulkUnblockDay();
        });
        btnBlockFullDay.setOnClickListener(v -> {
            if (isPastDate) {
                Toast.makeText(this, "Cannot modify past schedules", Toast.LENGTH_SHORT).show();
                return;
            }
            showBulkBlockDayDialog();
        });

        bulkDayOptions = findViewById(R.id.bulkDayOptions);

        rvScheduleSlots = findViewById(R.id.rvScheduleSlots);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        selectionBar = findViewById(R.id.selectionBar);
        tvSelectionCount = findViewById(R.id.tvSelectionCount);

        View btnBulkBlock = findViewById(R.id.btnBulkBlock);
        View btnBulkUnblock = findViewById(R.id.btnBulkUnblock);
        View btnCancelSelection = findViewById(R.id.btnCancelSelection);

        slotList = new ArrayList<>();
        adapter = new StaffScheduleAdapter(this, slotList);
        rvScheduleSlots.setLayoutManager(new LinearLayoutManager(this));
        rvScheduleSlots.setAdapter(adapter);

        adapter.setSelectionListener(new StaffScheduleAdapter.SelectionListener() {
            @Override
            public void onSelectionModeChanged(boolean enabled) {
                if (isPastDate && enabled) {
                    Toast.makeText(ViewScheduleActivity.this, "Cannot modify past schedules", Toast.LENGTH_SHORT).show();
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
            showBulkSelectionBlockDialog();
        });
        btnBulkUnblock.setOnClickListener(v -> {
            if (isPastDate) return;
            executeBulkSelectionUnblock();
        });

        setupDateChips(calendarView);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String selected = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            long currentTime = System.currentTimeMillis();

            // Double Tap Detection
            if (selected.equals(lastTapDate) && (currentTime - lastTapTime) < 500) {
                Intent intent = new Intent(this, DailySlotsActivity.class);
                intent.putExtra("labId", labId);
                intent.putExtra("date", selected);
                startActivity(intent);
            }

            lastTapTime = currentTime;
            lastTapDate = selected;

            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            updateViewForDate(cal);
        });

        updateViewForDate(Calendar.getInstance());
    }

    private void updateViewForDate(Calendar cal) {
        String display   = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.getTime());
        String formatted = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        
        boolean isPickDate = (dateChipGroup != null && dateChipGroup.getCheckedChipId() == R.id.chipPickDate);
        if (isPickDate) {
            tvSelectedDate.setText("Scheduled on " + display);
        } else {
            tvSelectedDate.setText(display);
        }
        
        updateHeader(roomName, display);
        currentSelectedDate = formatted;
        
        checkIfPastDate(formatted);

        // Fetch day status to show as text since CalendarView doesn't support coloring
        fetchDayStatus(formatted);
    }

    private void fetchDayStatus(String date) {
        if (tvDayStatus != null) {
            tvDayStatus.setText("Status: Loading...");
            tvDayStatus.setTextColor(0xFF64748B);
        }
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        // Remove old listener if it exists
        if (dayStatusListener != null) {
            repo.removeScheduleListener(labId, currentSelectedDate, dayStatusListener);
        }

        dayStatusListener = repo.observeSchedulesForLab(labId, date, result -> runOnUiThread(() -> {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            DataSnapshot snapshot = null;
            if (result instanceof Result.Success) snapshot = ((Result.Success<DataSnapshot>) result).data;

            int availableCount = 0;
            int blockedCount = 0;
            boolean anyFound = false;

            List<StaffScheduleAdapter.SlotItem> tempSlots = new ArrayList<>();
            if (snapshot != null && snapshot.exists()) {
                anyFound = true;
                for (DataSnapshot s : snapshot.getChildren()) {
                    String status = s.child("status").getValue(String.class);
                    String start = s.child("start").getValue(String.class);
                    String end = s.child("end").getValue(String.class);
                    if (status == null || (!status.equalsIgnoreCase("BOOKED") && !status.equalsIgnoreCase("BLOCKED"))) {
                        status = "AVAILABLE";
                    }

                    boolean isPastSlot = isPastDate || isSlotInPast(date, s.getKey());
                    
                    if ("AVAILABLE".equalsIgnoreCase(status)) {
                        if (!isPastSlot) availableCount++;
                    } else if ("BLOCKED".equalsIgnoreCase(status)) {
                        blockedCount++;
                    }

                    StaffScheduleAdapter.SlotItem item = new StaffScheduleAdapter.SlotItem();
                    item.timeRange = (start != null && end != null) ? start + " – " + end : s.getKey();
                    item.status = status;
                    item.spaceId = labId;
                    item.date = date;
                    item.slotKey = s.getKey();
                    tempSlots.add(item);
                }
            }

            slotList.clear();
            slotList.addAll(tempSlots);
            adapter.setPastDate(isPastDate);
            adapter.notifyDataSetChanged();

            boolean isPickDate = (dateChipGroup != null && dateChipGroup.getCheckedChipId() == R.id.chipPickDate);
            
            if (isPickDate) {
                if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.GONE);
                if (rvScheduleSlots != null) rvScheduleSlots.setVisibility(View.GONE);
            } else if (!slotList.isEmpty()) {
                if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.GONE);
                if (rvScheduleSlots != null) rvScheduleSlots.setVisibility(View.VISIBLE);
            } else {
                if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.VISIBLE);
                if (rvScheduleSlots != null) rvScheduleSlots.setVisibility(View.GONE);
            }

            // Update Summary Text
            String statusText;
            int color;
            View btnUnblock = findViewById(R.id.btnUnblockFullDay);
            View btnBlock = findViewById(R.id.btnBlockFullDay);
            
            if (bulkDayOptions != null) {
                bulkDayOptions.setVisibility(isPastDate ? View.GONE : View.VISIBLE);
            }

            if (!anyFound) {
                statusText = "Status: Not Initialized";
                color = 0xFF64748B;
                if (btnUnblock != null) btnUnblock.setEnabled(false);
                if (btnBlock != null) btnBlock.setEnabled(false);
            } else {

                boolean isAllBlocked = (anyFound && blockedCount == slotList.size() && slotList.size() > 0);
                if (btnUnblock != null) {
                    btnUnblock.setVisibility(isAllBlocked && !isPastDate ? View.VISIBLE : View.GONE);
                }
                if (btnBlock != null) {
                    btnBlock.setVisibility(!isPastDate && availableCount > 0 ? View.VISIBLE : View.GONE);
                }

                if (availableCount > 0) {
                    if (isPastDate) {
                        statusText = "Status: " + availableCount + " Unused Slots";
                        color = 0xFF64748B;
                    } else {
                        statusText = "Status: " + availableCount + " Slots Available";
                        color = 0xFF10B981;
                    }
                } else if (blockedCount > 0) {
                    statusText = "Status: FULLY BLOCKED";
                    color = 0xFFEF4444;
                } else {
                    statusText = "Status: No Slots Found";
                    color = 0xFF64748B;
                }
            }

            if (tvDayStatus != null) {
                tvDayStatus.setText(statusText);
                tvDayStatus.setTextColor(color);
            }

            // Fetch Bookings for Booked Slots
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

    private void showBulkSelectionBlockDialog() {
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
                    executeBulkSelectionBlock(reason);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void executeBulkSelectionBlock(String reason) {
        java.util.Set<StaffScheduleAdapter.SlotItem> selected = new java.util.HashSet<>(adapter.getSelectedSlots());
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        java.util.List<StaffScheduleAdapter.SlotItem> toBlock = new java.util.ArrayList<>();
        for (StaffScheduleAdapter.SlotItem item : selected) {
            if (isPastDate || isSlotInPast(item.date, item.slotKey)) continue;
            if (!"BLOCKED".equalsIgnoreCase(item.status)) toBlock.add(item);
        }

        if (toBlock.isEmpty()) {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            adapter.exitSelectionMode();
            return;
        }

        final int numToBlock = toBlock.size();
        final int[] completed = {0};

        for (StaffScheduleAdapter.SlotItem item : toBlock) {
            repo.blockSlot(item.spaceId, item.date, item.slotKey, reason, result -> {
                completed[0]++;
                if (completed[0] == numToBlock) {
                    runOnUiThread(() -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, numToBlock + " slots blocked successfully", Toast.LENGTH_SHORT).show();
                        adapter.exitSelectionMode();
                    });
                }
            });
        }
    }

    private void executeBulkSelectionUnblock() {
        java.util.Set<StaffScheduleAdapter.SlotItem> selected = new java.util.HashSet<>(adapter.getSelectedSlots());
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        java.util.List<StaffScheduleAdapter.SlotItem> toUnblock = new java.util.ArrayList<>();
        for (StaffScheduleAdapter.SlotItem item : selected) {
            if (isPastDate || isSlotInPast(item.date, item.slotKey)) continue;
            if (!"AVAILABLE".equalsIgnoreCase(item.status)) toUnblock.add(item);
        }

        if (toUnblock.isEmpty()) {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            adapter.exitSelectionMode();
            return;
        }

        final int numToUnblock = toUnblock.size();
        final int[] completed = {0};

        for (StaffScheduleAdapter.SlotItem item : toUnblock) {
            repo.unblockSlot(item.spaceId, item.date, item.slotKey, result -> {
                completed[0]++;
                if (completed[0] == numToUnblock) {
                    runOnUiThread(() -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, numToUnblock + " slots unblocked successfully", Toast.LENGTH_SHORT).show();
                        adapter.exitSelectionMode();
                    });
                }
            });
        }
    }

    private void checkIfPastDate(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            
            Calendar selected = Calendar.getInstance();
            selected.setTime(sdf.parse(dateStr));

            isPastDate = selected.before(today);
        } catch (Exception e) {
            isPastDate = false;
        }
    }

    private boolean isSlotInPast(String dateStr, String slotKey) {
        if (dateStr == null || slotKey == null) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            java.util.Date slotDate = sdf.parse(dateStr);
            Calendar slotCal = Calendar.getInstance();
            slotCal.setTime(slotDate);
            
            String digits = slotKey.replaceAll("[^0-9]", "");
            if (digits.length() >= 4) {
               int hour = Integer.parseInt(digits.substring(0, 2));
               int min = Integer.parseInt(digits.substring(2, 4));
               slotCal.set(Calendar.HOUR_OF_DAY, hour);
               slotCal.set(Calendar.MINUTE, min);
            } else {
               return isPastDate;
            }
            return slotCal.getTime().before(new java.util.Date());
        } catch (Exception e) {
            return isPastDate;
        }
    }

    private void updateHeader(String title, String subtitle) {
        TextView tvTitle = findViewById(R.id.header_title);
        TextView tvSubtitle = findViewById(R.id.header_subtitle);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(subtitle);
    }

    private void showBulkBlockDayDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Reason for blocking full day");

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Block Entire Day")
            .setMessage("All slots for " + currentSelectedDate + " will be blocked.")
            .setView(input)
            .setPositiveButton("Block All", (dialog, which) -> {
                String reason = input.getText().toString().trim();
                executeBulkBlockDay(reason.isEmpty() ? "Lab Closed" : reason);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void executeBulkBlockDay(String reason) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        repo.initializeBookingSlots(labId, currentSelectedDate, res -> {
            repo.getSchedulesForLab(labId, currentSelectedDate, slotsRes -> {
                if (slotsRes instanceof Result.Success) {
                    DataSnapshot snapshot = ((Result.Success<DataSnapshot>) slotsRes).data;
                    Map<String, String> slotsToUpdate = new HashMap<>();
                    int alreadyBlockedCount = 0;

                    if (snapshot.exists()) {
                        for (DataSnapshot s : snapshot.getChildren()) {
                            String key = s.getKey();
                            if (isSlotInPast(currentSelectedDate, key) || isPastDate) continue;

                            String status = s.child("status").getValue(String.class);
                            if ("BLOCKED".equalsIgnoreCase(status)) {
                                alreadyBlockedCount++;
                            } else {
                                slotsToUpdate.put(key, status);
                            }
                        }
                    }

                    if (slotsToUpdate.isEmpty()) {
                        runOnUiThread(() -> {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "All slots are already blocked", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    final int total = slotsToUpdate.size();
                    final int[] completed = {0};
                    final int finalAlreadyBlocked = alreadyBlockedCount;

                    for (String key : slotsToUpdate.keySet()) {
                        repo.blockSlot(labId, currentSelectedDate, key, reason, r -> {
                            completed[0]++;
                            if (completed[0] == total) {
                                runOnUiThread(() -> {
                                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                                    String msg = total + " slots blocked";
                                    if (finalAlreadyBlocked > 0) msg += ", " + finalAlreadyBlocked + " already blocked";
                                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
                    }
                }
            });
        });
    }

    private void executeBulkUnblockDay() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        repo.getSchedulesForLab(labId, currentSelectedDate, slotsRes -> {
            if (slotsRes instanceof Result.Success) {
                DataSnapshot snapshot = ((Result.Success<DataSnapshot>) slotsRes).data;
                List<String> keysToUnblock = new ArrayList<>();
                int alreadyAvailableCount = 0;

                if (snapshot.exists()) {
                    for (DataSnapshot s : snapshot.getChildren()) {
                        String key = s.getKey();
                        if (isSlotInPast(currentSelectedDate, key) || isPastDate) continue;

                        String status = s.child("status").getValue(String.class);
                        if ("AVAILABLE".equalsIgnoreCase(status)) {
                            alreadyAvailableCount++;
                        } else {
                            keysToUnblock.add(key);
                        }
                    }
                }

                if (keysToUnblock.isEmpty()) {
                    runOnUiThread(() -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "All slots are already available", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                final int total = keysToUnblock.size();
                final int[] completed = {0};
                final int finalAvailable = alreadyAvailableCount;

                for (String key : keysToUnblock) {
                    repo.unblockSlot(labId, currentSelectedDate, key, r -> {
                        completed[0]++;
                        if (completed[0] == total) {
                            runOnUiThread(() -> {
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                                String msg = total + " slots unblocked";
                                if (finalAvailable > 0) msg += ", " + finalAvailable + " already available";
                                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                }
            }
        });
    }



    private void setupDateChips(CalendarView calendarView) {
        calendarHint = findViewById(R.id.bottomNote);

        dateChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            Calendar cal = Calendar.getInstance();
            
            if (id == R.id.chipToday) {
                calendarCard.setVisibility(View.GONE);
                if (calendarHint != null) calendarHint.setVisibility(View.GONE);
                updateViewForDate(cal);
            } else if (id == R.id.chipTomorrow) {
                calendarCard.setVisibility(View.GONE);
                if (calendarHint != null) calendarHint.setVisibility(View.GONE);
                cal.add(Calendar.DAY_OF_YEAR, 1);
                updateViewForDate(cal);
            } else if (id == R.id.chipPickDate) {
                calendarCard.setVisibility(View.VISIBLE);
                if (calendarHint != null) {
                    calendarHint.setVisibility(View.VISIBLE);
                    if (calendarHint instanceof TextView) {
                        ((TextView) calendarHint).setText("💡 Double-tap a date to view individual slots");
                    }
                }
                updateViewForDate(cal);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dayStatusListener != null) {
            repo.removeScheduleListener(labId, currentSelectedDate, dayStatusListener);
        }
    }
}
