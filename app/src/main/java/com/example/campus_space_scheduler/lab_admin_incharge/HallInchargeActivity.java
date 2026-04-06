package com.example.campus_space_scheduler.lab_admin_incharge;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.campus_space_scheduler.R;
import com.example.campus_space_scheduler.databinding.ActivityHallInchargeBinding;
import com.example.campus_space_scheduler.lab_admin_incharge.utils.InchargeNotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HallInchargeActivity extends BaseInchargeActivity {

    private ActivityHallInchargeBinding binding;
    private DatabaseReference bookingsRef;
    private final List<String> allowedSpaces = Arrays.asList("CSED Seminar Hall", "CSED Discussion Room", "APJ Hall");
    private Set<String> notifiedBookings = new HashSet<>();
    private String syncStartTimeKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHallInchargeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupDrawer(binding.toolbar, binding.navView, binding.drawerLayout);
        bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");
        
        syncStartTimeKey = bookingsRef.push().getKey();

        binding.cardPending.setOnClickListener(v -> startActivity(new Intent(this, PendingRequestsActivity.class)));
        binding.cardSchedule.setOnClickListener(v -> startActivity(new Intent(this, ViewScheduleActivity.class)));
        binding.cardLiveStatus.setOnClickListener(v -> startActivity(new Intent(this, LiveStatusActivity.class)));
        binding.cardHistory.setOnClickListener(v -> startActivity(new Intent(this, ApprovalHistoryActivity.class)));
        binding.btnNotificationsContainer.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));

        setupInstantNotificationListener();
        checkUnreadNotifications();
    }

    private void setupInstantNotificationListener() {
        bookingsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (snapshot.getKey() != null && snapshot.getKey().compareTo(syncStartTimeKey) > 0) {
                    checkAndNotify(snapshot);
                }
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
        String status = String.valueOf(snapshot.child("status").getValue());
        String spaceName = String.valueOf(snapshot.child("spaceName").getValue());
        String bookingId = snapshot.getKey();

        if ("Pending".equalsIgnoreCase(status) && allowedSpaces.contains(spaceName) && !notifiedBookings.contains(bookingId)) {
            notifiedBookings.add(bookingId);
            String userId = String.valueOf(snapshot.child("bookedBy").getValue());
            fetchUserAndNotify(userId, spaceName, bookingId);
        }
    }

    private void fetchUserAndNotify(String userId, String spaceName, String bookingId) {
        FirebaseDatabase.getInstance().getReference("users").child(userId).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String userName = snapshot.exists() ? String.valueOf(snapshot.getValue()) : "A user";
                        String currentUid = FirebaseAuth.getInstance().getUid();
                        if (currentUid != null) {
                            InchargeNotificationHelper.sendAndSaveNotification(
                                    HallInchargeActivity.this, currentUid,
                                    "New Booking Request",
                                    userName + " requested " + spaceName,
                                    "pending_request", bookingId
                            );
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void checkUnreadNotifications() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        
        FirebaseDatabase.getInstance().getReference("notifications").child(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean hasUnread = false;
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Boolean read = ds.child("read").getValue(Boolean.class);
                            if (read != null && !read) {
                                hasUnread = true;
                                break;
                            }
                        }
                        binding.notificationBadge.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}
