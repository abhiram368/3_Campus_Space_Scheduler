package com.example.campus_space_scheduler;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campus_space_scheduler.app_admin.AdminActivity;
import com.example.campus_space_scheduler.booking_user.BookingUserActivity;
import com.example.campus_space_scheduler.csed_office.CsedOfficeStaffActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private TextView nameText;
    private TextView roleText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_activity_main);

        nameText = findViewById(R.id.nameText);
        roleText = findViewById(R.id.roleText);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        checkUserSession();
    }

    private void checkUserSession() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            navigateTo(LoginActivity.class);
        } else {
            fetchUserRoleAndRedirect(currentUser.getUid());
        }
    }

    private void fetchUserRoleAndRedirect(String uid) {

        mDatabase.child("users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (!snapshot.exists()) {
                            mAuth.signOut();
                            navigateTo(LoginActivity.class);
                            return;
                        }

                        String role = snapshot.child("role").getValue(String.class);
                        String name = snapshot.child("name").getValue(String.class);

                        nameText.setText(name);
                        roleText.setText(role);

                        // delay so user sees welcome screen
                        new Handler().postDelayed(() -> {

                            if ("App admin".equals(role)) {
                                navigateTo(AdminActivity.class);

                            } else if ("Student".equals(role) || "Faculty".equals(role)) {
                                Intent intent = new Intent(MainActivity.this, BookingUserActivity.class);
                                intent.putExtra("ROLE", role);
                                startActivity(intent);
                                finish();

                            } else if ("HOD".equals(role)) {
                                navigateTo(BookingUserActivity.class);

                            } else if ("Faculty Incharge".equals(role)) {
                                navigateTo(BookingUserActivity.class);

                            } else if ("Lab admin".equals(role)) {
                                navigateTo(BookingUserActivity.class);

                            } else if ("CSED Staff".equals(role)) {
                                navigateTo(CsedOfficeStaffActivity.class);

                            } else if ("Hall Incharge".equals(role)) {
                                navigateTo(OtherUserActivity.class);

                            } else if ("Staff Incharge".equals(role)) {
                                navigateTo(OtherUserActivity.class);

                            } else {
                                navigateTo(LoginActivity.class);
                            }

                        }, 1000); // 1 seconds
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MainActivity.this,
                                "Database Error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateTo(Class<?> destinationClass) {
        Intent intent = new Intent(MainActivity.this, destinationClass);
        startActivity(intent);
        finish();
    }
}