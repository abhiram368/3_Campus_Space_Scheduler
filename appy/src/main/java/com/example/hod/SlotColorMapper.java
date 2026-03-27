package com.example.hod;

import android.graphics.Color;

public class SlotColorMapper {

    public static int getColor(SlotStatus status) {

        switch (status) {

            case AVAILABLE:
                return Color.parseColor("#4CAF50");

            case BOOKED:
                return Color.parseColor("#36f4cbff");

            case PENDING:
                return Color.parseColor("#FF9800");

            case BLOCKED:
                return Color.parseColor("#000000ff");

            case MAINTENANCE:
                return Color.parseColor("#FFC107");

            default:
                return Color.WHITE;
        }
    }
}