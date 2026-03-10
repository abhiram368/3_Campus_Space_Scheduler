package com.example.hod.staff;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;
import com.example.hod.models.Lab;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LabDetailsActivity extends AppCompatActivity {

    private EditText etLabName, etLabId, etCapacity, etSystems, etLocation, etAdmin;
    private Button btnEdit, btnSave;
    private DatabaseReference mDatabase;
    private static final String LAB_ID = "CS-101"; // Constant for demo, can be dynamic

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab_details);

        mDatabase = FirebaseDatabase.getInstance().getReference("labs").child(LAB_ID);

        etLabName = findViewById(R.id.etLabName);
        etLabId = findViewById(R.id.etLabId);
        etCapacity = findViewById(R.id.etCapacity);
        etSystems = findViewById(R.id.etSystems);
        etLocation = findViewById(R.id.etLocation);
        etAdmin = findViewById(R.id.etLabAdmin);
        btnEdit = findViewById(R.id.btnEdit);
        btnSave = findViewById(R.id.btnSave);

        loadLabDetails();

        btnEdit.setOnClickListener(v -> {
            setEditable(true);
            btnEdit.setVisibility(View.GONE);
            btnSave.setVisibility(View.VISIBLE);
        });

        btnSave.setOnClickListener(v -> {
            saveLabDetails();
        });
    }

    private void loadLabDetails() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Lab lab = dataSnapshot.getValue(Lab.class);
                if (lab != null) {
                    etLabName.setText(lab.name);
                    etLabId.setText(lab.id);
                    etCapacity.setText(lab.capacity);
                    etSystems.setText(lab.systems);
                    etLocation.setText(lab.location);
                    etAdmin.setText(lab.admin);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(LabDetailsActivity.this, "Failed to load lab details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveLabDetails() {
        String name = etLabName.getText().toString();
        String id = etLabId.getText().toString();
        String capacity = etCapacity.getText().toString();
        String systems = etSystems.getText().toString();
        String location = etLocation.getText().toString();
        String admin = etAdmin.getText().toString();

        Lab lab = new Lab(name, id, capacity, systems, location, admin);

        mDatabase.setValue(lab).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                setEditable(false);
                btnSave.setVisibility(View.GONE);
                btnEdit.setVisibility(View.VISIBLE);
                Toast.makeText(LabDetailsActivity.this, "Lab details updated.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(LabDetailsActivity.this, "Update failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setEditable(boolean enabled) {
        etLabName.setEnabled(enabled);
        etLabId.setEnabled(enabled);
        etCapacity.setEnabled(enabled);
        etSystems.setEnabled(enabled);
        etLocation.setEnabled(enabled);
        etAdmin.setEnabled(enabled);
    }
}
