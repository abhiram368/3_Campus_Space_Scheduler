package com.example.hod.hod;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.campussync.appy.R;
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
    }

    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, HodProfileActivity.class));
            } else if (id == R.id.nav_dark) {
                // Switch handled via ActionView logic below
                return false;
            } else if (id == R.id.nav_notifications) {
                startActivity(new Intent(this, HodNotificationsActivity.class));
            } else if (id == R.id.nav_help) {
                startActivity(new Intent(this, HodHelpAboutActivity.class));
            } else if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                try {
                    Intent intent = new Intent();
                    intent.setClassName(this, "com.example.campus_space_scheduler.LoginActivity");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    Toast.makeText(this, "Logout failed", Toast.LENGTH_SHORT).show();
                }
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
        View navHeader = navigationView.getHeaderView(0);
        if (navHeader != null) {
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

        findViewById(R.id.btnBook).setOnClickListener(v ->
                Toast.makeText(this, "Resource booking flow disabled for HOD", Toast.LENGTH_SHORT).show());
    }

    private void fetchLiveInsights() {
        FirebaseDatabase.getInstance().getReference("bookings")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long activeCount = 0;
                        for (DataSnapshot s : snapshot.getChildren()) {
                            String stat = s.child("status").getValue(String.class);
                            if ("APPROVED".equalsIgnoreCase(stat) || "BOOKED".equalsIgnoreCase(stat) || "COMPLETED".equalsIgnoreCase(stat)) {
                                activeCount++;
                            }
                        }
                        if (tvTotalBookings != null) {
                            tvTotalBookings.setText(activeCount + " Active Bookings");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}
