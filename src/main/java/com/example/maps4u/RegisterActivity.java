package com.example.maps4u;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dbHelper = new DatabaseHelper(this);

        EditText username = findViewById(R.id.register_username);
        EditText password = findViewById(R.id.register_password);
        EditText confirmPassword = findViewById(R.id.register_confirm_password);
        Button registerButton = findViewById(R.id.register_button);
        TextView errorMessage = findViewById(R.id.error_message);

        registerButton.setOnClickListener(v -> {
            String user = username.getText().toString().trim();
            String pass = password.getText().toString().trim();
            String confirmPass = confirmPassword.getText().toString().trim();

            // Reset error message
            errorMessage.setVisibility(View.GONE);

            // Validare câmpuri goale
            if (user.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                errorMessage.setText("Please fill in all fields!");
                errorMessage.setVisibility(View.VISIBLE);
                return;
            }

            // Verificare dacă parolele coincid
            if (!pass.equals(confirmPass)) {
                errorMessage.setText("Passwords do not match!");
                errorMessage.setVisibility(View.VISIBLE);
                return;
            }

            // Verificare dacă username-ul există deja
            if (dbHelper.getUserPassword(user) != null) {
                errorMessage.setText("Username already exists!");
                errorMessage.setVisibility(View.VISIBLE);
                return;
            }

            // Înregistrare utilizator
            boolean isInserted = dbHelper.insertUser(user, pass, null, "user");
            if (isInserted) {
                Toast.makeText(RegisterActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Închide activitatea de înregistrare
            } else {
                errorMessage.setText("Registration failed. Please try again.");
                errorMessage.setVisibility(View.VISIBLE);
            }
        });
    }
}