package com.example.hod.staff;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.campussync.appy.R;
import android.widget.TextView;

public class LabAdminsHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab_admins_home);

        // Header Configuration
        View headerView = findViewById(R.id.header_layout);
        if (headerView != null) {
            TextView title = headerView.findViewById(R.id.header_title);
            View btnBack = headerView.findViewById(R.id.btnBack);
            if (title != null) title.setText("Lab Personnel");
            if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        }

        String labId = getIntent().getStringExtra("labId");
        if (labId == null || labId.isEmpty()) {
            Toast.makeText(this, "Error: missing Lab ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        updateHeader("Admin Management", "Staff & Scheduling");

        View btnViewAdmins = findViewById(R.id.btnViewAdmins);
        View btnViewAdminSchedule = findViewById(R.id.btnViewAdminSchedule);

        if (btnViewAdmins != null) {
            btnViewAdmins.setOnClickListener(v -> {
                Intent intent = new Intent(this, ViewAdminsActivity.class);
                intent.putExtra("labId", labId);
                startActivity(intent);
            });
        }

        if (btnViewAdminSchedule != null) {
            btnViewAdminSchedule.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminScheduleActivity.class);
                intent.putExtra("labId", labId);
                startActivity(intent);
            });
        }
    }

    private void updateHeader(String title, String subtitle) {
        android.widget.TextView tvTitle = findViewById(R.id.header_title);
        android.widget.TextView tvSubtitle = findViewById(R.id.header_subtitle);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(subtitle);
    }
}
