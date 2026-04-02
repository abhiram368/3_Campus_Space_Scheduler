package com.example.campus_space_scheduler.booking_user;

import android.content.Context;
import android.content.Intent;
import android.provider.CalendarContract;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CalendarHelper {

    public static void addBookingToCalendar(
            Context context,
            String title,
            String description,
            String location,
            String date,
            String timeSlot
    ) {
        try {
            // Expected date format: yyyy-MM-dd
            // Expected timeSlot format: HH:mm - HH:mm
            String[] times = timeSlot.split("-");
            if (times.length != 2) return;

            String startTimeStr = times[0].trim();
            String endTimeStr = times[1].trim();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            
            Calendar startCalendar = Calendar.getInstance();
            startCalendar.setTime(sdf.parse(date + " " + startTimeStr));

            Calendar endCalendar = Calendar.getInstance();
            endCalendar.setTime(sdf.parse(date + " " + endTimeStr));

            Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.Events.DESCRIPTION, description)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startCalendar.getTimeInMillis())
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endCalendar.getTimeInMillis())
                .putExtra(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_PUBLIC)
                .putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY);

            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Could not add to calendar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
