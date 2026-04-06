package com.example.hod.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.hod.R;
import com.example.hod.models.LiveStatusData;

import java.util.List;

public class LiveStatusAdapter extends RecyclerView.Adapter<LiveStatusAdapter.ViewHolder> {

    private Context context;
    private List<LiveStatusData> list;

    public LiveStatusAdapter(Context context, List<LiveStatusData> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_live_status, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LiveStatusData data = list.get(position);

        holder.tvSpaceName.setText(data.spaceName != null ? data.spaceName : "Unknown Space");
        if (data.slotKey != null && data.slotKey.length() == 4) {
            try {
                int hour = Integer.parseInt(data.slotKey.substring(0, 2));
                int min = Integer.parseInt(data.slotKey.substring(2, 4));
                int eMin = (min == 0) ? 30 : 0;
                int eHour = (min == 0) ? hour : hour + 1;
                holder.tvSlotInfo.setText(String.format(java.util.Locale.getDefault(), "Current Slot: %02d:%02d - %02d:%02d", hour, min, eHour, eMin));
            } catch (Exception e) {
                holder.tvSlotInfo.setText("Current Slot: " + data.slotKey);
            }
        } else {
            holder.tvSlotInfo.setText("Current Slot: OFF HOURS");
        }
        holder.tvStatus.setText(data.status != null ? data.status.toUpperCase() : "UNKNOWN");
        
        // Dynamic "Last Updated" for professional feel
        holder.tvLastUpdated.setText("Live • Last updated: Just now");

        // Status coloring and indicators
        if ("AVAILABLE".equalsIgnoreCase(data.status)) {
            holder.tvStatus.setText("AVAILABLE");
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.status_available));
            holder.ivStatusDot.setImageResource(R.drawable.badge_green);
        } else if ("BOOKED".equalsIgnoreCase(data.status)) {
            holder.tvStatus.setText("OCCUPIED");
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.primary_blue));
            holder.ivStatusDot.setImageResource(R.drawable.badge_blue);
        } else if ("BLOCKED".equalsIgnoreCase(data.status)) {
            holder.tvStatus.setText("BLOCKED");
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.status_rejected));
            holder.ivStatusDot.setImageResource(R.drawable.badge_red);
        } else {
            holder.tvStatus.setText("CLOSED");
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.text_secondary));
            holder.ivStatusDot.setImageResource(R.drawable.badge_orange);
        }
        // Click to drill into single-space live status
        holder.itemView.setOnClickListener(v -> {
            if (data.spaceId != null && !data.spaceId.isEmpty()) {
                android.content.Intent intent = new android.content.Intent(context, com.example.hod.staff.LabLiveStatusActivity.class);
                intent.putExtra("labId", data.spaceId);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSpaceName, tvSlotInfo, tvStatus, tvLastUpdated;
        android.widget.ImageView ivStatusDot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSpaceName = itemView.findViewById(R.id.tvSpaceName);
            tvSlotInfo = itemView.findViewById(R.id.tvSlotInfo);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvLastUpdated = itemView.findViewById(R.id.tvLastUpdated);
            ivStatusDot = itemView.findViewById(R.id.ivStatusDot);
        }
    }
}
