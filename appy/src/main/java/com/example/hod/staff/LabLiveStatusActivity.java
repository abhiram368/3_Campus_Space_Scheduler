package com.example.hod.staff;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;
import com.example.hod.models.LiveStatusData;
import com.example.hod.models.Space;
import com.example.hod.repository.FirebaseRepository;
import com.example.hod.utils.Result;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class LabLiveStatusActivity extends AppCompatActivity {

    private TextView tvStatus, tvCurrentSlot, tvRequester, tvPurpose, tvLabName;
    private LinearLayout bookedDetailsLayout;
    private String labId;
    private FirebaseRepository repo;
    
    private java.util.Map<String, LiveStatusData> dailySlots = new java.util.HashMap<>();
    private com.google.firebase.database.ValueEventListener liveStatusListener;
    private String listenerDate = ""; // Track which date the listener is attached to

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab_live_status);

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

    private final android.os.Handler refreshHandler = new android.os.Handler();
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
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

        if (!currentDate.equals(listenerDate)) {
            Log.d("LiveStatus", "Date changed or first load. Re-attaching listener for: " + currentDate);
            
            if (liveStatusListener != null && !listenerDate.isEmpty()) {
                repo.removeLiveStatusListener(labId, listenerDate, (com.google.firebase.database.ValueEventListener) liveStatusListener);
            }

            listenerDate = currentDate;
            liveStatusListener = (com.google.firebase.database.ValueEventListener) repo.getLiveSlotStatus(labId, currentDate, result -> {
                if (result instanceof Result.Success) {
                    dailySlots = ((Result.Success<java.util.Map<String, LiveStatusData>>) result).data;
                    updateUI();
                } else if (result instanceof Result.Error) {
                    Log.e("LiveStatus", "Error fetching slots: " + ((Result.Error<?>) result).exception.getMessage());
                }
            });
        } else {
            updateUI();
        }
    }

    private void updateUI() {
        if (dailySlots == null || dailySlots.isEmpty()) {
            showEmptyState();
            return;
        }

        java.util.Calendar cal = java.util.Calendar.getInstance();
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = cal.get(java.util.Calendar.MINUTE);
        
        int normalizedMin = (minute < 30) ? 0 : 30;
        String currentSlotKey = String.format(Locale.getDefault(), "%02d:%02d - %02d:%02d", hour, normalizedMin, (normalizedMin == 0 ? hour : hour + 1), (normalizedMin == 0 ? 30 : 0));
        
        LiveStatusData matchedData = dailySlots.get(currentSlotKey);
        
        if (matchedData == null) {
            tvStatus.setText("CLOSED (OFF HOURS)");
            tvStatus.setTextColor(Color.parseColor("#9CA3AF")); 
            bookedDetailsLayout.setVisibility(View.GONE);
            return;
        }

        String status = matchedData.status != null ? matchedData.status.toUpperCase() : "AVAILABLE";
        
        if ("BOOKED".equals(status) || "OCCUPIED".equals(status)) {
            tvStatus.setText("OCCUPIED");
            tvStatus.setTextColor(Color.parseColor("#EF4444"));
            bookedDetailsLayout.setVisibility(View.VISIBLE);
            
            if (matchedData.booking != null) {
                String requester = matchedData.booking.getRequesterName();
                if (requester == null || requester.isEmpty()) requester = matchedData.booking.getBookedBy();
                tvRequester.setText(requester);
                tvPurpose.setText(matchedData.booking.getPurpose());
                
                final String finalRequester = requester;
                bookedDetailsLayout.setOnClickListener(v -> {
                    android.content.Intent intent = new android.content.Intent(LabLiveStatusActivity.this, com.example.hod.staff.StaffCompletedRequestDetailActivity.class);
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
            tvStatus.setTextColor(Color.parseColor("#F59E0B"));
            bookedDetailsLayout.setVisibility(View.GONE);
        } else {
            tvStatus.setText("FREE");
            tvStatus.setTextColor(Color.parseColor("#10B981"));
            bookedDetailsLayout.setVisibility(View.GONE);
        }

        tvCurrentSlot.setText(currentSlotKey);
    }

    private void showEmptyState() {
        tvStatus.setText("No Schedule");
        tvStatus.setTextColor(Color.parseColor("#9CA3AF"));
        tvCurrentSlot.setText("No slots found for today");
        bookedDetailsLayout.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        refreshHandler.removeCallbacks(refreshRunnable);
        if (liveStatusListener != null && repo != null && !listenerDate.isEmpty()) {
            repo.removeLiveStatusListener(labId, listenerDate, (com.google.firebase.database.ValueEventListener) liveStatusListener);
        }
    }
}
