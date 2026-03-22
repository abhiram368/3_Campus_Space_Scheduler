package com.example.campus_space_scheduler.csed_office;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.campus_space_scheduler.R;
import com.example.campus_space_scheduler.databinding.PActivityCsedOfficeStaffBinding;

public class CsedOfficeStaffActivity extends AppCompatActivity {
    private PActivityCsedOfficeStaffBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PActivityCsedOfficeStaffBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Setup Adapter
        OfficeStaffPagerAdapter adapter = new OfficeStaffPagerAdapter(this);
        binding.viewPager.setAdapter(adapter);

        // 2. Sync BottomNav with ViewPager
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == com.example.campus_space_scheduler.R.id.nav_home) binding.viewPager.setCurrentItem(0);
            else if (id == com.example.campus_space_scheduler.R.id.nav_schedule) binding.viewPager.setCurrentItem(1);
            else if (id == com.example.campus_space_scheduler.R.id.nav_users ) binding.viewPager.setCurrentItem(2);
            else if (id == com.example.campus_space_scheduler.R.id.nav_spaces) binding.viewPager.setCurrentItem(3);
            else if (id == com.example.campus_space_scheduler.R.id.nav_settings) binding.viewPager.setCurrentItem(4);
            return true;
        });

        // 3. Sync ViewPager with BottomNav (For Swiping)
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                switch (position) {
                    case 0:
                        binding.bottomNav.setSelectedItemId(com.example.campus_space_scheduler.R.id.nav_home);
                        binding.toolbar.setTitle("CSED Office Portal");
                        break;
                    case 1:
                        binding.bottomNav.setSelectedItemId(com.example.campus_space_scheduler.R.id.nav_schedule);
                        binding.toolbar.setTitle("Classroom Bookings");
                        break;
                    case 2:
                        binding.bottomNav.setSelectedItemId(com.example.campus_space_scheduler.R.id.nav_users);
                        binding.toolbar.setTitle("Bookings History");
                        break;
                    case 3:
                        binding.bottomNav.setSelectedItemId(com.example.campus_space_scheduler.R.id.nav_spaces);
                        binding.toolbar.setTitle("Keys Records");
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