package com.example.campus_space_scheduler.lab_admin_incharge.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.campus_space_scheduler.R;
import com.example.campus_space_scheduler.lab_admin_incharge.BookingDetailsActivity;
import com.example.campus_space_scheduler.lab_admin_incharge.FacultyBookingDetailsActivity;
import com.example.campus_space_scheduler.lab_admin_incharge.models.NotificationModel;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class InchargeNotificationHelper {

    private static final String CHANNEL_ID = "incharge_notifications";

    public static void sendAndSaveNotification(Context context, String targetUid, String title, String message, String type, String bookingId) {
        // 1. Save to Firebase for the notification history
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance().getReference("notifications").child(targetUid);
        String notificationId = notificationsRef.push().getKey();
        
        NotificationModel notification = new NotificationModel(
                notificationId,
                title,
                message,
                System.currentTimeMillis(),
                type,
                bookingId
        );
        
        if (notificationId != null) {
            notificationsRef.child(notificationId).setValue(notification);
        }

        // 2. Show local push notification with intent to go to details
        showLocalNotification(context, title, message, type, bookingId);
    }

    public static void showLocalNotification(Context context, String title, String message, String type, String bookingId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Incharge Notifications", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent;
        if ("forwarded_request".equals(type)) {
            intent = new Intent(context, FacultyBookingDetailsActivity.class);
        } else {
            intent = new Intent(context, BookingDetailsActivity.class);
        }
        intent.putExtra("bookingId", bookingId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(bookingId.hashCode(), builder.build());
    }
}
