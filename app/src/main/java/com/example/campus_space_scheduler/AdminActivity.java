package com.example.campus_space_scheduler;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.activity.OnBackPressedCallback;

import com.example.campus_space_scheduler.databinding.ActivityAdminBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private ActivityAdminBinding binding;
    private DatabaseReference usersRef, spacesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        drawerLayout = binding.drawerLayout;
        binding.navView.setNavigationItemSelectedListener(this);

        // 1. Initialize Firebase References
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        spacesRef = FirebaseDatabase.getInstance().getReference("spaces");

        // 2. Start Listening for Data Changes
        fetchLiveStats();

        setupAdminButtons(binding);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void fetchLiveStats() {
        // Listen for User Count
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                binding.cardUsers.statLabel.setText("Active Users");
                binding.cardUsers.statValue.setText(String.valueOf(count));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminActivity.this, "Failed to load users", Toast.LENGTH_SHORT).show();
            }
        });

        // Listen for Space Count
        spacesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                binding.cardSpaces.statLabel.setText("Total Spaces");
                binding.cardSpaces.statValue.setText(String.valueOf(count));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminActivity.this, "Failed to load spaces", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAdminButtons(ActivityAdminBinding binding) {
        binding.btnManageUsers.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminManagementActivity.class);
            intent.putExtra("MANAGEMENT_MODE", "USER");
            startActivity(intent);
        });

        binding.btnManageSpaces.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminManagementActivity.class);
            intent.putExtra("MANAGEMENT_MODE", "SPACE");
            startActivity(intent);
        });

        binding.btnManageSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(this, SpaceSelectionActivity.class);
            startActivity(intent);
        });

        binding.btnViewLogs.setOnClickListener(v -> {
            Intent intent = new Intent(this, ViewLogsActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_settings) {
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}