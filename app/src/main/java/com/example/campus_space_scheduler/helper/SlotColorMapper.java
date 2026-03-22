package com.example.campus_space_scheduler.helper;

import android.graphics.Color;

import com.example.campus_space_scheduler.R;
import com.example.campus_space_scheduler.enums.SlotStatus;

public class SlotColorMapper {

    public static int getColor(SlotStatus status) {

        switch (status) {

            case AVAILABLE:
                return Color.parseColor("#4CAF50"); // soft green

            case BOOKED:
                return Color.parseColor("#F44336"); // soft red

            case PENDING:
                return Color.parseColor("#FF9800"); // soft orange

            case BLOCKED:
                return Color.parseColor("#9E9E9E"); // light gray

            case MAINTENANCE:
                return Color.parseColor("#FFC107"); // soft yellow

            default:
                return Color.parseColor("#FFFFFF");
        }
    }

    public static int getDrawable(SlotStatus status) {
        switch (status) {
            case AVAILABLE: return R.drawable.slot_available;
            case BOOKED: return R.drawable.slot_booked;
            case PENDING: return R.drawable.slot_pending;
            case BLOCKED: return R.drawable.slot_blocked;
            case MAINTENANCE: return R.drawable.slot_maintenance;
            default: return R.drawable.slot_default;
        }
    }
}