package com.example.campus_space_scheduler.lab_admin_incharge;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;

import com.example.campus_space_scheduler.databinding.ActivityProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends BaseInchargeActivity {

    private ActivityProfileBinding binding;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Sidebar
        setupDrawer(binding.toolbar, binding.navView, binding.drawerLayout);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            loadUserProfile(user.getUid());
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadUserProfile(String uid) {
        binding.profileProgressBar.setVisibility(View.VISIBLE);
        
        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                binding.profileProgressBar.setVisibility(View.GONE);
                if (dataSnapshot.exists()) {
                    String name = dataSnapshot.child("name").getValue(String.class);
                    String email = dataSnapshot.child("emailId").getValue(String.class);
                    String phone = dataSnapshot.child("phoneNumber").getValue(String.class);
                    String role = dataSnapshot.child("role").getValue(String.class);
                    String rollNo = dataSnapshot.child("rollNo").getValue(String.class);

                    binding.tvProfileName.setText(name != null ? name : "N/A");
                    binding.tvProfileEmail.setText(email != null ? email : "N/A");
                    binding.tvProfilePhone.setText(phone != null ? phone : "N/A");
                    binding.tvProfileRole.setText(role != null ? role : "N/A");
                    binding.tvProfileRollNo.setText(rollNo != null ? rollNo : "N/A");
                } else {
                    Toast.makeText(ProfileActivity.this, "Profile data not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                binding.profileProgressBar.setVisibility(View.GONE);
                Toast.makeText(ProfileActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}