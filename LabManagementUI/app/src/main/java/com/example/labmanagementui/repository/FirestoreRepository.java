package com.example.labmanagementui.repository;

import com.example.labmanagementui.models.Booking;
import com.example.labmanagementui.models.Space;
import com.example.labmanagementui.models.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class FirestoreRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference usersRef = db.collection("users");
    private final CollectionReference bookingsRef = db.collection("bookings");
    private final CollectionReference spacesRef = db.collection("spaces");

    // User Operations
    public Task<DocumentSnapshot> getUser(String uid) {
        return usersRef.document(uid).get();
    }

    public Task<Void> createUser(User user) {
        return usersRef.document(user.uid).set(user);
    }

    // Space Operations
    public Query getAvailableSpaces() {
        return spacesRef.whereEqualTo("availability", true);
    }

    // Booking Operations
    public Task<com.google.firebase.firestore.DocumentReference> createBooking(Booking booking) {
        return bookingsRef.add(booking);
    }

    public Query getBookingsForStaff() {
        return bookingsRef.whereEqualTo("staffApproval", false).orderBy("timestamp", Query.Direction.DESCENDING);
    }

    public Query getBookingsForFaculty() {
        return bookingsRef.whereEqualTo("staffApproval", true)
                .whereEqualTo("facultyApproval", false)
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

    public Query getBookingsForHOD() {
        return bookingsRef.whereEqualTo("facultyApproval", true)
                .whereEqualTo("hodApproval", false)
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

    public Task<Void> updateStaffApproval(String bookingId, boolean approved) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("staffApproval", approved);
        updates.put("status", approved ? "staff_approved" : "staff_rejected");
        return bookingsRef.document(bookingId).update(updates);
    }

    public Task<Void> updateFacultyApproval(String bookingId, boolean approved) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("facultyApproval", approved);
        updates.put("status", approved ? "faculty_approved" : "faculty_rejected");
        return bookingsRef.document(bookingId).update(updates);
    }

    public Task<Void> updateHODApproval(String bookingId, boolean approved) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("hodApproval", approved);
        updates.put("status", approved ? "approved" : "rejected");
        return bookingsRef.document(bookingId).update(updates);
    }
}
