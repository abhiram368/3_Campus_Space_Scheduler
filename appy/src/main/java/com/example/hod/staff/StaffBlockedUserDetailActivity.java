package com.example.hod.staff;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.campussync.appy.R;
import com.example.hod.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StaffBlockedUserDetailActivity extends AppCompatActivity {

    private String userId, userName, userEmail, blockReason, blockedBy;
    private TextView tvUserName, tvUserEmail, tvBlockReason, tvAvatarInitial, tvBlockedBy;
    private Button btnUnblock;
    private ProgressBar progressBar;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_blocked_user_detail);

        setupHeader();

        tvAvatarInitial = findViewById(R.id.tvAvatarInitial);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvBlockReason = findViewById(R.id.tvBlockReason);
        tvBlockedBy = findViewById(R.id.tvBlockedBy);
        btnUnblock = findViewById(R.id.btnUnblock);
        progressBar = findViewById(R.id.progressBar);

        userId = getIntent().getStringExtra("userId");
        userName = getIntent().getStringExtra("userName");
        userEmail = getIntent().getStringExtra("userEmail");
        blockReason = getIntent().getStringExtra("blockReason");
        blockedBy = getIntent().getStringExtra("blockedBy");

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Invalid user ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        populateData();

        btnUnblock.setOnClickListener(v -> confirmUnblock());
    }

    private void setupHeader() {
        View headerView = findViewById(R.id.header_layout);
        if (headerView != null) {
            TextView title = headerView.findViewById(R.id.header_title);
            TextView subtitle = headerView.findViewById(R.id.header_subtitle);
            View btnBack = headerView.findViewById(R.id.btnBack);

            if (title != null) title.setText("Blocked Details");
            if (subtitle != null) subtitle.setText("Review and Unblock");
            if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        }
    }

    private void populateData() {
        String nameStr = userName != null && !userName.isEmpty() ? userName : "Unknown User";
        tvUserName.setText(nameStr);
        tvAvatarInitial.setText(nameStr.substring(0, 1).toUpperCase());
        tvUserEmail.setText(userEmail != null ? userEmail : "No email");
        tvBlockReason.setText(blockReason != null ? blockReason : "No reason provided");

        if (blockedBy != null && !blockedBy.isEmpty()) {
            fetchBlockedByName(blockedBy);
        } else {
            tvBlockedBy.setText("Unknown Staff");
        }
    }

    private void fetchBlockedByName(String staffUid) {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseDatabase.getInstance().getReference("users").child(staffUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        progressBar.setVisibility(View.GONE);
                        User staff = snapshot.getValue(User.class);
                        if (staff != null && staff.getName() != null) {
                            tvBlockedBy.setText(staff.getName());
                        } else {
                            tvBlockedBy.setText("Staff (" + staffUid.substring(0, 5) + "... )");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        tvBlockedBy.setText("Unknown");
                    }
                });
    }

    private void confirmUnblock() {
        new AlertDialog.Builder(this)
                .setTitle("Unblock User")
                .setMessage("Are you sure you want to restore " + (userName != null ? userName : "this user") + "'s access?")
                .setPositiveButton("Unblock", (dialog, which) -> executeUnblock())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeUnblock() {
        progressBar.setVisibility(View.VISIBLE);
        userRef.child("blocked").setValue(false);
        userRef.child("blockReason").removeValue();
        userRef.child("blockedBy").removeValue().addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                Toast.makeText(this, "User unblocked successfully", Toast.LENGTH_SHORT).show();
                finish(); // Return to the list
            } else {
                Toast.makeText(this, "Failed to unblock user", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
