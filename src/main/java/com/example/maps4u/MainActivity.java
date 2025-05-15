package com.example.maps4u;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        EditText username = findViewById(R.id.username);
        EditText password = findViewById(R.id.password);
        Button loginButton = findViewById(R.id.login_button);
        Button registerButton = findViewById(R.id.register_button);

        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        loginButton.setOnClickListener(v -> {
            String user = username.getText().toString().trim();
            String pass = password.getText().toString().trim();

            String role = dbHelper.checkUser(user, pass);
            if (role != null) {
                int userId = dbHelper.getUserId(user);

                Toast.makeText(MainActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                intent.putExtra("USERNAME", user);
                intent.putExtra("ROLE", role);
                intent.putExtra("USER_ID", userId);

                startActivity(intent);
                finish();
            } else {
                Toast.makeText(MainActivity.this, "Invalid username or password!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}