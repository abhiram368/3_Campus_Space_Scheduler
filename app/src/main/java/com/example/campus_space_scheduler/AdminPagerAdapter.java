package com.example.campus_space_scheduler;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class AdminPagerAdapter extends FragmentStateAdapter {

    public AdminPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new DashboardFragment();
            case 1: return new ScheduleFragment();
            case 2: return ManagementFragment.newInstance("USER");
            case 3: return ManagementFragment.newInstance("SPACE");
            case 4: return new SettingsFragment();
            default: return new DashboardFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}