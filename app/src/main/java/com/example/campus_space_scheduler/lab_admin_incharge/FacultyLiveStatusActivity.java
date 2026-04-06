package com.example.campus_space_scheduler.lab_admin_incharge;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.campus_space_scheduler.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FacultyLiveStatusActivity extends AppCompatActivity {

    private TextView tvStatusLab, tvCurrentTime, tvLabRoomName;
    private View indicatorLab;
    private DatabaseReference spacesRef, schedulesRef;
    private String labName;
    private Handler handler = new Handler();
    private Runnable refreshRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_live_status);

        labName = getIntent().getStringExtra("labName");

        tvStatusLab = findViewById(R.id.tvFacultyStatusLab);
        tvCurrentTime = findViewById(R.id.tvFacultyCurrentTime);
        tvLabRoomName = findViewById(R.id.tvFacultyLabRoomName);
        indicatorLab = findViewById(R.id.indicatorFacultyLab);

        if (labName != null) {
            tvLabRoomName.setText(labName + " Lab");
        }

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
        tvCurrentTime.setText("Current Time: " + currentTime);
    }

    private void fetchLiveStatus() {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String currentTime24 = new SimpleDateFormat("HHmm", Locale.getDefault()).format(new Date());
        int now = Integer.parseInt(currentTime24);

        spacesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean labFound = false;
                for (DataSnapshot spaceSnap : snapshot.getChildren()) {
                    String rawName = String.valueOf(spaceSnap.child("roomName").getValue());
                    if (rawName.contains(labName)) {
                        String spaceId = spaceSnap.getKey();
                        checkSlot(spaceId, currentDate, now, tvStatusLab, indicatorLab);
                        labFound = true;
                        break;
                    }
                }
                if (!labFound) tvStatusLab.setText("Lab Not Found");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkSlot(String spaceId, String date, int now, TextView tvStatus, View indicator) {
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
                        } catch (Exception e) {}
                    }
                }
                if (!found) updateUI(tvStatus, indicator, "AVAILABLE");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateUI(TextView tv, View ind, String status) {
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