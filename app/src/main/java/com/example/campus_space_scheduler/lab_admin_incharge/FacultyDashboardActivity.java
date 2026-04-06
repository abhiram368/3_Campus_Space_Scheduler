package com.example.campus_space_scheduler.lab_admin_incharge;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import com.example.campus_space_scheduler.R;
import com.example.campus_space_scheduler.databinding.ActivityFacultyDashboardBinding;

public class FacultyDashboardActivity extends BaseInchargeActivity {

    private ActivityFacultyDashboardBinding binding;
    private String labName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFacultyDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        labName = getIntent().getStringExtra("labName");
        
        // Setup Sidebar using BaseActivity method
        setupDrawer(binding.toolbar, binding.navView, binding.drawerLayout);

        if (labName != null) {
            binding.tvFacultyLabName.setText(labName + " Incharge");
        }

        // Card Clicks
        binding.cardFacultyPending.setOnClickListener(v -> {
            Intent intent = new Intent(this, FacultyPendingRequestsActivity.class);
            intent.putExtra("labName", labName);
            startActivity(intent);
        });

        binding.cardFacultyLiveStatus.setOnClickListener(v -> {
            Intent intent = new Intent(this, FacultyLiveStatusActivity.class);
            intent.putExtra("labName", labName);
            startActivity(intent);
        });

        binding.cardFacultySchedule.setOnClickListener(v -> {
            Intent intent = new Intent(this, FacultyViewScheduleActivity.class);
            intent.putExtra("labName", labName);
            startActivity(intent);
        });

        binding.cardFacultyHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, FacultyApprovalHistoryActivity.class);
            intent.putExtra("labName", labName);
            startActivity(intent);
        });

        binding.cardFacultyLabDetails.setOnClickListener(v -> {
            Intent intent = new Intent(this, LabDetailsActivity.class);
            intent.putExtra("labName", labName);
            startActivity(intent);
        });

        binding.btnNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationsActivity.class);
            startActivity(intent);
        });
    }
}