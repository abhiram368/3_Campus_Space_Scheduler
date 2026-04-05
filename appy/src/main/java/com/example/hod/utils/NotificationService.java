package com.example.hod.utils;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.hod.firebase.FirebaseClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

public class NotificationService extends Service {
    private static final String TAG = "NotificationService";
    private DatabaseReference notificationsRef;
    private ChildEventListener listener;
    private String currentUid;
    private long startTime;

    @Override
    public void onCreate() {
        super.onCreate();
        startTime = System.currentTimeMillis() - 60000; // 60s grace period for clock skew
        try {
            currentUid = FirebaseAuth.getInstance().getUid();
            Log.d(TAG, "Service Created. UID: " + currentUid);
            if (currentUid != null) {
                FirebaseClient client = FirebaseClient.getInstance();
                if (client != null) {
                    notificationsRef = client.notificationsRef().child(currentUid);
                    startListening();
                } else {
                    Log.e(TAG, "FirebaseClient instance is NULL");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in service onCreate: " + e.getMessage());
        }
    }

    private void startListening() {
        if (notificationsRef == null || listener != null) return;

        listener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                try {
                    Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                    String message = snapshot.child("message").getValue(String.class);
                    Boolean read = snapshot.child("read").getValue(Boolean.class);

                    Log.d(TAG, "New notification detected: " + message);

                    // Only show notifications that are new (added after service started) and unread
                    if (timestamp != null && timestamp >= startTime) {
                        if (read == null || !read) {
                            NotificationHelper.showNotification(getApplicationContext(), "Campus Sync Update", message != null ? message : "New notification received");
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing notification", e);
                }
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error", error.toException());
            }
        };

        notificationsRef.addChildEventListener(listener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String newUid = FirebaseAuth.getInstance().getUid();
        
        // If user changed or service restarted without UID, update listener
        if (newUid != null && !newUid.equals(currentUid)) {
            Log.d(TAG, "UID changed or refreshed: " + newUid + ". Restarting listener.");
            if (notificationsRef != null && listener != null) {
                notificationsRef.removeEventListener(listener);
                listener = null;
            }
            currentUid = newUid;
            notificationsRef = FirebaseClient.getInstance().notificationsRef().child(currentUid);
            startListening();
        } else if (currentUid == null && newUid != null) {
            currentUid = newUid;
            notificationsRef = FirebaseClient.getInstance().notificationsRef().child(currentUid);
            startListening();
        }
        
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service Destroyed");
        if (notificationsRef != null && listener != null) {
            notificationsRef.removeEventListener(listener);
        }
    }
}
