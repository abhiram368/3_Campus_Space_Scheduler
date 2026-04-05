package com.example.campus_space_scheduler;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.campus_space_scheduler.app_admin.AdminActivity;
import com.example.campus_space_scheduler.booking_user.BookingUserActivity;
import com.example.campus_space_scheduler.csed_office.CsedOfficeStaffActivity;
import com.example.hod.hod.HodDashboardActivity;
import com.example.hod.staff.StaffDashboardActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.example.hod.utils.NotificationService;

public class SplashActivity extends AppCompatActivity {

    private static final int PERM_NOTIF = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.hod.R.layout.activity_splash);
        
        checkPermissionsAndProceed();
    }

    private void checkPermissionsAndProceed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERM_NOTIF);
            } else {
                startAppLogic();
            }
        } else {
            startAppLogic();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERM_NOTIF) {
            startAppLogic();
        }
    }

    private void startAppLogic() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            verifyUserAndRedirect(currentUser);
        } else {
            new android.os.Handler().postDelayed(() -> {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }, 1500);
        }
    }

    private void verifyUserAndRedirect(FirebaseUser user) {
        try {
            Intent serviceIntent = new Intent(this, NotificationService.class);
            startService(serviceIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String role = snapshot.child("role").getValue(String.class);
                        String name = snapshot.child("name").getValue(String.class);
                        String inchargeToSpace = snapshot.child("inchargeToSpace").getValue(String.class);
                        
                        redirectToDashboard(role, name, inchargeToSpace);
                    } else {
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Connection error.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                });
    }

    private void redirectToDashboard(String role, String name, String inchargeToSpace) {
        Intent intent;
        if ("App admin".equals(role)) {
            intent = new Intent(this, AdminActivity.class);
        } else if ("HoD".equals(role)) {
            intent = new Intent(this, HodDashboardActivity.class);
        } else if ("StaffIncharge".equals(role)) {
            intent = new Intent(this, StaffDashboardActivity.class);
            intent.putExtra("ROLE", role);
            intent.putExtra("labId", inchargeToSpace);
            intent.putExtra("userName", name);
        } else if ("CSED Staff".equals(role)) {
            intent = new Intent(this, CsedOfficeStaffActivity.class);
        } else if ("Hall Incharge".equals(role)) {
            intent = new Intent(this, OtherUserActivity.class);
        } else if ("Student".equals(role) || "Faculty".equals(role) || 
                   "Faculty Incharge".equals(role) || "Lab admin".equals(role)) {
            intent = new Intent(this, BookingUserActivity.class);
            intent.putExtra("ROLE", role);
        } else {
            intent = new Intent(this, LoginActivity.class);
        }
        
        startActivity(intent);
        finish();
    }
}
