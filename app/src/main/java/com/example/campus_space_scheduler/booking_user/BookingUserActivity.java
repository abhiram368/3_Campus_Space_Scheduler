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

        adapter = new ArrayAdapter<>(this, R.layout.spinner_item, spaceNames);
        spinnerWorkspace.setAdapter(adapter);

        spacesRef = FirebaseDatabase.getInstance().getReference("spaces");
        schedulesRef = FirebaseDatabase.getInstance().getReference("schedules");
        bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");

        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchSpaces();
            if (currentSelectedSpaceId != null) {
                observeLiveStatus(currentSelectedSpaceId);
            } else {
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        fetchSpaces();
        setupBookingStatusObserver();

        spinnerWorkspace.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            currentSelectedSpaceId = spaceIdMap.get(selected);
            Log.d(TAG, "Selected Space: " + selected + " ID: " + currentSelectedSpaceId);
            if (currentSelectedSpaceId != null) {
                observeLiveStatus(currentSelectedSpaceId);
            } else {
                cardLiveStatus.setVisibility(View.GONE);
            }
        });

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String monthStr = (month + 1) < 10 ? "0" + (month + 1) : String.valueOf(month + 1);
            String dayStr = dayOfMonth < 10 ? "0" + dayOfMonth : String.valueOf(dayOfMonth);
            selectedDate = year + "-" + monthStr + "-" + dayStr;

            if (isPastDate(selectedDate)) {
                Toast.makeText(BookingUserActivity.this, "Past slots are not available", Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedSpaceName = spinnerWorkspace.getText().toString();
            if (!selectedSpaceName.isEmpty()) {
                String selectedSpaceId = spaceIdMap.get(selectedSpaceName);
                String selectedSpaceType = spaceTypeMap.get(selectedSpaceName);

                if (selectedSpaceId != null) {
                    Intent intent = new Intent(BookingUserActivity.this, AvailableTimeSlotsActivity.class);
                    intent.putExtra("SPACE_NAME", selectedSpaceName);
                    intent.putExtra("SPACE_ID", selectedSpaceId);
                    intent.putExtra("SPACE_TYPE", selectedSpaceType);
                    intent.putExtra("DATE", selectedDate);
                    intent.putExtra("ROLE", userRole);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Please select a valid space", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please select a workspace first", Toast.LENGTH_SHORT).show();
            }
        });

        buttonCancelRequest.setOnClickListener(v -> {
            Intent intent = new Intent(BookingUserActivity.this, CancelRequestActivity.class);
            intent.putExtra("ROLE", userRole);
            startActivity(intent);
        });

        // Set up bottom navigation to act as buttons
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_none); // Select the invisible dummy item
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_history) {
                    startActivity(new Intent(BookingUserActivity.this, BookingHistoryActivity.class));
                } else if (itemId == R.id.nav_profile) {
                    Intent intent = new Intent(BookingUserActivity.this, ProfileActivity.class);
                    intent.putExtra("ROLE", userRole);
                    startActivity(intent);
                }
                return false; // Prevents the item from staying selected
            });
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted");
            } else {
                Toast.makeText(this, "Notifications are disabled. You won't receive booking updates.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupBookingStatusObserver() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        SharedPreferences prefs = getSharedPreferences("BookingStatusPrefs", MODE_PRIVATE);

        bookingStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
                    String bookingId = bookingSnapshot.getKey();
                    String status = bookingSnapshot.child("status").getValue(String.class);
                    String spaceName = bookingSnapshot.child("spaceName").getValue(String.class);

                    if (bookingId != null && status != null) {
                        String lastStatus = prefs.getString(bookingId, null);

                        // If lastStatus is null, it's the first time we're seeing this booking
                        // We store the current status but don't notify unless it's not "Pending"
                        if (lastStatus == null) {
                            prefs.edit().putString(bookingId, status).apply();
                            continue;
                        }

                        if (!status.equalsIgnoreCase(lastStatus)) {
                            // Status changed!
                            String message = "";
                            if (status.equalsIgnoreCase("Approved") || status.equalsIgnoreCase("Accepted")) {
                                message = "Your booking for " + spaceName + " has been approved!";
                            } else if (status.equalsIgnoreCase("Rejected")) {
                                message = "Your booking for " + spaceName + " has been rejected.";
                            } else if (status.equalsIgnoreCase("Forwarded")) {
                                message = "Your booking for " + spaceName + " has been forwarded to the HOD.";
                            }

                            if (!message.isEmpty()) {
                                NotificationHelper.showNotification(BookingUserActivity.this, "Booking Update", message);
                            }

                            // Save new status
                            prefs.edit().putString(bookingId, status).apply();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Booking observer failed: " + error.getMessage());
            }
        };

        bookingsRef.orderByChild("bookedBy").equalTo(currentUserId).addValueEventListener(bookingStatusListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Clear focus from workspace selector
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) currentFocus.clearFocus();

        // Reset bottom navigation selection state
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_none);
        }
    }

    private boolean isPastDate(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
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
