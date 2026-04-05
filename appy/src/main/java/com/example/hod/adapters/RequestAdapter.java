package com.example.hod.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hod.R;
import com.example.hod.models.Booking;
import com.example.hod.staff.RequestDetailActivity;
import com.example.hod.staff.StaffCompletedRequestDetailActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private final Context context;
    private final List<Booking> bookingList;
    private final String mode;
    private boolean isSelectionMode = false;
    private final java.util.Set<String> selectedItems = new java.util.HashSet<>();

    public RequestAdapter(Context context, List<Booking> bookingList, String mode) {
        this.context = context;
        this.bookingList = bookingList;
        this.mode = mode;
    }

    public void setSelectionMode(boolean enabled) {
        this.isSelectionMode = enabled;
        if (!enabled) selectedItems.clear();
        notifyDataSetChanged();
    }

    public java.util.Set<String> getSelectedItems() {
        return selectedItems;
    }

    public void selectAll(boolean select) {
        selectedItems.clear();
        if (select) {
            for (Booking b : bookingList) {
                if (b.getBookingId() != null) selectedItems.add(b.getBookingId());
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.list_item_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        Booking booking = bookingList.get(position);
        if (booking == null) return;

        // Selection UI
        if (holder.cbSelect != null) {
            holder.cbSelect.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
            holder.cbSelect.setChecked(selectedItems.contains(booking.getBookingId()));
            holder.cbSelect.setOnClickListener(v -> {
                if (selectedItems.contains(booking.getBookingId())) {
                    selectedItems.remove(booking.getBookingId());
                } else {
                    selectedItems.add(booking.getBookingId());
                }
            });
        }

        // Priority: Show real name instantly if stored in the booking record
        String existingName = booking.getRequesterName();
        if (existingName != null && !existingName.isEmpty()) {
            holder.tvUsername.setText(existingName);
            // Role display for cached name case
            String role = booking.getRequesterRole();
            if (role != null && !role.isEmpty()) {
                holder.tvRequesterRole.setText(capitalizeWords(role));
                holder.tvRequesterRole.setVisibility(View.VISIBLE);
            } else {
                holder.tvRequesterRole.setVisibility(View.GONE);
            }
        } else {
            // Fallback: Show UID initially and fetch real name from RTDB users/{uid}
            String uid = booking.getBookedBy();
            holder.tvUsername.setText(uid != null ? uid : "Unknown");
            holder.tvRequesterRole.setVisibility(View.GONE);

            if (uid != null && !uid.isEmpty()) {
                FirebaseDatabase.getInstance().getReference("users").child(uid)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String name = snapshot.child("name").getValue(String.class);
                                String role = snapshot.child("role").getValue(String.class);
                                if (name != null && !name.isEmpty()) {
                                    booking.setRequesterName(name);
                                    holder.tvUsername.setText(name);
                                }
                                if (role != null && !role.isEmpty()) {
                                    booking.setRequesterRole(role);
                                    holder.tvRequesterRole.setText(capitalizeWords(role));
                                    holder.tvRequesterRole.setVisibility(View.VISIBLE);
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
            }
        }

        // --- RESTORED UI LOGIC ---
        holder.tvLabName.setText(booking.getSpaceName() != null ? booking.getSpaceName() : "Unknown Space");

        String dateStr = booking.getDate() != null ? booking.getDate() : "";
        String timeStr = booking.getTimeSlot() != null ? booking.getTimeSlot() : "";
        holder.tvSlot.setText(dateStr + (dateStr.isEmpty() || timeStr.isEmpty() ? "" : " | ") + timeStr);

        String status = booking.getStatus() != null ? booking.getStatus() : "Pending";
        String displayStatus = status.replace("_", " ").toUpperCase();
        
        if (displayStatus.equals("REJECTED EXPIRED")) {
            displayStatus = "EXPIRED";
        }
        
        // Improve formatting for long forwarded status
        if (displayStatus.contains("FORWARDED TO FACULTY")) {
            displayStatus = displayStatus.replace("FORWARDED TO ", "FORWARDED TO\n")
                                       .replace("FACULTYINCHARGE", "FACULTY INCHARGE");
        }
        
        holder.tvStatus.setText(displayStatus);
        applyStatusStyle(holder.tvStatus, status);

        // -------------------------

        if (holder.ivArrow != null) {
            holder.ivArrow.setImageResource(R.drawable.ic_chevron_right);
        }

        // On card click (Universal "Lightning Check" to avoid flicker)
        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                if (selectedItems.contains(booking.getBookingId())) {
                    selectedItems.remove(booking.getBookingId());
                } else {
                    selectedItems.add(booking.getBookingId());
                }
                notifyItemChanged(position);
                return;
            }

            String bId = booking.getBookingId();
            if (bId == null || bId.isEmpty()) return;

            FirebaseDatabase.getInstance().getReference("bookings").child(bId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;

                        String status = snapshot.child("status").getValue(String.class);
                        if (status == null) status = "pending";
                        String lowerStatus = status.toLowerCase();

                        // Determine if decision is already made. For Staff, Forwarded means they are done.
                        boolean isProcessed = lowerStatus.equals("approved") || 
                                            lowerStatus.contains("rejected") || 
                                            lowerStatus.contains("cancelled") || 
                                            lowerStatus.contains("expired") ||
                                            lowerStatus.contains("forwarded") ||
                                            lowerStatus.equals("booked") ||
                                            lowerStatus.equals("used");

                        // Parse the latest booking data from the lightning check
                        com.example.hod.models.Booking latestBooking = snapshot.getValue(com.example.hod.models.Booking.class);
                        if (latestBooking != null && latestBooking.getBookingId() == null) {
                            latestBooking.setBookingId(snapshot.getKey());
                        }

                        Intent intent;
                        // Determine context while allowing for live status override
                        if (lowerStatus.contains("forwarded_to_faculty") || "escalated".equalsIgnoreCase(mode)) {
                            // HOD Context
                            if (isProcessed) {
                                intent = new Intent(context, com.example.hod.hod.HodCompletedRequestDetailActivity.class);
                            } else {
                                intent = new Intent(context, com.example.hod.hod.HodRequestDetailActivity.class);
                            }
                        } else if ("hod_history".equalsIgnoreCase(mode)) {
                            intent = new Intent(context, com.example.hod.hod.HodCompletedRequestDetailActivity.class);
                        } else {
                            // Staff Context
                            if (isProcessed) {
                                intent = new Intent(context, StaffCompletedRequestDetailActivity.class);
                            } else {
                                intent = new Intent(context, RequestDetailActivity.class);
                            }
                        }

                        intent.putExtra("booking", latestBooking != null ? latestBooking : booking); 
                        intent.putExtra("bookingId", bId);
                        intent.putExtra("requesterName", holder.tvUsername.getText().toString());
                        context.startActivity(intent);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Fallback logic
                        Intent intent;
                        if ("pending".equalsIgnoreCase(mode)) intent = new Intent(context, RequestDetailActivity.class);
                        else if ("escalated".equalsIgnoreCase(mode)) intent = new Intent(context, com.example.hod.hod.HodRequestDetailActivity.class);
                        else if ("hod_history".equalsIgnoreCase(mode)) intent = new Intent(context, com.example.hod.hod.HodCompletedRequestDetailActivity.class);
                        else intent = new Intent(context, StaffCompletedRequestDetailActivity.class);
                        
                        intent.putExtra("booking", booking);
                        intent.putExtra("requesterName", holder.tvUsername.getText().toString());
                        context.startActivity(intent);
                    }
                });
        });
    }

    private void applyStatusStyle(TextView tv, String status) {
        if (status == null) status = "pending";
        String lowerStatus = status.toLowerCase();
        
        int badgeRes;
        if (lowerStatus.contains("rejected") || lowerStatus.contains("expired") || lowerStatus.contains("cancelled")) {
            badgeRes = R.drawable.badge_red;
        } else if (lowerStatus.equals("approved") || lowerStatus.equals("available")) {
            badgeRes = R.drawable.badge_green;
        } else if (lowerStatus.equals("booked")) {
            badgeRes = R.drawable.badge_blue;
        } else {
            badgeRes = R.drawable.badge_orange; // Pending, Forwarded, etc.
        }
        
        tv.setBackgroundResource(badgeRes);
        tv.setTextColor(context.getResources().getColor(R.color.status_text_color));
    }

    private String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) sb.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString().trim();
    }

    @Override
    public int getItemCount() { return bookingList.size(); }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvRequesterRole, tvLabName, tvSlot, tvStatus, tvReason;
        com.google.android.material.checkbox.MaterialCheckBox cbSelect;
        android.widget.ImageView ivArrow;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvRequesterRole = itemView.findViewById(R.id.tvRequesterRole);
            tvLabName  = itemView.findViewById(R.id.tvLabName);
            tvSlot     = itemView.findViewById(R.id.tvSlot);
            tvStatus   = itemView.findViewById(R.id.tvStatus);
            tvReason   = itemView.findViewById(R.id.tvReason);
            cbSelect   = itemView.findViewById(R.id.cbSelect);
            ivArrow    = itemView.findViewById(R.id.ivArrow);
        }
    }
}
