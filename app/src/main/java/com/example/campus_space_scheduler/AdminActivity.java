package com.example.campus_space_scheduler;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.campus_space_scheduler.databinding.ActivityAdminBinding;
import com.google.firebase.database.*;

public class AdminActivity extends AppCompatActivity {
    private ActivityAdminBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Setup Adapter
        AdminPagerAdapter adapter = new AdminPagerAdapter(this);
        binding.viewPager.setAdapter(adapter);

        // 2. Sync BottomNav with ViewPager
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) binding.viewPager.setCurrentItem(0);
            else if (id == R.id.nav_schedule) binding.viewPager.setCurrentItem(1);
            else if (id == R.id.nav_users ) binding.viewPager.setCurrentItem(2);
            else if (id == R.id.nav_spaces) binding.viewPager.setCurrentItem(3);
            else if (id == R.id.nav_settings) binding.viewPager.setCurrentItem(4);
            return true;
        });

        // 3. Sync ViewPager with BottomNav (For Swiping)
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                switch (position) {
                    case 0:
                        binding.bottomNav.setSelectedItemId(R.id.nav_home);
                        binding.toolbar.setTitle("Admin Portal");
                        break;
                    case 1:
                        binding.bottomNav.setSelectedItemId(R.id.nav_schedule);
                        binding.toolbar.setTitle("Schedule Management");
                        break;
                    case 2:
                        binding.bottomNav.setSelectedItemId(R.id.nav_users);
                        binding.toolbar.setTitle("Manage Users");
                        break;
                    case 3:
                        binding.bottomNav.setSelectedItemId(R.id.nav_spaces);
                        binding.toolbar.setTitle("Manage Spaces");
                        break;
                    case 4:
                        binding.bottomNav.setSelectedItemId(R.id.nav_settings);
                        binding.toolbar.setTitle("Settings");
                        break;
                }
            }
        });
    }
}