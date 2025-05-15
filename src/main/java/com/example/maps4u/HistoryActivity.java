package com.example.maps4u;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class HistoryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(HistoryActivity.this, HomeActivity.class);
            intent.putExtra("USERNAME", getIntent().getStringExtra("USERNAME"));
            intent.putExtra("ROLE", getIntent().getStringExtra("ROLE"));
            startActivity(intent);
            finish();
        });

        try {
            RecyclerView recyclerView = findViewById(R.id.history_recycler_view);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            String username = getIntent().getStringExtra("USERNAME");
            if (username != null) {
                DatabaseHelper dbHelper = new DatabaseHelper(this);
                int userId = dbHelper.getUserId(username);

                if (userId != -1) {
                    Cursor cursor = dbHelper.getUserHistory(userId);
                    if (cursor != null && cursor.getCount() > 0) {
                        HistoryAdapter adapter = new HistoryAdapter(cursor);
                        recyclerView.setAdapter(adapter);
                    } else {
                        Toast.makeText(this, "No history found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "User not identified", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading history", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}