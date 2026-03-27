package com.example.hod.staff;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campussync.appy.R;
import com.example.hod.models.User;
import com.example.hod.repository.FirebaseRepository;
import com.example.hod.utils.Result;

import java.util.List;

public class ViewAdminsActivity extends AppCompatActivity {

    private String labId;
    private LinearLayout adminsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_admins);

        // Header Configuration
        View headerView = findViewById(R.id.header_layout);
        if (headerView != null) {
            TextView title = headerView.findViewById(R.id.header_title);
            View btnBack = headerView.findViewById(R.id.btnBack);
            if (title != null) title.setText("Lab Admins");
            if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        }

        labId = getIntent().getStringExtra("labId");
        if (labId == null || labId.isEmpty()) {
            Toast.makeText(this, "Error: missing Lab ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adminsContainer = findViewById(R.id.adminsContainer);
        View btnAddAdmin = findViewById(R.id.btnAddAdmin);

        updateHeader("Managing Admins", 0);

        if (btnAddAdmin != null) {
            btnAddAdmin.setOnClickListener(v -> {
                Intent intent = new Intent(this, AddLabAdminActivity.class);
                intent.putExtra("labId", labId);
                startActivity(intent);
            });
        }
    }

    private void updateHeader(String title, int count) {
        TextView tvTitle = findViewById(R.id.header_title);
        TextView tvSubtitle = findViewById(R.id.header_subtitle);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(count + " active admins");
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAdmins();
    }

    private void loadAdmins() {
        if (adminsContainer == null) return;

        FirebaseRepository repo = new FirebaseRepository();
        repo.getLabAdmins(labId, result -> {
            if (result instanceof Result.Success) {
                adminsContainer.removeAllViews();
                List<User> admins = ((Result.Success<List<User>>) result).data;
                if (admins != null) {
                    for (User admin : admins) {
                        addAdminCard(admin);
                    }
                    updateHeader("Managing Admins", admins.size());
                }
            } else {
                String errorMsg = (result instanceof Result.Error) ? ((Result.Error) result).exception.getMessage() : "Unknown error";
                Toast.makeText(this, "Failed to load admins: " + errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void addAdminCard(User admin) {
        View card = getLayoutInflater().inflate(R.layout.item_admin_card, adminsContainer, false);
        TextView tvName = card.findViewById(R.id.tvAdminName);
        TextView tvRoll = card.findViewById(R.id.tvAdminRoll);

        tvName.setText(admin.name);
        tvRoll.setText(admin.rollNo);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminDetailsActivity.class);
            intent.putExtra("name", admin.name);
            intent.putExtra("roll", admin.rollNo);
            intent.putExtra("phone", admin.phoneNumber != null ? admin.phoneNumber : "N/A");
            intent.putExtra("email", admin.emailId);
            intent.putExtra("uid", admin.uid);
            intent.putExtra("spaceId", labId);
            startActivity(intent);
        });

        adminsContainer.addView(card);
    }
}