package com.example.campus_space_scheduler.app_admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_space_scheduler.databinding.AItemBookingBinding;
import com.example.campus_space_scheduler.model.BookingModel;

import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.ViewHolder> {

    public interface OnDeleteClick {
        void onDelete(String key);
    }

    private List<ManageBookingsActivity.DataNode> list;
    private final OnDeleteClick listener;

    public BookingAdapter(List<ManageBookingsActivity.DataNode> list, OnDeleteClick listener) {
        this.list = list;
        this.listener = listener;
    }

    public void updateList(List<ManageBookingsActivity.DataNode> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        AItemBookingBinding binding = AItemBookingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        ManageBookingsActivity.DataNode node = list.get(position);
        BookingModel m = node.model;

        String primary = m.getSpaceName() + " | " + m.getPurpose();

        String secondary =
                "Date: " + (m.getBookedTime() != null ? m.getBookedTime().getDate() : "") +
                        "\nTime: " + m.getSlotStart() +
                        "\nStatus: " + m.getStatus();

        holder.binding.textPrimary.setText(primary);
        holder.binding.textSecondary.setText(secondary);

        holder.binding.btnDelete.setOnClickListener(v -> listener.onDelete(node.key));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        AItemBookingBinding binding;

        ViewHolder(AItemBookingBinding b) {
            super(b.getRoot());
            binding = b;
        }
    }
}
