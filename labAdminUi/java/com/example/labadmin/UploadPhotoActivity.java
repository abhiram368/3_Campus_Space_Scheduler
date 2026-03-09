package com.example.labadmin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class UploadPhotoActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 101;
    private static final int CAMERA_PERMISSION_CODE = 102;

    private ImageView imgPreview;
    private MaterialButton btnCapture;
    private Bitmap photo;

    private FirestoreRepository repository;
    private FirebaseStorage storage;
    private String currentUserId;
    private String bookingId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_photo);

        repository = new FirestoreRepository();
        storage = FirebaseStorage.getInstance();
        
        bookingId = getIntent().getStringExtra("bookingId");
        if (bookingId == null) {
            Toast.makeText(this, "No booking ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            currentUserId = "admin_001";
        }

        imgPreview = findViewById(R.id.imgPreview);
        btnCapture = findViewById(R.id.btnCapture);

        btnCapture.setOnClickListener(v -> checkCameraPermission());
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK && data != null) {
            photo = (Bitmap) data.getExtras().get("data");
            imgPreview.setImageBitmap(photo);
            uploadToFirebaseStorage(photo);
        }
    }

    private void uploadToFirebaseStorage(Bitmap bitmap) {
        Toast.makeText(this, "Uploading Proof...", Toast.LENGTH_SHORT).show();

        long timestamp = System.currentTimeMillis();
        String fileName = "proof_" + timestamp + ".jpg";
        StorageReference storageRef = storage.getReference().child("proofs/" + bookingId + "/" + fileName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = storageRef.putBytes(data);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                saveUploadInfo(uri.toString(), timestamp);
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void saveUploadInfo(String downloadUrl, long timestamp) {
        Map<String, Object> proofData = new HashMap<>();
        proofData.put("bookingId", bookingId);
        proofData.put("imageUrl", downloadUrl);
        proofData.put("uploadedBy", currentUserId);
        proofData.put("timestamp", timestamp);

        repository.saveProofInfo(proofData, new FirestoreRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(UploadPhotoActivity.this, "Proof Uploaded Successfully!", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(UploadPhotoActivity.this, "Error saving proof: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
