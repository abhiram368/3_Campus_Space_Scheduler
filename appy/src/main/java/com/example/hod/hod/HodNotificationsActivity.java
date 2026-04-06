package com.example.hod.hod;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.hod.R;
import com.example.hod.adapters.NotificationAdapter;
import com.example.hod.models.NotificationModel;
import com.example.hod.repository.FirebaseRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class HodNotificationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<NotificationModel> notificationList;
    private View emptyState;
    private MaterialButton btnDeleteSelected, btnMarkAllRead, btnSelectAll, btnDeselectAll;
    private LinearLayout selectionActions;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appy_notifications);

        recyclerView = findViewById(R.id.notifications_recycler);
        emptyState = findViewById(R.id.empty_state);
        btnDeleteSelected = findViewById(R.id.btn_delete_selected);
        btnMarkAllRead = findViewById(R.id.btn_mark_all_read);
        btnSelectAll = findViewById(R.id.btn_select_all);
        btnDeselectAll = findViewById(R.id.btn_deselect_all);
        selectionActions = findViewById(R.id.selection_actions);


        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        setupHeader();

        adapter.setOnSelectionListener(count -> {
            if (adapter.isSelectionMode()) {
                selectionActions.setVisibility(View.VISIBLE);
                btnMarkAllRead.setVisibility(View.GONE);
                btnDeleteSelected.setText("Delete (" + count + ")");
                
                boolean allSelected = count == notificationList.size();
                btnSelectAll.setVisibility(allSelected ? View.GONE : View.VISIBLE);
                btnDeselectAll.setVisibility(allSelected ? View.VISIBLE : View.GONE);

            } else {
                selectionActions.setVisibility(View.GONE);
                btnMarkAllRead.setVisibility(View.VISIBLE);
            }
        });

        btnDeleteSelected.setOnClickListener(v -> confirmDeleteSelected());
        btnSelectAll.setOnClickListener(v -> adapter.selectAll());
        btnDeselectAll.setOnClickListener(v -> adapter.clearSelection());


        setupMarkAllRead();
        fetchNotifications();
    }

    private void confirmDeleteSelected() {
        Set<String> ids = adapter.getSelectedIds();
        new AlertDialog.Builder(this)
                .setTitle("Delete Notifications")
                .setMessage("Are you sure you want to delete " + ids.size() + " selected notification(s)?")
                .setPositiveButton("Delete", (dialog, which) -> deleteSelectedNotifications(ids))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSelectedNotifications(Set<String> ids) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("notifications")
                .child(uid);

        for (String id : ids) {
            ref.child(id).removeValue();
        }

        Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show();
        adapter.setSelectionMode(false);
        // Manually trigger the listener to update the UI state
        if (adapter.getOnSelectionListener() != null) {
            adapter.getOnSelectionListener().onSelectionChanged(0);
        }
    }

    private void setupMarkAllRead() {
        if (btnMarkAllRead != null) {
            btnMarkAllRead.setOnClickListener(v -> {
                String uid = FirebaseAuth.getInstance().getUid();
                if (uid != null) {
                    new FirebaseRepository().markAllNotificationsAsRead(uid, result -> {
                        if (result instanceof com.example.hod.utils.Result.Success) {
                            for (NotificationModel m : notificationList) m.setRead(true);
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
            });
        }
    }

    private void setupHeader() {
        View header = findViewById(R.id.header_layout);
        if (header != null) {
            TextView title = header.findViewById(R.id.header_title);
            TextView subtitle = header.findViewById(R.id.header_subtitle);
            View btnBack = header.findViewById(R.id.btnBack);

            if (title != null) title.setText("Notifications");
            if (subtitle != null) subtitle.setText("Updates & Alerts");
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> {
                    if (adapter != null && adapter.isSelectionMode()) {
                        adapter.setSelectionMode(false);
                        // Manually trigger the listener to update the UI state
                        if (adapter.getOnSelectionListener() != null) {
                            adapter.getOnSelectionListener().onSelectionChanged(0);
                        }
                    } else {
                        finish();
                    }
                });
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (adapter.isSelectionMode()) {
            adapter.setSelectionMode(false);
            // Manually trigger the listener to update the UI state
            if (adapter.getOnSelectionListener() != null) {
                adapter.getOnSelectionListener().onSelectionChanged(0);
            }
        } else {
            super.onBackPressed();
        }
    }

    private void fetchNotifications() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("notifications")
                .child(uid);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notificationList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    NotificationModel notification = ds.getValue(NotificationModel.class);
                    if (notification != null) {
                        notificationList.add(notification);
                    }
                }
                Collections.sort(notificationList, (n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));
                adapter.notifyDataSetChanged();

                if (emptyState != null) {
                    boolean empty = notificationList.isEmpty();
                    emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                    recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
}
