package com.example.campus_space_scheduler;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "LoginActivity";
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        MaterialButton googleLoginButton = findViewById(R.id.buttonLoginViaGoogle);
        if (googleLoginButton != null) {
            googleLoginButton.setOnClickListener(v -> signIn());
        }
    }

    private void signIn() {
        Log.d(TAG, "signIn: Initiating sign in process");
        mAuth.signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            mGoogleSignInClient.revokeAccess().addOnCompleteListener(this, task2 -> {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            });
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    Log.d(TAG, "Google Sign-In successful for: " + account.getEmail());
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Log.e(TAG, "Google sign in failed. Status Code: " + e.getStatusCode() + " Message: " + e.getMessage());
                Toast.makeText(this, "Google sign in failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d(TAG, "Firebase Auth successful for: " + (user != null ? user.getUid() : "null"));
                        verifyUserInDatabase(user);
                    } else {
                        Log.e(TAG, "Firebase Auth Failed", task.getException());
                        Toast.makeText(this, "Firebase Auth Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void verifyUserInDatabase(FirebaseUser user) {
        if (user == null) return;

        String uid = user.getUid();
        String email = user.getEmail();
        Log.d(TAG, "Verifying user in database via UID: " + uid);

        if (email != null && email.endsWith("@nitc.ac.in")) {
            // Access the specific user node directly using UID
            DatabaseReference userNodeRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
            
            userNodeRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Log.d(TAG, "Database check returned. Snapshot exists: " + snapshot.exists());
                    if (snapshot.exists()) {
                        String role = snapshot.child("role").getValue(String.class);
                        if (role == null) role = "student";
                        
                        Boolean isBlocked = snapshot.child("isBlocked").getValue(Boolean.class);
                        Log.d(TAG, "User profile loaded. Role: " + role + ", isBlocked: " + isBlocked);
                        
                        if (isBlocked != null && isBlocked) {
                            Toast.makeText(LoginActivity.this, "Your account is blocked.", Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                            mGoogleSignInClient.signOut();
                        } else {
                            Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                            navigateToDashboard(role);
                        }
                    } else {
                        Log.w(TAG, "No user record found in database for UID: " + uid);
                        Toast.makeText(LoginActivity.this, "User not registered in database.", Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        mGoogleSignInClient.signOut();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Database query FAILED (check rules). Error: " + error.getMessage());
                    Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            Log.w(TAG, "Non-NITC email attempted login: " + email);
            Toast.makeText(this, "Please use your NITC Gmail", Toast.LENGTH_LONG).show();
            mAuth.signOut();
            mGoogleSignInClient.signOut();
        }
    }

    private void navigateToDashboard(String role) {
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        intent.putExtra("ROLE", role);
        startActivity(intent);
        finish();
    }
}
