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
import com.example.campus_space_scheduler.lab_admin_incharge.FacultyDashboardActivity;
import com.example.campus_space_scheduler.lab_admin_incharge.HallInchargeActivity;
import com.example.campus_space_scheduler.lab_admin_incharge.LabAdminDashboardActivity;
import com.example.hod.hod.HodDashboardActivity;
import com.example.hod.staff.StaffDashboardActivity;
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
                        String inchargeToSpace = snapshot.child("inchargeToSpace").getValue(String.class);

                        if (name != null) nameText.setText(name);
                        if (role != null) roleText.setText(role);

                        // delay so user sees welcome screen
                        new Handler().postDelayed(() -> {

                            if ("App admin".equals(role)) {
                                navigateTo(AdminActivity.class);

                            } else if ("Student".equals(role) || "Faculty".equals(role)) {
                                Intent intent = new Intent(MainActivity.this, BookingUserActivity.class);
                                intent.putExtra("ROLE", role);
                                startActivity(intent);
                                finish();

                            } else if ("HoD".equals(role)) {
                                navigateTo(HodDashboardActivity.class);

                            } else if ("Faculty Incharge".equalsIgnoreCase(role)) {
                                if (inchargeToSpace != null) {
                                    resolveSpaceIdAndNavigate(inchargeToSpace, FacultyDashboardActivity.class);
                                } else {
                                    navigateTo(FacultyDashboardActivity.class, "labName", "Unknown Lab");
                                }
                            } else if ("Lab admin".equalsIgnoreCase(role)) {
                                if (inchargeToSpace != null) {
                                    resolveSpaceIdAndNavigate(inchargeToSpace, LabAdminDashboardActivity.class);
                                } else {
                                    navigateTo(LabAdminDashboardActivity.class, "labName", "Unknown Lab");
                                }
                            } else if ("Hall Incharge".equalsIgnoreCase(role)) {
                                if (inchargeToSpace != null) {
                                    resolveSpaceIdAndNavigate(inchargeToSpace, HallInchargeActivity.class);
                                } else {
                                    navigateTo(HallInchargeActivity.class);
                                }
                            } else if ("CSED Staff".equalsIgnoreCase(role)) {
                                navigateTo(CsedOfficeStaffActivity.class);
                            } else if ("StaffIncharge".equalsIgnoreCase(role)) {
                                Intent intent = new Intent(MainActivity.this, StaffDashboardActivity.class);
                                intent.putExtra("ROLE", role);
                                intent.putExtra("labId", inchargeToSpace);
                                intent.putExtra("userName", name);
                                startActivity(intent);
                                finish();

                            } else {
                                navigateTo(LoginActivity.class);
                            }

                        }, 1000); // 1 seconds
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MainActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resolveSpaceIdAndNavigate(String spaceId, Class<?> targetActivity) {
        mDatabase.child("spaces").child(spaceId).child("roomName")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String roomName = snapshot.getValue(String.class);
                        Intent intent = new Intent(MainActivity.this, targetActivity);
                        // Pass resolved roomName (e.g. "SSL") or fallback to "Unknown Lab"
                        intent.putExtra("labName", roomName != null ? roomName : "Unknown Lab");
                        // Also passing the spaceId just in case activities need the direct ID
                        intent.putExtra("spaceId", spaceId);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Intent intent = new Intent(MainActivity.this, targetActivity);
                        intent.putExtra("labName", "Unknown Lab");
                        startActivity(intent);
                        finish();
                    }
                });
    }

    private void navigateTo(Class<?> destinationClass) {
        Intent intent = new Intent(MainActivity.this, destinationClass);
        startActivity(intent);
        finish();
    }

    private void navigateTo(Class<?> destinationClass, String extraKey, String extraValue) {
        Intent intent = new Intent(MainActivity.this, destinationClass);
        intent.putExtra(extraKey, extraValue);
        startActivity(intent);
        finish();
    }
}