package com.example.hod.firebase;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseClient {

    private static FirebaseClient instance;
    private final FirebaseDatabase database;

    private FirebaseClient() {
        database = FirebaseDatabase.getInstance();
    }

    public static synchronized FirebaseClient getInstance() {
        if (instance == null) {
            instance = new FirebaseClient();
        }
        return instance;
    }

    public DatabaseReference rootRef() {
        return database.getReference();
    }

    public DatabaseReference bookingsRef() {
        return rootRef().child(FirebasePaths.BOOKINGS);
    }

    public DatabaseReference schedulesRef() {
        return rootRef().child(FirebasePaths.SCHEDULES);
    }

    public DatabaseReference spacesRef() {
        return rootRef().child(FirebasePaths.SPACES);
    }

    public DatabaseReference usersRef() {
        return rootRef().child(FirebasePaths.USERS);
    }
}

