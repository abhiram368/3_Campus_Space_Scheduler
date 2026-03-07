package com.example.campus_space_scheduler;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Tip: Use a simple layout with just a ProgressBar centered in the screen
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        checkUserSession();
    }

    private void checkUserSession() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // No one is logged in, send to Login
            navigateTo(LoginActivity.class);
        } else {
            // User is logged in, now fetch their role from the UID key
            fetchUserRoleAndRedirect(currentUser.getUid());
        }
    }

    private void fetchUserRoleAndRedirect(String uid) {
        // Look specifically in your "users" node for this UID
        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String role = snapshot.child("role").getValue(String.class);

                    if ("App admin".equals(role)) {
                        navigateTo(AdminActivity.class);
                    } else if ("Student".equals(role) || "Faculty".equals(role) || "HoD".equals(role)
                            || "Faculty Incharge".equals(role) || "Lab admin".equals(role)) {
                        navigateTo(BookingUserActivity.class); // student and faculty users go here
                    } else if ("CSED Staff".equals(role) || "Hall Incharge".equals(role) ||
                            "Staff Incharge".equals(role)) {
                        // All other roles (Faculty, Staff, HoD) go here
                        navigateTo(OtherUserActivity.class);
                    } else {
                        navigateTo(LoginActivity.class);
                    }
                } else {
                    // Safety Valve: Auth exists but DB record is missing
                    mAuth.signOut();
                    navigateTo(LoginActivity.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Log error or show a Toast - usually network related
                Toast.makeText(MainActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateTo(Class<?> destinationClass) {
        Intent intent = new Intent(MainActivity.this, destinationClass);
        startActivity(intent);
        finish(); // CRITICAL: This prevents the user from going 'back' to this screen
    }
}