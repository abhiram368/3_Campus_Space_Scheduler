package com.example.hod.staff;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campussync.appy.R;
import com.example.hod.firebase.FirebaseClient;
import com.example.hod.firebase.FirebasePaths;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

public class StaffProfileActivity extends AppCompatActivity {

    private TextView tvAvatarInitial, tvProfileName, tvProfileRole;
    private TextView tvProfileEmail, tvProfileContact, tvProfileRoleDetail, tvProfileNameCard;
    private android.widget.EditText etProfileName, etProfileContact;
    private TextView tvInchargeSpace;
    private LinearLayout rowInchargeSpace;
    private View dividerIncharge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_profile);

        // Header Configuration
        View headerView = findViewById(R.id.header_layout);
        if (headerView != null) {
            TextView title = headerView.findViewById(R.id.header_title);
            View btnBack = headerView.findViewById(R.id.btnBack);
            if (title != null) title.setText(R.string.staff_profile_title);
            if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        }

        tvAvatarInitial    = findViewById(R.id.tvAvatarInitial);
        tvProfileName      = findViewById(R.id.tvProfileName);
        tvProfileRole      = findViewById(R.id.tvProfileRole);
        tvProfileEmail     = findViewById(R.id.tvProfileEmail);
        tvProfileContact   = findViewById(R.id.tvProfileContact);
        tvProfileRoleDetail = findViewById(R.id.tvProfileRoleDetail);
        tvProfileNameCard  = findViewById(R.id.tvProfileNameCard);
        etProfileName      = findViewById(R.id.etProfileName);
        etProfileContact   = findViewById(R.id.etProfileContact);
        
        View btnEditProfile     = findViewById(R.id.btnEditProfile);
        View llEditActions      = findViewById(R.id.llEditActions);
        View btnCancelEdit      = findViewById(R.id.btnCancelEdit);
        View btnSaveProfile     = findViewById(R.id.btnSaveProfile);
        
        tvInchargeSpace    = findViewById(R.id.tvInchargeSpace);
        rowInchargeSpace   = findViewById(R.id.rowInchargeSpace);
        dividerIncharge    = findViewById(R.id.dividerIncharge);

        updateHeader(getString(R.string.my_profile), getString(R.string.account_settings));

        btnEditProfile.setOnClickListener(v -> toggleEditMode(true));
        btnCancelEdit.setOnClickListener(v -> toggleEditMode(false));
        btnSaveProfile.setOnClickListener(v -> saveProfileChanges());
        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());

        loadUserProfile();
    }

    private void updateHeader(String title, String subtitle) {
        TextView tvTitle = findViewById(R.id.header_title);
        TextView tvSubtitle = findViewById(R.id.header_subtitle);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(subtitle);
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = currentUser.getUid();

        // Load from Realtime Database – structure: users/{uid}/name, email, contact, role, spaceId
        FirebaseClient.getInstance().usersRef().child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Toast.makeText(StaffProfileActivity.this,
                                    "Profile not found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String name           = getStr(snapshot, "name");
                        String emailId        = getStr(snapshot, "emailId");
                        String phoneNumber    = getStr(snapshot, "phoneNumber");
                        String role           = getStr(snapshot, "role");
                        String inchargeToSpace = getStr(snapshot, "inchargeToSpace");

                        // Avatar initial
                        String initial = (name != null && !name.isEmpty())
                                ? String.valueOf(name.charAt(0)).toUpperCase() : "S";
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

                        // Show incharge-to-space row when the field is populated
                        if (inchargeToSpace != null && !inchargeToSpace.isEmpty()) {
                            dividerIncharge.setVisibility(View.VISIBLE);
                            rowInchargeSpace.setVisibility(View.VISIBLE);
                            // inchargeToSpace may be a space name or ID; try resolving room name first
                            loadSpaceName(inchargeToSpace);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(StaffProfileActivity.this,
                                "Failed to load profile: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Tries to resolve a space name from the spaces/{id} node.
     * Falls back to displaying the raw value if the node doesn't exist.
     */
    private void loadSpaceName(String spaceValue) {
        FirebaseClient.getInstance().spacesRef().child(spaceValue)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String roomName = getStr(snapshot, "roomName");
                            tvInchargeSpace.setText(roomName != null ? roomName : spaceValue);
                        } else {
                            // inchargeToSpace is already a human-readable name
                            tvInchargeSpace.setText(spaceValue);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tvInchargeSpace.setText(spaceValue);
                    }
                });
    }

    private void logout() {
        // Shut down the RTDB connection cleanly so goOnline() in LoginActivity restores it fresh
        FirebaseDatabase.getInstance().goOffline();
        FirebaseAuth.getInstance().signOut();
        
        Intent intent = new Intent();
        intent.setClassName(this, "com.example.campus_space_scheduler.LoginActivity");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void toggleEditMode(boolean enable) {
        // Visibility toggles
        tvProfileNameCard.setVisibility(enable ? View.GONE : View.VISIBLE);
        etProfileName.setVisibility(enable ? View.VISIBLE : View.GONE);
        
        tvProfileContact.setVisibility(enable ? View.GONE : View.VISIBLE);
        etProfileContact.setVisibility(enable ? View.VISIBLE : View.GONE);
        
        View btnEditProfile = findViewById(R.id.btnEditProfile);
        View llEditActions = findViewById(R.id.llEditActions);
        
        if (btnEditProfile != null) btnEditProfile.setVisibility(enable ? View.GONE : View.VISIBLE);
        if (llEditActions != null) llEditActions.setVisibility(enable ? View.VISIBLE : View.GONE);
        
        if (!enable) {
            // Reset EditTexts to current values if cancelled
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

        // Confirmation Dialog
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Save Changes")
                .setMessage("Are you sure you want to update your profile details?")
                .setPositiveButton("Save", (dialog, which) -> {
                    performSave(newName, newContact);
                })
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
                    Toast.makeText(StaffProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    toggleEditMode(false);
                    loadUserProfile(); // Refresh view
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(StaffProfileActivity.this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String formatRole(String role) {
        if (role == null) return "User";
        switch (role.toLowerCase()) {
            case "staffincharge":
            case "staff incharge":
            case "staff":      return "Staff Incharge";
            case "facultyincharge":
            case "faculty incharge":
            case "faculty":    return "Faculty Incharge";
            case "head of department":
            case "hod":        return "Head of Department";
            default:           return role;
        }
    }

    /** Safely reads a String child from a DataSnapshot. */
    private String getStr(DataSnapshot snap, String key) {
        Object val = snap.child(key).getValue();
        return val != null ? val.toString() : null;
    }
}
