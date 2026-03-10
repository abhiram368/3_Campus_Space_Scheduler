package com.example.hod.staff;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;

public class ViewAdminsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_admins);

        View admin1 = findViewById(R.id.admin1);
        View admin2 = findViewById(R.id.admin2);

        // ADMIN 1
        if (admin1 != null) {
            admin1.setOnClickListener(v -> {
                Intent intent = new Intent(
                        ViewAdminsActivity.this,
                        AdminDetailsActivity.class
                );
                intent.putExtra("name", "Ramesh Kumar");
                intent.putExtra("roll", "LABADM001");
                intent.putExtra("phone", "9876543210");
                intent.putExtra("email", "ramesh@nitc.ac.in");
                startActivity(intent);
            });
        }

        // ADMIN 2
        if (admin2 != null) {
            admin2.setOnClickListener(v -> {
                Intent intent = new Intent(
                        ViewAdminsActivity.this,
                        AdminDetailsActivity.class
                );
                intent.putExtra("name", "Suresh Naik");
                intent.putExtra("roll", "LABADM002");
                intent.putExtra("phone", "9123456789");
                intent.putExtra("email", "suresh@nitc.ac.in");
                startActivity(intent);
            });
        }
    }
}