package com.example.campus_space_scheduler;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.campus_space_scheduler.databinding.FragmentSettingsBinding;
import com.example.campus_space_scheduler.databinding.ItemSettingsRowBinding;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);

        setupRows();
        return binding.getRoot();
    }

    private void setupRows() {
        // Account Section
        configureRow(binding.btnProfile.getRoot(), "Profile", R.drawable.ic_person, false);
        configureRow(binding.btnChangePassword.getRoot(), "Change Password", R.drawable.ic_lock, false);

        // Logout (Special Red Styling)
        configureRow(binding.btnLogout.getRoot(), "Logout", R.drawable.ic_logout, true);
        binding.btnLogout.getRoot().setOnClickListener(v -> handleLogout());

        // Integrations
        configureRow(binding.btnEmailTemplates.getRoot(), "Edit Email Templates", R.drawable.ic_mail, false);

        // Row with Status Badge
        ItemSettingsRowBinding syncRow = ItemSettingsRowBinding.bind(binding.btnGoogleSync.getRoot());
        syncRow.rowTitle.setText("Google Calendar Sync");
        syncRow.rowIcon.setImageResource(R.drawable.ic_calendar);
        syncRow.rowStatus.setVisibility(View.VISIBLE);
        syncRow.rowStatus.setText("Active");
        syncRow.rowStatus.setTextColor(android.graphics.Color.parseColor("#10B981"));

        // App Version (No Chevron)
        ItemSettingsRowBinding versionRow = ItemSettingsRowBinding.bind(binding.btnAppVersion.getRoot());
        versionRow.rowTitle.setText("App Version");
        versionRow.rowIcon.setImageResource(R.drawable.ic_info);
        versionRow.rowChevron.setVisibility(View.GONE);
        versionRow.rowStatus.setVisibility(View.VISIBLE);
        versionRow.rowStatus.setText("v2.4.12-pro");
    }

    private void configureRow(View rowView, String title, int iconRes, boolean isDestructive) {
        // We bind to the specific root view passed in
        ItemSettingsRowBinding rowBinding = ItemSettingsRowBinding.bind(rowView);
        rowBinding.rowTitle.setText(title);
        rowBinding.rowIcon.setImageResource(iconRes);

        if (isDestructive) {
            // Red theme for logout
            rowBinding.rowTitle.setTextColor(android.graphics.Color.parseColor("#EF4444"));
            rowBinding.rowIcon.setImageTintList(android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#EF4444")));
            rowBinding.rowIcon.setBackgroundResource(R.drawable.bg_rounded_red_tint);
        }
    }

    private void handleLogout() {
        // Logic will be added later
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}