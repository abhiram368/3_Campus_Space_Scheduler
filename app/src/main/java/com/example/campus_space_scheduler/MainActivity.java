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
import com.example.hod.hod.HodDashboardActivity;
import com.example.hod.hod.HodNotificationsActivity;
import com.example.hod.staff.StaffDashboardActivity;
import com.example.hod.staff.NotificationsActivity;
import com.example.hod.utils.NotificationService;
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

        handleIntent(getIntent());
        checkUserSession();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Update the intent for this activity
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("OPEN_NOTIFICATIONS", false)) {
            // We need the user role to redirect. If we don't have it yet, 
            // the fetchUserRoleAndRedirect will catch it via getIntent().
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                fetchUserRoleAndRedirect(user.getUid());
            }
        }
    }

    private void checkUserSession() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            navigateTo(LoginActivity.class);
        } else {
            // Start the background notification listener service
            try {
                Intent serviceIntent = new Intent(this, NotificationService.class);
                startService(serviceIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
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

                        // If opened from a notification click, redirect directly
                        if (getIntent().getBooleanExtra("OPEN_NOTIFICATIONS", false)) {
                            redirecttoNotifications(role, inchargeToSpace, name);
                            return;
                        }

                        if (nameText != null) nameText.setText(name);
                        if (roleText != null) roleText.setText(role);

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

                            } else if ("Faculty Incharge".equals(role)) {
                                navigateTo(BookingUserActivity.class);

                            } else if ("Lab admin".equals(role)) {
                                navigateTo(BookingUserActivity.class);

                            } else if ("CSED Staff".equals(role)) {
                                navigateTo(CsedOfficeStaffActivity.class);

                            } else if ("Hall Incharge".equals(role)) {
                                navigateTo(OtherUserActivity.class);

                            } else if ("StaffIncharge".equals(role)) {
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
                        Toast.makeText(MainActivity.this,
                                "Database Error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void redirecttoNotifications(String role, String labId, String name) {
        Intent intent;
        if ("HoD".equalsIgnoreCase(role)) {
            intent = new Intent(this, HodNotificationsActivity.class);
        } else if ("StaffIncharge".equalsIgnoreCase(role)) {
            intent = new Intent(this, NotificationsActivity.class);
            intent.putExtra("labId", labId);
            intent.putExtra("userName", name);
        } else {
            // Default to Staff Notifications which handles any UID
            intent = new Intent(this, NotificationsActivity.class);
            intent.putExtra("labId", labId);
            intent.putExtra("userName", name);
        }
        startActivity(intent);
        finish();
    }

    private void navigateTo(Class<?> destinationClass) {
        Intent intent = new Intent(MainActivity.this, destinationClass);
        startActivity(intent);
        finish();
    }
}
