package com.example.campus_space_scheduler.booking_user;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.campus_space_scheduler.R;
import com.example.hod.utils.NotificationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BookingUserActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";
    private static final int NOTIFICATION_PERMISSION_CODE = 102;
    
    private String selectedDate;
    private String userRole;
    private DatabaseReference spacesRef;
    private DatabaseReference schedulesRef;
    private DatabaseReference bookingsRef;
    private List<String> spaceNames;
    private Map<String, String> spaceIdMap; // Maps roomName to spaceId
    private Map<String, String> spaceTypeMap; // Maps roomName to space type (role)
    private ArrayAdapter<String> adapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Live Status Views
    private View cardLiveStatus;
    private View statusIndicator;
    private TextView textViewLiveStatus;
    private TextView textViewStatusDetails;

    private ValueEventListener liveStatusListener;
    private ValueEventListener bookingStatusListener;
    private Query currentQuery;
    private String currentSelectedSpaceId;

    public enum SlotStatus {
        AVAILABLE,
        BOOKED,
        PENDING,
        BLOCKED,
        MAINTENANCE
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.t_activity_dashboard);

        userRole = getIntent().getStringExtra("ROLE");
        Log.d(TAG, "User Role in Dashboard: " + userRole);

        // Initialize Notification Channel
        NotificationHelper.createNotificationChannel(this);
        checkNotificationPermission();

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        AutoCompleteTextView spinnerWorkspace = findViewById(R.id.spinnerWorkspace);
        CalendarView calendarView = findViewById(R.id.calendarView);
        Button buttonCancelRequest = findViewById(R.id.buttonCancelRequest);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        // Initialize Status Views
        cardLiveStatus = findViewById(R.id.cardLiveStatus);
        statusIndicator = findViewById(R.id.statusIndicator);
        textViewLiveStatus = findViewById(R.id.textViewLiveStatus);
        textViewStatusDetails = findViewById(R.id.textViewStatusDetails);

        spaceNames = new ArrayList<>();
        spaceIdMap = new HashMap<>();
        spaceTypeMap = new HashMap<>();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, spaceNames);
        spinnerWorkspace.setAdapter(adapter);

        spacesRef = FirebaseDatabase.getInstance().getReference("spaces");
        schedulesRef = FirebaseDatabase.getInstance().getReference("schedules");
        bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");

        // Request initial notification state
        updateBookingStatusListener();

        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchSpaces();
            if (currentSelectedSpaceId != null) {
                observeLiveStatus(currentSelectedSpaceId);
            }
        });

        spinnerWorkspace.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSpace = spaceNames.get(position);
            currentSelectedSpaceId = spaceIdMap.get(selectedSpace);
            Log.d(TAG, "Selected Space ID: " + currentSelectedSpaceId);
            
            if (currentSelectedSpaceId != null) {
                observeLiveStatus(currentSelectedSpaceId);
            }
        });

        // Use today's date by default
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            Log.d(TAG, "Selected date: " + selectedDate);

            if (currentSelectedSpaceId == null) {
                Toast.makeText(this, "Please select a space first", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isDateBeforeToday(selectedDate)) {
                Toast.makeText(this, "Cannot book for past dates", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, BookingFormActivity.class);
            intent.putExtra("SPACE_ID", currentSelectedSpaceId);
            intent.putExtra("DATE", selectedDate);
            intent.putExtra("ROLE", userRole);
            startActivity(intent);
        });

        buttonCancelRequest.setOnClickListener(v -> {
            // Simplified Cancel Request logic - typically would show a dialog or history
            Toast.makeText(this, "Please go to History to manage your bookings", Toast.LENGTH_SHORT).show();
        });

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_history) {
                startActivity(new Intent(this, BookingHistoryActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile) {
                // Should navigate to ProfileActivity if available
                Toast.makeText(this, "Profile feature coming soon", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        fetchSpaces();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void updateBookingStatusListener() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        if (bookingStatusListener != null) {
            bookingsRef.removeEventListener(bookingStatusListener);
        }

        bookingStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Monitor for status changes to show local notifications if needed
                // or update UI badges
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        bookingsRef.orderByChild("userId").equalTo(uid).addValueEventListener(bookingStatusListener);
    }

    private boolean isDateBeforeToday(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date date = sdf.parse(dateStr);
            if (date == null) return false;

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar selected = Calendar.getInstance();
            selected.setTime(date);
            selected.set(Calendar.HOUR_OF_DAY, 0);
            selected.set(Calendar.MINUTE, 0);
            selected.set(Calendar.SECOND, 0);
            selected.set(Calendar.MILLISECOND, 0);

            return selected.before(today);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void observeLiveStatus(String spaceId) {
        if (currentQuery != null && liveStatusListener != null) {
            currentQuery.removeEventListener(liveStatusListener);
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Log.d(TAG, "Observing live status for " + spaceId + " on " + today);

        cardLiveStatus.setVisibility(View.VISIBLE);
        textViewLiveStatus.setText("Checking status...");
        textViewStatusDetails.setText("");
        statusIndicator.setBackgroundColor(Color.GRAY);

        liveStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DataSnapshot currentSchedule = null;
                for (DataSnapshot child : snapshot.getChildren()) {
                    String date = child.child("date").getValue(String.class);
                    if (today.equals(date)) {
                        currentSchedule = child;
                        break;
                    }
                }

                if (currentSchedule == null) {
                    updateStatusUI(true, "AVAILABLE", "No bookings for today");
                    swipeRefreshLayout.setRefreshing(false);
                    return;
                }

                String currentTime = new SimpleDateFormat("HHmm", Locale.getDefault()).format(new Date());
                int currentInt = Integer.parseInt(currentTime);

                DataSnapshot slotsSnapshot = currentSchedule.child("slots");
                String currentStatus = "AVAILABLE";
                String endTimeStr = "";

                for (DataSnapshot slot : slotsSnapshot.getChildren()) {
                    try {
                        String startStr = slot.child("start").getValue(String.class);
                        String endStr = slot.child("end").getValue(String.class);
                        String status = slot.child("status").getValue(String.class);

                        if (startStr != null && endStr != null) {
                            int startTime = Integer.parseInt(startStr.replace(":", ""));
                            int endTime = Integer.parseInt(endStr.replace(":", ""));

                            if (currentInt >= startTime && currentInt < endTime) {
                                currentStatus = (status != null) ? status.toUpperCase() : "AVAILABLE";
                                endTimeStr = endStr;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing slot time", e);
                    }
                }

                String details = endTimeStr.isEmpty() ? "" : "Until " + formatTime(endTimeStr);

                switch (currentStatus) {
                    case "BOOKED":
                        updateStatusUI(false, "BOOKED", details);
                        break;
                    case "BLOCKED":
                        updateStatusUI(false, "BLOCKED", details);
                        break;
                    case "MAINTENANCE":
                        updateStatusUI(false, "MAINTENANCE", details);
                        break;
                    case "PENDING":
                        textViewLiveStatus.setText("PENDING");
                        textViewStatusDetails.setText(details);
                        statusIndicator.setBackgroundColor(Color.parseColor("#FFA500")); // Orange
                        break;
                    case "AVAILABLE":
                    default:
                        updateStatusUI(true, "AVAILABLE", "");
                        break;
                }
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                cardLiveStatus.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }
        };

        currentQuery = schedulesRef.orderByChild("spaceId").equalTo(spaceId);
        currentQuery.addValueEventListener(liveStatusListener);
    }

    private void updateStatusUI(boolean available, String statusText, String details) {
        textViewLiveStatus.setText(statusText);
        textViewStatusDetails.setText(details);
        statusIndicator.setBackgroundColor(available ? Color.GREEN : Color.RED);
    }

    private String formatTime(String rawTime) {
        if (rawTime == null) return "";
        if (rawTime.contains(":")) return rawTime;
        if (rawTime.length() < 3) return rawTime;
        if (rawTime.length() == 3) rawTime = "0" + rawTime;
        return rawTime.substring(0, 2) + ":" + rawTime.substring(2);
    }

    private void fetchSpaces() {
        Log.d(TAG, "Fetching spaces for role: " + userRole);
        spacesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                spaceNames.clear();
                spaceIdMap.clear();
                spaceTypeMap.clear();
                for (DataSnapshot spaceSnapshot : snapshot.getChildren()) {
                    String roomName = spaceSnapshot.child("roomName").getValue(String.class);
                    String id = spaceSnapshot.child("spaceId").getValue(String.class);
                    String spaceRole = spaceSnapshot.child("role").getValue(String.class);

                    if (id == null) id = spaceSnapshot.getKey();

                    // Filter Logic: If user is Faculty, hide Classrooms
                    if (userRole != null && userRole.equalsIgnoreCase("Faculty")) {
                        if (spaceRole != null && spaceRole.equalsIgnoreCase("Classroom")) {
                            Log.d(TAG, "Filtering out Classroom: " + roomName);
                            continue;
                        }
                    }

                    if (roomName != null) {
                        spaceNames.add(roomName);
                        spaceIdMap.put(roomName, id);
                        spaceTypeMap.put(roomName, spaceRole);
                    }
                }

                if (spaceNames.isEmpty()) {
                    Log.d(TAG, "No eligible spaces found.");
                }

                adapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentQuery != null && liveStatusListener != null) {
            currentQuery.removeEventListener(liveStatusListener);
        }
        if (bookingsRef != null && bookingStatusListener != null) {
            bookingsRef.removeEventListener(bookingStatusListener);
        }
    }
}
