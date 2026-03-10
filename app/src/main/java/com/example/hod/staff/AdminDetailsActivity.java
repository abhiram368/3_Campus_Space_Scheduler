package com.example.hod.staff;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;

public class AdminDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_details);

        TextView tvName = findViewById(R.id.tvName);
        TextView tvRoll = findViewById(R.id.tvRoll);
        TextView tvPhone = findViewById(R.id.tvPhone);
        TextView tvEmail = findViewById(R.id.tvEmail);

        if (getIntent() != null) {
            tvName.setText("Name: " + getIntent().getStringExtra("name"));
            tvRoll.setText("Roll No: " + getIntent().getStringExtra("roll"));
            tvPhone.setText("Phone: " + getIntent().getStringExtra("phone"));
            tvEmail.setText("Email: " + getIntent().getStringExtra("email"));
        }
    }
}