package com.example.hod.repository;

import androidx.annotation.NonNull;

import android.util.Log;

import com.example.hod.firebase.FirebaseClient;
import com.example.hod.models.Booking;
import com.example.hod.models.Space;
import com.example.hod.models.User;
import com.example.hod.utils.Result;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseRepository {

    public interface Callback<T> {
        void onComplete(Result<T> result);
    }

    private final DatabaseReference bookingsRef;
    private final DatabaseReference schedulesRef;
    private final DatabaseReference spacesRef;
    private final DatabaseReference usersRef;

    public FirebaseRepository() {
        FirebaseClient client = FirebaseClient.getInstance();
        bookingsRef = client.bookingsRef();
        schedulesRef = client.schedulesRef();
        spacesRef = client.spacesRef();
        usersRef = client.usersRef();
    }

    // region Bookings

    public void getPendingBookings(Callback<List<Booking>> callback) {

        bookingsRef.orderByChild("status").equalTo("pending")
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        List<Booking> bookings = new ArrayList<>();

                        for (DataSnapshot child : snapshot.getChildren()) {

                            Booking booking = child.getValue(Booking.class);

                            if (booking != null) {

                                if (booking.getBookingId() == null) {
                                    booking.setBookingId(child.getKey());
                                }

                                if (booking.getBookedBy() == null) {
                                    booking.setBookedBy("Unknown User");
                                }

                                bookings.add(booking);
                            }
                        }

                        callback.onComplete(new Result.Success<>(bookings));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onComplete(new Result.Error<>(error.toException()));
                    }
                });
    }

    /**
     * Updates approval fields in bookings/{bookingId}.
     *
     * DB schema: flat fields facultyInchargeApproval (bool), hodApproval (bool).
     * There is NO staffApproval field — staff action moves status to forwarded_to_faculty.
     *
     * Status transitions:
     *   staff  + approved  → status = "forwarded_to_faculty"
     *   staff  + rejected  → status = "rejected"
     *   faculty + approved  → status = "forwarded_to_hod", facultyInchargeApproval = true
     *   faculty + rejected  → status = "rejected"
     *   hod    + approved  → status = "approved",  hodApproval = true
     *   hod    + rejected  → status = "rejected"
     */
    public void updateApprovalStatus(String bookingId,
                                     String role,
                                     boolean approved,
                                     String remark,
                                     String decidedByUid,
                                     Callback<Void> callback) {

        String currentTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                .format(new java.util.Date());
        Map<String, Object> updates = new HashMap<>();
        updates.put("decisionTime", currentTime);
        if (decidedByUid != null) updates.put("approvedBy", decidedByUid);

        if (!approved) {
            // Any role rejecting → immediate rejected
            updates.put("status", "rejected");
            updates.put("remark", remark != null ? remark : "");
        } else {
            switch (role.toLowerCase()) {
                case "staff":
                case "staffincharge":
                    // Staff has no approval field in DB – just forward
                    updates.put("status", "forwarded_to_hod");
                    break;
                case "faculty":
                    updates.put("facultyInchargeApproval", true);
                    updates.put("status", "forwarded_to_hod");
                    break;
                case "hod":
                    updates.put("hodApproval", true);
                    updates.put("status", "approved");
                    break;
            }
            updates.put("remark", remark != null ? remark : "");
        }

        bookingsRef.child(bookingId).updateChildren(updates, (error, ref) -> {
            if (error != null) {
                callback.onComplete(new Result.Error<>(error.toException()));
            } else {
                // Sync schedule slot status after booking decision
                String finalStatus = (String) updates.get("status");
                if ("approved".equals(finalStatus) || "rejected".equals(finalStatus)) {
                    syncSlotStatusFromBooking(bookingId, finalStatus);
                }
                callback.onComplete(new Result.Success<>(null));
            }
        });
    }

    /**
     * Reads the booking's scheduleId + timeSlot from the DB and updates the
     * matching slot status in schedules/{scheduleId}/slots/{slotKey}/status.
     */
    private void syncSlotStatusFromBooking(String bookingId, String newStatus) {
        bookingsRef.child(bookingId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                String scheduleId = snapshot.child("scheduleId").getValue(String.class);
                String rawTimeSlot = snapshot.child("timeSlot").getValue(String.class);
                if (scheduleId == null || rawTimeSlot == null) return;
                
                String timeSlot = formatSlotKey(rawTimeSlot);
                String slotStatus = "Available"; // Default
                if ("approved".equalsIgnoreCase(newStatus)) {
                    slotStatus = "BOOKED";
                } else if ("Cancelled".equalsIgnoreCase(newStatus)) {
                    slotStatus = "Available";
                } else {
                    // For rejections or middle states, we generally want it Available
                    slotStatus = "Available";
                }
                final String finalSlotStatus = slotStatus; // Declare as final here
                updateSlotStatus(scheduleId, timeSlot, slotStatus, r -> {
                    Log.d("FirebaseRepo", "Slot sync: " + scheduleId + "/" + timeSlot + " → " + finalSlotStatus);
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseRepo", "syncSlotStatusFromBooking cancelled: " + error.getMessage());
            }
        });
    }

    /**
     * Directly update the status of a schedule slot.
     */
    public void updateSlotStatus(String scheduleId, String slotKey, String status, Callback<Void> callback) {
        String finalKey = formatSlotKey(slotKey);
        schedulesRef.child(scheduleId).child("slots").child(finalKey).child("status")
                .setValue(status)
                .addOnSuccessListener(v -> callback.onComplete(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onComplete(new Result.Error<>(e)));
    }

    public void getBookingForSlot(String spaceId, String date, String timeRange, Callback<Booking> callback) {
        String scheduleId = spaceId + "_" + date;
        bookingsRef.orderByChild("scheduleId").equalTo(scheduleId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Booking b = safeParseBooking(child);
                            // Standardize both for comparison
                            if (b != null && formatSlotKey(timeRange).equalsIgnoreCase(formatSlotKey(b.getTimeSlot()))) {
                                callback.onComplete(new Result.Success<>(b));
                                return;
                            }
                        }
                        callback.onComplete(new Result.Success<>(null));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onComplete(new Result.Error<>(error.toException()));
                    }
                });
    }

    // endregion

    // region Schedules

    /**
     * Loads slot map for the given hall name (space roomName) and date.
     */
    public void getSlotsForHallAndDate(String hallName,
                                       String date,
                                       Callback<Map<String, Boolean>> callback) {
        spacesRef.orderByChild("roomName").equalTo(hallName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            callback.onComplete(new Result.Success<>(new HashMap<>()));
                            return;
                        }

                        for (DataSnapshot spaceSnapshot : snapshot.getChildren()) {
                            String spaceKey = spaceSnapshot.getKey();
                            if (spaceKey == null) {
                                continue;
                            }
                            String scheduleKey = spaceKey + "_" + date;
                            schedulesRef.child(scheduleKey).child("slots")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            Map<String, Boolean> slots = new HashMap<>();
                                            for (DataSnapshot slotSnapshot : snapshot.getChildren()) {
                                                Boolean booked = slotSnapshot.getValue(Boolean.class);
                                                slots.put(slotSnapshot.getKey(),
                                                        booked != null && booked);
                                            }
                                            callback.onComplete(new Result.Success<>(slots));
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            callback.onComplete(new Result.Error<>(error.toException()));
                                        }
                                    });
                            break;
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onComplete(new Result.Error<>(error.toException()));
                    }
                });
    }

    // endregion

    // region Stability Pass Additions

    // region Live Status (Dynamic from Schedules)

    public void getAllSpaces(Callback<List<Space>> callback) {
        spacesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Space> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Space space = child.getValue(Space.class);
                    if (space != null) {
                        if (space.getSpaceId() == null) space.setSpaceId(child.getKey());
                        list.add(space);
                    }
                }
                callback.onComplete(new Result.Success<>(list));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onComplete(new Result.Error<>(error.toException()));
            }
        });
    }

    public com.google.firebase.database.ValueEventListener getLiveSlotStatus(String spaceId, String currentDate, Callback<Map<String, com.example.hod.models.LiveStatusData>> callback) {
        String scheduleId = spaceId + "_" + currentDate;
        Log.d("FirebaseRepo", "Listening for ALL daily slots on: " + scheduleId);

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, com.example.hod.models.LiveStatusData> slotsMap = new HashMap<>();
                if (!snapshot.exists()) {
                    callback.onComplete(new Result.Success<>(slotsMap));
                    return;
                }

                DataSnapshot slotsSnapshot = snapshot.child("slots");
                for (DataSnapshot slotSnap : slotsSnapshot.getChildren()) {
                    String rawKey = slotSnap.getKey();
                    if (rawKey == null) continue;
                    
                    String standardizedKey = formatSlotKey(rawKey);
                    String status = slotSnap.child("status").getValue(String.class);
                    if (status == null) status = "AVAILABLE";

                    // Handle REJECTED as AVAILABLE
                    if ("REJECTED".equalsIgnoreCase(status)) status = "AVAILABLE";

                    final String finalStatus = status;
                    final String finalSlot = standardizedKey;

                    if ("BOOKED".equalsIgnoreCase(status)) {
                        // Fetch booking details
                        getBookingForSlot(spaceId, currentDate, rawKey, bookingResult -> {
                            if (bookingResult instanceof Result.Success) {
                                com.example.hod.models.Booking b = ((Result.Success<com.example.hod.models.Booking>) bookingResult).data;
                                slotsMap.put(finalSlot, new com.example.hod.models.LiveStatusData(finalStatus, b, finalSlot, currentDate));
                            } else {
                                slotsMap.put(finalSlot, new com.example.hod.models.LiveStatusData(finalStatus, null, finalSlot, currentDate));
                            }
                            callback.onComplete(new Result.Success<>(slotsMap));
                        });
                    } else {
                        slotsMap.put(finalSlot, new com.example.hod.models.LiveStatusData(finalStatus, null, finalSlot, currentDate));
                    }
                }
                callback.onComplete(new Result.Success<>(slotsMap));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onComplete(new Result.Error<>(error.toException()));
            }
        };

        schedulesRef.child(scheduleId).addValueEventListener(listener);
        return listener;
    }

    public void removeLiveStatusListener(String spaceId, String currentDate, ValueEventListener listener) {
        String scheduleId = spaceId + "_" + currentDate;
        schedulesRef.child(scheduleId).removeEventListener(listener);
    }

    public void getCurrentSlotStatus(String spaceId, String currentDate, String currentTime, Callback<com.example.hod.models.LiveStatusData> callback) {
        String scheduleId = spaceId + "_" + currentDate;
        schedulesRef.child(scheduleId).child("slots").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    callback.onComplete(new Result.Success<>(null));
                    return;
                }

                for (DataSnapshot slotSnap : snapshot.getChildren()) {
                    String slotKey = slotSnap.getKey();
                    if (slotKey != null && isTimeInSlot(currentTime, slotKey)) {
                        String status = slotSnap.child("status").getValue(String.class);
                        if (status == null) status = "AVAILABLE";
                        status = status.toUpperCase();
                        if ("REJECTED".equals(status)) status = "AVAILABLE";

                        final String finalStatus = status;
                        final String finalSlot = formatSlotKey(slotKey);

                        if ("BOOKED".equals(finalStatus)) {
                            getBookingForSlot(spaceId, currentDate, slotKey, bookingResult -> {
                                com.example.hod.models.Booking b = null;
                                if (bookingResult instanceof Result.Success) {
                                    b = ((Result.Success<com.example.hod.models.Booking>) bookingResult).data;
                                }
                                callback.onComplete(new Result.Success<>(new com.example.hod.models.LiveStatusData(finalStatus, b, finalSlot, currentDate)));
                            });
                        } else {
                            callback.onComplete(new Result.Success<>(new com.example.hod.models.LiveStatusData(finalStatus, null, finalSlot, currentDate)));
                        }
                        return;
                    }
                }
                callback.onComplete(new Result.Success<>(null)); // No matching slot (Off Hours)
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onComplete(new Result.Error<>(error.toException()));
            }
        });
    }

    private boolean isTimeInSlot(String currentTime, String slotRange) {
        try {
            // range format: "HH:mm - HH:mm" or "HH:mm-HH:mm"
            String[] parts = slotRange.split("-");
            if (parts.length != 2) return false;

            String startStr = parts[0].trim();
            String endStr = parts[1].trim();

            int current = timeToMinutes(currentTime);
            int start = timeToMinutes(startStr);
            int end = timeToMinutes(endStr);

            // Handle midnight wrap if necessary, but lab hours are usually within same day
            if (end == 0) end = 1440; // 00:00 is end of day

            return current >= start && current < end;
        } catch (Exception e) {
            return false;
        }
    }

    public String formatSlotKey(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        try {
            String[] parts = raw.split("-");
            if (parts.length != 2) return raw;

            String start = parts[0].replaceAll("[^0-9:]", "").trim();
            String end = parts[1].replaceAll("[^0-9:]", "").trim();

            // Ensure HH:mm format (add leading zero if necessary)
            if (start.length() == 4 && start.contains(":")) start = "0" + start;
            if (end.length() == 4 && end.contains(":")) end = "0" + end;

            return start + " - " + end;
        } catch (Exception e) {
            return raw;
        }
    }

    private int timeToMinutes(String time) {
        if (time == null || time.isEmpty()) return -1;
        
        try {
            String upper = time.toUpperCase().trim();
            boolean isPM = upper.contains("PM");
            boolean isAM = upper.contains("AM");

            String cleanTime = upper.replaceAll("[^0-9:]", "").trim();
            String[] parts = cleanTime.split(":");
            if (parts.length < 2) return -1;

            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);

            if (isPM && hours < 12) hours += 12;
            if (isAM && hours == 12) hours = 0;

            return hours * 60 + minutes;
        } catch (Exception e) {
            return -1;
        }
    }

    @Deprecated
    public void getDynamicLiveStatus(String spaceId, String currentDate, String timeSlot, Callback<String> callback) {
        // Use getLiveSlotStatus instead for real-time and range support
    }

    public void getSpaceDetails(String spaceId, Callback<Space> callback) {
        Log.d("FirebaseRepo", "Fetching Space Details for spaceId: " + spaceId);
        spacesRef.child(spaceId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d("FirebaseRepo", "Space details fetched. Exists: " + snapshot.exists());
                        if (snapshot.exists()) {
                            callback.onComplete(new Result.Success<>(snapshot.getValue(Space.class)));
                        } else {
                            callback.onComplete(new Result.Success<>(null));
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FirebaseRepo", "Failed to fetch Space Details: " + error.getMessage());
                        callback.onComplete(new Result.Error<>(error.toException()));
                    }
                });
    }

    public void updateSpaceDetails(String spaceId, Space space, Callback<Void> callback) {
        Log.d("FirebaseRepo", "Updating Space Details for spaceId: " + spaceId);
        spacesRef.child(spaceId)
                .setValue(space).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FirebaseRepo", "Space details updated successfully.");
                        callback.onComplete(new Result.Success<>(null));
                    } else {
                        Log.e("FirebaseRepo", "Failed to update Space Details", task.getException());
                        callback.onComplete(new Result.Error<>(task.getException()));
                    }
                });
    }

    public void getLabAdmin(String labId, Callback<String> callback) {
        Log.d("FirebaseRepo", "Fetching Admin from approvalHierarchy for labId: " + labId);
        // Note: From the schema, it looks like approvalHierarchy/labs/<labId> or /halls/<spaceId>
        // Depending on the role of the space. Let's assume it stores the admin UID or Name directly.
        FirebaseDatabase.getInstance().getReference("approvalHierarchy").child("labs").child(labId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d("FirebaseRepo", "Lab admin hierarchy fetched. Exists: " + snapshot.exists());
                        if (snapshot.exists()) {
                            // Assuming the node value is the admin's UID or Name
                            String adminIdentifier = snapshot.getValue(String.class);
                            callback.onComplete(new Result.Success<>(adminIdentifier));
                        } else {
                            callback.onComplete(new Result.Success<>("Admin Not Assigned"));
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FirebaseRepo", "Failed to fetch Admin: " + error.getMessage());
                        callback.onComplete(new Result.Error<>(error.toException()));
                    }
                });
    }

    public ValueEventListener observeSchedulesForLab(String labId, String date, Callback<DataSnapshot> callback) {
        String scheduleKey = labId + "_" + date;
        Log.d("FirebaseRepo", "Observing Schedules for: " + scheduleKey);

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                callback.onComplete(new Result.Success<>(snapshot.child("slots")));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onComplete(new Result.Error<>(error.toException()));
            }
        };

        schedulesRef.child(scheduleKey).addValueEventListener(listener);
        return listener;
    }

    public void removeScheduleListener(String labId, String date, ValueEventListener listener) {
        String scheduleKey = labId + "_" + date;
        schedulesRef.child(scheduleKey).removeEventListener(listener);
    }

    public void getSchedulesForLab(String labId, String date, Callback<DataSnapshot> callback) {
        String scheduleKey = labId + "_" + date;
        Log.d("FirebaseRepo", "One-shot fetch for: " + scheduleKey);
        schedulesRef.child(scheduleKey).child("slots")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        callback.onComplete(new Result.Success<>(snapshot));
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onComplete(new Result.Error<>(error.toException()));
                    }
                });
    }

    /**
     * Disable legacy 1-hour slot initialization. 
     * The app now accepts strictly pre-generated 30-minute slots.
     */
    public void initializeBookingSlots(String spaceId, String date, Callback<Void> callback) {
        // Just return success to satisfy internal UI loaders, without writing bad 1-hour structures to Firebase.
        callback.onComplete(new Result.Success<>(null));
    }

    public void blockSlot(String spaceId, String date, String slotKey, String reason, Callback<Void> callback) {
        String scheduleId = spaceId + "_" + date;
        String finalKey = formatSlotKey(slotKey);
        DatabaseReference slotRef = schedulesRef.child(scheduleId).child("slots").child(finalKey);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "BLOCKED");
        updates.put("blockReason", reason);

        // 1. Check if it was booked
        slotRef.child("status").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String oldStatus = snapshot.getValue(String.class);
                
                // 2. Perform Block
                slotRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                    if ("BOOKED".equalsIgnoreCase(oldStatus)) {
                        // 3. Notify user if booked
                        getBookingForSlot(spaceId, date, slotKey, bookingResult -> {
                            if (bookingResult instanceof Result.Success) {
                                Booking b = ((Result.Success<Booking>) bookingResult).data;
                                if (b != null && b.getBookedBy() != null) {
                                    String msg = "Your booking for " + spaceId + " on " + date + " (" + slotKey + ") has been cancelled because the slot was blocked: " + reason;
                                    sendNotification(b.getBookedBy(), msg, r -> {});
                                    
                                    // Also mark booking as cancelled
                                    bookingsRef.child(b.getBookingId()).child("status").setValue("Cancelled");
                                }
                            }
                        });
                    }
                    callback.onComplete(new Result.Success<>(null));
                }).addOnFailureListener(e -> callback.onComplete(new Result.Error<>(e)));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onComplete(new Result.Error<>(error.toException()));
            }
        });
    }

    public void unblockSlot(String spaceId, String date, String slotKey, Callback<Void> callback) {
        String scheduleId = spaceId + "_" + date;
        String finalKey = formatSlotKey(slotKey);
        DatabaseReference slotRef = schedulesRef.child(scheduleId).child("slots").child(finalKey);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "AVAILABLE");
        updates.put("blockReason", null);

        slotRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> callback.onComplete(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onComplete(new Result.Error<>(e)));
    }

    public void sendNotification(String userId, String message, Callback<Void> callback) {
        DatabaseReference notifRef = usersRef.child(userId).child("notifications").push();
        Map<String, Object> data = new HashMap<>();
        data.put("message", message);
        data.put("timestamp", System.currentTimeMillis());
        data.put("read", false);
        
        notifRef.setValue(data)
                .addOnSuccessListener(aVoid -> callback.onComplete(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onComplete(new Result.Error<>(e)));
    }

    public void getSpaceIdFromSchedule(String scheduleId, Callback<String> callback) {
        schedulesRef.child(scheduleId).child("spaceId")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            callback.onComplete(new Result.Success<>(snapshot.getValue(String.class)));
                        } else {
                            callback.onComplete(new Result.Success<>(null));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onComplete(new Result.Error<>(error.toException()));
                    }
                });
    }

    public void observeApprovals(String labId, Callback<List<Booking>> callback) {
         Log.d("FirebaseRepo", "Observing Approvals for labId: " + labId);
         bookingsRef.addValueEventListener(new ValueEventListener() {
             @Override
             public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                 Log.d("FirebaseRepo", "Approvals fetched. Count: " + dataSnapshot.getChildrenCount());
                 List<Booking> bookingList = new ArrayList<>();
                 for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                     Booking booking = safeParseBooking(snapshot);
                     if (booking != null && booking.getStatus() != null) {
                         // Filter by Space ID
                         String bookingSpaceId = snapshot.child("spaceId").getValue(String.class);
                         // Some old records may have 'labId' or use 'scheduleId' to parse space ID.
                         // Modern standard in app is 'spaceId'
                         if (bookingSpaceId == null && booking.getScheduleId() != null) {
                             bookingSpaceId = booking.getScheduleId().split("_")[0];
                         }
                         
                         if (labId != null && !labId.isEmpty() && !labId.equals(bookingSpaceId)) {
                             continue; // Skip if it doesn't match the Staff's managed space
                         }

                         String status = booking.getStatus().toLowerCase();
                         if (status.equals("approved") || status.contains("rejected") || status.contains("forwarded") || status.equals("cancelled")) {
                             bookingList.add(booking);
                         }
                         }
                     }
                 callback.onComplete(new Result.Success<>(bookingList));
             }
             @Override
             public void onCancelled(@NonNull DatabaseError databaseError) {
                 Log.e("FirebaseRepo", "Failed to fetch approvals: " + databaseError.getMessage());
                 callback.onComplete(new Result.Error<>(databaseError.toException()));
             }
         });
    }

    /**
     * Fetches all bookings that the HOD has already actioned
     * (status == "approved" or "rejected") regardless of space.
     */
    public void observeHodHistory(Callback<List<Booking>> callback) {
        Log.d("FirebaseRepo", "observeHodHistory: fetching approved/rejected bookings for HOD");
        bookingsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Booking> result = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Booking booking = safeParseBooking(snapshot);
                    if (booking != null && booking.getStatus() != null) {
                        String status = booking.getStatus().toLowerCase();
                        if (status.equals("approved") || status.contains("rejected") || status.equals("cancelled") || status.contains("forwarded")) {
                            String currentUid = FirebaseAuth.getInstance().getUid();
                            if (currentUid != null && currentUid.equals(booking.getApprovedBy())) {
                                result.add(booking);
                            }
                        }
                    }
                }
                Log.d("FirebaseRepo", "observeHodHistory: found " + result.size() + " records");
                callback.onComplete(new Result.Success<>(result));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseRepo", "observeHodHistory cancelled: " + error.getMessage());
                callback.onComplete(new Result.Error<>(error.toException()));
            }
        });
    }

    public com.google.firebase.database.ValueEventListener observePendingRequests(String labId, Callback<List<Booking>> callback) {
        Log.d("FirebaseRepo", "Fetching Pending Requests for labId: " + labId);

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("FirebaseRepo", "All bookings fetched. Count: " + dataSnapshot.getChildrenCount());

                // Step 1: Collect all bookings with status == pending
                List<Booking> pendingBookings = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Booking booking = safeParseBooking(snapshot);
                    if (booking != null
                            && booking.getStatus() != null
                            && booking.getStatus().equalsIgnoreCase("pending")) {
                        pendingBookings.add(booking);
                    }
                }

                Log.d("FirebaseRepo", "Pending status bookings: " + pendingBookings.size());

                // If no labId filter, return all pending bookings directly
                if (labId == null || labId.isEmpty()) {
                    callback.onComplete(new Result.Success<>(pendingBookings));
                    return;
                }

                // If no pending bookings, short-circuit
                if (pendingBookings.isEmpty()) {
                    callback.onComplete(new Result.Success<>(new ArrayList<>()));
                    return;
                }

                // Step 2: For each pending booking, look up schedules/{scheduleId}/spaceId
                //         and include only if spaceId == labId
                List<Booking> matched = new ArrayList<>();
                java.util.concurrent.atomic.AtomicInteger remaining =
                        new java.util.concurrent.atomic.AtomicInteger(pendingBookings.size());

                for (Booking booking : pendingBookings) {
                    String scheduleId = booking.getScheduleId();

                    if (scheduleId == null || scheduleId.isEmpty()) {
                        // No scheduleId → cannot match; decrement and maybe fire callback
                        if (remaining.decrementAndGet() == 0) {
                            Log.d("FirebaseRepo", "Matched pending for labId=" + labId + ": " + matched.size());
                            callback.onComplete(new Result.Success<>(matched));
                        }
                        continue;
                    }

                    schedulesRef.child(scheduleId).child("spaceId")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot spaceSnap) {
                                    String spaceId = spaceSnap.getValue(String.class);
                                    Log.d("FirebaseRepo", "scheduleId=" + scheduleId
                                            + " spaceId=" + spaceId + " labId=" + labId);
                                    if (labId.equals(spaceId)) {
                                        synchronized (matched) {
                                            matched.add(booking);
                                        }
                                    }
                                    if (remaining.decrementAndGet() == 0) {
                                        Log.d("FirebaseRepo", "Matched pending for labId=" + labId
                                                + ": " + matched.size());
                                        callback.onComplete(new Result.Success<>(matched));
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e("FirebaseRepo", "Schedule lookup failed: " + error.getMessage());
                                    if (remaining.decrementAndGet() == 0) {
                                        callback.onComplete(new Result.Success<>(matched));
                                    }
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseRepo", "Failed to fetch bookings: " + databaseError.getMessage());
                callback.onComplete(new Result.Error<>(databaseError.toException()));
            }
        };
        bookingsRef.addValueEventListener(listener);
        return listener;
    }

    public void removePendingRequestsListener(com.google.firebase.database.ValueEventListener listener) {
        if (listener != null) {
            bookingsRef.removeEventListener(listener);
        }
    }

    // endregion

    public void getUserDetails(String uid, Callback<User> callback) {
        usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User u = snapshot.getValue(User.class);
                    if (u != null) u.uid = snapshot.getKey();
                    callback.onComplete(new Result.Success<>(u));
                } else {
                    callback.onComplete(new Result.Success<>(null));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onComplete(new Result.Error<>(error.toException()));
            }
        });
    }

    public void getLabAdmins(String spaceId, Callback<List<User>> callback) {
        FirebaseDatabase.getInstance().getReference("labAdminsDetails")
                .orderByChild("spaceId").equalTo(spaceId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<User> list = new ArrayList<>();
                        java.util.Set<String> uids = new java.util.HashSet<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            com.example.hod.models.LabAdminDetail det = child.getValue(com.example.hod.models.LabAdminDetail.class);
                            if (det != null && !uids.contains(det.uid)) {
                                uids.add(det.uid);
                                User u = new User();
                                u.uid = det.uid;
                                u.name = det.name;
                                u.emailId = det.emailId;
                                u.phoneNumber = det.phoneNumber;
                                u.rollNo = det.rollNo;
                                u.role = "labAdmin";
                                u.inchargeToSpace = det.spaceId;
                                list.add(u);
                            }
                        }
                        callback.onComplete(new Result.Success<>(list));
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onComplete(new Result.Error<>(error.toException()));
                    }
                });
    }

    public void searchUsers(String query, Callback<List<User>> callback) {
        Log.d("FirebaseRepo", "searchUsers (Real-time): query=" + query);
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("FirebaseRepo", "onDataChange: snapshot count=" + snapshot.getChildrenCount());
                List<User> list = new ArrayList<>();
                String searchQuery = (query != null) ? query.toLowerCase().trim() : "";

                if (searchQuery.isEmpty()) {
                    callback.onComplete(new Result.Success<>(list));
                    return;
                }

                try {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Object val = child.getValue();
                        if (!(val instanceof Map)) continue;

                        Map<String, Object> map = (Map<String, Object>) val;
                        
                        // Strict Student Role Filtering
                        String role = map.containsKey("role") ? String.valueOf(map.get("role")) : "";
                        if (!"student".equalsIgnoreCase(role)) continue;

                        User u = new User();
                        u.uid = child.getKey();
                        u.name = map.containsKey("name") ? String.valueOf(map.get("name")) : "";
                        u.rollNo = map.containsKey("rollNo") ? String.valueOf(map.get("rollNo")) : "";
                        u.role = role;
                        u.emailId = map.containsKey("emailId") ? String.valueOf(map.get("emailId")) : "";
                        u.phoneNumber = map.containsKey("phoneNumber") ? String.valueOf(map.get("phoneNumber")) : "";

                        // Match against Name OR RollNo
                        boolean nameMatch = u.name.toLowerCase().contains(searchQuery);
                        boolean rollMatch = u.rollNo.toLowerCase().contains(searchQuery);

                        if (nameMatch || rollMatch) {
                            list.add(u);
                        }
                    }

                    // Intelligent Sorting: Prefix first, then alphabetical
                    java.util.Collections.sort(list, (u1, u2) -> {
                        String name1 = u1.name.toLowerCase();
                        String name2 = u2.name.toLowerCase();
                        String roll1 = u1.rollNo.toLowerCase();
                        String roll2 = u2.rollNo.toLowerCase();

                        boolean starts1 = name1.startsWith(searchQuery) || roll1.startsWith(searchQuery);
                        boolean starts2 = name2.startsWith(searchQuery) || roll2.startsWith(searchQuery);

                        if (starts1 && !starts2) return -1;
                        if (!starts1 && starts2) return 1;

                        return u1.name.compareToIgnoreCase(u2.name);
                    });

                    Log.d("FirebaseRepo", "Search complete. Found " + list.size() + " matches.");
                    callback.onComplete(new Result.Success<>(list));
                } catch (Exception e) {
                    Log.e("FirebaseRepo", "Critical error during user search loop", e);
                    callback.onComplete(new Result.Error<>(e));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseRepo", "Search cancelled: " + error.getMessage());
                callback.onComplete(new Result.Error<>(error.toException()));
            }
        });
    }

    public void updateToLabAdmin(String uid, String spaceId, Callback<Void> callback) {
        if (uid == null) {
            callback.onComplete(new Result.Error<>(new Exception("UID is null")));
            return;
        }

        // 1. Resolve room name first for the details node
        getSpaceDetails(spaceId, spaceResult -> {
            if (spaceResult instanceof Result.Success) {
                Space space = ((Result.Success<Space>) spaceResult).data;
                String roomName = (space != null) ? space.getRoomName() : "Unknown";

                // 2. Fetch User Info for details node
                getUserDetails(uid, userResult -> {
                    if (userResult instanceof Result.Success) {
                        User user = ((Result.Success<User>) userResult).data;
                        if (user != null) {
                             // 3. Prepare atomic updates
                             String labAdminId = FirebaseDatabase.getInstance().getReference("labAdminsDetails").push().getKey();

                             Map<String, Object> updates = new HashMap<>();
                             updates.put("/users/" + uid + "/role", "labAdmin");
                             updates.put("/users/" + uid + "/inchargeToSpace", spaceId);
                             
                             // Convert data to Map for updateChildren
                             Map<String, Object> detailsMap = new HashMap<>();
                             detailsMap.put("labAdminId", labAdminId);
                             detailsMap.put("uid", uid);
                             detailsMap.put("name", user.name);
                             detailsMap.put("labName", roomName);
                             detailsMap.put("spaceId", spaceId);
                             detailsMap.put("phoneNumber", user.phoneNumber != null ? user.phoneNumber : "N/A");
                             detailsMap.put("emailId", user.emailId);
                             detailsMap.put("rollNo", user.rollNo);
                             
                             updates.put("/labAdminsDetails/" + labAdminId, detailsMap);

                             Log.d("FirebaseRepo", "Performing multi-node update for admin promotion: " + uid);
                             
                             FirebaseDatabase.getInstance().getReference().updateChildren(updates)
                                     .addOnSuccessListener(aVoid -> {
                                         Log.d("FirebaseRepo", "Admin promotion successful. Triggering schedule regeneration.");
                                         generateWeeklySchedule(roomName, spaceId, callback);
                                     })
                                     .addOnFailureListener(e -> {
                                         Log.e("FirebaseRepo", "Admin promotion failed: " + e.getMessage());
                                         callback.onComplete(new Result.Error<>(e));
                                     });
                        } else {
                            callback.onComplete(new Result.Error<>(new Exception("User not found")));
                        }
                    } else {
                        callback.onComplete(new Result.Error<>(((Result.Error<?>) userResult).exception));
                    }
                });
            } else {
                callback.onComplete(new Result.Error<>(((Result.Error<?>) spaceResult).exception));
            }
        });
    }

    public void assignLabAdminToSlot(String spaceId, String date, String slotKey, String adminId, Callback<Void> callback) {
        String scheduleId = spaceId + "_" + date;
        schedulesRef.child(scheduleId).child("slots").child(slotKey).child("labAdminId").setValue(adminId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) callback.onComplete(new Result.Success<>(null));
                    else callback.onComplete(new Result.Error<>(task.getException()));
                });
    }

    public void generateWeeklySchedule(String labName, String spaceId, Callback<Void> callback) {
        FirebaseDatabase.getInstance().getReference("labAdminsDetails")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<com.example.hod.models.LabAdminDetail> admins = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            com.example.hod.models.LabAdminDetail admin = child.getValue(com.example.hod.models.LabAdminDetail.class);
                            if (admin != null) {
                                boolean isMatch = spaceId.equals(admin.spaceId);
                                if (!isMatch && admin.spaceId == null && labName.equalsIgnoreCase(admin.labName)) {
                                    child.getRef().child("spaceId").setValue(spaceId);
                                    isMatch = true;
                                }
                                if (isMatch) admins.add(admin);
                            }
                        }

                        if (admins.isEmpty()) {
                            Log.d("FirebaseRepo", "generateWeeklySchedule Result: No admins found for " + labName);
                            // Clear schedule node
                            FirebaseDatabase.getInstance().getReference("labAdminWeeklySchedule").child(labName).removeValue()
                                    .addOnCompleteListener(t -> callback.onComplete(new Result.Error<>(new Exception("No admins assigned to this lab. Cannot generate schedule."))));
                            return;
                        }

                        Log.d("FirebaseRepo", "generateWeeklySchedule Result: found " + admins.size() + " matches. Generating...");

                        // 2. Sort stable by UID
                        Collections.sort(admins, (a1, a2) -> a1.uid.compareTo(a2.uid));

                        // 3. Prepare generation
                        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
                        Map<String, Object> weeklyUpdates = new HashMap<>();

                        for (int i = 0; i < days.length; i++) {
                            String day = days[i];
                            boolean isWeekend = (day.equals("Saturday") || day.equals("Sunday"));
                            
                            // Rotate starting admin Day-by-Day (Monday starts from Admin 0, Tuesday from Admin 1...)
                            int adminStartIndex = i % admins.size();
                            
                            if (!isWeekend) {
                                // Weekday Blocks: 5-7 (2h), 7-9 (2h), 9-12 (3h)
                                String[][] weekdayBlocks = {
                                    {"17:00-18:00", "18:00-19:00"},
                                    {"19:00-20:00", "20:00-21:00"},
                                    {"21:00-22:00", "22:00-23:00", "23:00-00:00"}
                                };
                                assignBlocksAdvanced(weeklyUpdates, labName, day, weekdayBlocks, admins, adminStartIndex);
                            } else {
                                // Weekend Blocks: 8-12 (4h), 12-4 (4h), 4-8 (4h), 8-12 (4h)
                                String[][] weekendBlocks = {
                                    {"08:00-09:00", "09:00-10:00", "10:00-11:00", "11:00-12:00"},
                                    {"12:00-13:00", "13:00-14:00", "14:00-15:00", "15:00-16:00"},
                                    {"16:00-17:00", "17:00-18:00", "18:00-19:00", "19:00-20:00"},
                                    {"20:00-21:00", "21:00-22:00", "22:00-23:00", "23:00-00:00"}
                                };
                                assignBlocksAdvanced(weeklyUpdates, labName, day, weekendBlocks, admins, adminStartIndex);
                            }
                        }

                        // 4. Atomic Write (Delete old, then update)
                        DatabaseReference weeklyRef = FirebaseDatabase.getInstance().getReference("labAdminWeeklySchedule").child(labName);
                        weeklyRef.removeValue().addOnCompleteListener(task -> {
                            FirebaseDatabase.getInstance().getReference().updateChildren(weeklyUpdates)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("FirebaseRepo", "Weekly schedule auto-generated and saved for " + labName);
                                        callback.onComplete(new Result.Success<>(null));
                                    })
                                    .addOnFailureListener(e -> callback.onComplete(new Result.Error<>(e)));
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onComplete(new Result.Error<>(error.toException()));
                    }
                });
    }

    private void assignBlocksAdvanced(Map<String, Object> updates, String labName, String day, String[][] blocks, 
                                      List<com.example.hod.models.LabAdminDetail> admins, int startIndex) {
        for (int b = 0; b < blocks.length; b++) {
            com.example.hod.models.LabAdminDetail assigned = admins.get((startIndex + b) % admins.size());
            for (String slot : blocks[b]) {
                Map<String, Object> slotData = new HashMap<>();
                slotData.put("slotTime", slot);
                slotData.put("labAdminId", assigned.labAdminId);
                slotData.put("uid", assigned.uid);
                slotData.put("name", assigned.name);
                slotData.put("rollNo", assigned.rollNo);
                
                updates.put("/labAdminWeeklySchedule/" + labName + "/" + day + "/" + slot, slotData);
            }
        }
    }

    public void getLabAdminWeeklySchedule(String labName, String day, Callback<List<Map<String, String>>> callback) {
        FirebaseDatabase.getInstance().getReference("labAdminWeeklySchedule")
                .child(labName).child(day)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d("LabAdminSchedule", "onDataChange: snapshot exists=" + snapshot.exists());
                        List<Map<String, String>> slots = new ArrayList<>();
                        if (!snapshot.exists()) {
                            callback.onComplete(new Result.Success<>(slots));
                            return;
                        }

                        for (DataSnapshot child : snapshot.getChildren()) {
                            Map<String, Object> data = (Map<String, Object>) child.getValue();
                            if (data != null) {
                                Map<String, String> map = new HashMap<>();
                                map.put("slot", String.valueOf(data.get("slotTime")));
                                map.put("name", String.valueOf(data.get("name")));
                                map.put("rollNo", String.valueOf(data.get("rollNo")));
                                map.put("uid", String.valueOf(data.get("uid")));
                                slots.add(map);
                            }
                        }
                        
                        Collections.sort(slots, (s1, s2) -> s1.get("slot").compareTo(s2.get("slot")));
                        callback.onComplete(new Result.Success<>(slots));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onComplete(new Result.Error<>(error.toException()));
                    }
                });
    }

    public void removeFromLabAdmin(String uid, String labName, String spaceId, Callback<Void> callback) {
        if (uid == null || labName == null || spaceId == null) {
            callback.onComplete(new Result.Error<>(new Exception("Missing required fields for removal")));
            return;
        }

        // 1. Find the admin detail record by uid and spaceId
        FirebaseDatabase.getInstance().getReference("labAdminsDetails")
                .orderByChild("uid").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String labAdminIdToDelete = null;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            com.example.hod.models.LabAdminDetail det = child.getValue(com.example.hod.models.LabAdminDetail.class);
                            if (det != null && spaceId.equals(det.spaceId)) {
                                labAdminIdToDelete = child.getKey();
                                break;
                            }
                        }

                        // 2. Perform atomic updates
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("/users/" + uid + "/role", "student");
                        updates.put("/users/" + uid + "/inchargeToSpace", null);
                        if (labAdminIdToDelete != null) {
                            updates.put("/labAdminsDetails/" + labAdminIdToDelete, null);
                        }

                        Log.d("FirebaseRepo", "Performing atomic removal of lab admin: " + uid);
                        FirebaseDatabase.getInstance().getReference().updateChildren(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("FirebaseRepo", "Removal successful. Regenerating schedule.");
                                    generateWeeklySchedule(labName, spaceId, callback);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("FirebaseRepo", "Removal failed: " + e.getMessage());
                                    callback.onComplete(new Result.Error<>(e));
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onComplete(new Result.Error<>(error.toException()));
                    }
                });
    }

    public void observeEscalatedRequests(Callback<List<Booking>> callback) {
        Log.d("FirebaseRepo", "Observing Escalated Requests (forwarded_to_hod)");

        bookingsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Booking> escalated = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Booking booking = safeParseBooking(snapshot);
                    if (booking != null
                            && booking.getStatus() != null
                            && booking.getStatus().equalsIgnoreCase("forwarded_to_faculty_incharge")) {
                        escalated.add(booking);
                    }
                }
                Log.d("FirebaseRepo", "Escalated bookings found: " + escalated.size());
                callback.onComplete(new Result.Success<>(escalated));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseRepo", "Failed to fetch escalated bookings: " + databaseError.getMessage());
                callback.onComplete(new Result.Error<>(databaseError.toException()));
            }
        });
    }

    private Booking safeParseBooking(DataSnapshot snapshot) {
        try {
            Booking booking = new Booking();
            booking.setBookingId(safeStr(snapshot, "bookingId"));
            if (booking.getBookingId() == null) booking.setBookingId(snapshot.getKey());
            booking.setBookedBy(safeStr(snapshot, "bookedBy"));
            booking.setDate(safeStr(snapshot, "date"));
            booking.setDescription(safeStr(snapshot, "description"));
            booking.setPurpose(safeStr(snapshot, "purpose"));
            booking.setSlotStart(safeStr(snapshot, "slotStart"));
            booking.setSpaceName(safeStr(snapshot, "spaceName"));
            booking.setStatus(safeStr(snapshot, "status"));
            booking.setTimeSlot(safeStr(snapshot, "timeSlot"));
            booking.setRemark(safeStr(snapshot, "remark"));
            booking.setApprovedBy(safeStr(snapshot, "approvedBy"));
            booking.setScheduleId(safeStr(snapshot, "scheduleId"));
            booking.setLorUpload(safeStr(snapshot, "lorUpload"));
            booking.setRequesterName(safeStr(snapshot, "requesterName"));
            booking.setDecisionTime(safeStr(snapshot, "decisionTime"));
            
            DataSnapshot bookedTimeSnap = snapshot.child("bookedTime");
            if (bookedTimeSnap.exists()) {
                java.util.Map<String, String> btMap = new java.util.HashMap<>();
                for (DataSnapshot child : bookedTimeSnap.getChildren()) {
                    btMap.put(child.getKey(), child.getValue(String.class));
                }
                booking.setBookedTime(btMap);
            }

            // Boolean approval fields (Firebase stores as boolean, not nested)
            DataSnapshot facSnap = snapshot.child("facultyInchargeApproval");
            if (facSnap.exists()) {
                Boolean b = facSnap.getValue(Boolean.class);
                booking.setFacultyInchargeApproval(b != null && b);
            }
            DataSnapshot hodSnap = snapshot.child("hodApproval");
            if (hodSnap.exists()) {
                Boolean b = hodSnap.getValue(Boolean.class);
                booking.setHodApproval(b != null && b);
            }

            return booking;
        } catch (Exception e) {
            Log.e("FirebaseRepo", "safeParseBooking failed for " + snapshot.getKey() + ": " + e.getMessage());
            return null;
        }
    }

    private String safeStr(DataSnapshot parent, String key) {
        DataSnapshot child = parent.child(key);
        if (!child.exists()) return null;
        Object val = child.getValue();
        if (val == null) return null;
        if (val instanceof String) return (String) val;
        return val.toString();
    }
}