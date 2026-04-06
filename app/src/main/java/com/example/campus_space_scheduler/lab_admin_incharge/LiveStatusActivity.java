package com.example.campus_space_scheduler.lab_admin_incharge;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.campus_space_scheduler.R;
import com.example.campus_space_scheduler.databinding.ActivityLiveStatusBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LiveStatusActivity extends BaseInchargeActivity {

    private ActivityLiveStatusBinding binding;
    private DatabaseReference spacesRef, schedulesRef;
    private final Handler handler = new Handler();
    private Runnable refreshRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLiveStatusBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Sidebar using BaseActivity method
        setupDrawer(binding.toolbar, binding.navView, binding.drawerLayout);

        spacesRef = FirebaseDatabase.getInstance().getReference("spaces");
        schedulesRef = FirebaseDatabase.getInstance().getReference("schedules");

        startLiveUpdates();
    }

    private void startLiveUpdates() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                updateTimeDisplay();
                fetchLiveStatus();
                handler.postDelayed(this, 30000);
            }
        };
        handler.post(refreshRunnable);
    }

    private void updateTimeDisplay() {
        String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        binding.tvCurrentTime.setText("Current Time: " + currentTime);
    }

    private void fetchLiveStatus() {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String currentTime24 = new SimpleDateFormat("HHmm", Locale.getDefault()).format(new Date());
        int now = Integer.parseInt(currentTime24);

        spacesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean seminarFound = false, discussionFound = false, apjFound = false;
                
                for (DataSnapshot spaceSnap : snapshot.getChildren()) {
                    String rawName = String.valueOf(spaceSnap.child("roomName").getValue());
                    String name = rawName.trim();
                    String spaceId = spaceSnap.getKey();
                    
                    if (name.equalsIgnoreCase("CSED Seminar Hall")) {
                        checkSlot(spaceId, currentDate, now, binding.tvStatusSeminar, binding.indicatorSeminar);
                        seminarFound = true;
                    } else if (name.equalsIgnoreCase("CSED Discussion Room")) {
                        checkSlot(spaceId, currentDate, now, binding.tvStatusDiscussion, binding.indicatorDiscussion);
                        discussionFound = true;
                    } else if (name.equalsIgnoreCase("APJ Hall")) {
                        checkSlot(spaceId, currentDate, now, binding.tvStatusApj, binding.indicatorApj);
                        apjFound = true;
                    }
                }
                
                if (!seminarFound) binding.tvStatusSeminar.setText("Room Not Found");
                if (!discussionFound) binding.tvStatusDiscussion.setText("Room Not Found");
                if (!apjFound) binding.tvStatusApj.setText("Room Not Found");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkSlot(String spaceId, String date, int now, android.widget.TextView tvStatus, View indicator) {
        String scheduleId = spaceId + "_" + date;
        schedulesRef.child(scheduleId).child("slots").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean found = false;
                if (snapshot.exists()) {
                    for (DataSnapshot slotSnap : snapshot.getChildren()) {
                        String startStr = String.valueOf(slotSnap.child("start").getValue()).replace(":", "");
                        String endStr = String.valueOf(slotSnap.child("end").getValue()).replace(":", "");
                        
                        try {
                            int start = Integer.parseInt(startStr);
                            int end = Integer.parseInt(endStr);
                            
                            if (now >= start && now < end) {
                                String status = String.valueOf(slotSnap.child("status").getValue());
                                updateUI(tvStatus, indicator, status);
                                found = true;
                                break;
                            }
                        } catch (Exception e) {
                            Log.e("LiveStatus", "Error parsing times for " + spaceId);
                        }
                    }
                }
                
                if (!found) {
                    updateUI(tvStatus, indicator, "AVAILABLE");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateUI(android.widget.TextView tv, View ind, String status) {
        tv.setText(status);
        if ("AVAILABLE".equalsIgnoreCase(status)) {
            tv.setTextColor(ContextCompat.getColor(this, R.color.green_icon));
            ind.setBackgroundColor(ContextCompat.getColor(this, R.color.green_icon));
        } else {
            tv.setTextColor(ContextCompat.getColor(this, R.color.yellow_icon));
            ind.setBackgroundColor(ContextCompat.getColor(this, R.color.yellow_icon));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(refreshRunnable);
    }
}