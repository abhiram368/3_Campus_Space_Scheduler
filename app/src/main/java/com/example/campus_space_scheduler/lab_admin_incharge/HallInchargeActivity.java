package com.example.campus_space_scheduler.lab_admin_incharge;

import android.content.Intent;
import android.os.Bundle;
import com.example.campus_space_scheduler.R;
import com.example.campus_space_scheduler.databinding.ActivityHallInchargeBinding;

public class HallInchargeActivity extends BaseInchargeActivity {

    private ActivityHallInchargeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHallInchargeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Sidebar using BaseActivity method
        setupDrawer(binding.toolbar, binding.navView, binding.drawerLayout);

        binding.cardPending.setOnClickListener(v -> {
            Intent intent = new Intent(this, PendingRequestsActivity.class);
            startActivity(intent);
        });

        binding.cardSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(this, ViewScheduleActivity.class);
            startActivity(intent);
        });

        binding.cardLiveStatus.setOnClickListener(v -> {
            Intent intent = new Intent(this, LiveStatusActivity.class);
            startActivity(intent);
        });

        binding.cardHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, ApprovalHistoryActivity.class);
            startActivity(intent);
        });
        
        binding.btnNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationsActivity.class);
            startActivity(intent);
        });
    }
}