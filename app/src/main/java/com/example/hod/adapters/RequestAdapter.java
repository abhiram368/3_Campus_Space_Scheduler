package com.example.hod.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hod.R;
import com.example.hod.models.Booking;
import com.example.hod.staff.RequestDetailActivity;

import java.io.Serializable;
import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private Context context;
    private List<Booking> bookingList;

    public RequestAdapter(Context context, List<Booking> bookingList) {
        this.context = context;
        this.bookingList = bookingList;
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

        holder.tvUsername.setText("User: " + booking.getUserId());
        holder.tvLabName.setText("Space: " + booking.getSpaceId());
        holder.tvSlot.setText("Slot: " + booking.getSlotId());
        holder.tvReason.setText("Purpose: " + booking.getPurpose());
        holder.tvStatus.setText("Status: " + booking.getStatus());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, RequestDetailActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable("booking", (Serializable) booking);
            intent.putExtras(bundle);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {

        TextView tvUsername, tvLabName, tvSlot, tvReason, tvStatus;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvLabName = itemView.findViewById(R.id.tvLabName);
            tvSlot = itemView.findViewById(R.id.tvSlot);
            tvReason = itemView.findViewById(R.id.tvReason);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
