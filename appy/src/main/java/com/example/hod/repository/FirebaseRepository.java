package com.example.hod.repository;

import androidx.annotation.NonNull;

import android.util.Log;

import com.example.hod.firebase.FirebaseClient;
import com.example.hod.models.Booking;
import com.example.hod.models.LiveStatusData;
import com.example.hod.models.Space;
import com.example.hod.models.User;
import com.example.hod.utils.Result;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FirebaseRepository {

    public interface Callback<T> {
        void onComplete(@NonNull Result<T> result);
    }

    private static final String TAG = "FirebaseRepository";
    private static final String DATE_FORMAT_FULL = "yyyy-MM-dd HH:mm";
    private static final String DATE_FORMAT_ISO = "yyyy-MM-dd";
    private static final String DAY_FORMAT = "EEEE";
    private static final int MINUTES_IN_DAY = 1440;

    private final DatabaseReference bookingsRef;
    private final DatabaseReference schedulesRef;
    private final DatabaseReference spacesRef;
    private final DatabaseReference notificationsRef;
    private final DatabaseReference usersRef;
    private final DatabaseReference labAdminsDetailsRef;

    public FirebaseRepository() {
        FirebaseClient client = FirebaseClient.getInstance();
        bookingsRef = client.bookingsRef();
        schedulesRef = client.schedulesRef();
        spacesRef = client.spacesRef();
        usersRef = client.usersRef();
        notificationsRef = client.notificationsRef();
        labAdminsDetailsRef = client.labAdminsDetailsRef();
    }

    public DatabaseReference usersRef() {
        return usersRef;
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

    public void createBooking(Booking b, Callback<String> callback) {
        String bookingId = bookingsRef.push().getKey();
        if (bookingId == null) {
            callback.onComplete(new Result.Error<>(new Exception("Could not generate booking ID")));
            return;
        }
        b.setBookingId(bookingId);
        
        bookingsRef.child(bookingId).setValue(b).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Notify Staff Incharge
                String spaceIdForNotification = b.getScheduleId() != null ? b.getScheduleId().split("_")[0] : null;
                if (spaceIdForNotification != null) {
                    notifyStaffInchargeForSpace(spaceIdForNotification, b.getSpaceName(), 
                        "New booking request received for " + b.getSpaceName(), 
                        bookingId, b.getBookedBy(), "booking", "staff");
                }
                callback.onComplete(new Result.Success<>(bookingId));
            } else {
                callback.onComplete(new Result.Error<>(task.getException()));
            }
        });
    }

    public void updateApprovalStatus(String bookingId,
                                     String role,
                                     boolean approved,
                                     String remark,
                                     String decidedByUid,
                                     Callback<Void> callback) {
        updateApprovalStatus(bookingId, role, approved, remark, decidedByUid, null, callback);
    }

    public void updateApprovalStatus(String bookingId,
                                     String role,
                                     boolean approved,
                                     String remark,
                                     String decidedByUid,
                                     String statusValue,
                                     Callback<Void> callback) {

        String currentTime = new SimpleDateFormat(DATE_FORMAT_FULL, Locale.getDefault())
                .format(new java.util.Date());
        Map<String, Object> updates = new HashMap<>();
        updates.put("decisionTime", currentTime);
        if (decidedByUid != null) updates.put("approvedBy", decidedByUid);

        if (statusValue != null) {
            updates.put("status", statusValue);
            updates.put("remark", remark != null ? remark : "");
        } else if (!approved) {
            updates.put("status", "rejected");
            updates.put("remark", remark != null ? remark : "");
        } else {
            switch (role.toLowerCase()) {
                case "staff":
                case "staffincharge":
                case "labadmin":
                    updates.put("status", "forwarded_to_facultyIncharge");
                    break;
                case "faculty":
                case "facultyincharge":
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

        String finalStatus = (String) updates.get("status");

        bookingsRef.child(bookingId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Booking b = safeParseBooking(snapshot);
                if (b == null) {
                    callback.onComplete(new Result.Error<>(new Exception("Booking not found")));
                    return;
                }

                bookingsRef.child(bookingId).updateChildren(updates, (error, ref) -> {
                    if (error != null) {
                        callback.onComplete(new Result.Error<>(error.toException()));
                    } else {
                        // SPEC-COMPLIANT NOTIFICATIONS
                        String msgForStudent = "";
                        String msgForNext = "";
                        String type = "notification"; // Default type
                        if ("expired".equalsIgnoreCase(finalStatus)) {
                            String roleLabel = capitalize(role);
                            if (roleLabel.toLowerCase().contains("staff")) roleLabel = "Staff Incharge";
                            else if (roleLabel.toLowerCase().contains("faculty")) roleLabel = "Faculty Incharge";
                            else if (roleLabel.toLowerCase().contains("hod")) roleLabel = "HOD";
                            
                            msgForStudent = "Your booking for " + b.getSpaceName() + " expired. Respected authority (" + roleLabel + ") gave reason: " + (remark != null ? remark : "None provided");
                            type = "rejection"; // Treat as rejection for icon/color purposes
                        } else if (!approved) {
                            msgForStudent = "Your booking for " + b.getSpaceName() + " was rejected by " + capitalize(role);
                            type = "rejection";
                        } else {
                            // Fix: Use finalStatus to decide message instead of just role
                            if ("approved".equalsIgnoreCase(finalStatus)) {
                                msgForStudent = "Your booking for " + b.getSpaceName() + " has been approved!";
                            } else if (finalStatus != null && finalStatus.toLowerCase().contains("faculty")) {
                                msgForNext = "Booking forwarded for approval";
                                msgForStudent = "Your booking for " + b.getSpaceName() + " has been forwarded to Faculty Incharge.";
                            } else if (finalStatus != null && finalStatus.toLowerCase().contains("hod")) {
                                msgForNext = "Booking forwarded to HOD for final approval";
                                msgForStudent = "Your booking for " + b.getSpaceName() + " has been forwarded to HOD.";
                            } else {
                                // Fallback to role-based if status is unknown but approved
                                switch (role.toLowerCase()) {
                                    case "staff":
                                    case "staffincharge":
                                    case "labadmin":
                                        msgForNext = "Booking forwarded by Staff for approval";
                                        msgForStudent = "Your booking for " + b.getSpaceName() + " has been forwarded to Faculty Incharge.";
                                        break;
                                    case "faculty":
                                    case "facultyincharge":
                                        msgForNext = "Booking forwarded to HOD for final approval";
                                        msgForStudent = "Your booking for " + b.getSpaceName() + " has been forwarded to HOD.";
                                        break;
                                    case "hod":
                                        msgForStudent = "Your booking for " + b.getSpaceName() + " has been approved!";
                                        break;
                                }
                            }
                        }

                        // Send to Student
                        if (!msgForStudent.isEmpty()) {
                            sendNotification(b.getBookedBy(), msgForStudent, bookingId, finalStatus, b.getBookedBy(), type, "student", r -> {});
                        }

                        // Notify NEXT AUTHORITY if forwarded
                        if (approved) {
                            // Robust check for finalStatus to notify next authority
                            if (finalStatus != null && finalStatus.toLowerCase().contains("hod") && !finalStatus.equalsIgnoreCase("approved")) {
                                notifyRole("Hod", msgForNext, bookingId, b.getBookedBy(), "escalation", "hod");
                            } else if (finalStatus != null && finalStatus.toLowerCase().contains("faculty")) {
                                notifyRole("facultyIncharge", msgForNext, bookingId, b.getBookedBy(), "escalation", "faculty");
                            }
                        }

                        // Sync schedule slot
                        if ("approved".equals(finalStatus) || "rejected".equals(finalStatus) || "Cancelled".equals(finalStatus)) {
                            syncSlotStatusFromBooking(bookingId, finalStatus);
                        }
                        callback.onComplete(new Result.Success<>(null));
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onComplete(new Result.Error<>(error.toException()));
            }
        });
    }

    private void syncSlotStatusFromBooking(String bookingId, String newStatus) {
        bookingsRef.child(bookingId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                String scheduleId = snapshot.child("scheduleId").getValue(String.class);
                String rawTimeSlot = snapshot.child("timeSlot").getValue(String.class);
                if (scheduleId == null || rawTimeSlot == null) return;
                
                String timeSlot = rawTimeSlot;
                Map<String, Object> slotUpdates = new HashMap<>();
                if ("approved".equalsIgnoreCase(newStatus)) {
                    slotUpdates.put("status", "BOOKED");
                    slotUpdates.put("bookedBy", snapshot.child("bookedBy").getValue());
                    slotUpdates.put("requesterName", snapshot.child("requesterName").getValue());
                    slotUpdates.put("purpose", snapshot.child("purpose").getValue());
                } else {
                    slotUpdates.put("status", "AVAILABLE");
                    slotUpdates.put("bookedBy", null);
                    slotUpdates.put("requesterName", null);
                    slotUpdates.put("purpose", null);
                }
                
                updateSlotStatus(scheduleId, timeSlot, slotUpdates, r -> {
                    Log.d("FirebaseRepo", "Slot sync: " + scheduleId + "/" + timeSlot + " → " + slotUpdates.get("status"));
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseRepo", "syncSlotStatusFromBooking cancelled: " + error.getMessage());
            }
        });
    }

    public void updateSlotStatus(@NonNull String scheduleId, @NonNull String slotKey, @NonNull Map<String, Object> updates, @NonNull Callback<Void> callback) {
        String finalKey = formatSlotKey(slotKey);
        schedulesRef.child(scheduleId).child("slots").child(finalKey).updateChildren(updates)
                .addOnSuccessListener(v -> callback.onComplete(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onComplete(new Result.Error<>(e)));
    }

    public void updateSlotStatus(@NonNull String scheduleId, @NonNull String slotKey, @NonNull String status, @NonNull Callback<Void> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updateSlotStatus(scheduleId, slotKey, updates, callback);
    }

    public void getBookingForSlot(String spaceId, String date, String timeRange, Callback<Booking> callback) {
        String scheduleId = spaceId + "_" + date;
        bookingsRef.orderByChild("scheduleId").equalTo(scheduleId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Booking bestMatch = null;
                        int highestPriority = -1;

                        for (DataSnapshot child : snapshot.getChildren()) {
                            Booking b = safeParseBooking(child);
                            if (b != null) {
                                String bSlot = b.getTimeSlot();
                                String bStart = b.getSlotStart();

                                // Triple-Check Match
                                boolean match = timeRange.equalsIgnoreCase(bSlot) || 
                                            timeRange.equalsIgnoreCase(bStart) || 
                                            formatSlotKey(timeRange).equalsIgnoreCase(formatSlotKey(bSlot));

                                if (match) {
                                    String status = b.getStatus() != null ? b.getStatus().toLowerCase() : "";
                                    int priority = 0;

                                    // Rank the statuses. We want Approved/Booked to win!
                                    if (status.equals("rejected") || status.equals("cancelled")) priority = 1;
                                    else if (status.equals("pending")) priority = 2;
                                    else if (status.contains("forwarded")) priority = 3;
                                    else if (status.equals("approved") || status.equals("booked")) priority = 4;

                                    if (priority > highestPriority) {
                                        highestPriority = priority;
                                        bestMatch = b;
                                    }
                                }
                            }
                        }
                        // Return the highest priority booking found
                        callback.onComplete(new Result.Success<>(bestMatch));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onComplete(new Result.Error<>(error.toException()));
                    }
                });
    }

    public void deleteBooking(String bookingId, Callback<Void> callback) {
        if (bookingId == null) {
            callback.onComplete(new Result.Error<>(new Exception("Invalid Booking ID")));
            return;
        }
        bookingsRef.child(bookingId).removeValue()
                .addOnSuccessListener(v -> callback.onComplete(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onComplete(new Result.Error<>(e)));
    }

    // region Schedules

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
                                                Object val = slotSnapshot.getValue();
                                                boolean isBooked = false;
                                                if (val instanceof Boolean) {
                                                    isBooked = (Boolean) val;
                                                } else {
                                                    String status = slotSnapshot.child("status").getValue(String.class);
                                                    isBooked = "BOOKED".equalsIgnoreCase(status);
                                                }
                                                slots.put(slotSnapshot.getKey(), isBooked);
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
                int totalSlots = (int) slotsSnapshot.getChildrenCount();
                if (totalSlots == 0) {
                    callback.onComplete(new Result.Success<>(slotsMap));
                    return;
                }

                java.util.concurrent.atomic.AtomicInteger remaining = new java.util.concurrent.atomic.AtomicInteger(totalSlots);

                for (DataSnapshot slotSnap : slotsSnapshot.getChildren()) {
                    String rawKey = slotSnap.getKey();
                    if (rawKey == null) {
                        if (remaining.decrementAndGet() == 0) callback.onComplete(new Result.Success<>(slotsMap));
                        continue;
                    }
                    
                    String standardizedKey = rawKey;
                    String status = "AVAILABLE";
                    Object value = slotSnap.getValue();
                    if (value instanceof Boolean) {
                        status = ((Boolean) value) ? "BOOKED" : "AVAILABLE";
                    } else {
                        status = slotSnap.child("status").getValue(String.class);
                        if (status == null) status = "AVAILABLE";
                    }

                    if ("REJECTED".equalsIgnoreCase(status)) status = "AVAILABLE";

                    final String finalStatus = status;
                    final String finalSlot = standardizedKey;
                    final String normalizedKey = normalizeSlotKey(finalSlot);

                    if ("BOOKED".equalsIgnoreCase(status)) {
                        getBookingForSlot(spaceId, currentDate, rawKey, bookingResult -> {
                            if (bookingResult instanceof Result.Success) {
                                com.example.hod.models.Booking b = ((Result.Success<com.example.hod.models.Booking>) bookingResult).data;
                                LiveStatusData data = new com.example.hod.models.LiveStatusData(finalStatus, b, finalSlot, currentDate);
                                slotsMap.put(finalSlot, data);
                                slotsMap.put(normalizedKey, data);
                            } else {
                                LiveStatusData data = new com.example.hod.models.LiveStatusData(finalStatus, null, finalSlot, currentDate);
                                slotsMap.put(finalSlot, data);
                                slotsMap.put(normalizedKey, data);
                            }
                            if (remaining.decrementAndGet() == 0) {
                                callback.onComplete(new Result.Success<>(slotsMap));
                            }
                        });
                    } else {
                        LiveStatusData data = new com.example.hod.models.LiveStatusData(finalStatus, null, finalSlot, currentDate);
                        slotsMap.put(finalSlot, data);
                        slotsMap.put(normalizedKey, data);
                        if (remaining.decrementAndGet() == 0) {
                            callback.onComplete(new Result.Success<>(slotsMap));
                        }
                    }
                }
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
                        if (status == null) {
                            Object val = slotSnap.getValue();
                            if (val instanceof Boolean) {
                                status = (Boolean) val ? "BOOKED" : "AVAILABLE";
                            } else {
                                status = "AVAILABLE";
                            }
                        }
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
                // Fallback: search by normalized key if direct time match failed
                for (DataSnapshot slotSnap : snapshot.getChildren()) {
                    String slotKey = slotSnap.getKey();
                    if (slotKey != null && normalizeSlotKey(slotKey).equals(normalizeSlotKey(currentTime))) {
                        // ... handle match ...
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

    private boolean isTimeInSlot(String currentTime, String slotRange) {
        try {
            String[] parts = slotRange.split("-");
            if (parts.length != 2) return false;

            String startStr = parts[0].trim();
            String endStr = parts[1].trim();

            int current = timeToMinutes(currentTime);
            int start = timeToMinutes(startStr);
            int end = timeToMinutes(endStr);

            if (end == 0) end = MINUTES_IN_DAY; 

            return current >= start && current < end;
        } catch (Exception e) {
            return false;
        }
    }

    public String formatSlotKey(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        try {
            // Changed split logic: using "-" is much safer than stripping spaces
            String[] parts = raw.split("-");
            if (parts.length < 2) return raw.trim();

            String start = convertTo24h(parts[0].trim());
            String end = convertTo24h(parts[1].trim());

            return start + " - " + end;
        } catch (Exception e) {
            return raw.trim();
        }
    }

    public String normalizeSlotStatus(String status) {
        if (status == null) return "AVAILABLE";
        String upperStatus = status.toUpperCase().trim();
        if (upperStatus.contains("APPROVED") || upperStatus.contains("BOOKED") || upperStatus.contains("USED") || upperStatus.contains("COMPLETED")) return "BOOKED";
        if (upperStatus.contains("BLOCKED")) return "BLOCKED";
        // All others (PENDING, REJECTED, CANCELLED, etc.) become AVAILABLE
        return "AVAILABLE";
    }

    public String convertTo24h(String time) {
        if (time == null || time.isEmpty()) return "00:00";
        try {
            String upper = time.toUpperCase().trim();
            boolean isPM = upper.contains("PM");
            boolean isAM = upper.contains("AM");

            String cleanTime = upper.replaceAll("[^0-9:]", "").trim();
            if (cleanTime.isEmpty()) return "00:00";

            int h, m;
            if (cleanTime.contains(":")) {
                String[] h_m = cleanTime.split(":");
                h = Integer.parseInt(h_m[0]);
                m = (h_m.length > 1) ? Integer.parseInt(h_m[1]) : 0;
            } else if (cleanTime.length() == 4) {
                h = Integer.parseInt(cleanTime.substring(0, 2));
                m = Integer.parseInt(cleanTime.substring(2, 4));
            } else if (cleanTime.length() == 3) {
                h = Integer.parseInt(cleanTime.substring(0, 1));
                m = Integer.parseInt(cleanTime.substring(1, 3));
            } else if (cleanTime.length() <= 2) {
                h = Integer.parseInt(cleanTime);
                m = 0;
            } else {
                return cleanTime;
            }

            if (isPM && h < 12) h += 12;
            if (isAM && h == 12) h = 0;

            return String.format(Locale.getDefault(), "%02d:%02d", h, m);
        } catch (Exception e) {
            return "00:00";
        }
    }

    public String normalizeSlotKey(String key) {
        if (key == null || key.isEmpty()) return "";
        try {
            // "09:00 - 09:30" OR "0900"
            if (key.contains("-")) {
                String[] parts = key.split("-");
                return convertTo24h(parts[0].trim());
            } else {
                return convertTo24h(key);
            }
        } catch (Exception e) {
            return key;
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

    public void getSpaceDetails(String spaceId, Callback<Space> callback) {
        spacesRef.child(spaceId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            callback.onComplete(new Result.Success<>(snapshot.getValue(Space.class)));
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

    public void blockSlot(String spaceId, String date, String slotKey, String reason, Callback<Void> callback) {
        String scheduleId = spaceId + "_" + date;
        String finalKey = formatSlotKey(slotKey);
        DatabaseReference slotRef = schedulesRef.child(scheduleId).child("slots").child(finalKey);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "BLOCKED");
        updates.put("blockReason", reason);

        slotRef.child("status").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String oldStatus = snapshot.getValue(String.class);
                
                slotRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                    if ("BOOKED".equalsIgnoreCase(oldStatus)) {
                        getBookingForSlot(spaceId, date, slotKey, bookingResult -> {
                            if (bookingResult instanceof Result.Success) {
                                Booking b = ((Result.Success<Booking>) bookingResult).data;
                                if (b != null && b.getBookedBy() != null) {
                                    String displayName = (b.getSpaceName() != null && !b.getSpaceName().isEmpty()) ? b.getSpaceName() : spaceId;
                                    String msg = String.format(Locale.getDefault(), "Your booking for %s on %s (%s) has been cancelled because the slot was blocked: %s", displayName, date, slotKey, reason);
                                    sendNotification(b.getBookedBy(), msg, b.getBookingId(), "Cancelled", b.getBookedBy(), "rejection", "student", r -> {});
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

    public void sendNotification(String userId, String message, String bookingId, String targetStatus, String bookedBy, String type, String roleTarget, Callback<Void> callback) {
        String id = notificationsRef.child(userId).push().getKey();
        if (id == null) return;
        
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("message", message);
        data.put("timestamp", System.currentTimeMillis());
        data.put("read", false);
        data.put("type", type);
        data.put("relatedBookingId", bookingId);
        data.put("targetStatus", targetStatus);
        data.put("bookedBy", bookedBy);
        data.put("roleTarget", roleTarget);
        
        notificationsRef.child(userId).child(id).setValue(data)
                .addOnSuccessListener(aVoid -> callback.onComplete(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onComplete(new Result.Error<>(e)));
    }

    public void sendNotification(String userId, String message, String bookingId, String targetStatus, String bookedBy, Callback<Void> callback) {
        sendNotification(userId, message, bookingId, targetStatus, bookedBy, "notification", "user", callback);
    }

    public void markAllNotificationsAsRead(String userId, Callback<Void> callback) {
        notificationsRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    updates.put(child.getKey() + "/read", true);
                }
                if (updates.isEmpty()) {
                    callback.onComplete(new Result.Success<>(null));
                    return;
                }
                notificationsRef.child(userId).updateChildren(updates)
                        .addOnSuccessListener(v -> callback.onComplete(new Result.Success<>(null)))
                        .addOnFailureListener(e -> callback.onComplete(new Result.Error<>(e)));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onComplete(new Result.Error<>(error.toException()));
            }
        });
    }

    public void deleteNotification(String userId, String notificationId) {
        notificationsRef.child(userId).child(notificationId).removeValue()
                .addOnFailureListener(e -> Log.e("FirebaseRepo", "Failed to delete notification", e));
    }

    public void observeApprovals(String labId, Callback<List<Booking>> callback) {
         bookingsRef.addValueEventListener(new ValueEventListener() {
             @Override
             public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                 List<Booking> bookingList = new ArrayList<>();
                 for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                     Booking booking = safeParseBooking(snapshot);
                     if (booking != null && booking.getStatus() != null) {
                         String bookingSpaceId = snapshot.child("spaceId").getValue(String.class);
                         if (bookingSpaceId == null && booking.getScheduleId() != null) {
                             bookingSpaceId = booking.getScheduleId().split("_")[0];
                         }
                         
                         if (labId != null && !labId.isEmpty() && !labId.equals(bookingSpaceId)) {
                             continue; 
                         }

                         String status = booking.getStatus().toLowerCase();
                         if (status.equals("approved") || status.contains("rejected") || status.contains("forwarded") || status.equals("cancelled") || status.equals("expired")) {
                             bookingList.add(booking);
                         }
                         }
                     }
                 callback.onComplete(new Result.Success<>(bookingList));
             }
             @Override
             public void onCancelled(@NonNull DatabaseError databaseError) {
                 callback.onComplete(new Result.Error<>(databaseError.toException()));
             }
         });
    }

    public void observeHodHistory(Callback<List<Booking>> callback) {
        bookingsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Booking> result = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Booking booking = safeParseBooking(snapshot);
                    if (booking != null && booking.getStatus() != null) {
                        String status = booking.getStatus().toLowerCase();
                        if (status.equals("approved") || status.contains("rejected") || status.equals("cancelled") || status.contains("forwarded") || status.equals("expired")) {
                            String currentUid = FirebaseAuth.getInstance().getUid();
                            if (currentUid != null && currentUid.equals(booking.getApprovedBy())) {
                                result.add(booking);
                            }
                        }
                    }
                }
                callback.onComplete(new Result.Success<>(result));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onComplete(new Result.Error<>(error.toException()));
            }
        });
    }

    public com.google.firebase.database.ValueEventListener observePendingRequests(String labId, Callback<List<Booking>> callback) {
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Booking> pendingBookings = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Booking booking = safeParseBooking(snapshot);
                    if (booking != null
                            && booking.getStatus() != null
                            && booking.getStatus().equalsIgnoreCase("pending")) {
                        pendingBookings.add(booking);
                    }
                }

                if (labId == null || labId.isEmpty()) {
                    callback.onComplete(new Result.Success<>(pendingBookings));
                    return;
                }

                if (pendingBookings.isEmpty()) {
                    callback.onComplete(new Result.Success<>(new ArrayList<>()));
                    return;
                }

                List<Booking> matched = new ArrayList<>();
                java.util.concurrent.atomic.AtomicInteger remaining =
                        new java.util.concurrent.atomic.AtomicInteger(pendingBookings.size());

                for (Booking booking : pendingBookings) {
                    String scheduleId = booking.getScheduleId();

                    if (scheduleId == null || scheduleId.isEmpty()) {
                        if (remaining.decrementAndGet() == 0) {
                            callback.onComplete(new Result.Success<>(matched));
                        }
                        continue;
                    }

                    schedulesRef.child(scheduleId).child("spaceId")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot spaceSnap) {
                                    String spaceId = spaceSnap.getValue(String.class);
                                    if (labId.equals(spaceId)) {
                                        synchronized (matched) {
                                            matched.add(booking);
                                        }
                                    }
                                    if (remaining.decrementAndGet() == 0) {
                                        callback.onComplete(new Result.Success<>(matched));
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    if (remaining.decrementAndGet() == 0) {
                                        callback.onComplete(new Result.Success<>(matched));
                                    }
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onComplete(new Result.Error<>(error.toException()));
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

    /**
     * Scans for bookings that have passed their time slot without a decision
     * and automatically marks them as 'expired', freeing up the lab slots.
     * If labId is null, it performs a global sweep across all labs.
     */
    public void performAutoExpirySweep(String labId, Callback<Integer> callback) {
        bookingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int sweepCount = 0;
                String currentTime = new SimpleDateFormat(DATE_FORMAT_FULL, Locale.getDefault()).format(new java.util.Date());
                
                for (DataSnapshot child : snapshot.getChildren()) {
                    Booking b = safeParseBooking(child);
                    if (b == null || b.getBookingId() == null) continue;

                    // Filter for this lab context (if provided)
                    if (labId != null && !labId.isEmpty()) {
                        String bLabId = b.getScheduleId() != null ? b.getScheduleId().split("_")[0] : null;
                        if (!labId.equals(bLabId)) continue;
                    }

                    String status = b.getStatus() != null ? b.getStatus().toLowerCase() : "pending";
                    
                    // Only sweep bookings that are still in a "Decision Required" or "Placeholder" state
                    boolean isActionable = status.equals("pending") || 
                                         status.contains("forwarded") || 
                                         status.equals("booked"); // Booked but never used/confirmed?

                    if (isActionable && b.isExpired()) {
                        sweepCount++;
                        
                        // User Request Fix: No automatic status update to 'expired'.
                        // The administrator should manually delete after seeing the 'Expired' warning on their pending list.
                        
                        // Inform ONLY the authority who didn't take action (Action Missed)
                        String authorityMsg = "Action Missed: A booking request for " + 
                            (b.getSpaceName() != null ? b.getSpaceName() : "Lab") + 
                            " has reached its start time without a decision.";
                        
                        if (status.equalsIgnoreCase("pending")) {
                            String spaceId = b.getScheduleId() != null ? b.getScheduleId().split("_")[0] : null;
                            if (spaceId != null) {
                                notifyStaffInchargeForSpace(spaceId, b.getSpaceName(), authorityMsg, b.getBookingId(), b.getBookedBy(), "notification", "staff");
                            }
                        } else if (status.contains("faculty")) {
                            notifyRole("facultyIncharge", authorityMsg, b.getBookingId(), b.getBookedBy(), "notification", "faculty");
                        } else if (status.contains("hod")) {
                            notifyRole("Hod", authorityMsg, b.getBookingId(), b.getBookedBy(), "notification", "hod");
                        }
                    }
                }
                if (callback != null) callback.onComplete(new Result.Success<>(sweepCount));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) callback.onComplete(new Result.Error<>(error.toException()));
            }
        });
    }

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
        labAdminsDetailsRef
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

    public void observeEscalatedRequests(Callback<List<Booking>> callback) {
        bookingsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Booking> escalated = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Booking booking = safeParseBooking(snapshot);
                    if (booking != null
                            && booking.getStatus() != null
                            && booking.getStatus().equalsIgnoreCase("forwarded_to_hod")) {
                        escalated.add(booking);
                    }
                }
                callback.onComplete(new Result.Success<>(escalated));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
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

    public void notifyStaffInchargeForSpace(String spaceId, String spaceName, String message, String bookingId, String bookedBy, String type, String roleTarget) {
        final java.util.Set<String> notifiedUids = new java.util.HashSet<>();
        
        getLabAdmins(spaceId, result -> {
            if (result instanceof Result.Success) {
                List<User> admins = ((Result.Success<List<User>>) result).data;
                if (admins != null && !admins.isEmpty()) {
                    for (User admin : admins) {
                        if (admin.uid != null) {
                            notifiedUids.add(admin.uid);
                            sendNotification(admin.uid, message, bookingId, "pending", bookedBy, type, roleTarget, r -> {});
                        }
                    }
                }
            }
            
            FirebaseDatabase.getInstance().getReference("users")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot userSnap : snapshot.getChildren()) {
                                String uid = userSnap.getKey();
                                String userIncharge = userSnap.child("inchargeToSpace").getValue(String.class);
                                
                                if (uid != null && userIncharge != null && !notifiedUids.contains(uid)) {
                                    boolean matchesId = userIncharge.trim().equalsIgnoreCase(spaceId.trim());
                                    boolean matchesName = spaceName != null && userIncharge.trim().equalsIgnoreCase(spaceName.trim());
                                    
                                    if (matchesId || matchesName) {
                                        sendNotification(uid, message, bookingId, "pending", bookedBy, type, roleTarget, r -> {});
                                    }
                                }
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
        });
    }

    public void notifyRole(String role, String message, String bookingId, String bookedBy, String type, String roleTarget) {
        FirebaseDatabase.getInstance().getReference("users")
                .orderByChild("role").equalTo(role)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            String uid = userSnap.getKey();
                            if (uid != null) {
                                sendNotification(uid, message, bookingId, "pending", bookedBy, type, roleTarget, r -> {});
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    // region New Methods for compatibility

    public void cleanOldData(String labId, Callback<Void> callback) {
        String today = new SimpleDateFormat(DATE_FORMAT_ISO, Locale.getDefault()).format(new java.util.Date());

        com.google.android.gms.tasks.Task<DataSnapshot> bookingsTask = bookingsRef.get();
        com.google.android.gms.tasks.Task<DataSnapshot> schedulesTask = schedulesRef.get();

        com.google.android.gms.tasks.Tasks.whenAllComplete(bookingsTask, schedulesTask)
            .addOnCompleteListener(task -> {
                List<com.google.android.gms.tasks.Task<Void>> deleteTasks = new ArrayList<>();

                if (bookingsTask.isSuccessful() && bookingsTask.getResult() != null) {
                    for (DataSnapshot child : bookingsTask.getResult().getChildren()) {
                        Booking booking = safeParseBooking(child);
                        if (booking != null && booking.getDate() != null && booking.getDate().compareTo(today) < 0) {
                            String bookingLabId = booking.getScheduleId() != null ? booking.getScheduleId().split("_")[0] : null;
                            if (labId == null || labId.isEmpty() || labId.equals(bookingLabId)) {
                                deleteTasks.add(child.getRef().removeValue());
                            }
                        }
                    }
                }

                if (schedulesTask.isSuccessful() && schedulesTask.getResult() != null) {
                    for (DataSnapshot child : schedulesTask.getResult().getChildren()) {
                        String key = child.getKey();
                        if (key != null && key.contains("_")) {
                            String[] parts = key.split("_", 2);
                            String scheduleLabId = parts[0];
                            String schedDate = parts[1];
                            if (schedDate.compareTo(today) < 0) {
                                if (labId == null || labId.isEmpty() || labId.equals(scheduleLabId)) {
                                    deleteTasks.add(child.getRef().removeValue());
                                }
                            }
                        }
                    }
                }

                if (deleteTasks.isEmpty()) {
                    callback.onComplete(new Result.Success<>(null));
                    return;
                }

                com.google.android.gms.tasks.Tasks.whenAll(deleteTasks)
                    .addOnCompleteListener(delTask -> {
                        if (delTask.isSuccessful()) {
                            callback.onComplete(new Result.Success<>(null));
                        } else {
                            callback.onComplete(new Result.Error<>(delTask.getException()));
                        }
                    });
            });
    }

    public void getSchedulesForLab(String spaceId, String date, Callback<DataSnapshot> callback) {
        String scheduleId = spaceId + "_" + date;
        schedulesRef.child(scheduleId).child("slots").addListenerForSingleValueEvent(new ValueEventListener() {
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

    public ValueEventListener observeSchedulesForLab(String spaceId, String date, Callback<DataSnapshot> callback) {
        String scheduleId = spaceId + "_" + date;
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
        schedulesRef.child(scheduleId).addValueEventListener(listener);
        return listener;
    }

    public int parseStartTime(String timeRange) {
        if (timeRange == null || timeRange.isEmpty()) return -1;
        try {
            // Split "08:30 PM - 09:00 PM" into parts and parse the start time
            String[] parts = timeRange.split("-");
            return timeToMinutes(parts[0].trim());
        } catch (Exception e) {
            return -1; 
        }
    }

    public void removeScheduleListener(@NonNull String spaceId, @NonNull String date, @NonNull ValueEventListener listener) {
        String scheduleId = spaceId + "_" + date;
        schedulesRef.child(scheduleId).removeEventListener(listener);
    }

    public void unblockSlot(@NonNull String spaceId, @NonNull String date, @NonNull String slotKey, @NonNull Callback<Void> callback) {
        String scheduleId = spaceId + "_" + date;
        String finalKey = formatSlotKey(slotKey);
        schedulesRef.child(scheduleId).child("slots").child(finalKey).child("status").setValue("AVAILABLE")
                .addOnSuccessListener(aVoid -> callback.onComplete(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onComplete(new Result.Error<>(e)));
    }

    public void getSpaceIdFromSchedule(String scheduleId, Callback<String> callback) {
        schedulesRef.child(scheduleId).child("spaceId").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                callback.onComplete(new Result.Success<>(snapshot.getValue(String.class)));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onComplete(new Result.Error<>(error.toException()));
            }
        });
    }

    public void searchUsers(String query, Callback<List<User>> callback) {
        usersRef.orderByChild("name").startAt(query).endAt(query + "\uf8ff")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<User> users = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            User u = child.getValue(User.class);
                            if (u != null && "student".equalsIgnoreCase(u.role)) {
                                u.uid = child.getKey();
                                users.add(u);
                            }
                        }
                        callback.onComplete(new Result.Success<>(users));
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onComplete(new Result.Error<>(error.toException()));
                    }
                });
    }

    public void updateToLabAdmin(String uid, String labId, Callback<Void> callback) {
        getUserDetails(uid, res -> {
            if (res instanceof Result.Success) {
                User u = ((Result.Success<User>) res).data;
                if (u == null) {
                    callback.onComplete(new Result.Error<>(new Exception("User not found")));
                    return;
                }

                usersRef.child(uid).child("role").setValue("labAdmin")
                        .addOnSuccessListener(aVoid -> {
                            usersRef.child(uid).child("inchargeToSpace").setValue(labId)
                                    .addOnSuccessListener(aVoid2 -> {
                                        getSpaceDetails(labId, spaceRes -> {
                                            String labName = "Unknown_Lab";
                                            if (spaceRes instanceof Result.Success) {
                                                Space s = ((Result.Success<Space>) spaceRes).data;
                                                if (s != null) {
                                                    labName = s.getRoomName() != null ? s.getRoomName() : labId;
                                                }
                                            }
                                            
                                            final String sanitizedLabName = sanitizeFirebaseKey(labName);
                                            String pushKey = labAdminsDetailsRef.push().getKey();
                                            if (pushKey == null) pushKey = "admin_" + System.currentTimeMillis();

                                            com.example.hod.models.LabAdminDetail detail = new com.example.hod.models.LabAdminDetail(
                                                pushKey, 
                                                uid, 
                                                u.name != null ? u.name : "N/A", 
                                                sanitizedLabName, 
                                                labId, 
                                                u.phoneNumber != null ? u.phoneNumber : "N/A", 
                                                u.emailId != null ? u.emailId : "N/A", 
                                                u.rollNo != null ? u.rollNo : "N/A"
                                            );
                                            
                                            labAdminsDetailsRef.child(pushKey).setValue(detail)
                                                .addOnSuccessListener(aVoid3 -> {
                                                    // Initial creation: perform a full sync
                                                    generateWeeklySchedule(sanitizedLabName, labId, true, r -> {
                                                        callback.onComplete(new Result.Success<>(null));
                                                    });
                                                })
                                                .addOnFailureListener(e -> callback.onComplete(new Result.Error<>(e)));
                                        });
                                    })
                                    .addOnFailureListener(e -> callback.onComplete(new Result.Error<>(e)));
                        })
                        .addOnFailureListener(e -> callback.onComplete(new Result.Error<>(e)));
            } else {
                callback.onComplete(new Result.Error<>(((Result.Error<?>) res).exception));
            }
        });
    }

    public void removeFromLabAdmin(String uid, String labName, String spaceId, Callback<Void> callback) {
        final String sanitizedLabName = sanitizeFirebaseKey(labName);
        usersRef.child(uid).child("role").setValue("student")
                .addOnSuccessListener(aVoid -> {
                    usersRef.child(uid).child("inchargeToSpace").removeValue()
                            .addOnSuccessListener(aVoid2 -> {
                                labAdminsDetailsRef.orderByChild("uid").equalTo(uid)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                for (DataSnapshot child : snapshot.getChildren()) {
                                                    String sId = child.child("spaceId").getValue(String.class);
                                                    if (spaceId.equals(sId)) {
                                                        child.getRef().removeValue();
                                                    }
                                                }
                                                // Initial creation: perform a full sync
                                                generateWeeklySchedule(sanitizedLabName, spaceId, true, r -> {
                                                    callback.onComplete(new Result.Success<>(null));
                                                });
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                callback.onComplete(new Result.Error<>(error.toException()));
                                            }
                                        });
                            })
                            .addOnFailureListener(e -> callback.onComplete(new Result.Error<>(e)));
                })
                .addOnFailureListener(e -> callback.onComplete(new Result.Error<>(e)));
    }

    public void initializeBookingSlots(String labId, String date, Callback<Void> callback) {
        String scheduleId = labId + "_" + date;
        String dayOfWeek = getDayOfWeek(date);

        // Fetch using labId directly now
        getLabAdminWeeklySchedule(labId, dayOfWeek, res -> {
            Map<String, Object> slots = new HashMap<>();

            if (res instanceof Result.Success) {
                List<Map<String, String>> weeklySlots = ((Result.Success<List<Map<String, String>>>) res).data;
                if (weeklySlots != null) {
                    for (Map<String, String> slot : weeklySlots) {
                        String timeRange = slot.get("slot");
                        if (timeRange != null) {
                            String stdKey = formatSlotKey(timeRange);
                            Map<String, Object> s = new HashMap<>();
                            s.put("status", "AVAILABLE");
                            s.put("name", slot.get("name"));
                            s.put("rollNo", slot.get("rollNo"));
                            s.put("uid", slot.get("uid"));
                            slots.put(stdKey, s);
                        }
                    }
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("spaceId", labId);
            data.put("date", date);
            data.put("slots", slots);

            // FIX: Atomic write. Replaces the node without creating a temporary empty state.
            schedulesRef.child(scheduleId).setValue(data)
                .addOnCompleteListener(t -> callback.onComplete(new Result.Success<>(null)));
        });
    }

    private String getDayOfWeek(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_ISO, Locale.ENGLISH);
            java.util.Date date = sdf.parse(dateStr);
            return new SimpleDateFormat(DAY_FORMAT, Locale.ENGLISH).format(date);
        } catch (Exception e) {
            return "Monday";
        }
    }

    public void getLabAdminWeeklySchedule(String spaceId, String dayOfWeek, Callback<List<Map<String, String>>> callback) {
        if (spaceId == null || spaceId.isEmpty()) {
            callback.onComplete(new Result.Error<>(new Exception("Space ID is missing")));
            return;
        }
        
        // Unified Path: Use the constant directly from root (Matches generation)
        FirebaseDatabase.getInstance().getReference(com.example.hod.firebase.FirebasePaths.LAB_ADMIN_WEEKLY_SCHEDULE)
                .child(spaceId).child(dayOfWeek)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Map<String, String>> slots = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Map<String, String> slot = new HashMap<>();
                            slot.put("slot", child.getKey());
                            slot.put("name", String.valueOf(child.child("name").getValue()));
                            slot.put("rollNo", String.valueOf(child.child("rollNo").getValue()));
                            slot.put("uid", String.valueOf(child.child("uid").getValue())); 
                            slots.add(slot);
                        }
                        callback.onComplete(new Result.Success<>(slots));
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onComplete(new Result.Error<>(error.toException()));
                    }
                });
    }

    public void cleanupAllSchedules(Callback<String> callback) {
        schedulesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();
                final int[] labsProcessed = {0};
                final int[] slotsMerged = {0};

                for (DataSnapshot scheduleSnap : snapshot.getChildren()) {
                    String scheduleId = scheduleSnap.getKey(); // This is correctly "labId_date"
                    DataSnapshot slotsSnap = scheduleSnap.child("slots");
                    
                    if (!slotsSnap.exists()) continue;
                    
                    Map<String, Object> newSlots = new HashMap<>();
                    boolean changeNeeded = false;
                    labsProcessed[0]++;

                    for (DataSnapshot slotSnap : slotsSnap.getChildren()) {
                        String rawKey = slotSnap.getKey();
                        if (rawKey == null) continue;
                        
                        String formattedKey = formatSlotKey(rawKey);
                        Object slotData = slotSnap.getValue();
                        
                        // Migrate status PENDING/REJECTED/CANCELLED to AVAILABLE for slots
                        try {
                            if (slotData instanceof Map) {
                                Map<String, Object> current = (Map<String, Object>) slotData;
                                String status = (String) current.get("status");
                                if ("PENDING".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)) {
                                    current.put("status", "AVAILABLE");
                                    changeNeeded = true;
                                }
                            }
                        } catch (Exception ignored) {}

                        if (newSlots.containsKey(formattedKey)) {
                            changeNeeded = true;
                            slotsMerged[0]++;
                            // Prioritize BOOKED or BLOCKED statuses during merge
                            try {
                                Map<String, Object> existing = (Map<String, Object>) newSlots.get(formattedKey);
                                Map<String, Object> current = (Map<String, Object>) slotData;
                                String currentStatus = (current != null) ? (String) current.get("status") : "AVAILABLE";
                                
                                if (current != null && !"AVAILABLE".equalsIgnoreCase(currentStatus)) {
                                    newSlots.put(formattedKey, slotData);
                                }
                            } catch (Exception ignored) {}
                        } else {
                            if (!rawKey.equals(formattedKey)) changeNeeded = true;
                            newSlots.put(formattedKey, slotData);
                        }
                    }

                    if (changeNeeded) {
                        updates.put(scheduleId + "/slots", newSlots);
                    }
                }

                if (updates.isEmpty()) {
                    callback.onComplete(new Result.Success<>("All schedules are already standardized."));
                } else {
                    schedulesRef.updateChildren(updates).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Also cleanup weekly templates
                            cleanupWeeklyTemplates(msg -> {
                                // Final step: Trigger a historical sync for the most active labs to restore bookings
                                java.util.Set<String> uniqueIds = new java.util.HashSet<>();
                                for (DataSnapshot s : snapshot.getChildren()) {
                                    String id = s.getKey();
                                    if (id != null && id.contains("_")) {
                                        uniqueIds.add(id.split("_")[0]);
                                    }
                                }
                                
                                // Sync the last 7 days for up to 5 unique labs found in recent schedules
                                int syncCount = 0;
                                for (String spaceId : uniqueIds) {
                                    forceHistoricalSync(spaceId, r -> {});
                                    syncCount++;
                                    if (syncCount >= 5) break; 
                                }
                                
                                callback.onComplete(new Result.Success<>("Standardized " + labsProcessed[0] + " days. Recovered history for " + syncCount + " labs."));
                            });
                        } else {
                            callback.onComplete(new Result.Error<>(task.getException()));
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onComplete(new Result.Error<>(error.toException()));
            }
        });
    }

    /**
     * Recovery: CROSS-REFERENCE the schedule with the official booking history.
     * If a booking is 'approved' in history but missing/available in schedule, reinstate it.
     */
    public void syncScheduleWithBookings(String spaceId, String date, Callback<Void> callback) {
        String targetScheduleId = spaceId + "_" + date;
        
        // Fetch current standardized slots first (to ensure we only write to valid 30-min keys)
        schedulesRef.child(targetScheduleId).child("slots").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot templateSnapshot) {
                Set<String> valid30MinKeys = new HashSet<>();
                for (DataSnapshot child : templateSnapshot.getChildren()) {
                    if (child.getKey() != null) valid30MinKeys.add(formatSlotKey(child.getKey()));
                }

                bookingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, Object> updates = new HashMap<>();
                        for (DataSnapshot bSnap : snapshot.getChildren()) {
                            String bScheduleId = bSnap.child("scheduleId").getValue(String.class);
                            String bStatus = bSnap.child("status").getValue(String.class);
                            String bSlot = bSnap.child("timeSlot").getValue(String.class);
                            
                            if (bScheduleId != null && bScheduleId.equals(targetScheduleId) && 
                                bSlot != null && bStatus != null &&
                                (bStatus.equalsIgnoreCase("approved") || bStatus.equalsIgnoreCase("booked"))) {
                                
                                String[] rawParts = bSlot.contains(" - ") ? bSlot.split(" - ") : bSlot.split("_");
                                if (rawParts.length >= 2) {
                                    String start = rawParts[0].trim();
                                    String end = rawParts[1].trim();
                                    
                                    // Split 1-hour legacy bookings into 30-min slots
                                    if (isOneHourSlot(start, end)) {
                                        String mid = getMiddleTime(start);
                                        if (mid != null) {
                                            safeAddSyncUpdate(updates, start, mid, bSnap, valid30MinKeys);
                                            safeAddSyncUpdate(updates, mid, end, bSnap, valid30MinKeys);
                                        } else {
                                            safeAddSyncUpdate(updates, start, end, bSnap, valid30MinKeys);
                                        }
                                    } else {
                                        safeAddSyncUpdate(updates, start, end, bSnap, valid30MinKeys);
                                    }
                                }
                            }
                        }
                        
                        if (updates.isEmpty()) {
                            callback.onComplete(new Result.Success<>(null));
                        } else {
                            schedulesRef.child(targetScheduleId).child("slots").updateChildren(updates)
                                .addOnCompleteListener(t -> callback.onComplete(new Result.Success<>(null)));
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        callback.onComplete(new Result.Error<>(e.toException()));
                    }
                });
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                callback.onComplete(new Result.Error<>(error.toException()));
            }
        });
    }

    private void safeAddSyncUpdate(Map<String, Object> updates, String start, String end, DataSnapshot bSnap, Set<String> validKeys) {
        String key = formatSlotKey(start + " - " + end);
        
        // REGEX SCRUB: Strictly enforce HH:mm - HH:mm format (24-hour)
        // This instantly vaporizes any variations like "09:00-09:30" or duplicates
        String regex = "^([01]\\d|2[0-3]):[0-5]\\d - ([01]\\d|2[0-3]):[0-5]\\d$";
        if (key.matches(regex) && validKeys.contains(key)) {
            Map<String, Object> bookedSlot = new HashMap<>();
            bookedSlot.put("status", "BOOKED");
            bookedSlot.put("bookedBy", bSnap.child("bookedBy").getValue());
            bookedSlot.put("requesterName", bSnap.child("requesterName").getValue());
            bookedSlot.put("start", formatTime(start));
            bookedSlot.put("end", formatTime(end));
            updates.put(key, bookedSlot);
        }
    }

    /**
     * Aggressive Sync: Forces the last 7 days of schedule to match the bookings node.
     */
    public void forceHistoricalSync(String spaceId, Callback<Void> callback) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -7); // Start from 7 days ago

        final int numDays = 8; // -7rd to today
        final int[] completed = {0};

        for (int i = 0; i < numDays; i++) {
            String date = sdf.format(cal.getTime());
            syncScheduleWithBookings(spaceId, date, r -> {
                completed[0]++;
                if (completed[0] == numDays) {
                    callback.onComplete(new Result.Success<>(null));
                }
            });
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void cleanupWeeklyTemplates(Callback<String> callback) {
        FirebaseDatabase.getInstance().getReference(com.example.hod.firebase.FirebasePaths.LAB_ADMIN_WEEKLY_SCHEDULE)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Map<String, Object> updates = new HashMap<>();
                    for (DataSnapshot labSnap : snapshot.getChildren()) {
                        String labName = labSnap.getKey();
                        for (DataSnapshot daySnap : labSnap.getChildren()) {
                            String day = daySnap.getKey();
                            Map<String, Object> newSlots = new HashMap<>();
                            boolean change = false;
                            for (DataSnapshot slotSnap : daySnap.getChildren()) {
                                String raw = slotSnap.getKey();
                                // Heuristic: if it's "01:00" - "05:00", it was likely intended to be PM (13:00-17:00)
                                String migrated = raw;
                                if (raw.contains("01:0") || raw.contains("02:0") || raw.contains("03:0") || raw.contains("04:0") || raw.contains("05:0")) {
                                    migrated = raw.replace("01:", "13:").replace("02:", "14:")
                                                  .replace("03:", "15:").replace("04:", "16:").replace("05:", "17:");
                                }
                                String fmt = formatSlotKey(migrated);
                                if (!raw.equals(fmt)) change = true;
                                newSlots.put(fmt, slotSnap.getValue());
                            }
                            if (change) updates.put(labName + "/" + day, newSlots);
                        }
                    }
                    if (updates.isEmpty()) callback.onComplete(new Result.Success<>("Done"));
                    else FirebaseDatabase.getInstance().getReference(com.example.hod.firebase.FirebasePaths.LAB_ADMIN_WEEKLY_SCHEDULE)
                        .updateChildren(updates).addOnCompleteListener(t -> callback.onComplete(new Result.Success<>("Done")));
                }
                @Override public void onCancelled(@NonNull DatabaseError e) {}
            });
    }

    public void healLabTemplate(String labId, Callback<Void> callback) {
        getSpaceDetails(labId, res -> {
            if (res instanceof Result.Success) {
                com.example.hod.models.Space space = ((Result.Success<com.example.hod.models.Space>) res).data;
                String roomName = (space != null) ? space.getRoomName() : null;
                if (roomName != null) {
                    // Manual Repair: Disable auto-sync to prevent background race condition during 'Fix'
                    generateWeeklySchedule(roomName, labId, false, callback);
                } else {
                    callback.onComplete(new Result.Error<>(new Exception("Room name not found")));
                }
            } else {
                callback.onComplete(new Result.Error<>(new Exception("Space details failed")));
            }
        });
    }

    /**
     * Automatic Global Audit: Scans -7 to +21 days and repairs only dates with > 29 slots.
     */
    public void performGlobalScheduleAudit(String userId, Callback<String> progressCallback) {
        getUserIdentity(userId, userRes -> {
            if (userRes instanceof Result.Success) {
                Map<String, String> details = ((Result.Success<Map<String, String>>) userRes).data;
                String labId = details.get("inchargeToSpace"); // Should be the labId or room name
                
                if (labId == null || labId.isEmpty() || labId.equalsIgnoreCase("N/A")) {
                    progressCallback.onComplete(new Result.Error<>(new Exception("No lab assigned to this profile.")));
                    return;
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -7); // Start from 7 days ago

                final int totalDays = 30; // ~1 month scan
                final int[] processed = {0};
                final int[] fixed = {0};

                for (int i = 0; i < totalDays; i++) {
                    final String date = sdf.format(cal.getTime());
                    final String scheduleId = labId + "_" + date;

                    schedulesRef.child(scheduleId).child("slots").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            long count = snapshot.getChildrenCount();
                            if (count > 29) {
                                // AUTO-REPAIR TRIGGERED: This date is corrupted
                                processed[0]++;
                                fixed[0]++;
                                progressCallback.onComplete(new Result.Success<>("Fixing " + date + " (" + count + " slots found)..."));
                                
                                // Step-by-step repair
                                initializeBookingSlots(labId, date, res -> {
                                    syncScheduleWithBookings(labId, date, syncRes -> {
                                        checkCompletion(processed, fixed, totalDays, progressCallback);
                                    });
                                });
                            } else {
                                // Safe date: Skip
                                processed[0]++;
                                checkCompletion(processed, fixed, totalDays, progressCallback);
                            }
                        }

                        @Override public void onCancelled(@NonNull DatabaseError error) {
                            processed[0]++;
                            checkCompletion(processed, fixed, totalDays, progressCallback);
                        }
                    });
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                }
            } else {
                progressCallback.onComplete(new Result.Error<>(new Exception("Failed to identify user lab")));
            }
        });
    }

    private void checkCompletion(int[] processed, int[] fixed, int total, Callback<String> callback) {
        if (processed[0] >= total) {
            callback.onComplete(new Result.Success<>("FINISHED: Audited " + total + " days. Repaired " + fixed[0] + " corrupted schedules."));
        }
    }

    public void generateWeeklySchedule(String labName, String spaceId, boolean shouldAutoSync, Callback<Void> callback) {
        if (spaceId == null || spaceId.isEmpty()) {
            callback.onComplete(new Result.Error<>(new Exception("Invalid lab details")));
            return;
        }

        getLabAdmins(spaceId, res -> {
            if (res instanceof Result.Success) {
                java.util.List<User> admins = ((Result.Success<java.util.List<User>>) res).data;
                if (admins != null && !admins.isEmpty()) {
                    Map<String, Object> weeklyData = new HashMap<>();
                    String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
                    
                    for (String d : days) {
                        Map<String, Object> dayMap = new HashMap<>();
                        // Updated to 08:00 - 23:00 for ALL days as per user request
                        int startHour = 8;
                        int endHour = 23;
                        
                        for (int h = startHour; h < endHour; h++) {
                            // Slot 1: hh:00 - hh:30
                            String t1 = String.format(java.util.Locale.US, "%02d:00 - %02d:30", h, h);
                            User admin1 = admins.get((h % admins.size()));
                            Map<String, String> a1 = new HashMap<>();
                            a1.put("name", admin1.name);
                            a1.put("rollNo", admin1.rollNo);
                            a1.put("uid", admin1.uid);
                            a1.put("status", "AVAILABLE");
                            dayMap.put(formatSlotKey(t1), a1);

                            // Slot 2: hh:30 - (h+1):00
                            String t2 = String.format(java.util.Locale.US, "%02d:30 - %02d:00", h, h + 1);
                            User admin2 = admins.get(((h + 1) % admins.size()));
                            Map<String, String> a2 = new HashMap<>();
                            a2.put("name", admin2.name);
                            a2.put("rollNo", admin2.rollNo);
                            a2.put("uid", admin2.uid);
                            a2.put("status", "AVAILABLE");
                            dayMap.put(formatSlotKey(t2), a2);
                        }
                        weeklyData.put(d, dayMap);
                    }

                    // FIX: Use spaceId as the definitive key and use an atomic setValue at Root Level constant
                    DatabaseReference weeklyRef = FirebaseDatabase.getInstance().getReference(com.example.hod.firebase.FirebasePaths.LAB_ADMIN_WEEKLY_SCHEDULE).child(spaceId);
                    weeklyRef.setValue(weeklyData).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (shouldAutoSync) {
                                syncTemplateToDailySchedules(spaceId, weeklyData, 365, callback);
                            } else {
                                callback.onComplete(new Result.Success<>(null));
                            }
                        } else {
                            callback.onComplete(new Result.Error<>(task.getException()));
                        }
                    });
                } else {
                    callback.onComplete(new Result.Error<>(new Exception("Failed to fetch lab admins")));
                }
            } else {
                callback.onComplete(new Result.Error<>(new Exception("Failed to fetch lab admins")));
            }
        });
    }

    private void syncTemplateToDailySchedules(String spaceId, Map<String, Object> weeklyData, int numDays, Callback<Void> callback) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", java.util.Locale.US);
        Calendar cal = Calendar.getInstance();
        
        final String todayStr = sdf.format(cal.getTime());
        final int currentMinuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);

        int totalDays = numDays > 0 ? numDays : 365;
        // Optimized 365-day sync via range query
        long startMillis = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L);
        long endMillis = System.currentTimeMillis() + ((long)totalDays * 24 * 60 * 60 * 1000L);
        String startDateStr = sdf.format(new java.util.Date(startMillis));
        String endDateStr = sdf.format(new java.util.Date(endMillis));

        schedulesRef.orderByKey().startAt(spaceId + "_" + startDateStr).endAt(spaceId + "_" + endDateStr)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot schedulesSnapshot) {
                    Map<String, Object> batchUpdates = new HashMap<>();
                    Calendar runner = Calendar.getInstance();
                    runner.setTimeInMillis(startMillis); // Start 30 days ago
                    
                    int iterations = totalDays + 30; // Total range (past + future)
                    for (int i = 0; i < iterations; i++) {
                        String dStr = sdf.format(runner.getTime());
                        String dayName = dayFormat.format(runner.getTime());
                        Map<String, Object> template = (Map<String, Object>) weeklyData.get(dayName);
                        if (template != null) {
                            String sid = spaceId + "_" + dStr;
                            DataSnapshot existingSlots = schedulesSnapshot.child(sid).child("slots");
                            for (Map.Entry<String, Object> entry : template.entrySet()) {
                                String sKey = entry.getKey();
                                if (!existingSlots.child(sKey).exists()) {
                                    Map<String, String> admin = (Map<String, String>) entry.getValue();
                                    Map<String, Object> slot = new HashMap<>();
                                    slot.put("status", "AVAILABLE");
                                    String[] pts = sKey.split(" - ");
                                    slot.put("start", formatTime(pts[0]));
                                    slot.put("end", formatTime(pts[pts.length-1]));
                                    slot.put("adminName", admin.get("name"));
                                    slot.put("adminRoll", admin.get("rollNo"));
                                    slot.put("adminUid", admin.get("uid"));
                                    batchUpdates.put(sid + "/slots/" + sKey, slot);
                                }
                            }
                        }
                        runner.add(Calendar.DAY_OF_YEAR, 1);
                    }
                    if (!batchUpdates.isEmpty()) schedulesRef.updateChildren(batchUpdates);
                    if (callback != null) callback.onComplete(new Result.Success<>(null));
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {
                    if (callback != null) callback.onComplete(new Result.Error<>(error.toException()));
                }
            });
        
        if (true) return; // Skip old loop

        java.util.concurrent.atomic.AtomicInteger processedDays = new java.util.concurrent.atomic.AtomicInteger(0);
        
        // Use a synchronized map to safely collect global multi-path updates from parallel reads
        final Map<String, Object> globalUpdates = java.util.Collections.synchronizedMap(new HashMap<>());

        for (int i = 0; i < totalDays; i++) {
            final String date = sdf.format(cal.getTime());
            final String dayOfWeek = dayFormat.format(cal.getTime());
            final boolean isToday = date.equals(todayStr);
            final Map<String, Object> dayTemplate = (Map<String, Object>) weeklyData.get(dayOfWeek);
            final String scheduleId = spaceId + "_" + date;
            
            if (dayTemplate != null) {
                DatabaseReference dailyRef = schedulesRef.child(scheduleId).child("slots");
                dailyRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot slotSnap : snapshot.getChildren()) {
                            String key = slotSnap.getKey();
                            if (key == null) continue;
                            String status = slotSnap.child("status").getValue(String.class);
                            String cleanKey = formatSlotKey(key);
                            String slotPath = scheduleId + "/slots/" + key;
                            String cleanSlotPath = scheduleId + "/slots/" + cleanKey;

                            if (isToday) {
                                int slotStartMin = parseStartTime(cleanKey);
                                if (slotStartMin >= 0 && slotStartMin < currentMinuteOfDay) {
                                    continue; 
                                }
                            }
                            
                            boolean withinShift = isTimeWithinShift(dayOfWeek, cleanKey);
                            
                            if (!withinShift) {
                                boolean hasHistory = "BOOKED".equalsIgnoreCase(status) || "BLOCKED".equalsIgnoreCase(status) || "PENDING".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status);
                                if (!hasHistory) {
                                    globalUpdates.put(slotPath, null);
                                    if (!key.equals(cleanKey)) globalUpdates.put(cleanSlotPath, null);
                                    continue;
                                }
                            }

                            if (!key.equals(cleanKey)) {
                                if ("BOOKED".equalsIgnoreCase(status) || "BLOCKED".equalsIgnoreCase(status) || "PENDING".equalsIgnoreCase(status)) {
                                    globalUpdates.put(cleanSlotPath, slotSnap.getValue());
                                }
                                globalUpdates.put(slotPath, null); 
                                continue;
                            }
                            
                            if ("REJECTED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status) || "PENDING".equalsIgnoreCase(status)) {
                                globalUpdates.put(slotPath + "/status", "AVAILABLE");
                            }
                        }
                        
                        for (Map.Entry<String, Object> entry : dayTemplate.entrySet()) {
                            String slotKey = entry.getKey();
                            Map<String, String> adminInfo = (Map<String, String>) entry.getValue();
                            DataSnapshot existing = snapshot.child(slotKey);
                            String newSlotPath = scheduleId + "/slots/" + slotKey;
                            
                            // Prevent overwriting if we just migrated it in globalUpdates
                            if (!existing.exists() && !globalUpdates.containsKey(newSlotPath)) {
                                Map<String, Object> newSlot = new HashMap<>();
                                newSlot.put("status", "AVAILABLE");
                                
                                String[] parts = slotKey.contains(" - ") ? slotKey.split(" - ") : slotKey.split("_");
                                if (parts.length >= 2) {
                                    newSlot.put("start", formatTime(parts[0]));
                                    newSlot.put("end", formatTime(parts[1]));
                                } else {
                                    newSlot.put("start", slotKey);
                                    newSlot.put("end", slotKey);
                                }
                                newSlot.put("adminName", adminInfo.get("name"));
                                newSlot.put("adminRoll", adminInfo.get("rollNo"));
                                newSlot.put("adminUid", adminInfo.get("uid"));
                                globalUpdates.put(newSlotPath, newSlot);
                            } else {
                                globalUpdates.put(newSlotPath + "/adminName", adminInfo.get("name"));
                                globalUpdates.put(newSlotPath + "/adminRoll", adminInfo.get("rollNo"));
                                globalUpdates.put(newSlotPath + "/adminUid", adminInfo.get("uid"));
                            }
                        }
                        
                        // Fire the batch update once all days are read
                        if (processedDays.incrementAndGet() >= totalDays) {
                            if (!globalUpdates.isEmpty()) {
                                schedulesRef.updateChildren(globalUpdates).addOnCompleteListener(t -> {
                                    if (callback != null) callback.onComplete(new Result.Success<>(null));
                                });
                            } else {
                                if (callback != null) callback.onComplete(new Result.Success<>(null));
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        if (processedDays.incrementAndGet() >= totalDays) {
                            if (callback != null) callback.onComplete(new Result.Success<>(null));
                        }
                    }
                });
            } else {
                if (processedDays.incrementAndGet() >= totalDays) {
                    if (callback != null) callback.onComplete(new Result.Success<>(null));
                }
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private String formatTime(String time) {
        if (time == null) return "";
        if (time.length() == 4 && !time.contains(":")) {
            return time.substring(0, 2) + ":" + time.substring(2);
        }
        return time;
    }

    public void getUserName(String uid, Callback<String> callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onComplete(new Result.Success<>("Unknown User"));
            return;
        }
        usersRef.child(uid).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.getValue(String.class);
                callback.onComplete(new Result.Success<>(name != null ? name : uid));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onComplete(new Result.Success<>(uid)); // Fallback to UID
            }
        });
    }

    public void getUserIdentity(String uid, Callback<Map<String, String>> callback) {
        if (uid == null || uid.isEmpty()) {
            Map<String, String> unknown = new HashMap<>();
            unknown.put("name", "Unknown User");
            unknown.put("rollNo", "N/A");
            unknown.put("role", "N/A");
            callback.onComplete(new Result.Success<>(unknown));
            return;
        }
        usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, String> details = new HashMap<>();
                String name = snapshot.child("name").getValue(String.class);
                String roll = snapshot.child("rollNo").getValue(String.class);
                String role = snapshot.child("role").getValue(String.class);
                String space = snapshot.child("inchargeToSpace").getValue(String.class);
                
                details.put("name", name != null ? name : "Unknown");
                details.put("rollNo", roll != null ? roll : "N/A");
                details.put("role", role != null ? role : "N/A");
                details.put("inchargeToSpace", space != null ? space : "N/A");
                callback.onComplete(new Result.Success<>(details));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                callback.onComplete(new Result.Error<>(error.toException()));
            }
        });
    }

    private String sanitizeFirebaseKey(String key) {
        if (key == null || key.isEmpty()) return "Unknown_Lab";
        // Firebase keys cannot contain: . $ # [ ] /
        return key.replace(".", "_")
                  .replace("$", "_")
                  .replace("#", "_")
                  .replace("[", "_")
                  .replace("]", "_")
                  .replace("/", "_")
                  .trim();
    }
    // endregion

    private boolean isOneHourSlot(String start, String end) {
        try {
            int startTotal = timeToMinutes(start);
            int endTotal = timeToMinutes(end);
            return (endTotal - startTotal) > 40; // Approx 1 hour difference
        } catch (Exception e) { return false; }
    }

    private String getMiddleTime(String start) {
        try {
            int total = timeToMinutes(start);
            int mid = total + 30;
            int h = mid / 60;
            int m = mid % 60;
            return String.format(Locale.getDefault(), "%02d:%02d", h, m);
        } catch (Exception e) { return null; }
    }

    private void addSlotSyncUpdate(Map<String, Object> updates, String start, String end, DataSnapshot bSnap) {
        String key = formatSlotKey(start + " - " + end);
        Map<String, Object> bookedSlot = new HashMap<>();
        bookedSlot.put("status", "BOOKED");
        bookedSlot.put("bookedBy", bSnap.child("bookedBy").getValue());
        bookedSlot.put("requesterName", bSnap.child("requesterName").getValue());
        bookedSlot.put("start", formatTime(start));
        bookedSlot.put("end", formatTime(end));
        updates.put(key, bookedSlot);
    }

    public void checkAndPerformWeeklyMaintenance(String labId, String labName, Callback<Void> callback) {
        DatabaseReference syncRef = FirebaseDatabase.getInstance().getReference("labs").child(labId).child("lastWeeklySync");
        syncRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long lastSync = snapshot.getValue(Long.class);
                long now = System.currentTimeMillis();
                
                Calendar cal = Calendar.getInstance();
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                boolean isMonday = (dayOfWeek == Calendar.MONDAY);
                
                // Trigger if it's Monday OR more than 7 days since last sync
                boolean needsSync = (lastSync == null) || isMonday || (now - lastSync > 7 * 24 * 60 * 60 * 1000L);
                
                if (needsSync) {
                    if (isMonday || lastSync == null) {
                        Log.d("Maintenance", "Weekly maintenance triggered for " + labName);
                        generateWeeklySchedule(labName, labId, true, result -> {
                            if (result instanceof Result.Success) {
                                syncRef.setValue(now);
                            }
                            callback.onComplete(result);
                        });
                    } else {
                        callback.onComplete(new Result.Success<>(null));
                    }
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

    private boolean isTimeWithinShift(String dayOfWeek, String slotKey) {
        if (dayOfWeek == null || slotKey == null) return true;
        // Shift expanded to 08:00 - 23:00 for all days
        int startHour = 8;
        int endHour = 23;

        try {
            int slotStartMin = parseStartTime(slotKey);
            if (slotStartMin < 0) return true; // Safety

            int startMin = startHour * 60;
            int endMin = endHour * 60;

            return slotStartMin >= startMin && slotStartMin < endMin;
        } catch (Exception e) {
            return true; // Safety
        }
    }
}
