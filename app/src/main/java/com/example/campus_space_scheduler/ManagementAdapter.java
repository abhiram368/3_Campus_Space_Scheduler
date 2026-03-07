package com.example.campus_space_scheduler;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ManagementAdapter extends RecyclerView.Adapter<ManagementAdapter.ViewHolder> {

    private List<ManagementModel> list = new ArrayList<>();
    private final String mode; // USER or SPACE

    public ManagementAdapter(String mode) {
        this.mode = mode;
    }

    public void setData(List<ManagementModel> newList) {
        this.list = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_stat_card, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        ManagementModel item = list.get(position);

        if ("USER".equals(mode)) {

            String labelText = item.getName() + " (" + item.getRole() + ")";
            holder.label.setText(labelText);

            // show email + roll
            String valueText = item.getEmailId() + " • " + item.getRollNo();
            holder.value.setText(valueText);

        } else {

            String labelText = "Room: " + item.getRoomName();
            holder.label.setText(labelText);

            String valueText = "Capacity: " + item.getCapacity();
            holder.value.setText(valueText);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView label;
        TextView value;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            label = itemView.findViewById(R.id.stat_label);
            value = itemView.findViewById(R.id.stat_value);
        }
    }
}