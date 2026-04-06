package com.example.campus_space_scheduler.lab_admin_incharge;

import android.content.Intent;
import android.os.Bundle;
import com.example.campus_space_scheduler.databinding.ActivityFacultySelectionBinding;

public class FacultySelectionActivity extends BaseInchargeActivity {

    private ActivityFacultySelectionBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFacultySelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Sidebar using BaseActivity method
        setupDrawer(binding.toolbar, binding.navView, binding.drawerLayout);

        binding.cardSSL.setOnClickListener(v -> openDashboard("SSL"));
        binding.cardNSL.setOnClickListener(v -> openDashboard("NSL"));
        binding.cardBDL.setOnClickListener(v -> openDashboard("BDL"));
    }

    private void openDashboard(String labName) {
        Intent intent = new Intent(this, FacultyDashboardActivity.class);
        intent.putExtra("labName", labName);
        startActivity(intent);
    }
}