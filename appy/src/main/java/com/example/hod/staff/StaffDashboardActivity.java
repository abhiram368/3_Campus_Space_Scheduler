package com.example.hod.staff;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.hod.R;
import com.example.hod.models.Booking;
import com.example.hod.repository.FirebaseRepository;
import com.example.hod.utils.Result;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StaffDashboardActivity extends AppCompatActivity {

    private static final String TAG = "StaffDashboardActivity";
    private com.google.firebase.database.ValueEventListener dashboardPendingListener;
    private com.google.firebase.database.ValueEventListener notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_dashboard);

        // Header Configuration
        View headerView = findViewById(R.id.header_layout);
        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navView = findViewById(R.id.navigationView);

        if (headerView != null) {
            TextView title = headerView.findViewById(R.id.header_title);
            TextView subtitle = headerView.findViewById(R.id.header_subtitle);
            View btnBack = headerView.findViewById(R.id.btnBack);
            View menuIcon = headerView.findViewById(R.id.menuIcon);

            if (title != null) title.setText(R.string.staff_dashboard_title);
            if (subtitle != null) {
                String userName = getIntent().getStringExtra("userName");
                if (userName != null && !userName.isEmpty()) {
                    String capitalized = userName.substring(0, 1).toUpperCase(Locale.getDefault()) + userName.substring(1).toLowerCase(Locale.getDefault());
                    subtitle.setText(capitalized);
                } else {
                    subtitle.setText(R.string.subtitle_staff_portal);
                }
            }

            // Dashboard setup: Hide back, Show hamburger
            if (btnBack != null) btnBack.setVisibility(View.GONE);
            if (menuIcon != null) {
                menuIcon.setVisibility(View.VISIBLE);
                menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
            }
        }

        // Extract labId FIRST so it can be used in the drawer listener below
        String currentLabId = getIntent().getStringExtra("labId");
        final String labIdToPass;
        if (currentLabId == null || currentLabId.isEmpty()) {
            Toast.makeText(this, R.string.error_no_lab_assigned, Toast.LENGTH_LONG).show();
            labIdToPass = "";
        } else {
            labIdToPass = currentLabId;
        }

        // Setup Navigation Drawer Header Data
        if (navView != null) {
            View navHeader = navView.getHeaderView(0);
            TextView tvName = navHeader.findViewById(R.id.tvName);
            TextView tvRole = navHeader.findViewById(R.id.tvRole);
            TextView tvInitial = navHeader.findViewById(R.id.tvInitial);

            String userName = getIntent().getStringExtra("userName");
            if (userName != null && !userName.isEmpty()) {
                String capitalized = userName.substring(0, 1).toUpperCase(Locale.getDefault()) + userName.substring(1).toLowerCase(Locale.getDefault());
                tvName.setText(capitalized);
                tvInitial.setText(userName.substring(0, 1).toUpperCase(Locale.getDefault()));
            } else {
                tvName.setText(R.string.label_staff_user);
                tvInitial.setText("S");
            }
            tvRole.setText(R.string.role_staff_incharge);

            // Remove My Profile and Booking History as requested
            navView.getMenu().findItem(R.id.nav_profile).setVisible(false);
            navView.getMenu().findItem(R.id.nav_history).setVisible(false);

            // Navigate to profile when touching the header ("blue box")
            navHeader.setOnClickListener(v -> {
                startActivity(new Intent(this, StaffProfileActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });

            navView.setItemIconTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#3B82F6")));

            navView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_profile) {
                    startActivity(new Intent(this, StaffProfileActivity.class));
                } else if (id == R.id.nav_blocked_students) {
                    Intent blockedIntent = new Intent(this, StaffBlockedUsersActivity.class);
                    blockedIntent.putExtra("labId", labIdToPass);
                    startActivity(blockedIntent);
                } else if (id == R.id.nav_history) {
                    Toast.makeText(StaffDashboardActivity.this, R.string.msg_feature_to_be_implemented, Toast.LENGTH_SHORT).show();
                } else if (id == R.id.nav_dark) {
                    // Switch handled via ActionView logic below
                    return false;
                } else if (id == R.id.nav_data_reset) {
                    Intent dataUtilityIntent = new Intent(this, DataUtilityActivity.class);
                    dataUtilityIntent.putExtra("labId", labIdToPass);
                    startActivity(dataUtilityIntent);
                } else if (id == R.id.nav_notifications) {
                    startActivity(new Intent(this, NotificationsActivity.class));
                } else if (id == R.id.nav_help) {
                    startActivity(new Intent(this, HelpActivity.class));
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
            MenuItem darkItem = navView.getMenu().findItem(R.id.nav_dark);
            if (darkItem != null && darkItem.getActionView() != null) {
                View actionView = darkItem.getActionView();
                if (actionView instanceof SwitchCompat) {
                    SwitchCompat darkSwitch = (SwitchCompat) actionView;
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
            }
            
            observeNotifications(navView);
        }

        // Map the IDs correctly from activity_staff_dashboard.xml
        CardView btnEscalated = findViewById(R.id.btnEscalated);
        CardView btnLiveStatus = findViewById(R.id.btnLiveStatus);
        CardView btnViewSchedule = findViewById(R.id.btnViewSchedule);
        CardView btnApprovalHistory = findViewById(R.id.btnApprovalHistory);
        CardView btnLabDetails = findViewById(R.id.btnLabDetails);
        CardView btnLabAdmins = findViewById(R.id.btnLabAdmins);
        View btnBook = findViewById(R.id.btnBook);

        // Profile icon – top-right of header
        View btnProfile = findViewById(R.id.btnProfile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v ->
                    startActivity(new Intent(StaffDashboardActivity.this, StaffProfileActivity.class)));
        }

        if (btnEscalated != null) {
            btnEscalated.setOnClickListener(v -> {
                Intent intent = new Intent(StaffDashboardActivity.this, PendingRequestsActivity.class);
                intent.putExtra("labId", labIdToPass);
                startActivity(intent);
            });
        }

        if (btnLiveStatus != null) {
            btnLiveStatus.setOnClickListener(v -> {
                Intent intent = new Intent(StaffDashboardActivity.this, LabLiveStatusActivity.class);
                intent.putExtra("labId", labIdToPass);
                startActivity(intent);
            });
        }

        if (btnViewSchedule != null) {
            btnViewSchedule.setOnClickListener(v -> {
                Intent intent = new Intent(StaffDashboardActivity.this, ViewScheduleActivity.class);
                intent.putExtra("labId", labIdToPass);
                startActivity(intent);
            });
        }

        if (btnApprovalHistory != null) {
            btnApprovalHistory.setOnClickListener(v -> {
                Intent intent = new Intent(StaffDashboardActivity.this, ApprovalHistoryActivity.class);
                intent.putExtra("labId", labIdToPass);
                startActivity(intent);
            });
        }

        if (btnLabDetails != null) {
            btnLabDetails.setOnClickListener(v -> {
                Intent intent = new Intent(StaffDashboardActivity.this, LabDetailsActivity.class);
                intent.putExtra("labId", labIdToPass);
                startActivity(intent);
            });
        }

        if (btnLabAdmins != null) {
            btnLabAdmins.setOnClickListener(v -> {
                Intent intent = new Intent(StaffDashboardActivity.this, LabAdminsHomeActivity.class);
                intent.putExtra("labId", labIdToPass);
                startActivity(intent);
            });
        }


        // Feature disabled as per request
      if (btnBook != null) {
            btnBook.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent();
                    // Using full class name for cross-module navigation
                    intent.setClassName(StaffDashboardActivity.this,
                            "com.example.campus_space_scheduler.booking_user.BookingUserActivity");
                    intent.putExtra("ROLE", "Staff");
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(StaffDashboardActivity.this,
                            R.string.error_unable_open_booking, Toast.LENGTH_SHORT).show();
                }
            });
        }


        if (!labIdToPass.isEmpty()) {
            // Lazy Sync: Perform weekly maintenance (Monday check)
            new FirebaseRepository().checkAndPerformWeeklyMaintenance(labIdToPass, "Your Lab", result -> {
                if (result instanceof Result.Success) {
                    android.util.Log.d("Maintenance", "Weekly maintenance/14-day sync completed.");
                }
            });
            loadInsights(labIdToPass);
        }
    }

    private void observeNotifications(NavigationView navView) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

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
        MenuItem item = navView.getMenu().findItem(R.id.nav_notifications);
        if (item == null) return;
        
        if (count > 0) {
            item.setTitle(getString(R.string.label_notifications_unread, count));
        } else {
            item.setTitle(R.string.notification_title); // Assuming nav_notifications exists in menu XML or R class
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dashboardPendingListener != null) {
            new FirebaseRepository().removePendingRequestsListener(dashboardPendingListener);
        }
        if (notificationListener != null) {
            String uid = FirebaseAuth.getInstance().getUid();
            if (uid != null) {
                FirebaseDatabase.getInstance().getReference("notifications")
                        .child(uid)
                        .removeEventListener(notificationListener);
            }
        }
    }

    private void loadInsights(String labId) {
        TextView tvWeeklyBookings = findViewById(R.id.tvWeeklyBookings);
        TextView tvPendingCount = findViewById(R.id.tvPendingCount);
        TextView tvBlockedCount = findViewById(R.id.tvBlockedCount);
        TextView tvActiveSlots = findViewById(R.id.tvActiveSlots);

        FirebaseDatabase db = FirebaseDatabase.getInstance();

        // 1a. Pending count
        FirebaseRepository insightRepo = new FirebaseRepository();
        dashboardPendingListener = insightRepo.observePendingRequests(labId, result -> {
            if (result instanceof Result.Success) {
                List<Booking> pending = ((Result.Success<List<Booking>>) result).data;
                int count = pending != null ? pending.size() : 0;
                if (tvPendingCount != null)
                    tvPendingCount.setText(String.format(Locale.getDefault(), "%02d", count));
            }
        });

        // 1b. Weekly bookings for this lab
        db.getReference("bookings").orderByChild("spaceId").equalTo(labId)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    long weeklyTotal = 0;

                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    Date startOfWeek = cal.getTime();

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                    for (DataSnapshot child : snapshot.getChildren()) {
                        String dateStr = child.child("date").getValue(String.class);
                        DataSnapshot bookedTimeSnap = child.child("bookedTime");
                        if (bookedTimeSnap.exists() && bookedTimeSnap.child("date").getValue(String.class) != null) {
                            dateStr = bookedTimeSnap.child("date").getValue(String.class);
                        }
                        if (dateStr != null) {
                            try {
                                Date bookingDate = sdf.parse(dateStr);
                                if (bookingDate != null && !bookingDate.before(startOfWeek)) {
                                    weeklyTotal++;
                                }
                            } catch (Exception e) {}
                        }
                    }
                    if (tvWeeklyBookings != null) tvWeeklyBookings.setText(String.valueOf(weeklyTotal));
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });

        // 2. Blocked Students
        String currentStaffUid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                                 FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        db.getReference("users").orderByChild("blockedBy").equalTo(currentStaffUid)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    long blocked = snapshot.getChildrenCount();
                    if (tvBlockedCount != null) tvBlockedCount.setText(String.valueOf(blocked));
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });

        // 3. Available Slots
        db.getReference("schedules")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    long available = 0;

                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    Date startOfWeek = cal.getTime();

                    cal.add(Calendar.DAY_OF_YEAR, 7);
                    Date endOfWeek = cal.getTime();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                    for (DataSnapshot scheduleSnap : snapshot.getChildren()) {
                        String key = scheduleSnap.getKey();
                        if (key != null && key.startsWith(labId + "_")) {
                            String dateStr = key.replace(labId + "_", "");
                            try {
                                Date date = sdf.parse(dateStr);
                                if (date != null && !date.before(startOfWeek) && date.before(endOfWeek)) {
                                    DataSnapshot slotsSnap = scheduleSnap.child("slots");
                                    if (slotsSnap.exists()) {
                                        for (DataSnapshot child : slotsSnap.getChildren()) {
                                            String status = child.child("status").getValue(String.class);
                                            if ("AVAILABLE".equalsIgnoreCase(status)) {
                                                available++;
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {}
                        }
                    }
                    if (tvActiveSlots != null) tvActiveSlots.setText(String.valueOf(available));
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
    }
}
