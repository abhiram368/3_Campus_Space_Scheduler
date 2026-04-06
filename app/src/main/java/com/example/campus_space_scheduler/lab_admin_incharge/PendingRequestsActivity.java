package com.example.campus_space_scheduler.lab_admin_incharge;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campus_space_scheduler.R;
import com.example.campus_space_scheduler.databinding.ActivityPendingRequestsBinding;
import com.example.campus_space_scheduler.lab_admin_incharge.adapters.PendingRequestsAdapter;
import com.example.campus_space_scheduler.lab_admin_incharge.models.Booking;
import com.example.campus_space_scheduler.lab_admin_incharge.utils.InchargeNotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PendingRequestsActivity extends BaseInchargeActivity {

    private ActivityPendingRequestsBinding binding;
    private PendingRequestsAdapter adapter;
    private List<Booking> bookingList;
    private DatabaseReference databaseReference;
    private final List<String> allowedSpaces = Arrays.asList("CSED Seminar Hall", "CSED Discussion Room", "APJ Hall");
    private long activityStartTime;
    private Set<String> notifiedBookings = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPendingRequestsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        activityStartTime = System.currentTimeMillis();

        // Setup Sidebar using BaseActivity method
        setupDrawer(binding.toolbar, binding.navView, binding.drawerLayout);

        binding.rvPendingRequests.setLayoutManager(new LinearLayoutManager(this));
        bookingList = new ArrayList<>();
        
        adapter = new PendingRequestsAdapter(bookingList, new PendingRequestsAdapter.OnActionClickListener() {
            @Override
            public void onItemClick(Booking booking) {
                Intent intent = new Intent(PendingRequestsActivity.this, BookingDetailsActivity.class);
                intent.putExtra("bookingId", booking.getBookingId());
                startActivity(intent);
            }
        });
        
        binding.rvPendingRequests.setAdapter(adapter);
        
        databaseReference = FirebaseDatabase.getInstance().getReference("bookings");
        fetchPendingRequests();
        setupNotificationListener();
    }

    private void fetchPendingRequests() {
        binding.progressBar.setVisibility(View.VISIBLE);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bookingList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String spaceName = getStringValue(dataSnapshot, "spaceName").trim();
                    String status = getStringValue(dataSnapshot, "status").trim();

                    if ("Pending".equalsIgnoreCase(status) && allowedSpaces.contains(spaceName)) {
                        Booking booking = new Booking();
                        booking.setBookingId(dataSnapshot.getKey());
                        booking.setSpaceName(spaceName);
                        booking.setStatus(status);
                        booking.setPurpose(getStringValue(dataSnapshot, "purpose"));
                        booking.setDate(getStringValue(dataSnapshot, "date"));
                        booking.setTimeSlot(getStringValue(dataSnapshot, "timeSlot"));
                        booking.setBookedBy(getStringValue(dataSnapshot, "bookedBy"));
                        
                        bookingList.add(booking);
                    }
                }
                
                binding.progressBar.setVisibility(View.GONE);
                binding.tvNoRequests.setVisibility(bookingList.isEmpty() ? View.VISIBLE : View.GONE);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(PendingRequestsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupNotificationListener() {
        databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Ignore bookings added before activity started to avoid flood of old notifications
                // In a real app, you'd use a server timestamp or FCM
                checkAndNotify(snapshot);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                checkAndNotify(snapshot);
            }

            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkAndNotify(DataSnapshot snapshot) {
        String status = getStringValue(snapshot, "status");
        String spaceName = getStringValue(snapshot, "spaceName");
        String bookingId = snapshot.getKey();

        if ("Pending".equalsIgnoreCase(status) && allowedSpaces.contains(spaceName) && !notifiedBookings.contains(bookingId)) {
            notifiedBookings.add(bookingId);
            
            // Only notify if it's a new or recently updated request while the user is active
            String userId = getStringValue(snapshot, "bookedBy");
            fetchUserNameAndNotify(userId, spaceName, bookingId);
        }
    }

    private void fetchUserNameAndNotify(String userId, String spaceName, String bookingId) {
        FirebaseDatabase.getInstance().getReference("users").child(userId).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String userName = snapshot.exists() ? String.valueOf(snapshot.getValue()) : "A user";
                        String currentUid = FirebaseAuth.getInstance().getUid();
                        
                        if (currentUid != null) {
                            InchargeNotificationHelper.sendAndSaveNotification(
                                    PendingRequestsActivity.this,
                                    currentUid,
                                    "New Pending Request",
                                    userName + " has requested to book " + spaceName,
                                    "pending_request",
                                    bookingId
                            );
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private String getStringValue(DataSnapshot snapshot, String key) {
        Object value = snapshot.child(key).getValue();
        if (value == null) return "N/A";
        return String.valueOf(value);
    }
}
