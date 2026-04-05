package com.example.campus_space_scheduler.booking_user;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_space_scheduler.R;

import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private List<Booking> bookingList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Booking booking);
    }

    public BookingAdapter(List<Booking> bookingList, OnItemClickListener listener) {
        this.bookingList = bookingList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookingList.get(position);

        if (holder.textViewSpaceName != null) {
            holder.textViewSpaceName.setText(booking.getSpaceName() != null ? booking.getSpaceName() : "Loading...");
        }

        holder.textViewDate.setText(booking.getDate() != null ? booking.getDate() : "");
        holder.textViewTimeSlot.setText(booking.getTimeSlot() != null ? booking.getTimeSlot() : "");
        holder.textViewPurpose.setText("Purpose: " + (booking.getPurpose() != null ? booking.getPurpose() : "N/A"));

        String status = booking.getStatus();
        holder.textViewStatus.setText(status != null ? status.toUpperCase() : "PENDING");

        // Handle Remarks and Forwarded status as requested
        StringBuilder remarksBuilder = new StringBuilder();
        boolean isForwarded = status != null && status.equalsIgnoreCase("Forwarded");
        
        if (isForwarded) {
            remarksBuilder.append("Forwarded by: ").append(booking.getActionBy() != null && !booking.getActionBy().isEmpty() ? booking.getActionBy() : "Authority");
            if (booking.getRemarks() != null && !booking.getRemarks().trim().isEmpty()) {
                remarksBuilder.append("\nRemarks: ").append(booking.getRemarks());
            }
        } else if (booking.getRemarks() != null && !booking.getRemarks().trim().isEmpty()) {
            remarksBuilder.append("Remarks: ").append(booking.getRemarks());
        }

        if (remarksBuilder.length() > 0) {
            holder.textViewRemarks.setText(remarksBuilder.toString());
            holder.textViewRemarks.setVisibility(View.VISIBLE);
        } else {
            holder.textViewRemarks.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(booking));

        // UI styling for status
        if (status != null) {
            Drawable background = holder.textViewStatus.getBackground();
            if (background != null) {
                background = DrawableCompat.wrap(background).mutate();
                switch (status.toLowerCase()) {
                    case "accepted":
                    case "booked":
                    case "approved":
                        holder.textViewStatus.setBackgroundResource(R.drawable.status_accepted_bg);
                        DrawableCompat.setTintList(DrawableCompat.wrap(holder.textViewStatus.getBackground()), null);
                        break;
                    case "rejected":
                        holder.textViewStatus.setBackgroundResource(R.drawable.status_rejected_bg);
                        DrawableCompat.setTintList(DrawableCompat.wrap(holder.textViewStatus.getBackground()), null);
                        break;
                    case "forwarded":
                        // Orange for forwarded as requested
                        holder.textViewStatus.setBackgroundResource(R.drawable.status_pending_bg);
                        DrawableCompat.setTint(DrawableCompat.wrap(holder.textViewStatus.getBackground().mutate()), Color.parseColor("#FF9800")); 
                        break;
                    case "pending":
                    default:
                        holder.textViewStatus.setBackgroundResource(R.drawable.status_pending_bg);
                        DrawableCompat.setTintList(DrawableCompat.wrap(holder.textViewStatus.getBackground().mutate()), null);
                        break;
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView textViewSpaceName, textViewDate, textViewTimeSlot, textViewPurpose, textViewStatus, textViewRemarks;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewSpaceName = itemView.findViewById(R.id.textViewSpaceName);
            textViewDate = itemView.findViewById(R.id.textViewDate);
            textViewTimeSlot = itemView.findViewById(R.id.textViewTimeSlot);
            textViewPurpose = itemView.findViewById(R.id.textViewPurpose);
            textViewStatus = itemView.findViewById(R.id.textViewStatus);
            textViewRemarks = itemView.findViewById(R.id.textViewRemarks);
        }
    }
}
