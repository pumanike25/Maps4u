package com.example.maps4u;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdminSettingsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private List<User> userList;
    private EditText editUsername, editPassword, editRole;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_settings);

        dbHelper = new DatabaseHelper(this);
        userId = getIntent().getStringExtra("USERNAME");

        editUsername = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);
        editRole = findViewById(R.id.editRole);

        Button buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> {
            Intent intent = new Intent(AdminSettingsActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });

        Button buttonInsert = findViewById(R.id.buttonInsert);
        buttonInsert.setOnClickListener(v -> {
            String username = editUsername.getText().toString().trim();
            String password = editPassword.getText().toString().trim();
            String role = editRole.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty() || role.isEmpty()) {
                Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show();
            } else {
                boolean isInserted = dbHelper.insertUser(username, password, null, role); // null for imageUrl
                if (isInserted) {
                    Toast.makeText(this, "User inserted successfully!", Toast.LENGTH_SHORT).show();
                    refreshUserList();
                } else {
                    Toast.makeText(this, "Failed to insert user!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button buttonUpdate = findViewById(R.id.buttonUpdate);
        buttonUpdate.setOnClickListener(v -> {
            String username = editUsername.getText().toString().trim();
            String newPassword = editPassword.getText().toString().trim();
            String newRole = editRole.getText().toString().trim();

            if (username.isEmpty() || newPassword.isEmpty() || newRole.isEmpty()) {
                Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show();
            } else {
                boolean isUpdated = dbHelper.updateUser(username, newPassword, newRole);
                if (isUpdated) {
                    Toast.makeText(this, "User updated successfully!", Toast.LENGTH_SHORT).show();
                    refreshUserList();
                } else {
                    Toast.makeText(this, "Failed to update user!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button buttonDelete = findViewById(R.id.buttonDelete);
        buttonDelete.setOnClickListener(v -> {
            String username = editUsername.getText().toString().trim();

            if (username.isEmpty()) {
                Toast.makeText(this, "Please enter a username!", Toast.LENGTH_SHORT).show();
            } else {
                boolean isDeleted = dbHelper.deleteUser(username);
                if (isDeleted) {
                    Toast.makeText(this, "User deleted successfully!", Toast.LENGTH_SHORT).show();
                    refreshUserList();
                } else {
                    Toast.makeText(this, "Failed to delete user!", Toast.LENGTH_SHORT).show();
                }
            }
        });


        recyclerView = findViewById(R.id.recyclerViewUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        refreshUserList();
    }

    private void refreshUserList() {
        userList = dbHelper.getAllUsers();
        userAdapter = new UserAdapter(userList, false);
        recyclerView.setAdapter(userAdapter);
    }
}