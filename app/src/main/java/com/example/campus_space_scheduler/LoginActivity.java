package com.example.campus_space_scheduler;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campus_space_scheduler.databinding.AActivityLoginBinding;
import com.example.campus_space_scheduler.helper.LogHelper;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private AActivityLoginBinding binding;
    private FirebaseAuth auth;
    private GoogleSignInClient googleClient;

    private static final int RC_GOOGLE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = AActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        GoogleSignInOptions options =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();

        googleClient = GoogleSignIn.getClient(this, options);

        binding.btnLogin.setOnClickListener(v -> emailLogin());
        binding.btnGoogle.setOnClickListener(v -> googleLogin());
        binding.tvForgotPass.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        binding.tvSignUp.setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));
    }

    // ------------------------------------------------
    // EMAIL LOGIN
    // ------------------------------------------------

    private void emailLogin() {

        String email = binding.etUser.getText().toString().trim();
        String password = binding.etPass.getText().toString().trim();

        if(email.isEmpty() || password.isEmpty()){
            toast("Fields required");
            return;
        }

        auth.signInWithEmailAndPassword(email,password)
                .addOnSuccessListener(result -> verifyUser(result.getUser()))
                .addOnFailureListener(e -> toast("Login failed"));
    }

    // ------------------------------------------------
    // GOOGLE LOGIN
    // ------------------------------------------------

    private void googleLogin(){
        startActivityForResult(googleClient.getSignInIntent(),RC_GOOGLE);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,@Nullable Intent data){
        super.onActivityResult(requestCode,resultCode,data);

        if(requestCode==RC_GOOGLE){

            Task<GoogleSignInAccount> task =
                    GoogleSignIn.getSignedInAccountFromIntent(data);

            try{

                GoogleSignInAccount account = task.getResult(ApiException.class);

                AuthCredential credential =
                        GoogleAuthProvider.getCredential(account.getIdToken(),null);

                auth.signInWithCredential(credential)
                        .addOnSuccessListener(result -> verifyUser(result.getUser()))
                        .addOnFailureListener(e -> toast("Google auth failed"));

            }catch (Exception e){
                toast("Google login failed");
            }
        }
    }

    // ------------------------------------------------
    // VERIFY USER EXISTS IN DATABASE
    // ------------------------------------------------

    private void verifyUser(FirebaseUser user){

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {

                    if(snapshot.exists()){
                        LogHelper.log("LOGIN", user.getEmail() + " logged in");
                        startActivity(new Intent(this, MainActivity.class));
                        finish();

                    }else{

                        user.delete();
                        auth.signOut();
                        toast("User not authorized");

                    }

                })
                .addOnFailureListener(e -> toast("Database error"));
    }

    private void toast(String msg){
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
    }
}