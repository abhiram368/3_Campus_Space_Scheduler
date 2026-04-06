package com.example.hod.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hod.R;
import com.example.hod.models.Booking;

import java.util.List;

/**
 * Adapter for Staff Incharge's View Schedule screen.
 * Displays each slot as a glass-card matching the HOD schedule style.
 */
public class StaffScheduleAdapter extends RecyclerView.Adapter<StaffScheduleAdapter.SlotViewHolder> {

    public static class SlotItem implements java.io.Serializable {
        public String timeRange;  // e.g. "09:00 – 10:00"
        public String slotKey;    // raw Firebase key e.g. "09:00 - 10:00"
        public String status;     // e.g. "AVAILABLE", "BOOKED", etc.
        public String spaceId;
        public String date;
        public Booking booking;   // populated for BOOKED slots
        public String adminName; // Assigned Lab Admin for this slot
        public int startTimeMinutes; // Added for sorting and comparison
    }

    public interface SelectionListener {
        void onSelectionModeChanged(boolean enabled);
        void onSelectionCountChanged(int count);
    }

    private final Context context;
    private final List<SlotItem> slots;
    private boolean isSelectionMode = false;
    private final java.util.Set<SlotItem> selectedSlots = new java.util.HashSet<>();
    private SelectionListener selectionListener;

    private boolean isPastDate = false;

    public StaffScheduleAdapter(Context context, List<SlotItem> slots) {
        this.context = context;
        this.slots   = slots;
    }

    public void setPastDate(boolean pastDate) {
        this.isPastDate = pastDate;
        notifyDataSetChanged();
    }

    public void setSelectionListener(SelectionListener listener) {
        this.selectionListener = listener;
    }

