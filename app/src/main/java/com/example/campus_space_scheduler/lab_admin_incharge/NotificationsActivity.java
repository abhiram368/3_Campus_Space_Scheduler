package com.example.campus_space_scheduler.lab_admin_incharge;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campus_space_scheduler.R;
import com.example.campus_space_scheduler.databinding.ActivityNotificationsBinding;
import com.example.campus_space_scheduler.lab_admin_incharge.models.NotificationModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsActivity extends BaseInchargeActivity {

    private ActivityNotificationsBinding binding;
    private NotificationAdapter adapter;
    private List<NotificationModel> notificationList;
    private DatabaseReference notificationsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupDrawer(binding.toolbar, binding.navView, binding.drawerLayout);

        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList);
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotifications.setAdapter(adapter);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            notificationsRef = FirebaseDatabase.getInstance().getReference("notifications").child(uid);
            fetchNotifications();
        }

        binding.btnClearAll.setOnClickListener(v -> showClearAllDialog());
    }

    private void fetchNotifications() {
        binding.pbNotifications.setVisibility(View.VISIBLE);
        notificationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notificationList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    NotificationModel notification = dataSnapshot.getValue(NotificationModel.class);
                    if (notification != null) {
                        notificationList.add(0, notification); // Newest first
                    }
                }
                binding.pbNotifications.setVisibility(View.GONE);
                binding.tvNoNotifications.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                binding.btnClearAll.setVisibility(notificationList.isEmpty() ? View.GONE : View.VISIBLE);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.pbNotifications.setVisibility(View.GONE);
            }
        });
    }

    private void showClearAllDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Notifications")
                .setMessage("Are you sure you want to delete all notifications?")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    notificationsRef.removeValue().addOnSuccessListener(aVoid -> {
                        Toast.makeText(NotificationsActivity.this, "Notifications cleared", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
        private final List<NotificationModel> notifications;
        private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());

        NotificationAdapter(List<NotificationModel> notifications) {
            this.notifications = notifications;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NotificationModel notification = notifications.get(position);
            holder.title.setText(notification.getTitle());
            holder.message.setText(notification.getMessage() + "\n" + sdf.format(new Date(notification.getTimestamp())));
            
            holder.title.setTextColor(android.graphics.Color.WHITE);
            holder.message.setTextColor(android.graphics.Color.LTGRAY);
        }

        @Override
        public int getItemCount() { return notifications.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, message;
            ViewHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(android.R.id.text1);
                message = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}
