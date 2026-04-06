package com.example.campus_space_scheduler.lab_admin_incharge;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campus_space_scheduler.R;
import com.google.android.material.button.MaterialButton;

public class CapturePhotoActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private ImageView ivPreview;
    private MaterialButton btnCapture;
    private String bookingId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_photo);

        ivPreview = findViewById(R.id.ivPreview);
        btnCapture = findViewById(R.id.btnCapturePhoto);
        bookingId = getIntent().getStringExtra("bookingId");

        btnCapture.setOnClickListener(v -> dispatchTakePictureIntent());
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            ivPreview.setImageBitmap(imageBitmap);
            ivPreview.setAlpha(1.0f);
            
            // In a real app, you would upload this bitmap to Firebase Storage here
            Toast.makeText(this, "Photo captured! (Upload logic to be implemented)", Toast.LENGTH_LONG).show();
            
            // For now, let's just finish after a short delay
            btnCapture.setText("Finish");
            btnCapture.setOnClickListener(v -> finish());
        }
    }
}