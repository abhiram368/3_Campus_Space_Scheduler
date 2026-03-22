package com.example.campus_space_scheduler.csed_office;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_space_scheduler.R;
import com.example.campus_space_scheduler.model.BookingModel;

import java.util.List;

public class BookingViewAdapter extends RecyclerView.Adapter<BookingViewAdapter.ViewHolder> {

    private final List<BookingModel> list;

    public BookingViewAdapter(List<BookingModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingModel booking = list.get(position);

        holder.tvSpace.setText(booking.getSpaceName());
        holder.tvTime.setText(booking.getSlotStart() + " - " + (booking.getSlotStart() + "0030")); // adjust if different
        holder.tvStatus.setText(booking.getStatus());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvSpace, tvTime, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvSpace = itemView.findViewById(R.id.tvSpace);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}