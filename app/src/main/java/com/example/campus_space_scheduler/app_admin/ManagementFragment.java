package com.example.campus_space_scheduler.app_admin;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.campus_space_scheduler.R;
import com.example.campus_space_scheduler.databinding.AActivityAdminManagementBinding;
import com.example.campus_space_scheduler.databinding.ADialogAddItemBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Objects;

public class ManagementFragment extends Fragment {

    private AActivityAdminManagementBinding binding;
    private DatabaseReference dbRef;
    private String mode;
    private FirebaseAuth mAuth;

    private final ActivityResultLauncher<Intent> csvPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            handleCsvFile(result.getData().getData());
                        }
                    }
            );

    public static ManagementFragment newInstance(String mode) {
        ManagementFragment fragment = new ManagementFragment();
        Bundle args = new Bundle();
        args.putString("MODE", mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = AActivityAdminManagementBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();
        mode = getArguments() != null ? getArguments().getString("MODE") : "USER";
        dbRef = FirebaseDatabase.getInstance()
                .getReference(mode.equals("USER") ? "users" : "spaces");

        setupUI();
        setupBackNavigation();
        return binding.getRoot();
    }

    private void setupUI() {
        binding.btnUploadCsv.setOnClickListener(v -> openCsvPicker());
        binding.btnAddSingle.setOnClickListener(v -> showAddDialog());
        binding.btnViewAll.setOnClickListener(v -> openTable("All"));

        if (mode.equals("USER")) {
            binding.listHeaderTitle.setText("User Directory");
            String[] userCategories = {"Student", "Faculty", "Hall Incharge", "Staff Incharge", "CSED Staff", "HoD", "Faculty Incharge", "Lab admin", "App admin"};
            populateCategoryButtons(userCategories);
        } else {
            binding.listHeaderTitle.setText("Space Directory");
            String[] spaceCategories = {"Lab", "Hall", "Classroom"};
            populateCategoryButtons(spaceCategories);
        }
    }

    private void populateCategoryButtons(String[] categories) {
        binding.categoryContainer.removeAllViews();
        for (String category : categories) {
            addCategoryChip(category);
        }
    }

    private void addCategoryChip(String title) {
        MaterialButton chip = (MaterialButton) getLayoutInflater()
                .inflate(R.layout.a_item_category_chip, binding.categoryContainer, false);
        chip.setText(title);

        // Reset layout params to wrap_content so they don't stretch unnaturally
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        chip.setLayoutParams(params);

        chip.setOnClickListener(v -> openTable(title));
        binding.categoryContainer.addView(chip);
    }

    private void openTable(String role) {
        Intent intent = new Intent(requireContext(), UserTableActivity.class);
        intent.putExtra("FILTER_ROLE", role);
        intent.putExtra("DB_NODE", mode);
        startActivity(intent);
    }

    private void showAddDialog() {
        ADialogAddItemBinding dBinding = ADialogAddItemBinding.inflate(getLayoutInflater());

        if ("USER".equals(mode)) {
            // Hints for User Mode
            dBinding.layoutField1.setHint("Full Name");
            dBinding.layoutField2.setHint("College Email ID");
            dBinding.layoutField3.setHint("Phone Number");
            dBinding.layoutField4.setHint("Roll Number");

            // Ensure all are visible
            dBinding.layoutField3.setVisibility(View.VISIBLE);
            dBinding.layoutField4.setVisibility(View.VISIBLE);

            // Correct InputTypes for better UX
            dBinding.editField3.setInputType(InputType.TYPE_CLASS_PHONE);
            dBinding.editField4.setInputType(InputType.TYPE_CLASS_TEXT);
        } else {
            // Hints for Space Mode
            dBinding.layoutField1.setHint("Room Name (e.g., Lab 1)");
            dBinding.layoutField2.setHint("Capacity (e.g., 60)");

            // Hide unused fields for Rooms
            dBinding.layoutField3.setVisibility(View.GONE);
            dBinding.layoutField4.setVisibility(View.GONE);
        }

        String[] options = mode.equals("USER")
                ? new String[]{"Student", "Faculty", "Hall Incharge", "Staff Incharge", "CSED Staff", "HoD", "Faculty Incharge", "Lab admin", "App admin"}
                : new String[]{"Lab", "Hall", "Classroom", "Conference Room"};

        dBinding.roleDropdown.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, options));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add New " + mode)
                .setView(dBinding.getRoot())
                .setPositiveButton("Save", (d, w) -> {
                    String f1 = Objects.requireNonNull(dBinding.editField1.getText()).toString().trim();
                    String f2 = Objects.requireNonNull(dBinding.editField2.getText()).toString().trim();
                    String f3 = Objects.requireNonNull(dBinding.editField3.getText()).toString().trim();
                    String f4 = Objects.requireNonNull(dBinding.editField4.getText()).toString().trim();
                    String role = dBinding.roleDropdown.getText().toString();

                    if ("USER".equals(mode)) {
                        createAuthUserAndSaveToDb(f1, f2, f3, f4, role);
                    } else {
                        String key = dbRef.push().getKey();
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("roomName", f1);
                        map.put("capacity", f2);
                        map.put("role", role);
                        if (key != null) dbRef.child(key).setValue(map);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * THE REWRITTEN LOGIC
     * We use a dedicated temporary auth instance to avoid disrupting the current Admin session.
     */
    private void createAuthUserAndSaveToDb(String name, String email, String phone, String rollNo, String role) {

        FirebaseApp secondaryApp;

        try {
            secondaryApp = FirebaseApp.getInstance("Secondary");
        } catch (IllegalStateException e) {
            secondaryApp = FirebaseApp.initializeApp(
                    requireContext(),
                    FirebaseApp.getInstance().getOptions(),
                    "Secondary"
            );
        }

        FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);

        secondaryAuth.createUserWithEmailAndPassword(email, rollNo)
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        Toast.makeText(requireContext(),
                                "Auth error: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser newUser = task.getResult().getUser();
                    if (newUser == null) return;

                    String uid = newUser.getUid();

                    HashMap<String, Object> userData = new HashMap<>();
                    userData.put("name", name);
                    userData.put("emailId", email);
                    userData.put("phoneNumber", phone);
                    userData.put("rollNo", rollNo);
                    userData.put("role", role);
                    userData.put("uid", uid);
                    userData.put("passwordChanged", false);

                    dbRef.child(uid).setValue(userData)
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(requireContext(),
                                            "User added",
                                            Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(),
                                            "DB error: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show()
                            );
                });
    }

    private void openCsvPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/*");
        csvPickerLauncher.launch(intent);
    }

    private void handleCsvFile(Uri uri) {
        if (uri == null) return;

        ProgressBar progressBar = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Batch Processing...")
                .setView(progressBar)
                .setCancelable(false)
                .create();
        dialog.show();

        new Thread(() -> {
            try {
                InputStream is = requireContext().getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line;

                while ((line = reader.readLine()) != null) {
                    String[] p = line.split(",");
                    if (p.length < 2) continue;

                    // THREAD SLEEP: Important to avoid the "Device Blocked" lockout
                    Thread.sleep(1500);

                    if ("USER".equals(mode) && p.length >= 5) {
                        requireActivity().runOnUiThread(() ->
                                createAuthUserAndSaveToDb(p[0].trim(), p[1].trim(), p[2].trim(), p[3].trim(), p[4].trim())
                        );
                    } else if (!"USER".equals(mode)) {
                        String key = dbRef.push().getKey();
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("roomName", p[0].trim());
                        map.put("capacity", p[1].trim());
                        map.put("role", p.length >= 3 ? p[2].trim() : "Classroom");
                        if (key != null) dbRef.child(key).setValue(map);
                    }
                }
                if (isAdded()) requireActivity().runOnUiThread(dialog::dismiss);
            } catch (Exception e) {
                if (isAdded()) requireActivity().runOnUiThread(dialog::dismiss);
            }
        }).start();
    }

    private void setupBackNavigation() {
        binding.btnBackToCategories.setOnClickListener(v -> showCategoryLayer());
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.tableResultsUI.getVisibility() == View.VISIBLE) {
                    showCategoryLayer();
                } else {
                    setEnabled(false);
                    requireActivity().onBackPressed();
                }
            }
        });
    }

    private void showCategoryLayer() {
        binding.tableResultsUI.setVisibility(View.GONE);
        binding.mainManagementUI.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}