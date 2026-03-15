package com.example.hod.repository;

import androidx.annotation.NonNull;

import com.example.hod.firebase.FirebaseClient;
import com.example.hod.models.Booking;
import com.example.hod.utils.Result;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
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

    public FirebaseRepository() {
        FirebaseClient client = FirebaseClient.getInstance();
        bookingsRef = client.bookingsRef();
        schedulesRef = client.schedulesRef();
        spacesRef = client.spacesRef();
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

                                if (booking.getUserName() == null) {
                                    booking.setUserName(booking.getUserId());
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

    public void updateApprovalStatus(String bookingId,
                                     String role,
                                     boolean approved,
                                     String remark,
                                     Callback<Void> callback) {
        bookingsRef.child(bookingId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Booking booking = snapshot.getValue(Booking.class);
                        if (booking == null) {
                            callback.onComplete(new Result.Error<>(new IllegalStateException("Booking not found")));
                            return;
                        }

                        if (remark != null) {
                            booking.setRemark(remark);
                        }

                        if ("staff".equalsIgnoreCase(role)) {
                            booking.setStaffApproval(approved);
                        } else if ("faculty".equalsIgnoreCase(role)) {
                            booking.setFacultyInchargeApproval(approved);
                        } else if ("hod".equalsIgnoreCase(role)) {
                            booking.setHodApproval(approved);
                        }

                        if (!approved) {
                            booking.setStatus("rejected");
                        } else if (booking.isStaffApproval()
                                && booking.isFacultyInchargeApproval()
                                && booking.isHodApproval()) {
                            booking.setStatus("approved");
                        }

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("staffApproval", booking.isStaffApproval());
                        updates.put("facultyInchargeApproval", booking.isFacultyInchargeApproval());
                        updates.put("hodApproval", booking.isHodApproval());
                        updates.put("remark", booking.getRemark());
                        updates.put("status", booking.getStatus());

                        bookingsRef.child(bookingId).updateChildren(updates, (error, ref) -> {
                            if (error != null) {
                                callback.onComplete(new Result.Error<>(error.toException()));
                            } else {
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
}

