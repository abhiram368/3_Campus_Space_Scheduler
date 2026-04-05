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

import android.widget.ImageButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.hod.R;
import com.example.hod.adapters.StaffScheduleAdapter;
import com.example.hod.models.Booking;
import com.example.hod.repository.FirebaseRepository;
import com.example.hod.utils.Result;
import com.google.android.material.button.MaterialButton;
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
            ImageButton btnSync = headerView.findViewById(R.id.btnAction);
            
            if (title != null) title.setText("Loading...");
            if (btnBack != null) btnBack.setOnClickListener(v -> finish());
            
            if (btnSync != null) {
                btnSync.setVisibility(View.GONE);
            }
        }

        CalendarView calendarView = findViewById(R.id.calendarView);
        tvSelectedDate  = findViewById(R.id.tvSelectedDate);
        tvDayStatus     = findViewById(R.id.tvDayStatus);
        progressBar     = findViewById(R.id.progressBar);
        dateChipGroup   = findViewById(R.id.dateChipGroup);
        calendarCard    = findViewById(R.id.calendarCard);

        View btnUnblockFullDay = findViewById(R.id.btnUnblockFullDay);
        MaterialButton btnBlockFullDay = findViewById(R.id.btnBlockFullDay);

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
            showBulkUnblockDayDialog();
        });
        btnBlockFullDay.setOnClickListener(v -> showBulkBlockDayDialog());

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
            showBulkUnblockDialog();
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

        dayStatusListener = repo.observeSchedulesForLab(labId, date, result -> {
            runOnUiThread(() -> {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                DataSnapshot snapshot = null;
                if (result instanceof Result.Success) snapshot = ((Result.Success<DataSnapshot>) result).data;
                final int[] availableCount = {0};
                final int[] blockedCount = {0};
                final int[] bookedCount = {0};
                final int[] unusedCount = {0};
                boolean anyFound = snapshot != null && snapshot.exists();

                Map<String, StaffScheduleAdapter.SlotItem> deduplicated = new HashMap<>();
                if (anyFound) {
                    for (DataSnapshot s : snapshot.getChildren()) {
                        String rawKey = s.getKey();
                        if (rawKey == null) continue;

                        String status = s.child("status").getValue(String.class);
                        if (status == null) {
                            Object val = s.getValue();
                            status = (val instanceof Boolean && (Boolean) val) ? "BOOKED" : "AVAILABLE";
                        }
                        status = repo.normalizeSlotStatus(status);
                        
                        if (!"AVAILABLE".equalsIgnoreCase(status) && 
                            !"BOOKED".equalsIgnoreCase(status) && 
                            !"BLOCKED".equalsIgnoreCase(status) &&
                            !"USED".equalsIgnoreCase(status) &&
                            !"COMPLETED".equalsIgnoreCase(status)) {
                            continue;
                        }

                        String formattedKey = repo.formatSlotKey(rawKey);
                        String start = s.child("start").getValue(String.class);
                        String end = s.child("end").getValue(String.class);

                        StaffScheduleAdapter.SlotItem item = new StaffScheduleAdapter.SlotItem();
                        item.timeRange = (start != null && end != null) ? start + " – " + end : formattedKey;
                        item.status = status;
                        item.spaceId = labId;
                        item.date = date;
                        item.slotKey = rawKey; 
                        item.startTimeMinutes = repo.parseStartTime(rawKey);
                        item.adminName = s.child("adminName").getValue(String.class);

                        // PRE-POPULATE BOOKING if available in slot node
                        if (s.child("bookedBy").exists()) {
                            Booking b = new Booking();
                            b.setBookedBy(s.child("bookedBy").getValue(String.class));
                            b.setRequesterName(s.child("requesterName").getValue(String.class));
                            b.setPurpose(s.child("purpose").getValue(String.class));
                            b.setStatus(status);
                            item.booking = b;
                        }

                        if (deduplicated.containsKey(formattedKey)) {
                            StaffScheduleAdapter.SlotItem existing = deduplicated.get(formattedKey);
                            String currentStat = (status != null) ? status.toUpperCase() : "AVAILABLE";
                            String existingStat = (existing != null && existing.status != null) ? existing.status.toUpperCase() : "AVAILABLE";
                            if ("AVAILABLE".equals(existingStat) && !"AVAILABLE".equals(currentStat)) {
                                deduplicated.put(formattedKey, item);
                            }
                        } else {
                            deduplicated.put(formattedKey, item);
                        }
                    }
                }

                slotList.clear();
                slotList.addAll(deduplicated.values());

                for (StaffScheduleAdapter.SlotItem item : slotList) {
                    String sStat = (item.status != null) ? item.status.toUpperCase() : "AVAILABLE";
                    boolean isPastSlot = isPastDate || isSlotInPast(date, item.slotKey);

                    if ("AVAILABLE".equalsIgnoreCase(sStat)) {
                        if (isPastSlot) unusedCount[0]++;
                        else availableCount[0]++;
                    } else if ("BLOCKED".equalsIgnoreCase(sStat)) {
                        blockedCount[0]++;
                    } else if ("BOOKED".equalsIgnoreCase(sStat) || "USED".equalsIgnoreCase(sStat) || "COMPLETED".equalsIgnoreCase(sStat) || "APPROVED".equalsIgnoreCase(sStat)) {
                        bookedCount[0]++;
                    }

                    if ("BOOKED".equals(sStat) || "USED".equals(sStat) || "COMPLETED".equals(sStat) || "APPROVED".equals(sStat)) {
                        repo.getBookingForSlot(labId, date, item.slotKey, res -> {
                            if (res instanceof Result.Success) {
                                Booking b = ((Result.Success<Booking>) res).data;
                                
                                if (b != null) { // FIX: Check if booking actually exists
                                    item.booking = b;
                                    if (b.getBookedBy() != null) {
                                        String bBy = b.getBookedBy();
                                        repo.getUserName(bBy, nameRes -> {
                                            if (nameRes instanceof Result.Success) {
                                                String name = ((Result.Success<String>) nameRes).data;
                                                if (item.booking != null) {
                                                    item.booking.setRequesterName(name);
                                                }
                                                runOnUiThread(() -> adapter.notifyDataSetChanged());
                                            }
                                        });
                                    }
                                    runOnUiThread(() -> adapter.notifyDataSetChanged());
                                } else if (item.booking == null) {
                                    // FIX: Handle orphaned slots so it stops saying "Loading details..."
                                    Booking orphan = new Booking();
                                    orphan.setRequesterName("Unknown User");
                                    orphan.setPurpose("Details missing from database");
                                    item.booking = orphan;
                                    runOnUiThread(() -> adapter.notifyDataSetChanged());
                                }
                            }
                        });
                    }
                }
                
                java.util.Collections.sort(slotList, (a, b) -> {
                    if (a.startTimeMinutes != b.startTimeMinutes) {
                        return Integer.compare(a.startTimeMinutes, b.startTimeMinutes);
                    }
                    return a.timeRange.compareTo(b.timeRange);
                });
                
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

                String statusText;
                int color;
                View btnUnblock = findViewById(R.id.btnUnblockFullDay);
                View btnBlock = findViewById(R.id.btnBlockFullDay);
                
                SimpleDateFormat daySdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String todayStr = daySdf.format(new java.util.Date());
                boolean isToday = todayStr.equals(date);

                if (!anyFound) {
                    statusText = "Status: Not Initialized";
                    color = 0xFF64748B;
                    if (bulkDayOptions != null) bulkDayOptions.setVisibility(isPastDate ? View.GONE : View.VISIBLE);
                    if (btnUnblock != null) btnUnblock.setEnabled(false);
                    if (btnBlock != null) btnBlock.setEnabled(false);
                } else {
                    if (bulkDayOptions != null) {
                        bulkDayOptions.setVisibility(isPastDate ? View.GONE : View.VISIBLE);
                    }

                    if (btnUnblock != null) {
                        btnUnblock.setVisibility(blockedCount[0] > 0 && !isPastDate ? View.VISIBLE : View.GONE);
                    }
                    if (btnBlock != null) {
                        btnBlock.setVisibility(!isPastDate && availableCount[0] > 0 ? View.VISIBLE : View.GONE);
                    }
                    
                    if (availableCount[0] > 0) {
                        statusText = "Status: " + availableCount[0] + " Slots Available";
                        color = 0xFF10B981;
                    } else {
                        if (isToday || isPastDate) {
                            if (bookedCount[0] > 0 || blockedCount[0] > 0 || unusedCount[0] > 0) {
                                StringBuilder sb = new StringBuilder("Status: ");
                                if (unusedCount[0] > 0) sb.append(unusedCount[0]).append(" Unused");
                                if (bookedCount[0] > 0) {
                                    if (unusedCount[0] > 0) sb.append(", ");
                                    sb.append(bookedCount[0]).append(" Booked");
                                }
                                if (blockedCount[0] > 0) {
                                    if (unusedCount[0] > 0 || bookedCount[0] > 0) sb.append(", ");
                                    sb.append(blockedCount[0]).append(" Blocked");
                                }
                                statusText = sb.toString();
                                color = (bookedCount[0] > 0) ? 0xFF3B82F6 : 0xFF64748B;
                            } else {
                                statusText = "Status: No Slots Found";
                                color = 0xFF64748B;
                            }
                        } else {
                            if (blockedCount[0] == slotList.size()) {
                                statusText = "Status: FULLY BLOCKED";
                                color = 0xFFEF4444;
                            } else {
                                statusText = "Status: FULLY BOOKED";
                                color = 0xFF3B82F6;
                            }
                        }
                    }
                }

                if (tvDayStatus != null) {
                    tvDayStatus.setText(statusText);
                    tvDayStatus.setTextColor(color);
                }
            });
        });
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

    private void showBulkUnblockDialog() {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.layout_cancel_booking_dialog, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextView tvSubtitle = dialogView.findViewById(R.id.tv_dialog_subtitle);
        android.widget.EditText etRemark = dialogView.findViewById(R.id.et_cancel_remark);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        android.widget.Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);

        if (tvTitle != null) tvTitle.setText("Unblock Selected Slots");
        if (tvSubtitle != null) tvSubtitle.setText("Are you sure you want to unblock " + adapter.getSelectedSlots().size() + " slots?");
        if (etRemark != null) etRemark.setVisibility(android.view.View.GONE);
        if (btnConfirm != null) {
            btnConfirm.setText("Unblock Slots");
            btnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.status_approved)));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            executeBulkUnblock();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void executeBulkUnblock() {
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
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.layout_cancel_booking_dialog, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextView tvSubtitle = dialogView.findViewById(R.id.tv_dialog_subtitle);
        android.widget.EditText etRemark = dialogView.findViewById(R.id.et_cancel_remark);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        android.widget.Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);

        if (tvTitle != null) tvTitle.setText("Block Entire Day");
        if (tvSubtitle != null) tvSubtitle.setText("All " + slotList.size() + " slots for " + currentSelectedDate + " will be blocked.");
        if (etRemark != null) etRemark.setHint("Reason for blocking full day...");
        if (btnConfirm != null) {
            btnConfirm.setText("Block All");
            btnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.status_rejected)));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String reason = etRemark.getText().toString().trim();
            if (reason.isEmpty()) {
                etRemark.setError("Reason is required to block the entire day");
            } else {
                executeBulkBlockDay(reason);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void executeBulkBlockDay(String reason) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        // Fix: Fetch EXISTING slots instead of re-initializing from template
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
                        if (snapshot.exists()) {
                            Toast.makeText(this, "All slots are already blocked", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "No slots initialized for this day. Double-tap the date to add slots.", Toast.LENGTH_LONG).show();
                        }
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
            } else {
                runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to fetch slots", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showBulkUnblockDayDialog() {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.layout_cancel_booking_dialog, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextView tvSubtitle = dialogView.findViewById(R.id.tv_dialog_subtitle);
        android.widget.EditText etRemark = dialogView.findViewById(R.id.et_cancel_remark);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        android.widget.Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);

        if (tvTitle != null) tvTitle.setText("Unblock Entire Day");
        if (tvSubtitle != null) tvSubtitle.setText("Are you sure you want to unblock all slots for " + currentSelectedDate + "?");
        if (etRemark != null) etRemark.setVisibility(android.view.View.GONE);
        if (btnConfirm != null) {
            btnConfirm.setText("Unblock All");
            btnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.status_approved)));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            executeBulkUnblockDay();
            dialog.dismiss();
        });

        dialog.show();
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


    private void performManualSync() {
        if (labId == null || labId.isEmpty()) return;

        android.app.ProgressDialog progress = new android.app.ProgressDialog(this);
        progress.setMessage("Initializing 365-day schedule...");
        progress.setCancelable(false);
        progress.show();

        repo.getSpaceDetails(labId, res -> {
            String name = roomName;
            if (res instanceof Result.Success) {
                com.example.hod.models.Space space = ((Result.Success<com.example.hod.models.Space>) res).data;
                if (space != null && space.getRoomName() != null) name = space.getRoomName();
            }
            
            final String finalName = name;
            repo.generateWeeklySchedule(finalName, labId, true, result -> {
                runOnUiThread(() -> {
                    progress.dismiss();
                    if (result instanceof Result.Success) {
                        Toast.makeText(this, "Schedule initialized for 365 days", Toast.LENGTH_LONG).show();
                        fetchDayStatus(currentSelectedDate); // Refresh
                    } else {
                        Toast.makeText(this, "Sync failed", Toast.LENGTH_SHORT).show();
                    }
                });
            });
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
