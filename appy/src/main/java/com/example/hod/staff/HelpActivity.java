package com.example.hod.staff;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.campussync.appy.R;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        View header = findViewById(R.id.header_layout);
        if (header != null) {
            TextView title = header.findViewById(R.id.header_title);
            TextView subtitle = header.findViewById(R.id.header_subtitle);
            View btnBack = header.findViewById(R.id.btnBack);

            if (title != null) title.setText("Help & About");
            if (subtitle != null) subtitle.setText("App Information");
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> finish());
            }
        }
    }
}
