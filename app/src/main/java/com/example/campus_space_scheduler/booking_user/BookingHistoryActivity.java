package com.example.campus_space_scheduler.booking_user;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_space_scheduler.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class BookingHistoryActivity extends AppCompatActivity implements BookingAdapter.OnItemClickListener {

    private static final String TAG = "BookingHistoryActivity";
    private RecyclerView recyclerViewHistory;
    private BookingAdapter adapter;
    private List<Booking> historyList; // List currently shown
    private List<Booking> fullList;    // All records from DB
    private ProgressBar progressBar;
    private TextView emptyTextView;
    private DatabaseReference bookingsRef;
    private TabLayout tabLayoutFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.t_activity_booking_history);

        // Header and back button logic
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        ImageView buttonClearHistory = findViewById(R.id.buttonClearHistory);
        if (buttonClearHistory != null) {
            buttonClearHistory.setOnClickListener(v -> showClearHistoryConfirmation());
        }

        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.emptyTextView);
        tabLayoutFilter = findViewById(R.id.tabLayoutFilter);

        historyList = new ArrayList<>();
        fullList = new ArrayList<>();
        adapter = new BookingAdapter(historyList, this);

        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewHistory.setAdapter(adapter);

        bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");
        
        setupTabLayout();
        fetchUserBookings();
    }

    private void setupTabLayout() {
        if (tabLayoutFilter == null) return;
        
        tabLayoutFilter.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterHistory(tab.getText().toString());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void filterHistory(String filterText) {
        historyList.clear();
        if (filterText.equalsIgnoreCase("All")) {
            historyList.addAll(fullList);
        } else {
            for (Booking booking : fullList) {
                String status = booking.getStatus();
                if (status == null) continue;

                if (filterText.equalsIgnoreCase("Forwarded")) {
                    if (status.toLowerCase().contains("forwarded")) {
                        historyList.add(booking);
                    }
                } else if (filterText.equalsIgnoreCase("Approved")) {
                    if (status.equalsIgnoreCase("Approved") || status.equalsIgnoreCase("Accepted")) {
                        historyList.add(booking);
                    }
                } else if (filterText.equalsIgnoreCase("Expired")) {
                    // Filter for "rejection expired" or any status containing "expired" as requested
                    if (status.toLowerCase().contains("expired")) {
                        historyList.add(booking);
                    }
                } else if (filterText.equalsIgnoreCase("Rejected")) {
                    // Show standard Rejected bookings, excluding "rejection expired" which has its own tab
                    if (status.equalsIgnoreCase("Rejected") && !status.toLowerCase().contains("expired")) {
                        historyList.add(booking);
                    }
                } else {
                    if (status.equalsIgnoreCase(filterText)) {
                        historyList.add(booking);
                    }
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void showClearHistoryConfirmation() {
        if (fullList.isEmpty()) {
            Toast.makeText(this, "History is already empty", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this, R.style.Theme_CampusSpaceScheduler_Dialog_Custom)
                .setTitle("Clear History")
                .setMessage("Are you sure you want to clear your booking history? This will delete all your records.")
                .setPositiveButton("Clear All", (dialog, which) -> clearUserHistory())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void clearUserHistory() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        progressBar.setVisibility(View.VISIBLE);
        
        bookingsRef.orderByChild("bookedBy").equalTo(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            ds.getRef().removeValue();
                        }
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(BookingHistoryActivity.this, "History cleared successfully", Toast.LENGTH_SHORT).show();
                        fullList.clear();
                        historyList.clear();
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(BookingHistoryActivity.this, "Failed to clear history", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchUserBookings() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        bookingsRef.orderByChild("bookedBy").equalTo(currentUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        fullList.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Booking booking = dataSnapshot.getValue(Booking.class);
                            if (booking != null) {
                                // Basic info setup
                                if (booking.getDate() == null && booking.getBookedTime() != null) {
                                    booking.setDate(booking.getBookedTime().get("date"));
                                }
                                if (booking.getTimeSlot() == null && booking.getBookedTime() != null) {
                                    booking.setTimeSlot(booking.getBookedTime().get("time"));
                                }

                                fetchScheduleDetails(booking);
                                fullList.add(booking);
                            }
                        }

                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        
                        // Refresh the view with current tab filter
                        if (tabLayoutFilter != null) {
                            int selectedTabPos = tabLayoutFilter.getSelectedTabPosition();
                            if (selectedTabPos != -1) {
                                filterHistory(tabLayoutFilter.getTabAt(selectedTabPos).getText().toString());
                            } else {
                                filterHistory("All");
                            }
                        } else {
                            filterHistory("All");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Database error: " + error.getMessage());
                    }
                });
    }

    private void updateEmptyState() {
        if (historyList.isEmpty()) {
            if (emptyTextView != null) emptyTextView.setVisibility(View.VISIBLE);
        } else {
            if (emptyTextView != null) emptyTextView.setVisibility(View.GONE);
        }
    }

    private void fetchScheduleDetails(Booking booking) {
        if (booking.getScheduleId() == null) return;

        DatabaseReference scheduleRef = FirebaseDatabase.getInstance().getReference("schedules").child(booking.getScheduleId());
        scheduleRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String spaceId = snapshot.child("spaceId").getValue(String.class);
                    if (spaceId == null) spaceId = snapshot.child("spaceID").getValue(String.class);

                    if (spaceId != null) {
                        fetchSpaceName(booking, spaceId);
                    }

                    String date = snapshot.child("date").getValue(String.class);
                    String time = snapshot.child("time").getValue(String.class);

                    if (date != null) booking.setDate(date);
                    if (time != null) booking.setTimeSlot(time);

                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchSpaceName(Booking booking, String spaceId) {
        DatabaseReference spaceRef = FirebaseDatabase.getInstance().getReference("spaces").child(spaceId);
        spaceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String spaceName = snapshot.child("roomName").getValue(String.class);
                    if (spaceName != null) {
                        booking.setSpaceName(spaceName);
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    public void onItemClick(Booking booking) {
        Intent intent = new Intent(this, BookingDetailsActivity.class);
        intent.putExtra("BOOKING_ID", booking.getBookingId());
        intent.putExtra("SPACE_NAME", booking.getSpaceName());
        intent.putExtra("DATE", booking.getDate());
        intent.putExtra("TIME_SLOT", booking.getTimeSlot());
        intent.putExtra("PURPOSE", booking.getPurpose());
        intent.putExtra("DESCRIPTION", booking.getDescription());
        intent.putExtra("STATUS", booking.getStatus());
        intent.putExtra("LOR_UPLOAD", booking.getLorUpload());
        intent.putExtra("REMARKS", booking.getRemarks());
        intent.putExtra("ACTION_BY", booking.getActionBy());
        intent.putExtra("APPROVED_BY", booking.getApprovedBy());
        intent.putExtra("SCHEDULE_ID", booking.getScheduleId());
        intent.putExtra("SLOT_START", booking.getSlotStart());

        String reqDate = "";
        String reqTime = "";
        if (booking.getBookedTime() != null) {
            reqDate = booking.getBookedTime().get("date");
            reqTime = booking.getBookedTime().get("time");
        }
        intent.putExtra("REQUESTED_ON", (reqDate != null ? reqDate : "") + " " + (reqTime != null ? reqTime : ""));

        boolean hasLor = booking.getLorUpload() != null && !booking.getLorUpload().isEmpty();
        intent.putExtra("HAS_LOR", hasLor);
        intent.putExtra("BOOKED_BY_ID", booking.getBookedBy());

        startActivity(intent);
    }
}
