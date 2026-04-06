package com.example.hod.hod;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.hod.R;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HodDashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView tvTotalBookings;
    private ValueEventListener notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hod_dashboard);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        tvTotalBookings = findViewById(R.id.tvTotalBookings);

        // Header Configuration
        View headerView = findViewById(R.id.header_layout);
        if (headerView != null) {
            TextView title = headerView.findViewById(R.id.header_title);
            TextView subtitle = headerView.findViewById(R.id.header_subtitle);
            View btnBack = headerView.findViewById(R.id.btnBack);
            View menuIcon = headerView.findViewById(R.id.menuIcon);
            
            if (title != null) title.setText(getString(R.string.hod_dashboard_title));
            if (subtitle != null) subtitle.setText(getString(R.string.role_hod));
            if (btnBack != null) btnBack.setVisibility(View.GONE);
            if (menuIcon != null) {
                menuIcon.setVisibility(View.VISIBLE);
                menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
            }
        }

        setupNavigationDrawer();
        
        setupDashboardCards();
        fetchLiveInsights();

        // Universal Auto-Expiry Sweep: HOD performs a system-wide cleanup on load
        new com.example.hod.repository.FirebaseRepository().performAutoExpirySweep(null, result -> {
            if (result instanceof com.example.hod.utils.Result.Success) {
                int count = ((com.example.hod.utils.Result.Success<Integer>) result).data;
                if (count > 0) {
                    android.util.Log.d("HodDashboard", "Auto-Expiry Sweep: System-wide cleanup of " + count + " requests.");
                }
            }
        });

        // new com.example.hod.repository.FirebaseRepository().cleanupAllSchedules(result -> {
        //     if (result instanceof com.example.hod.utils.Result.Success) {
        //         Toast.makeText(this, "Schedule Data Cleaned Up!", Toast.LENGTH_SHORT).show();
        //     }
        // });

        // Notification Deep-link Handling
        if (getIntent().getBooleanExtra("OPEN_NOTIFICATIONS", false)) {
            startActivity(new Intent(this, HodNotificationsActivity.class));
        }
    }

    private void setupNavigationDrawer() {
        navigationView.setItemIconTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#3B82F6")));
        
        // Remove non-essential utilities from the drawer
        // MenuItem navDataReset = navigationView.getMenu().findItem(R.id.nav_data_reset);
        // if (navDataReset != null) navDataReset.setVisible(false);
        
        MenuItem navHistory = navigationView.getMenu().findItem(R.id.nav_history);
        if (navHistory != null) navHistory.setVisible(false);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, HodProfileActivity.class));
            } else if (id == R.id.nav_dark) {
                // Switch handled via ActionView logic below
                return false;
            } else if (id == R.id.nav_notifications) {
                startActivity(new Intent(this, HodNotificationsActivity.class));
            } else if (id == R.id.nav_data_reset) {
                Intent dataUtilityIntent = new Intent(this, com.example.hod.staff.DataUtilityActivity.class);
                startActivity(dataUtilityIntent);
            } else if (id == R.id.nav_help) {
                startActivity(new Intent(this, HodHelpAboutActivity.class));
            } else if (id == R.id.nav_logout) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dialog_title_logout)
                    .setMessage(R.string.dialog_msg_logout)
                    .setPositiveButton(R.string.btn_confirm_logout, (dialog, which) -> {
                        FirebaseAuth.getInstance().signOut();
                        try {
                            Intent intent = new Intent();
                            intent.setClassName(this, "com.example.campus_space_scheduler.LoginActivity");
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } catch (Exception e) {
                            Toast.makeText(this, R.string.msg_logout_failed, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.btn_no, null)
                    .show();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Handle Dark Mode Switch
        android.view.MenuItem darkItem = navigationView.getMenu().findItem(R.id.nav_dark);
        if (darkItem != null && darkItem.getActionView() != null) {
            MaterialSwitch darkSwitch = (MaterialSwitch) darkItem.getActionView();
            // Sync switch state with current mode
            int currentMode = AppCompatDelegate.getDefaultNightMode();
            darkSwitch.setChecked(currentMode == AppCompatDelegate.MODE_NIGHT_YES);
            
            darkSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
            });
        }

        // Update Nav Header with real user data
        // Ensure notification service is running
        try {
            Intent serviceIntent = new Intent(this, com.example.hod.utils.NotificationService.class);
            startService(serviceIntent);
        } catch (Exception e) {
            android.util.Log.e("HodDashboard", "Failed to refresh NotificationService: " + e.getMessage());
        }

        View navHeader = navigationView.getHeaderView(0);
        if (navHeader != null) {
            // Navigate to profile when touching the header
            navHeader.setOnClickListener(v -> {
                startActivity(new Intent(this, HodProfileActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });

            TextView tvName = navHeader.findViewById(R.id.tvName);
            TextView tvRole = navHeader.findViewById(R.id.tvRole);
            TextView tvInitial = navHeader.findViewById(R.id.tvInitial);

            tvRole.setText(getString(R.string.role_hod));
            String uid = FirebaseAuth.getInstance().getUid();
            if (uid != null) {
                FirebaseDatabase.getInstance().getReference("users").child(uid)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String name = snapshot.child("name").getValue(String.class);
                                if (name != null && !name.isEmpty()) {
                                    tvName.setText(name);
                                    tvInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
                
                observeNotifications(navigationView, uid);
            }
        }
    }

    private void observeNotifications(NavigationView navView, String uid) {
        notificationListener = FirebaseDatabase.getInstance().getReference("notifications")
                .child(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int unreadCount = 0;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Boolean read = child.child("read").getValue(Boolean.class);
                            if (read != null && !read) {
                                unreadCount++;
                            }
                        }
                        updateNotificationBadge(navView, unreadCount);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void updateNotificationBadge(@NonNull NavigationView navView, int count) {
        android.view.MenuItem item = navView.getMenu().findItem(R.id.nav_notifications);
        if (item == null) return;
        
        if (count > 0) {
            item.setTitle(getString(R.string.label_notifications_unread, count));
        } else {
            item.setTitle(R.string.notification_title); // Assuming nav_notifications exists in menu XML or I should add it
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) {
            String uid = FirebaseAuth.getInstance().getUid();
            if (uid != null) {
                FirebaseDatabase.getInstance().getReference("notifications")
                        .child(uid)
                        .removeEventListener(notificationListener);
            }
        }
    }

    private void setupDashboardCards() {
        findViewById(R.id.btnEscalatedRequests).setOnClickListener(v ->
                startActivity(new Intent(this, HodEscalatedRequestsActivity.class)));

        findViewById(R.id.btnLiveStatus).setOnClickListener(v ->
                startActivity(new Intent(this, LiveStatusActivity.class)));

        findViewById(R.id.btnViewSchedule).setOnClickListener(v ->
                startActivity(new Intent(this, HodViewScheduleHomeActivity.class)));

        findViewById(R.id.btnApprovalHistory).setOnClickListener(v ->
                startActivity(new Intent(this, HodApprovalHistoryActivity.class)));

        findViewById(R.id.btnProfile).setOnClickListener(v ->
                startActivity(new Intent(this, HodProfileActivity.class)));
        findViewById(R.id.btnBook).setOnClickListener(v -> {
            try {
                Intent intent = new Intent();
                // Use full class name for cross-module navigation
                intent.setClassName(this,
                        "com.example.campus_space_scheduler.booking_user.BookingUserActivity");
                intent.putExtra("ROLE", "HOD");
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open booking screen",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchLiveInsights() {
        FirebaseDatabase.getInstance().getReference("bookings")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long activeCount = 0;
                        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
                        
                        for (DataSnapshot s : snapshot.getChildren()) {
                            String stat = s.child("status").getValue(String.class);
                            String bDate = s.child("date").getValue(String.class);
                            
                            if (stat == null) continue;
                            
                            boolean isActiveStatus = stat.equalsIgnoreCase("APPROVED") || 
                                                   stat.equalsIgnoreCase("BOOKED") || 
                                                   stat.toLowerCase().contains("forwarded");
                                                   
                            if (isActiveStatus && bDate != null) {
                                if (bDate.compareTo(today) > 0) {
                                    // Future date
                                    activeCount++;
                                } else if (bDate.equals(today)) {
                                    // Today - check if slot is completed
                                    String nowTime = new java.text.SimpleDateFormat("HHmm", java.util.Locale.getDefault()).format(new java.util.Date());
                                    String tSlot = s.child("timeSlot").getValue(String.class);
                                    String sStart = s.child("slotStart").getValue(String.class);
                                    
                                    if (tSlot != null && tSlot.contains("-")) {
                                        String endPart = tSlot.split("-")[1].trim().replace(":", "");
                                        if (endPart.compareTo(nowTime) > 0) {
                                            activeCount++;
                                        }
                                    } else if (sStart != null && sStart.compareTo(nowTime) > 0) {
                                        activeCount++;
                                    }
                                }
                            }
                        }
                        if (tvTotalBookings != null) {
                            tvTotalBookings.setText(getString(R.string.label_active_bookings_count, (int) activeCount));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}