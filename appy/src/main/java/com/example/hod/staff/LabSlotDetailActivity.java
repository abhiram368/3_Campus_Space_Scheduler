package com.example.hod.staff;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campussync.appy.R;
import com.example.hod.adapters.StaffScheduleAdapter;
import com.example.hod.repository.FirebaseRepository;
import com.example.hod.utils.Result;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class LabSlotDetailActivity extends AppCompatActivity {

    private TextView tvDetailTime, tvDetailDate, tvDetailStatus, tvDetailBookedBy, tvDetailPurpose, tvBlockReason;
    private View bookerSection, blockSection;
    private MaterialButton btnBlockSlot, btnUnblockSlot;
    private ProgressBar progressBar;
    private FirebaseRepository repo;
    private StaffScheduleAdapter.SlotItem slot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab_slot_detail);

        repo = new FirebaseRepository();
        slot = (StaffScheduleAdapter.SlotItem) getIntent().getSerializableExtra("slotItem");

        if (slot == null) {
            Toast.makeText(this, "Error: Slot data missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupHeader();
        fetchSpaceName();
        initViews();
        displaySlotData();
    }

    private void setupHeader() {
        View headerView = findViewById(R.id.header_layout);
        if (headerView != null) {
            TextView title = headerView.findViewById(R.id.header_title);
            TextView subtitle = headerView.findViewById(R.id.header_subtitle);
            View btnBack = headerView.findViewById(R.id.btnBack);
            
            if (title != null) title.setText("Slot Management");
            if (subtitle != null) subtitle.setText(slot.spaceId + " • " + slot.date);
            if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        }
    }

    private void initViews() {
        tvDetailTime     = findViewById(R.id.tvDetailTime);
        tvDetailDate     = findViewById(R.id.tvDetailDate);
        tvDetailStatus   = findViewById(R.id.tvDetailStatus);
        tvDetailBookedBy = findViewById(R.id.tvDetailBookedBy);
        tvDetailPurpose  = findViewById(R.id.tvDetailPurpose);
        tvBlockReason    = findViewById(R.id.tvBlockReason);
        bookerSection    = findViewById(R.id.bookerSection);
        blockSection     = findViewById(R.id.blockSection);
        btnBlockSlot     = findViewById(R.id.btnBlockSlot);
        btnUnblockSlot   = findViewById(R.id.btnUnblockSlot);
        progressBar      = findViewById(R.id.progressBar);

        btnBlockSlot.setOnClickListener(v -> showBlockDialog());
        btnUnblockSlot.setOnClickListener(v -> unblockSlot());
    }

    private void displaySlotData() {
        tvDetailTime.setText(slot.timeRange);
        tvDetailDate.setText(slot.date);
        tvDetailStatus.setText(slot.status.toUpperCase());
        
        applyStatusBadge(slot.status);

        // Check if past date
        boolean isPast = false;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Calendar today = java.util.Calendar.getInstance();
            today.set(java.util.Calendar.HOUR_OF_DAY, 0);
            today.set(java.util.Calendar.MINUTE, 0);
            today.set(java.util.Calendar.SECOND, 0);
            today.set(java.util.Calendar.MILLISECOND, 0);
            
            java.util.Calendar selected = java.util.Calendar.getInstance();
            selected.setTime(sdf.parse(slot.date));
            isPast = selected.before(today);
        } catch (Exception ignored) {}

        if ("BOOKED".equalsIgnoreCase(slot.status) && slot.booking != null) {
            bookerSection.setVisibility(View.VISIBLE);
            tvDetailBookedBy.setText("Booked by: " + (slot.booking.getRequesterName() != null ? slot.booking.getRequesterName() : slot.booking.getBookedBy()));
            tvDetailPurpose.setText("Purpose: " + (slot.booking.getPurpose() != null ? slot.booking.getPurpose() : "N/A"));
        } else {
            bookerSection.setVisibility(View.GONE);
        }

        if (isPast) {
            // Past date (before today) — completed, no modifications
            btnBlockSlot.setVisibility(View.GONE);
            btnUnblockSlot.setVisibility(View.GONE);
            blockSection.setVisibility("BLOCKED".equalsIgnoreCase(slot.status) ? View.VISIBLE : View.GONE);
            if ("AVAILABLE".equalsIgnoreCase(slot.status)) {
                tvDetailStatus.setText("UNUSED");
                tvDetailStatus.setBackgroundResource(R.drawable.badge_orange);
            } else if ("BLOCKED".equalsIgnoreCase(slot.status)) {
                tvBlockReason.setText("Reason: Slot was blocked");
            }
        } else {
            // Today or future date — check if the specific slot time has passed
            boolean isSlotTimePast = isCurrentSlotInPast(slot.slotKey);

            if (isSlotTimePast) {
                // Slot has already completed today — no block option
                btnBlockSlot.setVisibility(View.GONE);
                btnUnblockSlot.setVisibility(View.GONE);
                blockSection.setVisibility("BLOCKED".equalsIgnoreCase(slot.status) ? View.VISIBLE : View.GONE);
                if ("AVAILABLE".equalsIgnoreCase(slot.status)) {
                    tvDetailStatus.setText("UNUSED");
                    tvDetailStatus.setBackgroundResource(R.drawable.badge_orange);
                } else if ("BLOCKED".equalsIgnoreCase(slot.status)) {
                    tvBlockReason.setText("Reason: Slot was blocked");
                }
            } else {
                // Future slot — allow Block / Unblock
                if ("BLOCKED".equalsIgnoreCase(slot.status)) {
                    blockSection.setVisibility(View.VISIBLE);
                    tvBlockReason.setText("Reason: Blocked by Staff");
                    btnBlockSlot.setVisibility(View.GONE);
                    btnUnblockSlot.setVisibility(View.VISIBLE);
                } else {
                    blockSection.setVisibility(View.GONE);
                    btnBlockSlot.setVisibility(View.VISIBLE);
                    btnUnblockSlot.setVisibility(View.GONE);
                }
            }
        }
    }

    private void applyStatusBadge(String status) {
        if (status == null) return;
        int background;
        switch (status.toUpperCase()) {
            case "AVAILABLE": background = R.drawable.badge_green; break;
            case "BOOKED":    background = R.drawable.badge_blue; break;
            case "BLOCKED":   background = R.drawable.badge_red; break;
            default:          background = R.drawable.badge_orange; break;
        }
        tvDetailStatus.setBackgroundResource(background);
    }

    private void showBlockDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 20, 60, 0);

        final EditText input = new EditText(this);
        input.setHint("Reason for blocking (e.g. Maintenance)");
        layout.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Block Slot")
                .setMessage("Blocking " + slot.timeRange + ". If booked, the student will be notified.")
                .setView(layout)
                .setPositiveButton("Block", (dialog, which) -> {
                    String reason = input.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(this, "Reason is required", Toast.LENGTH_SHORT).show();
                    } else {
                        blockSlot(reason);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void blockSlot(String reason) {
        progressBar.setVisibility(View.VISIBLE);
        repo.blockSlot(slot.spaceId, slot.date, slot.slotKey, reason, result -> {
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (result instanceof Result.Success) {
                    Toast.makeText(this, "Slot blocked successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Failed to block slot", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void unblockSlot() {
        progressBar.setVisibility(View.VISIBLE);
        repo.unblockSlot(slot.spaceId, slot.date, slot.slotKey, result -> {
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (result instanceof Result.Success) {
                    Toast.makeText(this, "Slot unblocked successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Failed to unblock slot", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private boolean isCurrentSlotInPast(String slotKey) {
        if (slotKey == null || slotKey.length() != 4) return false;
        try {
            int slotHour = Integer.parseInt(slotKey.substring(0, 2));
            int slotMin  = Integer.parseInt(slotKey.substring(2, 4));
            java.util.Calendar now = java.util.Calendar.getInstance();
            int nowHour = now.get(java.util.Calendar.HOUR_OF_DAY);
            int nowMin  = now.get(java.util.Calendar.MINUTE);
            int slotTotal = slotHour * 60 + slotMin;
            int nowTotal  = nowHour  * 60 + nowMin;
            // Slot ends 30 mins after start
            return (slotTotal + 30) <= nowTotal;
        } catch (Exception e) {
            return false;
        }
    }

    private void fetchSpaceName() {
        repo.getSpaceDetails(slot.spaceId, result -> {
            if (result instanceof Result.Success) {
                com.example.hod.models.Space space = ((Result.Success<com.example.hod.models.Space>) result).data;
                if (space != null && space.getRoomName() != null) {
                    runOnUiThread(() -> {
                        TextView subtitle = findViewById(R.id.header_subtitle);
                        if (subtitle != null) subtitle.setText(space.getRoomName() + " • " + slot.date);
                    });
                }
            }
        });
    }
}
