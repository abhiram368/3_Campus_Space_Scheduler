package com.example.hod.hod;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;
import com.example.hod.firebase.FirebaseClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class HodProfileActivity extends AppCompatActivity {

    private TextView tvAvatarInitial, tvProfileName, tvProfileRole;
    private TextView tvProfileEmail, tvProfileContact, tvProfileRoleDetail, tvProfileNameCard;
    private android.widget.EditText etProfileName, etProfileContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hod_profile);

        tvAvatarInitial    = findViewById(R.id.tvAvatarInitial);
        tvProfileName      = findViewById(R.id.tvProfileName);
        tvProfileRole      = findViewById(R.id.tvProfileRole);
        tvProfileEmail     = findViewById(R.id.tvProfileEmail);
        tvProfileContact   = findViewById(R.id.tvProfileContact);
        tvProfileRoleDetail = findViewById(R.id.tvProfileRoleDetail);
        tvProfileNameCard  = findViewById(R.id.tvProfileNameCard);
        etProfileName      = findViewById(R.id.etProfileName);
        etProfileContact   = findViewById(R.id.etProfileContact);
        
        updateHeader(getString(R.string.my_profile), getString(R.string.account_settings));

        findViewById(R.id.btnEditProfile).setOnClickListener(v -> toggleEditMode(true));
        findViewById(R.id.btnCancelEdit).setOnClickListener(v -> toggleEditMode(false));
        findViewById(R.id.btnSaveProfile).setOnClickListener(v -> saveProfileChanges());
        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());

        loadUserProfile();
    }

    private void updateHeader(String title, String subtitle) {
        TextView tvTitle = findViewById(R.id.header_title);
        TextView tvSubtitle = findViewById(R.id.header_subtitle);
        View btnBack = findViewById(R.id.btnBack);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(subtitle);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = currentUser.getUid();

        FirebaseClient.getInstance().usersRef().child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Toast.makeText(HodProfileActivity.this, "Profile not found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String name        = getStr(snapshot, "name");
                        String emailId     = getStr(snapshot, "emailId");
                        String phoneNumber = getStr(snapshot, "phoneNumber");
                        String role        = getStr(snapshot, "role");

                        // Avatar initial
                        String initial = (name != null && !name.isEmpty())
                                ? String.valueOf(name.charAt(0)).toUpperCase() : "H";
                        tvAvatarInitial.setText(initial);

                        tvProfileName.setText(name != null ? name : "—");
                        tvProfileNameCard.setText(name != null ? name : "—");
                        tvProfileEmail.setText(emailId != null ? emailId : "—");
                        tvProfileContact.setText(phoneNumber != null && !phoneNumber.isEmpty() ? phoneNumber : "—");

                        // Pre-fill EditTexts
                        etProfileName.setText(name != null ? name : "");
                        etProfileContact.setText(phoneNumber != null ? phoneNumber : "");

                        // Role display
                        String roleLabel = formatRole(role);
                        tvProfileRole.setText(roleLabel);
                        tvProfileRoleDetail.setText(roleLabel);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(HodProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void logout() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Do you really want to logout?")
            .setPositiveButton("Confirm Logout", (dialog, which) -> {
                FirebaseDatabase.getInstance().goOffline();
                FirebaseAuth.getInstance().signOut();
                try {
                    Intent intent = new Intent();
                    intent.setClassName(this, "com.example.campus_space_scheduler.LoginActivity");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    Toast.makeText(this, "Logout failed", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("No", null)
            .show();
    }

    private void toggleEditMode(boolean enable) {
        tvProfileNameCard.setVisibility(enable ? View.GONE : View.VISIBLE);
        etProfileName.setVisibility(enable ? View.VISIBLE : View.GONE);
        
        tvProfileContact.setVisibility(enable ? View.GONE : View.VISIBLE);
        etProfileContact.setVisibility(enable ? View.VISIBLE : View.GONE);
        
        View btnEditProfile = findViewById(R.id.btnEditProfile);
        View llEditActions = findViewById(R.id.llEditActions);
        
        if (btnEditProfile != null) btnEditProfile.setVisibility(enable ? View.GONE : View.VISIBLE);
        if (llEditActions != null) llEditActions.setVisibility(enable ? View.VISIBLE : View.GONE);
        
        if (!enable) {
            etProfileName.setText(tvProfileNameCard.getText());
            etProfileContact.setText(tvProfileContact.getText());
        }
    }

    private void saveProfileChanges() {
        String newName = etProfileName.getText().toString().trim();
        String newContact = etProfileContact.getText().toString().trim();

        if (newName.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Save Changes")
                .setMessage("Are you sure you want to update your profile details?")
                .setPositiveButton("Save", (dialog, which) -> performSave(newName, newContact))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performSave(String newName, String newContact) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("phoneNumber", newContact);

        FirebaseClient.getInstance().usersRef().child(uid).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(HodProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    toggleEditMode(false);
                    loadUserProfile();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(HodProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                });
    }

    private String formatRole(String role) {
        if (role == null) return getString(R.string.role_hod);
        if (role.toLowerCase().contains("hod") || role.toLowerCase().contains("head")) return getString(R.string.role_hod);
        return role;
    }

    private String getStr(DataSnapshot snap, String key) {
        Object val = snap.child(key).getValue();
        return val != null ? val.toString() : null;
    }
}
