package com.example.hod.hod;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;

public class ViewDocumentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_document);

        updateHeader("View Document", "");
        
        String imageUrl = getIntent().getStringExtra("imageUrl");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            android.widget.ImageView ivDocument = findViewById(R.id.ivDocument);
            // In a real app, use Glide/Picasso. For now, we assume it's a placeholder or local resource handled elsewhere.
        }
    }

    private void updateHeader(String title, String subtitle) {
        TextView tvTitle = findViewById(R.id.header_title);
        TextView tvSubtitle = findViewById(R.id.header_subtitle);
        View btnBack = findViewById(R.id.btnBack);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(subtitle);
        if (tvSubtitle != null && subtitle.isEmpty()) tvSubtitle.setVisibility(View.GONE);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }
}