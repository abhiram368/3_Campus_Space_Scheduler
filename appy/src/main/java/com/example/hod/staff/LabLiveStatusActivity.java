package com.example.hod.staff;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;
import com.example.hod.models.LiveStatusData;
import com.example.hod.models.Space;
import com.example.hod.repository.FirebaseRepository;
import com.example.hod.utils.Result;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LabLiveStatusActivity extends AppCompatActivity {

    private TextView tvStatus, tvCurrentSlot, tvRequester, tvPurpose, tvLabName;
    private LinearLayout bookedDetailsLayout;
    private String labId;
    private FirebaseRepository repo;
    
    // Deep-Dive Refactoring Fields
    private Map<String, LiveStatusData> dailySlots = new HashMap<>();
    private ValueEventListener liveStatusListener;
    private String listenerDate = ""; // Track which date the listener is attached to

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appy_lab_live_status);

        // Header Configuration
        View headerView = findViewById(R.id.header_layout);
        if (headerView != null) {
            TextView title = headerView.findViewById(R.id.header_title);
            View btnBack = headerView.findViewById(R.id.btnBack);
            if (title != null) title.setText("Live Status");
            if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        }

        tvStatus = findViewById(R.id.tvStatus);
        tvCurrentSlot = findViewById(R.id.tvCurrentSlot);
        tvRequester = findViewById(R.id.tvBookedByUser);
        tvPurpose = findViewById(R.id.tvBookingPurpose);
        tvLabName = findViewById(R.id.tvLabName);
        bookedDetailsLayout = findViewById(R.id.bookedDetailsLayout);

        labId = getIntent().getStringExtra("labId");
        if (labId == null || labId.isEmpty()) {
            Toast.makeText(this, "Error: missing Lab ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        updateHeader("Lab Live Status", "Real-time Occupancy");

        repo = new FirebaseRepository();
        fetchLabName();
        startListeningForStatus();
        setupAutoRefresh();
    }

    private final Handler refreshHandler = new Handler();
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            // Re-trigger the logic to check current time against slots
            startListeningForStatus();
            refreshHandler.postDelayed(this, 30000); // Check every 30 seconds
        }
    };

    private void setupAutoRefresh() {
        refreshHandler.postDelayed(refreshRunnable, 30000);
    }

    private void updateHeader(String title, String subtitle) {
        TextView tvTitle = findViewById(R.id.header_title);
        TextView tvSubtitle = findViewById(R.id.header_subtitle);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(subtitle);
    }

    private void fetchLabName() {
        repo.getSpaceDetails(labId, (Result<Space> result) -> {
            if (result instanceof Result.Success) {
                Space space = ((Result.Success<Space>) result).data;
                if (space != null) {
                    tvLabName.setText(space.getRoomName());
                }
            }
        });
    }

    private void startListeningForStatus() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = dateFormat.format(calendar.getTime());

        // 1. Midnight Transition / initialization
        if (!currentDate.equals(listenerDate)) {
            Log.d("LiveStatus", "Date changed or first load. Re-attaching listener for: " + currentDate);
            
            // Remove old listener if it exists (using the date it was attached to!)
            if (liveStatusListener != null && !listenerDate.isEmpty()) {
                repo.removeLiveStatusListener(labId, listenerDate, liveStatusListener);
            }

            listenerDate = currentDate;
            liveStatusListener = (ValueEventListener) repo.getLiveSlotStatus(labId, currentDate, result -> {
                if (result instanceof Result.Success) {
                    dailySlots = ((Result.Success<Map<String, LiveStatusData>>) result).data;
                    updateUI(); // Immediate update when DB changes
                } else if (result instanceof Result.Error) {
                    Log.e("LiveStatus", "Error fetching slots: " + ((Result.Error<?>) result).exception.getMessage());
                }
            });
        } else {
            // Same day, just ensure UI is fresh (the listener will handle DB changes)
            updateUI();
        }
    }

    private void updateUI() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int normalizedMin = (minute < 30) ? 0 : 30;

        if (dailySlots == null || dailySlots.isEmpty()) {
            showEmptyState();
            return;
        }

        MatchedSlot matched = findMatchedSlot(cal, dailySlots);
        LiveStatusData matchedData = matched.current;
        LiveStatusData nextData = matched.next;

        if (matchedData == null) {
            tvStatus.setText("CLOSED (OFF HOURS)");
            tvStatus.setTextColor(Color.parseColor("#9CA3AF")); // Gray
            
            if (nextData != null) {
                tvCurrentSlot.setText("Next Slot: " + nextData.slotKey);
            } else {
                String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.getTime());
                tvCurrentSlot.setText("No active slot for " + currentTime);
            }
            
            bookedDetailsLayout.setVisibility(View.GONE);
            return;
        }

        String status = matchedData.status != null ? matchedData.status.toUpperCase() : "AVAILABLE";
        
        if ("BOOKED".equals(status) || "OCCUPIED".equals(status)) {
            tvStatus.setText("OCCUPIED");
            tvStatus.setTextColor(Color.parseColor("#EF4444")); // Red
            bookedDetailsLayout.setVisibility(View.VISIBLE);
            
            if (matchedData.booking != null) {
                String requester = matchedData.booking.getRequesterName();
                if (requester == null || requester.isEmpty()) requester = matchedData.booking.getBookedBy();
                tvRequester.setText(requester);
                tvPurpose.setText(matchedData.booking.getPurpose());
                
                final String finalRequester = requester;
                bookedDetailsLayout.setOnClickListener(v -> {
                    Intent intent = new Intent(LabLiveStatusActivity.this, StaffCompletedRequestDetailActivity.class);
                    intent.putExtra("booking", matchedData.booking);
                    intent.putExtra("requesterName", finalRequester);
                    startActivity(intent);
                });
            } else {
                tvRequester.setText("Loading...");
                tvPurpose.setText("Syncing details...");
                bookedDetailsLayout.setOnClickListener(null);
            }
        } else if ("BLOCKED".equals(status)) {
            tvStatus.setText("BLOCKED");
            tvStatus.setTextColor(Color.parseColor("#F59E0B")); // Orange
            bookedDetailsLayout.setVisibility(View.GONE);
        } else {
            // AVAILABLE or REJECTED
            tvStatus.setText("FREE");
            tvStatus.setTextColor(Color.parseColor("#10B981")); // Emerald
            bookedDetailsLayout.setVisibility(View.GONE);
        }

        // Display current slot range
        if (matchedData.slotKey != null && !matchedData.slotKey.isEmpty()) {
            tvCurrentSlot.setText(matchedData.slotKey);
        } else {
            int endMin = (normalizedMin == 0) ? 30 : 0;
            int endHour = (normalizedMin == 0) ? hour : hour + 1;
            String displaySlot = String.format(Locale.getDefault(), "%02d:%02d - %02d:%02d", hour, normalizedMin, endHour, endMin);
            tvCurrentSlot.setText(displaySlot);
        }
    }

    private void showEmptyState() {
        tvStatus.setText("No Schedule");
        tvStatus.setTextColor(Color.parseColor("#9CA3AF")); // Gray
        tvCurrentSlot.setText("No slots found for today");
        bookedDetailsLayout.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        refreshHandler.removeCallbacks(refreshRunnable);
        if (liveStatusListener != null && repo != null && !listenerDate.isEmpty()) {
            repo.removeLiveStatusListener(labId, listenerDate, liveStatusListener);
        }
    }

    private MatchedSlot findMatchedSlot(Calendar cal, Map<String, LiveStatusData> slots) {
        MatchedSlot result = new MatchedSlot();
        if (slots == null || slots.isEmpty()) return result;

        int nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        int closestNextMin = Integer.MAX_VALUE;

        for (Map.Entry<String, LiveStatusData> entry : slots.entrySet()) {
            String key = entry.getKey();
            LiveStatusData data = entry.getValue();
            
            // Try to parse slot range from key (e.g. "09:00 - 09:30" or "0900")
            int startMin = -1;
            int endMin = -1;

            if (key.contains("-")) {
                String[] parts = key.split("-");
                if (parts.length == 2) {
                    startMin = timeToMinutes(parts[0].trim());
                    endMin = timeToMinutes(parts[1].trim());
                }
            } else if (key.length() == 4) {
                startMin = timeToMinutes(key);
                endMin = startMin + 30; // Default 30 min if only start is provided
            }

            if (startMin != -1 && endMin != -1) {
                if (nowMin >= startMin && nowMin < endMin) {
                    result.current = data;
                } else if (startMin > nowMin && startMin < closestNextMin) {
                    closestNextMin = startMin;
                    result.next = data;
                }
            }
        }
        return result;
    }

    private int timeToMinutes(String time) {
        try {
            String clean = time.replace(":", "").trim();
            if (clean.length() == 3 || clean.length() == 4) {
                if (clean.length() == 3) clean = "0" + clean;
                int h = Integer.parseInt(clean.substring(0, 2));
                int m = Integer.parseInt(clean.substring(2));
                return h * 60 + m;
            }
        } catch (Exception e) {
            Log.e("LiveStatus", "Error parsing time: " + time, e);
        }
        return -1;
    }

    private static class MatchedSlot {
        LiveStatusData current;
        LiveStatusData next;
    }
}
