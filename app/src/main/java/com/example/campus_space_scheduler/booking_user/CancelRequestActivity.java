package com.example.campus_space_scheduler.booking_user;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_space_scheduler.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CancelRequestActivity extends AppCompatActivity implements BookingAdapter.OnItemClickListener {

    private static final String TAG = "CancelRequestActivity";
    private RecyclerView recyclerViewBookings;
    private BookingAdapter adapter;
    private List<Booking> cancellableList; // Currently shown list
    private List<Booking> fullCancellableList; // All valid records for cancellation
    private ProgressBar progressBar;
    private TextView emptyTextView;
    private TabLayout tabLayoutFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.t_activity_cancel_request);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        recyclerViewBookings = findViewById(R.id.recyclerViewBookings);
        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.emptyTextView);
        tabLayoutFilter = findViewById(R.id.tabLayoutFilter);

        cancellableList = new ArrayList<>();
        fullCancellableList = new ArrayList<>();
        adapter = new BookingAdapter(cancellableList, this);

        recyclerViewBookings.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewBookings.setAdapter(adapter);

        setupTabLayout();
        fetchCancellableBookings();
    }

    private void setupTabLayout() {
        if (tabLayoutFilter == null) return;
        
        tabLayoutFilter.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterCancellableRequests(tab.getText().toString());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void filterCancellableRequests(String filterText) {
        cancellableList.clear();
        if (filterText.equalsIgnoreCase("All")) {
            cancellableList.addAll(fullCancellableList);
        } else {
            for (Booking booking : fullCancellableList) {
                String status = booking.getStatus();
                if (status == null) continue;

                if (filterText.equalsIgnoreCase("Forwarded")) {
                    if (status.toLowerCase().contains("forwarded")) {
                        cancellableList.add(booking);
                    }
                } else if (filterText.equalsIgnoreCase("Approved")) {
                    if (status.equalsIgnoreCase("Approved") || status.equalsIgnoreCase("Accepted")) {
                        cancellableList.add(booking);
                    }
                } else {
                    if (status.equalsIgnoreCase(filterText)) {
                        cancellableList.add(booking);
                    }
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void fetchCancellableBookings() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        DatabaseReference bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");
        bookingsRef.orderByChild("bookedBy").equalTo(currentUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        fullCancellableList.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Booking booking = dataSnapshot.getValue(Booking.class);
                            if (booking != null) {
                                String status = booking.getStatus();
                                // Only show requests that are NOT Rejected, Cancelled, or Expired
                                if (status != null && !status.equalsIgnoreCase("Rejected") 
                                        && !status.equalsIgnoreCase("Cancelled")
                                        && !status.toLowerCase().contains("expired")) {

                                    if (booking.getDate() == null && booking.getBookedTime() != null) {
                                        booking.setDate(booking.getBookedTime().get("date"));
                                    }
                                    if (booking.getTimeSlot() == null && booking.getBookedTime() != null) {
                                        booking.setTimeSlot(booking.getBookedTime().get("time"));
                                    }

                                    fetchScheduleDetails(booking);
                                    fullCancellableList.add(booking);
                                }
                            }
                        }

                        // Sort by recent first
                        Collections.reverse(fullCancellableList);

                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        
                        // Apply current tab filter
                        if (tabLayoutFilter != null) {
                            int selectedTabPos = tabLayoutFilter.getSelectedTabPosition();
                            if (selectedTabPos != -1) {
                                filterCancellableRequests(tabLayoutFilter.getTabAt(selectedTabPos).getText().toString());
                            } else {
                                filterCancellableRequests("All");
                            }
                        } else {
                            filterCancellableRequests("All");
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
        if (cancellableList.isEmpty()) {
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
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching schedule: " + error.getMessage());
            }
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
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching space name: " + error.getMessage());
            }
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
        intent.putExtra("REMARKS", booking.getRemark());
        intent.putExtra("ACTION_BY", booking.getActionBy());
        intent.putExtra("APPROVED_BY", booking.getApprovedBy());
        intent.putExtra("SCHEDULE_ID", booking.getScheduleId());
        intent.putExtra("SLOT_START", booking.getSlotStart());
        intent.putExtra("SPACE_TYPE", booking.getSpaceType()); // Added SPACE_TYPE

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

        // Indicate that this comes from Cancellation screen so "Cancel" button is visible
        intent.putExtra("SHOW_CANCEL_BUTTON", true);

        startActivity(intent);
    }
}
