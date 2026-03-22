package com.example.campus_space_scheduler.csed_office;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class OfficeStaffPagerAdapter extends FragmentStateAdapter {

    public OfficeStaffPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new ScheduleFragment();
            case 1: return new BookingsFragment();
            case 2: return new HistoryFragment();
            case 3: return new KeysFragment();
            case 4: return new SettingsFragment();
            default: return new ScheduleFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}