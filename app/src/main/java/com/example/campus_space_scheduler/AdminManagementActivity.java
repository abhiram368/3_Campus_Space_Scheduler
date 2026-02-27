package com.example.campus_space_scheduler;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campus_space_scheduler.databinding.ActivityAdminManagementBinding;
import com.example.campus_space_scheduler.databinding.DialogAddItemBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AdminManagementActivity extends AppCompatActivity {

    private ActivityAdminManagementBinding binding;
    private DatabaseReference dbRef;
    private String mode; // "USER" or "SPACE"

    private final ActivityResultLauncher<Intent> csvPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    handleCsvFile(result.getData().getData());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mode = getIntent().getStringExtra("MANAGEMENT_MODE");
        if (mode == null) mode = "USER";

        dbRef = FirebaseDatabase.getInstance().getReference(mode.equals("USER") ? "users" : "spaces");
        setupUI();
    }

    private void setupUI() {
        binding.toolbar.setTitle(mode.equals("USER") ? "User Management" : "Space Management");
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Basic Actions
        binding.layoutActions.btnUploadCsv.setOnClickListener(v -> openCsvPicker());
        binding.layoutActions.btnAddSingle.setOnClickListener(v -> showAddDialog());
        binding.layoutActions.btnEditDetails.setVisibility(View.GONE);

        if (mode.equals("USER")) {
            binding.listHeaderTitle.setText("User Categories");

            // Your complete list of categories
            String[] userCats = {
                    "Student", "Faculty", "Hall Incharge", "Staff Incharge",
                    "CSED Staff", "HoD", "Faculty Incharge", "Lab admin", "App admin"
            };

            populateCategoryButtons(userCats);
        } else {
            binding.listHeaderTitle.setText("Space Categories");
            String[] spaceCats = {"Lab", "Hall", "Classroom"};
            populateCategoryButtons(spaceCats);
        }

        binding.btnViewAll.setOnClickListener(v -> openUserTable("All"));
    }

    private void populateCategoryButtons(String[] categories) {
        // Clear the container first to avoid duplicates
        binding.categoryContainer.removeAllViews();

        for (String cat : categories) {
            // Create a Tonal Button programmatically
            Button btn = new Button(this, null, com.google.android.material.R.attr.materialButtonStyle);
            btn.setText(cat);
            // Add this inside your button creation loop
            btn.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary));
            btn.setAllCaps(false); // Keeps text casing natural

            // Set layout parameters for vertical stacking
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 12); // Add space between buttons
            btn.setLayoutParams(params);

            // Standard listener for all buttons
            btn.setOnClickListener(v -> openUserTable(cat));

            binding.categoryContainer.addView(btn);
        }
    }

    private void openCsvPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/*");
        csvPickerLauncher.launch(intent);
    }

    private void handleCsvFile(Uri uri) {
        if (uri == null) return;

        MaterialAlertDialogBuilder loadingBuilder = new MaterialAlertDialogBuilder(this);
        loadingBuilder.setTitle("Processing CSV");
        loadingBuilder.setMessage("Starting upload...");
        loadingBuilder.setCancelable(false);

        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(false);
        loadingBuilder.setView(progressBar);

        androidx.appcompat.app.AlertDialog loadingDialog = loadingBuilder.create();
        loadingDialog.show();

        // Use single-element arrays to allow modification inside the thread
        final int[] currentCount = {0};
        final int[] addedCount = {0};
        final int[] skippedCount = {0};

        new Thread(() -> {
            try {
                long totalLines = countLines(uri);
                progressBar.setMax((int) totalLines);

                InputStream is = getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Inside the while loop of handleCsvFile
                    String[] p = line.split(",");
                    if (p.length >= 2) {
                        String col1 = p[0].trim(); // Name
                        String col2 = p[1].trim(); // Identifier

                        // Improved role detection to support all 9 categories
                        String col3;
                        if (p.length >= 3 && !p[2].trim().isEmpty()) {
                            col3 = p[2].trim(); // Use the specific role from CSV (e.g., "HoD")
                        } else {
                            // Only use fallback if the CSV column is actually empty
                            col3 = mode.equals("USER") ? "Student" : "Classroom";
                        }

                        // Check for Duplicates
                        if (!checkIfExistsSync(col2, col3)) {
                            saveToFirebase(col1, col2, col3);
                            addedCount[0]++;
                        } else {
                            skippedCount[0]++;
                        }
                    }

                    currentCount[0]++; // Increment the value inside the array

                    // Final copies for the runOnUiThread lambda
                    int progress = currentCount[0];
                    int added = addedCount[0];
                    int skipped = skippedCount[0];

                    runOnUiThread(() -> {
                        progressBar.setProgress(progress);
                        loadingDialog.setMessage("Processed: " + progress + "/" + totalLines +
                                "\nAdded: " + added + " | Skipped: " + skipped);
                    });
                }

                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    Toast.makeText(this, "Finished! Added: " + addedCount[0] + ", Skipped: " + skippedCount[0], Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // Helper to count lines for the progress bar
    private long countLines(Uri uri) throws Exception {
        InputStream is = getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        long lines = 0;
        while (reader.readLine() != null) lines++;
        reader.close();
        return lines;
    }

    // Logic to check duplicates
    // Updated Duplicate Check in AdminManagementActivity.java
    private boolean checkIfExistsSync(String uniqueId, String role) {
        final boolean[] exists = {false};
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(mode.equals("USER") ? "users" : "spaces");

        String idField = mode.equals("USER") ? "identifier" : "roomName";

        try {
            // Query for the ID first
            com.google.android.gms.tasks.Task<DataSnapshot> task = ref.orderByChild(idField).equalTo(uniqueId).get();
            DataSnapshot snapshot = com.google.android.gms.tasks.Tasks.await(task);

            if (snapshot.exists()) {
                // Loop through entries with that ID to see if any have the same Role
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String existingRole = ds.child("role").getValue(String.class);
                    if (existingRole != null && existingRole.equalsIgnoreCase(role)) {
                        exists[0] = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            exists[0] = false;
        }
        return exists[0];
    }

    private void showAddDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        DialogAddItemBinding dBinding = DialogAddItemBinding.inflate(getLayoutInflater());

        if ("USER".equals(mode)) {
            dBinding.layoutField1.setHint("Name");
            dBinding.layoutField2.setHint("Email/ID");

            String[] roles = {"Student", "Faculty", "Hall Incharge", "Lab Incharge",
                    "CSED Staff", "HoD", "Faculty Incharge", "Lab admin", "App admin"};
            dBinding.roleDropdown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, roles));
        } else {
            dBinding.layoutField1.setHint("Room Name");
            dBinding.layoutField2.setHint("Capacity");
            String[] spaceTypes = {"Lab", "Hall", "Classroom"};
            dBinding.roleDropdown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, spaceTypes));
        }

        builder.setView(dBinding.getRoot())
                .setTitle("Add " + mode)
                .setPositiveButton("Save", (d, w) -> {
                    saveToFirebase(
                            dBinding.editField1.getText().toString(),
                            dBinding.editField2.getText().toString(),
                            dBinding.roleDropdown.getText().toString()
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveToFirebase(String n, String i, String r) {
        String key = dbRef.push().getKey();
        Map<String, Object> map = new HashMap<>();
        if (mode.equals("USER")) {
            map.put("name", n); map.put("identifier", i); map.put("role", r);
        } else {
            map.put("roomName", n); map.put("capacity", i); map.put("role", r);
        }
        if (key != null) dbRef.child(key).setValue(map);
    }

    private void openUserTable(String role) {
        Intent intent = new Intent(this, UserTableActivity.class);
        intent.putExtra("FILTER_ROLE", role);
        intent.putExtra("DB_NODE", mode);
        startActivity(intent);
    }
}