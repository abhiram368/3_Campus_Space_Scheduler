package com.example.hod.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.hod.R;
import com.example.hod.models.NotificationModel;
import com.example.hod.staff.RequestDetailActivity;
import com.example.hod.staff.StaffCompletedRequestDetailActivity;
import com.example.hod.hod.HodRequestDetailActivity;
import com.example.hod.hod.HodCompletedRequestDetailActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<NotificationModel> notificationList;
    private boolean isSelectionMode = false;
    private final Set<String> selectedIds = new HashSet<>();
    private OnSelectionListener selectionListener;

    public interface OnSelectionListener {
        void onSelectionChanged(int count);
    }

    public NotificationAdapter(List<NotificationModel> notificationList) {
        this.notificationList = notificationList;
    }

    public void setOnSelectionListener(OnSelectionListener listener) {
        this.selectionListener = listener;
    }

    public OnSelectionListener getOnSelectionListener() {
        return selectionListener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationModel notification = notificationList.get(position);
        holder.message.setText(notification.getMessage());
        
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                notification.getTimestamp(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS);
        holder.time.setText(timeAgo);

        if (notification.isRead()) {
            holder.dot.setVisibility(View.GONE);
            holder.message.setTypeface(null, Typeface.NORMAL);
            holder.itemView.setAlpha(0.7f);
        } else {
            holder.dot.setVisibility(View.VISIBLE);
            holder.message.setTypeface(null, Typeface.BOLD);
            holder.itemView.setAlpha(1.0f);
        }
        
        // Icon handling based on type
        String type = notification.getType() != null ? notification.getType().toLowerCase() : "";
        int colorRes = R.color.primary_blue;

        if (type.contains("approval")) {
            colorRes = android.R.color.holo_green_dark;
        } else if (type.contains("rejection") || type.contains("blocked") || type.contains("cancelled")) {
            colorRes = android.R.color.holo_red_dark;
        } else if (type.contains("escalation")) {
            colorRes = android.R.color.holo_orange_dark;
        }

        holder.icon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), colorRes));

        // Selection Mode UI
        holder.checkBox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(selectedIds.contains(notification.getId()));

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(notification.getId());
            } else {
                String uid = FirebaseAuth.getInstance().getUid();
                if (uid != null && notification.getId() != null && !notification.isRead()) {
                    notification.setRead(true);
                    FirebaseDatabase.getInstance().getReference("notifications")
                            .child(uid).child(notification.getId()).child("read").setValue(true);
                    notifyItemChanged(position);
                }
                
                if (notification.getRelatedBookingId() != null) {
                    navigateToDetail(v.getContext(), notification);
                }
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                setSelectionMode(true);
                toggleSelection(notification.getId());
                return true;
            }
            return false;
        });

        holder.checkBox.setOnClickListener(v -> toggleSelection(notification.getId()));
    }

    private void toggleSelection(String id) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id);
        } else {
            selectedIds.add(id);
        }
        notifyDataSetChanged();
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(selectedIds.size());
        }
    }

    public void selectAll() {
        for (NotificationModel notification : notificationList) {
            selectedIds.add(notification.getId());
        }
        notifyDataSetChanged();
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(selectedIds.size());
        }
    }

    public void clearSelection() {
        selectedIds.clear();
        notifyDataSetChanged();
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(selectedIds.size());
        }
    }

    public void setSelectionMode(boolean enabled) {
        if (this.isSelectionMode == enabled) return; // Recursion safety guard
        this.isSelectionMode = enabled;
        if (!enabled) selectedIds.clear();
        notifyDataSetChanged();
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(selectedIds.size());
        }
    }

    public Set<String> getSelectedIds() {
        return selectedIds;
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    private void navigateToDetail(Context context, NotificationModel notif) {
        String bookingId = notif.getRelatedBookingId();
        if (bookingId == null || bookingId.isEmpty()) return;

        String currentUid = FirebaseAuth.getInstance().getUid();
        String bookedBy = notif.getBookedBy();
        String roleTarget = notif.getRoleTarget() != null ? notif.getRoleTarget().toLowerCase() : "";

        // CASE 2: User Receipt (Student View)
        // PRIORITY: Check if this is an Authority-targeted action first
        String message = notif.getMessage() != null ? notif.getMessage().toLowerCase() : "";
        boolean isAuthorityRole = roleTarget.equals("staff") || 
                                  roleTarget.equals("hod") || 
                                  roleTarget.equals("faculty") ||
                                  message.contains("pending") || 
                                  message.contains("approval");

        boolean isStudentReceipt = !isAuthorityRole && ((currentUid != null && currentUid.equals(bookedBy)) || 
                                     roleTarget.equals("student") || 
                                     roleTarget.equals("user") || 
                                     (currentUid != null && currentUid.equals(roleTarget)));

        if (isStudentReceipt) {
            Intent intent = new Intent();
            intent.setClassName(context, "com.example.campus_space_scheduler.booking_user.BookingDetailsActivity");
            intent.putExtra("BOOKING_ID", bookingId);
            intent.putExtra("bookingId", bookingId);
            context.startActivity(intent);
            return;
        }

        // CASE 1: Authority View (Staff/HOD Approval)
        // Perform "Lightning Check" to avoid flicker
        FirebaseDatabase.getInstance().getReference("bookings").child(bookingId)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            // If booking deleted, just show original logic fallback
                            fallbackNavigation(context, notif);
                            return;
                        }

                        String status = snapshot.child("status").getValue(String.class);
                        if (status == null) status = "pending";
                        String lowerStatus = status.toLowerCase();

                        // Live status determines processing state
                        boolean isProcessed = lowerStatus.equals("approved") || 
                                            lowerStatus.contains("rejected") || 
                                            lowerStatus.contains("cancelled") || 
                                            lowerStatus.equals("booked") ||
                                            lowerStatus.equals("used");

                        String hostActivity = context.getClass().getSimpleName();
                        boolean isHodActivity = hostActivity.contains("Hod") || 
                                              (roleTarget.equals("hod"));

                        // Parse the full booking object to avoid empty detail pages
                        com.example.hod.models.Booking booking = snapshot.getValue(com.example.hod.models.Booking.class);
                        if (booking != null && booking.getBookingId() == null) {
                            booking.setBookingId(snapshot.getKey());
                        }

                        Intent intent;
                        if (isHodActivity) {
                            if (isProcessed) {
                                intent = new Intent(context, HodCompletedRequestDetailActivity.class);
                            } else {
                                intent = new Intent(context, HodRequestDetailActivity.class);
                            }
                        } else {
                            if (isProcessed) {
                                intent = new Intent(context, StaffCompletedRequestDetailActivity.class);
                            } else {
                                intent = new Intent(context, RequestDetailActivity.class);
                            }
                        }
                        
                        intent.putExtra("booking", booking);
                        intent.putExtra("bookingId", bookingId);
                        intent.putExtra("BOOKING_ID", bookingId);
                        context.startActivity(intent);
                    }

                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                        fallbackNavigation(context, notif);
                    }
                });
    }

    private void fallbackNavigation(Context context, NotificationModel notif) {
        String status = notif.getTargetStatus() != null ? notif.getTargetStatus().toLowerCase() : "";
        String bookingId = notif.getRelatedBookingId();
        String message = notif.getMessage() != null ? notif.getMessage().toLowerCase() : "";
        
        boolean isActionable = status.equals("pending") || 
                             status.contains("forwarded") || 
                             message.contains("new booking request");

        String hostActivity = context.getClass().getSimpleName();
        boolean isHodActivity = hostActivity.contains("Hod");

        Intent intent;
        if (isHodActivity) {
            if (isActionable) intent = new Intent(context, HodRequestDetailActivity.class);
            else intent = new Intent(context, HodCompletedRequestDetailActivity.class);
        } else {
            if (isActionable) intent = new Intent(context, RequestDetailActivity.class);
            else intent = new Intent(context, StaffCompletedRequestDetailActivity.class);
        }
        
        intent.putExtra("bookingId", bookingId);
        intent.putExtra("BOOKING_ID", bookingId);
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView message, time;
        View dot;
        ImageView icon;
        CheckBox checkBox;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.notification_message);
            time = itemView.findViewById(R.id.notification_time);
            dot = itemView.findViewById(R.id.unread_dot);
            icon = itemView.findViewById(R.id.notification_icon);
            checkBox = itemView.findViewById(R.id.notification_checkbox);
        }
    }
}
