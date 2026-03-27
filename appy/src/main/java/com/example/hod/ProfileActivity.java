package com.example.hod;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import com.campussync.appy.databinding.ActivityProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.headerLayout.btnBack.setOnClickListener(v -> finish());

        loadProfile();
    }

    private void loadProfile() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .get()
                .addOnSuccessListener(snapshot -> {

                    String name = snapshot.child("name").getValue(String.class);
                    String email = snapshot.child("emailId").getValue(String.class);
                    String role = snapshot.child("role").getValue(String.class);

                    Object phoneObj = snapshot.child("phoneNumber").getValue();
                    String phone = phoneObj != null ? phoneObj.toString() : "Not provided";

                    binding.tvName.setText(name);
                    binding.tvEmail.setText(email);
                    binding.tvPhone.setText(phone);
                    binding.tvRole.setText(role);
                });
    }
}
