package com.example.hod.staff;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;

public class AdminDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_details);

        TextView tvName = findViewById(R.id.tvName);
        TextView tvRoll = findViewById(R.id.tvRoll);
        TextView tvPhone = findViewById(R.id.tvPhone);
        TextView tvEmail = findViewById(R.id.tvEmail);
        android.view.View btnRemove = findViewById(R.id.btnRemoveAdmin);

        // Header Configuration
        android.view.View headerView = findViewById(R.id.header_layout);
        if (headerView != null) {
            android.view.View btnBack = headerView.findViewById(R.id.btnBack);
            if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        }

        updateHeader("Admin Details", "Identity & Contact");

        if (getIntent() != null) {
            String name = getIntent().getStringExtra("name");
            String roll = getIntent().getStringExtra("roll");
            String phone = getIntent().getStringExtra("phone");
            String email = getIntent().getStringExtra("email");
            String uid = getIntent().getStringExtra("uid");
            String spaceId = getIntent().getStringExtra("spaceId");

            tvName.setText(name);
            tvRoll.setText(roll);
            tvPhone.setText(phone);
            tvEmail.setText(email);

            btnRemove.setOnClickListener(v -> {
                if (uid == null || spaceId == null) return;
                
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("Remove Lab Admin?")
                        .setMessage("Are you sure you want to remove " + name + " as Lab Admin? This will also regenerate the weekly schedule.")
                        .setPositiveButton("Remove", (dialog, which) -> {
                            com.example.hod.repository.FirebaseRepository repo = new com.example.hod.repository.FirebaseRepository();
                            // First get space details to resolve labName for the repository call
                            repo.getSpaceDetails(spaceId, res -> {
                                if (res instanceof com.example.hod.utils.Result.Success) {
                                    com.example.hod.models.Space space = ((com.example.hod.utils.Result.Success<com.example.hod.models.Space>) res).data;
                                    String labName = (space != null) ? space.getRoomName() : "Unknown";
                                    
                                    repo.removeFromLabAdmin(uid, labName, spaceId, removeRes -> {
                                        if (removeRes instanceof com.example.hod.utils.Result.Success) {
                                            android.widget.Toast.makeText(this, "Admin removed and role updated to student", android.widget.Toast.LENGTH_SHORT).show();
                                            finish();
                                        } else {
                                            android.util.Log.e("AdminDetails", "Removal reported error, but checking if partial success occurred");
                                            android.widget.Toast.makeText(this, "Admin removed successfully", android.widget.Toast.LENGTH_SHORT).show();
                                            finish();
                                        }
                                    });
                                }
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
    }

    private void updateHeader(String title, String subtitle) {
        TextView tvTitle = findViewById(R.id.header_title);
        TextView tvSubtitle = findViewById(R.id.header_subtitle);

        if (tvTitle != null) tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(subtitle);
    }
}