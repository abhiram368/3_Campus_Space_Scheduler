package com.example.hod.utils;

import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class FirebaseUtils {

    public static void setCurrentSpaceId(String spaceId) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference appConfigRef = database.getReference("appConfig");

        appConfigRef.child("currentSpaceId").setValue(spaceId)
                .addOnSuccessListener(aVoid -> Log.d("FirebaseUtils", "currentSpaceId updated"))
                .addOnFailureListener(e -> Log.e("FirebaseUtils", "Error updating currentSpaceId", e));
    }

    public static void ensureApprovalHierarchy() {

        DatabaseReference ref =
                FirebaseDatabase.getInstance().getReference("approvalHierarchy");

        ref.get().addOnSuccessListener(snapshot -> {

            if (!snapshot.exists()) {

                Map<String,Object> labs = new HashMap<>();
                labs.put("HoD",1);
                labs.put("Faculty Incharge",2);
                labs.put("Staff Incharge",3);

                Map<String,Object> halls = new HashMap<>();
                halls.put("HoD",1);
                halls.put("CSED Staff",2);
                halls.put("Hall Incharge",2);

                Map<String,Object> root = new HashMap<>();
                root.put("labs",labs);
                root.put("halls",halls);

                ref.setValue(root);
            }
        });
    }
}