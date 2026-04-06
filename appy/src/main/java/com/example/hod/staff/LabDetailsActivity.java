package com.example.hod.staff;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;
import com.example.hod.models.Space;
import com.example.hod.repository.FirebaseRepository;
import com.example.hod.utils.Result;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Map;

public class LabDetailsActivity extends AppCompatActivity {

    private String labId;
    private TextView tvLabName;
    private DetailItem computersDir, studentsDir, adminsDir, addressDir;
    private Space currentSpace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appy_lab_details);

        // Header Configuration
        View headerView = findViewById(R.id.header_layout);
        if (headerView != null) {
            TextView title = headerView.findViewById(R.id.header_title);
            View btnBack = headerView.findViewById(R.id.btnBack);
            if (title != null) title.setText("Lab Details");
            if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        }

        labId = getIntent().getStringExtra("labId");
        if (labId == null || labId.isEmpty()) {
            Toast.makeText(this, "Error: missing Lab ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvLabName = findViewById(R.id.tvLabName);

        // Initialize Detail Items
        computersDir = new DetailItem(findViewById(R.id.detailComputers), "Number of Computers", "computerCount", InputType.TYPE_CLASS_NUMBER);
        studentsDir = new DetailItem(findViewById(R.id.detailStudents), "Number of Students Allowed", "studentCapacity", InputType.TYPE_CLASS_NUMBER);
        adminsDir = new DetailItem(findViewById(R.id.detailAdmins), "Number of Lab Admins", "labAdminsCount", InputType.TYPE_CLASS_NUMBER);
        addressDir = new DetailItem(findViewById(R.id.detailAddress), "Address of the Lab", "address", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        updateHeader("Lab Details", "View & Edit Specifications");

        loadLabDetails();
        fetchCalculatedAdmins();
    }

    private void updateHeader(String title, String subtitle) {
        TextView tvTitle = findViewById(R.id.header_title);
        TextView tvSubtitle = findViewById(R.id.header_subtitle);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(subtitle);
    }

    private void loadLabDetails() {
        android.util.Log.d("LabDetails", "Loading details for labId: " + labId);
        FirebaseRepository repo = new FirebaseRepository();
        
        // Fetch Room Name from original spaces node (Read-only)
        repo.getSpaceDetails(labId, result -> {
            if (result instanceof Result.Success) {
                Space space = ((Result.Success<Space>) result).data;
                if (space != null) {
                    currentSpace = space;
                    android.util.Log.d("LabDetails", "Fetched name from spaces/: " + space.getRoomName());
                    tvLabName.setText(space.getRoomName() != null ? space.getRoomName() : "Unknown");
                    
                    // Check labDetails node first (source of truth for staff updates)
                    fetchOrInitializeLabDetails(space.getRoomName());
                } else {
                    tvLabName.setText("Unknown Space");
                    fetchOrInitializeLabDetails("Unknown Space");
                }
            } else if (result instanceof Result.Error) {
                String errorMsg = ((Result.Error) result).exception.getMessage();
                Toast.makeText(this, "Error fetching name: " + errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void populateFromSpace(Space space) {
        // Fallback populator
        if (computersDir.tvValue.getText().toString().equals("Not set") && space.getComputerCount() != null) computersDir.setValue(space.getComputerCount());
        if (studentsDir.tvValue.getText().toString().equals("Not set") && space.getCapacity() != null) studentsDir.setValue(space.getCapacity());
        // if (adminsDir.tvValue.getText().toString().equals("Not set") && space.getLabAdminsCount() != null) adminsDir.setValue(space.getLabAdminsCount());
        if (addressDir.tvValue.getText().toString().equals("Not set") && space.getAddress() != null) addressDir.setValue(space.getAddress());
    }

    private void fetchOrInitializeLabDetails(String roomName) {
        com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("labDetails")
                .child(labId)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            com.example.hod.models.LabDetail detail = snapshot.getValue(com.example.hod.models.LabDetail.class);
                            if (detail != null) {
                                populateUI(detail);
                            }
                            // If some fields remain unset, fallback to spaces/ node
                            if (currentSpace != null) populateFromSpace(currentSpace);
                        } else {
                            // Automatically create default data
                            com.example.hod.models.LabDetail defaults = getDefaultLabDetail(roomName);
                            if (defaults != null) {
                                com.google.firebase.database.FirebaseDatabase.getInstance()
                                        .getReference("labDetails")
                                        .child(labId)
                                        .setValue(defaults)
                                        .addOnSuccessListener(aVoid -> {
                                            android.util.Log.d("LabDetails", "Default data created for " + roomName);
                                            populateUI(defaults);
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(LabDetailsActivity.this, "Failed to initialize details: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                        Toast.makeText(LabDetailsActivity.this, "Failed to load lab details: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void fetchCalculatedAdmins() {
        // Count unique users from labAdminsDetails node where spaceId == labId
        com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("labAdminsDetails")
                .orderByChild("spaceId")
                .equalTo(labId)
                .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        java.util.Set<String> uniqueUids = new java.util.HashSet<>();
                        for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                            String uid = child.child("uid").getValue(String.class);
                            if (uid != null) {
                                uniqueUids.add(uid);
                            }
                        }
                        int count = uniqueUids.size();
                        android.util.Log.d("LabDetails", "Calculated admins for " + labId + ": " + count);
                        adminsDir.setValue(String.valueOf(count));
                        // Force hide the edit button for adminsDir
                        adminsDir.btnEdit.setVisibility(View.GONE);
                    }

                    @Override
                    public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                        android.util.Log.e("LabDetails", "Failed to count admins: " + error.getMessage());
                    }
                });
    }

    private void populateUI(com.example.hod.models.LabDetail detail) {
        computersDir.setValue(detail.getComputerCount());
        studentsDir.setValue(detail.getStudentCapacity());
        // adminsDir.setValue(detail.getLabAdminsCount()); // Skip manual count, handled by fetchCalculatedAdmins
        addressDir.setValue(detail.getAddress());
    }

    private com.example.hod.models.LabDetail getDefaultLabDetail(String roomName) {
        if (roomName == null) return null;
        String name = roomName.toUpperCase();
        if (name.contains("SSL")) {
            return new com.example.hod.models.LabDetail("40", "60", "2", "CSE Block First Floor");
        } else if (name.contains("NSL")) {
            return new com.example.hod.models.LabDetail("35", "50", "2", "CSE Block First Floor");
        } else if (name.contains("BDL")) {
            return new com.example.hod.models.LabDetail("45", "65", "3", "CSE Block Ground Floor");
        } else if (name.contains("BCL")) {
            return new com.example.hod.models.LabDetail("30", "45", "2", "CSE Block Ground Floor");
        }
        return new com.example.hod.models.LabDetail("0", "0", "0", "Unknown");
    }

    private class DetailItem {
        View root;
        TextView tvLabel, tvValue;
        EditText etValue;
        ImageButton btnEdit, btnDone, btnClose;
        View editActions;
        String label, firebaseKey;

        DetailItem(View root, String label, String firebaseKey, int inputType) {
            this.root = root;
            this.label = label;
            this.firebaseKey = firebaseKey;

            tvLabel = root.findViewById(R.id.tvLabel);
            tvValue = root.findViewById(R.id.tvValue);
            etValue = root.findViewById(R.id.etValue);
            btnEdit = root.findViewById(R.id.btnAction);
            btnDone = root.findViewById(R.id.btnDone);
            btnClose = root.findViewById(R.id.btnClose);
            editActions = root.findViewById(R.id.editActions);

            tvLabel.setText(label);
            etValue.setInputType(inputType);

            btnEdit.setOnClickListener(v -> setEditMode(true));
            btnClose.setOnClickListener(v -> setEditMode(false));
            btnDone.setOnClickListener(v -> confirmUpdate());
        }

        void setValue(String value) {
            String display = (value == null || value.isEmpty()) ? "Not set" : value;
            tvValue.setText(display);
            etValue.setText(value);
        }

        void setEditMode(boolean editing) {
            tvValue.setVisibility(editing ? View.GONE : View.VISIBLE);
            etValue.setVisibility(editing ? View.VISIBLE : View.GONE);
            btnEdit.setVisibility(editing ? View.GONE : View.VISIBLE);
            editActions.setVisibility(editing ? View.VISIBLE : View.GONE);
            if (editing) {
                etValue.requestFocus();
                etValue.setSelection(etValue.getText().length());
            }
        }

        void confirmUpdate() {
            String newValue = etValue.getText().toString().trim();
            new MaterialAlertDialogBuilder(LabDetailsActivity.this)
                    .setTitle("Update " + label)
                    .setMessage("Are you sure you want to update this detail?")
                    .setPositiveButton("Update", (dialog, which) -> saveToFirebase(newValue))
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        void saveToFirebase(String newValue) {
            com.google.firebase.database.DatabaseReference ref = com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("labDetails")
                    .child(labId)
                    .child(firebaseKey);
            
            android.util.Log.d("LabDetails", "Writing to path: " + ref.toString());
            android.util.Log.d("LabDetails", "Value: " + newValue);

            // Write ONLY to labDetails node (unrestricted)
            // Writing to /spaces/ would fail with Permission Denied for Staff
            ref.setValue(newValue)
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("LabDetails", "Write SUCCESS");
                        setValue(newValue);
                        setEditMode(false);
                        Toast.makeText(LabDetailsActivity.this, label + " updated successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("LabDetails", "Write FAILED: " + e.getMessage());
                        Toast.makeText(LabDetailsActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }
}
