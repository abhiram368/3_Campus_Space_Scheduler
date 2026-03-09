package com.example.campus_space_scheduler;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class HourlyGridAdapter extends RecyclerView.Adapter<HourlyGridAdapter.ViewHolder> {

    private final List<HourSlot> slots;

    public HourlyGridAdapter(List<HourSlot> slots) {
        this.slots = slots;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflates the MaterialCardView layout for each hour slot
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hour_slot, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HourSlot slot = slots.get(position);

        // Formats the hour to a 24-hour string (e.g., 09:00)
        holder.tvLabel.setText(String.format("%02d:00", slot.getHour()));

        // Sets initial colors and text based on the slot state
        updateUI(holder, slot.isOpen());

        // Toggles the state between OPEN and CLOSED when clicked
        holder.itemView.setOnClickListener(v -> {
            slot.setOpen(!slot.isOpen());
            updateUI(holder, slot.isOpen());
        });
    }

    /**
     * Updates the visual state of the card.
     * Uses a light mint for 'Open' and a light gray for 'Closed'.
     */
    private void updateUI(ViewHolder holder, boolean isOpen) {
        holder.tvStatus.setText(isOpen ? "OPEN" : "CLOSED");

        if (isOpen) {
            holder.card.setCardBackgroundColor(Color.parseColor("#C8E6C9")); // Light Green
            holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"));       // Dark Green Text
        } else {
            holder.card.setCardBackgroundColor(Color.parseColor("#F5F5F5")); // Light Gray
            holder.tvStatus.setTextColor(Color.parseColor("#757575"));       // Gray Text
        }
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvLabel, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Links the XML IDs to the Java objects
            card = itemView.findViewById(R.id.card_hour);
            tvLabel = itemView.findViewById(R.id.tv_hour_label);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
    }
}