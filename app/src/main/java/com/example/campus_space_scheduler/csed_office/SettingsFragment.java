package com.example.campus_space_scheduler.csed_office;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.campus_space_scheduler.ForgotPasswordActivity;
import com.example.campus_space_scheduler.app_admin.ProfileActivity;
import com.example.campus_space_scheduler.helper.LogHelper;
import com.example.campus_space_scheduler.LoginActivity;
import com.example.campus_space_scheduler.R;
import com.example.campus_space_scheduler.databinding.PFragmentSettingsBinding;
import com.example.campus_space_scheduler.databinding.AItemSettingsRowBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends Fragment {

    private PFragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = PFragmentSettingsBinding.inflate(inflater, container, false);

        setupRows();
        return binding.getRoot();
    }

    private void setupRows() {
        // Account Section
        configureRow(binding.btnProfile.getRoot(), "Profile", R.drawable.ic_person, false);
        binding.btnProfile.getRoot().setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProfileActivity.class)));
        // Change Password

        configureRow(binding.btnChangePassword.getRoot(), "Change Password", R.drawable.ic_lock, false);
        binding.btnChangePassword.getRoot().setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ForgotPasswordActivity.class)));
        // Logout (Special Red Styling)
        configureRow(binding.btnLogout.getRoot(), "Logout", R.drawable.ic_logout, true);
        binding.btnLogout.getRoot().setOnClickListener(v -> handleLogout());

        // Integrations

        // Row with Status Badge
        AItemSettingsRowBinding syncRow = AItemSettingsRowBinding.bind(binding.btnGoogleSync.getRoot());
        syncRow.rowTitle.setText("Google Calendar Sync");
        syncRow.rowIcon.setImageResource(R.drawable.ic_calendar);
        syncRow.rowStatus.setVisibility(View.VISIBLE);
        syncRow.rowStatus.setText("Active");
        // ---------------------------------------------------------
        // UI COLOR CONFIGURATION: 'Active' status badge color
        // ---------------------------------------------------------
        String googleSyncStatusHexColor = "#10B981";
        int activeStatusBadgeColor = android.graphics.Color.parseColor(googleSyncStatusHexColor);

        syncRow.rowStatus.setTextColor(activeStatusBadgeColor);

        // App Version (No Chevron)
        AItemSettingsRowBinding versionRow = AItemSettingsRowBinding.bind(binding.btnAppVersion.getRoot());
        versionRow.rowTitle.setText("App Version");
        versionRow.rowIcon.setImageResource(R.drawable.ic_info);
        versionRow.rowChevron.setVisibility(View.GONE);
        versionRow.rowStatus.setVisibility(View.VISIBLE);
        versionRow.rowStatus.setText("v1.0.0");
    }

    private void configureRow(View rowView, String title, int iconRes, boolean isDestructive) {
        // We bind to the specific root view passed in
        AItemSettingsRowBinding rowBinding = AItemSettingsRowBinding.bind(rowView);
        rowBinding.rowTitle.setText(title);
        rowBinding.rowIcon.setImageResource(iconRes);

        if (isDestructive) {
            // ---------------------------------------------------------
            // UI COLOR CONFIGURATION: Destructive/Warning action color (Red)
            // ---------------------------------------------------------
            String destructiveHexColor = "#EF4444";
            int destructiveActionColor = android.graphics.Color.parseColor(destructiveHexColor);

            rowBinding.rowTitle.setTextColor(destructiveActionColor);
            rowBinding.rowIcon.setImageTintList(android.content.res.ColorStateList.valueOf(destructiveActionColor));

            // Note: The background drawable 'bg_rounded_red_tint' likely
            // contains color data as well.
            rowBinding.rowIcon.setBackgroundResource(R.drawable.bg_rounded_red_tint);
        }
    }

    private void handleLogout() {

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.a_dialog_exit_confirmation, null);
        dialog.setContentView(view);

        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirmExit);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancelExit);

        btnConfirm.setOnClickListener(v -> {

            FirebaseAuth.getInstance().signOut();
            LogHelper.log("LOGOUT", "User logged out");

            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}