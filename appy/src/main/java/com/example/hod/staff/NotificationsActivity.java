package com.example.hod.staff;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
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

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<NotificationModel> notificationList;
    private LinearLayout emptyState;
    private TextView btnDeleteSelected, btnMarkAllRead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        recyclerView = findViewById(R.id.notifications_recycler);
        emptyState = findViewById(R.id.empty_state);
        btnDeleteSelected = findViewById(R.id.btn_delete_selected);
        btnMarkAllRead = findViewById(R.id.btn_mark_all_read);
        
        // Selection Controls
        final View selectionActions = findViewById(R.id.selection_actions);
        final View btnSelectAll = findViewById(R.id.btn_select_all);
        final View btnDeselectAll = findViewById(R.id.btn_deselect_all);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        setupHeader();

        adapter.setOnSelectionListener(count -> {
            if (count > 0) {
                if (selectionActions != null) selectionActions.setVisibility(View.VISIBLE);
                btnDeleteSelected.setText("Delete (" + count + ")");
                btnMarkAllRead.setVisibility(View.GONE);
                
                if (btnSelectAll != null && btnDeselectAll != null) {
                    boolean allSelected = (count == notificationList.size());
                    btnSelectAll.setVisibility(allSelected ? View.GONE : View.VISIBLE);
                    btnDeselectAll.setVisibility(allSelected ? View.VISIBLE : View.GONE);
                }
            } else {
                if (selectionActions != null) selectionActions.setVisibility(View.GONE);
                btnMarkAllRead.setVisibility(View.VISIBLE);
            }
        });

        if (btnSelectAll != null) {
            btnSelectAll.setOnClickListener(v -> adapter.selectAll());
        }
        if (btnDeselectAll != null) {
            btnDeselectAll.setOnClickListener(v -> adapter.clearSelection());
        }

        btnDeleteSelected.setOnClickListener(v -> confirmDeleteSelected());

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
            View btnAction = header.findViewById(R.id.btnAction);

            if (title != null) title.setText("Notifications");
            if (subtitle != null) subtitle.setText("Updates & Alerts");
            
            if (btnAction != null && adapter != null) {
                btnAction.setVisibility(View.VISIBLE);
                btnAction.setOnClickListener(v -> {
                    boolean mode = !adapter.isSelectionMode();
                    adapter.setSelectionMode(mode);
                    ((ImageButton)btnAction).setImageResource(mode ? R.drawable.ic_close : R.drawable.ic_edit);
                });
                
                // Update icon based on adapter state
                ((ImageButton)btnAction).setImageResource(adapter.isSelectionMode() ? R.drawable.ic_close : R.drawable.ic_edit);
            }

            if (btnBack != null) {
                btnBack.setOnClickListener(v -> {
                    if (adapter != null && adapter.isSelectionMode()) {
                        adapter.setSelectionMode(false);
                        if (btnAction != null) ((ImageButton)btnAction).setImageResource(R.drawable.ic_edit);
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
