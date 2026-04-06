package com.example.campus_space_scheduler.booking_user;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.campus_space_scheduler.MainActivity;
import com.example.campus_space_scheduler.R;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "booking_notifications";
    private static final String CHANNEL_NAME = "Booking Updates";
    private static final String CHANNEL_DESC = "Notifications for booking requests and status updates";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
            channel.setDescription(CHANNEL_DESC);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created or verified");
            }
        }
    }

    public static void showNotification(Context context, String title, String message) {
        showNotification(context, title, message, null);
    }

    public static void showNotification(Context context, String title, String message, String bookingId) {
        Log.d(TAG, "Showing notification: " + title + " - " + message + (bookingId != null ? " for " + bookingId : ""));
        createNotificationChannel(context);

        // Redirect to MainActivity to handle smart role-based routing
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("OPEN_NOTIFICATIONS", true);
        if (bookingId != null) {
            intent.putExtra("BOOKING_ID", bookingId); // Preserved from backup branch
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent;
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message)) // Enable expandable notification
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        try {
            if (!notificationManager.areNotificationsEnabled()) {
                Log.w(TAG, "Notifications are disabled for this app");
                return;
            }

            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
            Log.d(TAG, "Notification sent to system successfully");
        } catch (SecurityException e) {
            Log.e(TAG, "Permission missing for notification: " + e.getMessage());
        }
    }
}