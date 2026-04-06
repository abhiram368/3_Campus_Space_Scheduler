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
import com.example.hod.models.Booking;
import com.example.hod.models.LiveStatusData;

import java.util.List;

public class HodScheduleAdapter extends RecyclerView.Adapter<HodScheduleAdapter.ViewHolder> {

    private Context context;
    private List<LiveStatusData> list;
    private String spaceName;
    private boolean isPastDate = false;

    public HodScheduleAdapter(Context context, List<LiveStatusData> list, String spaceName) {
        this.context = context;
        this.list = list;
        this.spaceName = spaceName;
    }

    public void setPastDate(boolean pastDate) {
        this.isPastDate = pastDate;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_slot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LiveStatusData data = list.get(position);

        String timeText;
        if (data.startTime != null && data.endTime != null) {
            timeText = formatTime(data.startTime) + " – " + formatTime(data.endTime);
        } else {
            timeText = data.slotKey != null ? data.slotKey : "Unknown Slot";
        }
        holder.tvSlotTime.setText(timeText);
        
        String statusText = data.status != null ? data.status : "AVAILABLE";
        holder.tvStatus.setText(statusText);

        if (isPastDate) {
            holder.tvStatus.setTextColor(Color.GRAY);
            holder.tvDetails.setText("Past Date");
        } else {
            // Status coloring
            if ("AVAILABLE".equalsIgnoreCase(statusText)) {
                holder.tvStatus.setTextColor(context.getResources().getColor(R.color.status_available));
                holder.tvDetails.setText("Open for bookings");
            } else if ("BOOKED".equalsIgnoreCase(statusText)) {
                holder.tvStatus.setTextColor(context.getResources().getColor(R.color.status_booked));
                if (data.booking != null) {
                    String name = data.booking.getRequesterName() != null ? data.booking.getRequesterName() : data.booking.getBookedBy();
                    holder.tvDetails.setText("Req: " + name + " | Purpose: " + data.booking.getPurpose());
                } else {
                    holder.tvDetails.setText("Loading details...");
                }
            } else if ("BLOCKED".equalsIgnoreCase(statusText)) {
                holder.tvStatus.setTextColor(Color.parseColor("#EF4444"));
                holder.tvDetails.setText("Blocked by Staff/Admin");
            } else {
                holder.tvStatus.setTextColor(Color.WHITE);
                holder.tvDetails.setText("-");
            }
        }

        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(context, com.example.hod.hod.HodLabSlotDetailActivity.class);
            intent.putExtra("slotData", data);
            intent.putExtra("spaceName", spaceName);
            intent.putExtra("date", data.date);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private String formatTime(String time) {
        if (time == null) return "";
        if (time.length() == 4 && !time.contains(":")) {
            return time.substring(0, 2) + ":" + time.substring(2);
        }
        return time;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSlotTime, tvStatus, tvDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSlotTime = itemView.findViewById(R.id.tvSlotTime);
            tvStatus = itemView.findViewById(R.id.tvAdminName);
            tvDetails = itemView.findViewById(R.id.tvAdminRoll);
        }
    }
}
