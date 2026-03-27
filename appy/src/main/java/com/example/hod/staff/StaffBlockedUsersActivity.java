package com.example.hod.staff;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.appy.R;
import com.example.hod.adapters.BlockedUserAdapter;
import com.example.hod.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class StaffBlockedUsersActivity extends AppCompatActivity {

    private RecyclerView rvBlockedUsers;
    private TextView noDataTextView;
    private ProgressBar progressBar;
    private BlockedUserAdapter adapter;
    private List<User> blockedUsersList;
    private String currentStaffUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_blocked_users);

        setupHeader();

        rvBlockedUsers = findViewById(R.id.rvBlockedUsers);
        noDataTextView = findViewById(R.id.noDataTextView);
        progressBar = findViewById(R.id.progressBar);

        rvBlockedUsers.setLayoutManager(new LinearLayoutManager(this));
        blockedUsersList = new ArrayList<>();
        adapter = new BlockedUserAdapter(this, blockedUsersList);
        rvBlockedUsers.setAdapter(adapter);

        currentStaffUid = FirebaseAuth.getInstance().getUid();
        if (currentStaffUid == null) {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchBlockedUsers();
    }

    private void setupHeader() {
        View headerView = findViewById(R.id.header_layout);
        if (headerView != null) {
            TextView title = headerView.findViewById(R.id.header_title);
            TextView subtitle = headerView.findViewById(R.id.header_subtitle);
            View btnBack = headerView.findViewById(R.id.btnBack);

            if (title != null) title.setText("Blocked Students");
            if (subtitle != null) subtitle.setText("Manage blocked user accounts");
            if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        }
    }

    private void fetchBlockedUsers() {
        progressBar.setVisibility(View.VISIBLE);
        noDataTextView.setVisibility(View.GONE);

        FirebaseDatabase.getInstance().getReference("users")
                .orderByChild("blockedBy")
                .equalTo(currentStaffUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        blockedUsersList.clear();
                        if (snapshot.exists()) {
                            for (DataSnapshot userSnap : snapshot.getChildren()) {
                                User user = userSnap.getValue(User.class);
                                if (user != null) {
                                    user.setUserId(userSnap.getKey());
                                    blockedUsersList.add(user);
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);
                        noDataTextView.setVisibility(blockedUsersList.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(StaffBlockedUsersActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
