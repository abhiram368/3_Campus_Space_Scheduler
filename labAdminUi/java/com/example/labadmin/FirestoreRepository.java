package com.example.labadmin;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.Map;

public class FirestoreRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    // Load sessions for a specific admin
    public ListenerRegistration getAdminSessions(String adminId, Callback<QuerySnapshot> callback) {
        return db.collection("labAdminSessions")
                .whereEqualTo("labAdminId", adminId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onError(error);
                    } else {
                        callback.onSuccess(value);
                    }
                });
    }

    // Load specific session details
    public ListenerRegistration getSessionByBookingId(String bookingId, Callback<DocumentSnapshot> callback) {
        return db.collection("labAdminSessions")
                .whereEqualTo("bookingId", bookingId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onError(error);
                    } else if (value != null && !value.isEmpty()) {
                        callback.onSuccess(value.getDocuments().get(0));
                    } else {
                        callback.onError(new Exception("No document found"));
                    }
                });
    }

    // Submit an issue
    public void reportIssue(Map<String, Object> issueData, Callback<Void> callback) {
        db.collection("issues").add(issueData)
                .addOnSuccessListener(documentReference -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    // Save proof metadata
    public void saveProofInfo(Map<String, Object> proofData, Callback<Void> callback) {
        db.collection("proofs").add(proofData)
                .addOnSuccessListener(documentReference -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    // Update assignment for reassignment
    public void reassignAdmin(String bookingId, String newAdminId, Callback<Void> callback) {
        db.collection("labAdminSessions")
                .whereEqualTo("bookingId", bookingId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String docId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        db.collection("labAdminSessions").document(docId)
                                .update("labAdminId", newAdminId)
                                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                                .addOnFailureListener(callback::onError);
                    } else {
                        callback.onError(new Exception("Booking not found"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }
}
