package com.example.hod.staff;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;
import com.example.hod.models.User;
import com.example.hod.repository.FirebaseRepository;
import com.example.hod.utils.Result;
import com.google.android.material.button.MaterialButton;

public class AddLabAdminActivity extends AppCompatActivity {

    private String labId;
    private EditText etSearch;
    private android.widget.LinearLayout userResultsContainer;
    private TextView tvResultLabel;
    private MaterialButton btnAddAsAdmin;
    private User selectedUser;
    private java.util.List<User> fullUserList = new java.util.ArrayList<>();

    // Search Performance Helpers
    private final android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable searchRunnable;
    private long lastSearchTimestamp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_lab_admin);

        labId = getIntent().getStringExtra("labId");
        if (labId == null) {
            finish();
            return;
        }

        etSearch = findViewById(R.id.etSearchName);
        userResultsContainer = findViewById(R.id.userResultsContainer);
        tvResultLabel = findViewById(R.id.tvResultLabel);
        btnAddAsAdmin = findViewById(R.id.btnAddAsAdmin);

        // Header Configuration
        View headerView = findViewById(R.id.header_layout);
        if (headerView != null) {
            TextView title = headerView.findViewById(R.id.header_title);
            TextView subtitle = headerView.findViewById(R.id.header_subtitle);
            View btnBack = headerView.findViewById(R.id.btnBack);
            
            if (title != null) title.setText("Add Lab Admin");
            if (subtitle != null) subtitle.setText("Find & Nominate Student");
            if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        }

        // Real-time Search with Debouncing
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                
                String query = s.toString().trim();
                searchRunnable = () -> {
                    long timestamp = System.currentTimeMillis();
                    lastSearchTimestamp = timestamp;
                    searchUsers(query, timestamp);
                };
                searchHandler.postDelayed(searchRunnable, 300); // 300ms Debounce
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        btnAddAsAdmin.setOnClickListener(v -> addAsAdmin());
        
        fetchStudents();
    }

    private void updateHeader(String title, String subtitle) {
        View headerView = findViewById(R.id.header_layout);
        if (headerView != null) {
            TextView tvTitle = headerView.findViewById(R.id.header_title);
            TextView tvSubtitle = headerView.findViewById(R.id.header_subtitle);
            if (tvTitle != null) tvTitle.setText(title);
            if (tvSubtitle != null) tvSubtitle.setText(subtitle);
        }
    }

    private void fetchStudents() {
        try {
            FirebaseRepository repo = new FirebaseRepository();
            repo.usersRef().orderByChild("role").equalTo("student")
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        fullUserList.clear();
                        for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                            User u = child.getValue(User.class);
                            if (u != null) {
                                u.uid = child.getKey();
                                fullUserList.add(u);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                        android.util.Log.e("AddAdmin", "Error fetching students: " + error.getMessage());
                    }
                });
        } catch (Exception e) {
            android.util.Log.e("AddAdmin", "Error initiating student fetch", e);
        }
    }

    private void filter(String text) {
        java.util.List<User> filteredList = new java.util.ArrayList<>();
        
        // Make sure the search text isn't null
        String query = text != null ? text.toLowerCase().trim() : "";
        
        if (query.isEmpty()) {
            userResultsContainer.removeAllViews();
            userResultsContainer.setVisibility(View.GONE);
            tvResultLabel.setVisibility(View.GONE);
            btnAddAsAdmin.setVisibility(View.GONE);
            selectedUser = null;
            return;
        }

        for (User user : fullUserList) {
            // Null-safe checks for every single field
            // Using getters we just added to User.java
            String name = user.getName() != null ? user.getName().toLowerCase() : "";
            String email = user.getEmailId() != null ? user.getEmailId().toLowerCase() : "";
            String roll = user.getRollNo() != null ? user.getRollNo().toLowerCase() : "";
            
            if (name.contains(query) || email.contains(query) || roll.contains(query)) {
                filteredList.add(user);
            }
        }

        // Update UI
        runOnUiThread(() -> {
            userResultsContainer.removeAllViews();
            selectedUser = null;
            btnAddAsAdmin.setVisibility(View.GONE);

            if (!filteredList.isEmpty()) {
                tvResultLabel.setVisibility(View.VISIBLE);
                tvResultLabel.setText(filteredList.size() + (filteredList.size() == 1 ? " STUDENT FOUND" : " STUDENTS FOUND"));
                displaySearchResults(filteredList);
            } else {
                tvResultLabel.setVisibility(View.VISIBLE);
                tvResultLabel.setText("NO STUDENTS FOUND");
                userResultsContainer.setVisibility(View.GONE);
            }
        });
    }

    private void searchUsers(String query, long requestTimestamp) {
        // Redirecting to the new null-safe local filter
        filter(query);
    }

    private void hideKeyboard() {
        try {
            android.view.View view = this.getCurrentFocus();
            if (view != null) {
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("AddAdmin", "Error hiding keyboard", e);
        }
    }

    private void displaySearchResults(java.util.List<User> users) {
        try {
            userResultsContainer.setVisibility(View.VISIBLE);
            
            for (User user : users) {
                View card = getLayoutInflater().inflate(R.layout.item_admin_card, userResultsContainer, false);
                
                TextView tvName = card.findViewById(R.id.tvAdminName);
                TextView tvRoll = card.findViewById(R.id.tvAdminRoll);
                
                if (tvName != null) tvName.setText(user.name != null ? user.name : "N/A");
                if (tvRoll != null) tvRoll.setText(user.getRoleLabel() + " • " + (user.rollNo != null ? user.rollNo : "N/A"));

                card.setOnClickListener(v -> {
                    try {
                        // Clear previous selections and reset button
                        for (int i = 0; i < userResultsContainer.getChildCount(); i++) {
                            View c = userResultsContainer.getChildAt(i);
                            c.setActivated(false);
                            if (c instanceof com.google.android.material.card.MaterialCardView) {
                                ((com.google.android.material.card.MaterialCardView) c).setStrokeColor(
                                        android.graphics.Color.TRANSPARENT);
                            }
                        }
                        
                        // Select this card
                        selectedUser = user;
                        card.setActivated(true);
                        if (card instanceof com.google.android.material.card.MaterialCardView) {
                            ((com.google.android.material.card.MaterialCardView) card).setStrokeColor(
                                    getResources().getColor(R.color.primary_blue));
                            ((com.google.android.material.card.MaterialCardView) card).setStrokeWidth(4);
                        }

                        // Move button right below the selected card
                        if (btnAddAsAdmin.getParent() != null) {
                            ((android.view.ViewGroup) btnAddAsAdmin.getParent()).removeView(btnAddAsAdmin);
                        }
                        
                        // Add button after current card index
                        int index = userResultsContainer.indexOfChild(card);
                        userResultsContainer.addView(btnAddAsAdmin, index + 1);
                        btnAddAsAdmin.setVisibility(View.VISIBLE);
                        
                        // Smooth scroll
                        btnAddAsAdmin.post(() -> {
                            try {
                                View parent = (View) userResultsContainer.getParent();
                                if (parent instanceof android.widget.ScrollView) {
                                    ((android.widget.ScrollView) parent).smoothScrollTo(0, btnAddAsAdmin.getBottom());
                                } else if (parent.getParent() instanceof android.widget.ScrollView) {
                                    ((android.widget.ScrollView) parent.getParent()).smoothScrollTo(0, btnAddAsAdmin.getBottom());
                                }
                            } catch (Exception e) {
                                android.util.Log.e("AddAdmin", "Error scrolling to button", e);
                            }
                        });
                    } catch (Exception e) {
                        android.util.Log.e("AddAdmin", "Error handling card click", e);
                    }
                });
                
                userResultsContainer.addView(card);
            }
        } catch (Exception e) {
            android.util.Log.e("AddAdmin", "Error inflating search results", e);
        }
    }

    private void addAsAdmin() {
        if (selectedUser == null) return;

        android.util.Log.d("AddAdmin", "Attempting to add user. Name: " + selectedUser.name + ", UID: " + selectedUser.uid + ", LabId: " + labId);

        if ("labAdmin".equalsIgnoreCase(selectedUser.role)) {
            Toast.makeText(this, "Already Lab Admin", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress on button
        btnAddAsAdmin.setEnabled(false);
        btnAddAsAdmin.setText("GENERATING SCHEDULE...");

        try {
            FirebaseRepository repo = new FirebaseRepository();
            repo.updateToLabAdmin(selectedUser.uid, labId, result -> {
                runOnUiThread(() -> {
                    try {
                        if (result instanceof Result.Success) {
                            android.util.Log.d("AddAdmin", "Update successful");
                            Toast.makeText(this, "Lab Admin Added & Schedule Generated", Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            btnAddAsAdmin.setEnabled(true);
                            btnAddAsAdmin.setText("Add as Lab Admin");
                            Exception e = ((Result.Error<?>) result).exception;
                            String msg = e != null ? e.getMessage() : "Unknown error";
                            android.util.Log.e("AddAdmin", "Update failed: " + msg);
                            Toast.makeText(this, "Failed to add lab admin: " + msg, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        android.util.Log.e("AddAdmin", "Error in add admin UI update", e);
                    }
                });
            });
        } catch (Exception e) {
            btnAddAsAdmin.setEnabled(true);
            btnAddAsAdmin.setText("Add as Lab Admin");
            android.util.Log.e("AddAdmin", "Error initiating admin update", e);
        }
    }
}