    @NonNull
    @Override
    public SlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_staff_slot, parent, false);
        return new SlotViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SlotViewHolder h, int position) {
        SlotItem slot = slots.get(position);

        h.tvTimeRange.setText(slot.timeRange);
        h.tvStatus.setText(formatStatus(slot));
        applyStatusColor(h, slot);

        boolean slotIsPast = isPastDate || isSlotInPast(slot);
        
        // Visual feedback for past dates
        h.itemView.setAlpha(slotIsPast ? 0.6f : 1.0f);

        // Selection UI
        if (isSelectionMode) {
            h.cbSelected.setVisibility(View.VISIBLE);
            h.cbSelected.setChecked(selectedSlots.contains(slot));
        } else {
            h.cbSelected.setVisibility(View.GONE);
        }

        // Participant/Booker/Admin Info
        String upper = slot.status != null ? slot.status.toUpperCase() : "";
        if ("BOOKED".equals(upper) && slot.booking != null) {
            String bookedBy = slot.booking.getBookedBy();
            h.tvBookerInfo.setText("Booked by: " + (slot.booking.getRequesterName() != null ? slot.booking.getRequesterName() : bookedBy));
        } else if ("AVAILABLE".equals(upper) && slot.adminName != null && !slot.adminName.isEmpty()) {
            h.tvBookerInfo.setText("Admin: " + slot.adminName);
        } else {
            h.tvBookerInfo.setText("");
        }

        // Clicks
        h.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                if (slotIsPast) {
                    Toast.makeText(context, "Cannot modify past schedules", Toast.LENGTH_SHORT).show();
                    return;
                }
                toggleSelection(slot);
            } else {
                Intent intent = new Intent(context, com.example.hod.staff.LabSlotDetailActivity.class);
                intent.putExtra("slotItem", slot);
                context.startActivity(intent);
            }
        });

        h.itemView.setOnLongClickListener(v -> {
            if (slotIsPast) {
                Toast.makeText(context, "Historical data is read-only", Toast.LENGTH_SHORT).show();
                return true;
            }
            if (!isSelectionMode) {
                enterSelectionMode();
                toggleSelection(slot);
                return true;
            }
            return false;
        });
    }

    private void toggleSelection(SlotItem slot) {
        int index = slots.indexOf(slot);
        if (selectedSlots.contains(slot)) {
            selectedSlots.remove(slot);
        } else {
            selectedSlots.add(slot);
        }
        if (index != -1) notifyItemChanged(index);
        
        if (selectionListener != null) {
            selectionListener.onSelectionCountChanged(selectedSlots.size());
            if (selectedSlots.isEmpty()) {
                exitSelectionMode();
            }
        }
    }

    public void enterSelectionMode() {
        isSelectionMode = true;
        selectedSlots.clear();
        notifyDataSetChanged();
        if (selectionListener != null) selectionListener.onSelectionModeChanged(true);
    }

    public void exitSelectionMode() {
        isSelectionMode = false;
        selectedSlots.clear();
        notifyDataSetChanged();
        if (selectionListener != null) selectionListener.onSelectionModeChanged(false);
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    public java.util.Set<SlotItem> getSelectedSlots() {
        return selectedSlots;
    }

    private boolean isSlotInPast(SlotItem slot) {
        if (slot.date == null || slot.slotKey == null) return false;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Date slotDate = sdf.parse(slot.date);
            if (slotDate == null) return isPastDate;
            
            java.util.Calendar slotCal = java.util.Calendar.getInstance();
            slotCal.setTime(slotDate);
            
            String digits = slot.slotKey.replaceAll("[^0-9]", "");
            if (digits.length() >= 4) {
               int hour = Integer.parseInt(digits.substring(0, 2));
               int min = Integer.parseInt(digits.substring(2, 4));
               slotCal.set(java.util.Calendar.HOUR_OF_DAY, hour);
               slotCal.set(java.util.Calendar.MINUTE, min);
            } else {
               return isPastDate;
            }
            return slotCal.getTime().before(new java.util.Date());
        } catch (Exception e) {
            return isPastDate;
        }
    }

    private String formatStatus(SlotItem slot) {
        String status = slot.status;
        if (status == null) status = "AVAILABLE";
        String upper = status.toUpperCase();
        
        boolean slotIsPast = isPastDate || isSlotInPast(slot);
        
        if (slotIsPast) {
            if ("AVAILABLE".equals(upper)) return "Unused";
            if ("BOOKED".equals(upper)) return "Used";
            if ("COMPLETED".equals(upper)) return "Used";
            if ("BLOCKED".equals(upper)) return "Blocked";
            return "Used"; // fallback for other past items
        }
        
        switch (upper) {
            case "AVAILABLE": return "Available";
            case "BOOKED":    return "Booked";
            case "USED":      return "Used";
            case "UNUSED":    return "Unused";
            case "CANCELLED": return "Cancelled";
            case "REJECTED":  return "Rejected";
            case "BLOCKED":   return "Blocked";
            default:          return status;
        }
    }

    private void applyStatusColor(SlotViewHolder h, SlotItem slot) {
        String status = slot.status;
        int color;
        if (status == null) {
            color = ContextCompat.getColor(context, R.color.text_secondary);
        } else {
            String upper = status.toUpperCase();
            boolean slotIsPast = isPastDate || isSlotInPast(slot);
            if (slotIsPast) {
                if ("AVAILABLE".equals(upper)) color = ContextCompat.getColor(context, R.color.text_secondary); // Unused (Gray)
                else if ("BOOKED".equals(upper) || "COMPLETED".equals(upper)) color = ContextCompat.getColor(context, R.color.primary_blue); // Used (Blue)
                else if ("BLOCKED".equals(upper)) color = ContextCompat.getColor(context, R.color.text_secondary);
                else color = ContextCompat.getColor(context, R.color.text_secondary);
            } else {
                switch (upper) {
                    case "AVAILABLE": color = ContextCompat.getColor(context, R.color.status_available); break;
                    case "BOOKED":    color = ContextCompat.getColor(context, R.color.status_booked); break;
                    case "USED":      color = ContextCompat.getColor(context, R.color.primary_blue); break;
                    case "CANCELLED":
                    case "REJECTED":  color = ContextCompat.getColor(context, R.color.status_rejected); break;
                    case "BLOCKED":   color = ContextCompat.getColor(context, R.color.text_secondary); break;
                    default:          color = ContextCompat.getColor(context, R.color.status_pending); break;
                }
            }
        }
        h.tvStatus.setTextColor(color);
    }

    @Override
    public int getItemCount() { return slots.size(); }

    static class SlotViewHolder extends RecyclerView.ViewHolder {
        TextView tvTimeRange, tvStatus, tvBookerInfo;
        android.widget.CheckBox cbSelected;

        SlotViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimeRange  = itemView.findViewById(R.id.tvTimeRange);
            tvStatus     = itemView.findViewById(R.id.tvStatus);
            tvBookerInfo = itemView.findViewById(R.id.tvBookerInfo);
            cbSelected   = itemView.findViewById(R.id.cbSelected);
        }
    }
}
