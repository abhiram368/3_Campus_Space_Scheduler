package com.example.campus_space_scheduler.lab_admin_incharge.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campus_space_scheduler.R;
import com.example.campus_space_scheduler.lab_admin_incharge.models.Booking;
import java.util.List;

public class PendingRequestsAdapter extends RecyclerView.Adapter<PendingRequestsAdapter.ViewHolder> {

    private List<Booking> bookings;
    private OnActionClickListener listener;

    public interface OnActionClickListener {
        void onItemClick(Booking booking);
    }

    public PendingRequestsAdapter(List<Booking> bookings, OnActionClickListener listener) {
        this.bookings = bookings;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Booking booking = bookings.get(position);
        holder.tvSpaceName.setText(booking.getSpaceName());
        holder.tvPurpose.setText(booking.getPurpose());
        holder.tvDate.setText("Date: " + booking.getDate());
        holder.tvTimeSlot.setText("Time: " + booking.getTimeSlot());
        holder.tvStatusLabel.setText("Status: " + booking.getStatus());

        holder.itemView.setOnClickListener(v -> listener.onItemClick(booking));
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSpaceName, tvPurpose, tvDate, tvTimeSlot, tvStatusLabel;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSpaceName = itemView.findViewById(R.id.tvSpaceName);
            tvPurpose = itemView.findViewById(R.id.tvPurpose);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTimeSlot = itemView.findViewById(R.id.tvTimeSlot);
            tvStatusLabel = itemView.findViewById(R.id.tvStatusLabel);
        }
    }
}