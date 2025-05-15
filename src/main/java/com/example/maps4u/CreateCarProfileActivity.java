package com.example.maps4u;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

public class CreateCarProfileActivity extends AppCompatActivity {

    private EditText carNameEditText;
    private EditText carYearEditText;
    private EditText fuelConsumptionEditText;
    private Spinner fuelTypeSpinner;
    private Spinner co2CategorySpinner;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_car_profile);

        carNameEditText = findViewById(R.id.carNameEditText);
        carYearEditText = findViewById(R.id.carYearEditText);
        fuelConsumptionEditText = findViewById(R.id.fuelConsumptionEditText);
        fuelTypeSpinner = findViewById(R.id.fuelTypeSpinner);
        co2CategorySpinner = findViewById(R.id.co2CategorySpinner);
        saveButton = findViewById(R.id.saveButton);

        // Set up fuel type spinner
        ArrayAdapter<CharSequence> fuelAdapter = ArrayAdapter.createFromResource(this,
                R.array.fuel_types, android.R.layout.simple_spinner_item);
        fuelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fuelTypeSpinner.setAdapter(fuelAdapter);

        // Set up CO2 category spinner
        ArrayAdapter<CharSequence> co2Adapter = ArrayAdapter.createFromResource(this,
                R.array.co2_categories, android.R.layout.simple_spinner_item);
        co2Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        co2CategorySpinner.setAdapter(co2Adapter);

        saveButton.setOnClickListener(v -> saveCarProfile());
    }

    private void saveCarProfile() {
        String carName = carNameEditText.getText().toString().trim();
        String carYear = carYearEditText.getText().toString().trim();
        String fuelConsumptionStr = fuelConsumptionEditText.getText().toString().trim();
        String fuelType = fuelTypeSpinner.getSelectedItem().toString();

        if (carName.isEmpty() || carYear.isEmpty() || fuelConsumptionStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double fuelConsumption = Double.parseDouble(fuelConsumptionStr);
            double co2Emissions = calculateCo2Emissions(fuelConsumption, fuelType);

            // Create car with all parameters
            Car customCar = new Car("Custom", carName, carYear,
                    fuelConsumption, co2Emissions, fuelType);

            // Save to database
            String userId = getIntent().getStringExtra("USERNAME");
            if (userId != null) {
                DatabaseHelper dbHelper = new DatabaseHelper(this);
                int userIdInt = dbHelper.getUserId(userId);

                // 1. Save to custom profiles table
                boolean isProfileSaved = dbHelper.saveCustomCarProfile(userIdInt, customCar);

                // 2. Also save as current car (optional)
                boolean isCurrentCarSaved = dbHelper.saveCar(userId, new Gson().toJson(customCar));

                if (isProfileSaved) {
                    Toast.makeText(this, "Car profile saved successfully", Toast.LENGTH_SHORT).show();

                    // Return to HomeActivity with success result
                    Intent resultIntent = new Intent();
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    Toast.makeText(this, "Failed to save car profile", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid fuel consumption", Toast.LENGTH_SHORT).show();
        }
    }

    private double calculateCo2Emissions(double fuelConsumption, String fuelType) {
        // These are approximate values - you can adjust them as needed
        switch (fuelType) {
            case "Gasoline":
                return fuelConsumption * 23.2; // Approx 23.2g CO2 per liter
            case "Diesel":
                return fuelConsumption * 26.5; // Approx 26.5g CO2 per liter
            case "LPG":
                return fuelConsumption * 16.8; // Approx 16.8g CO2 per liter
            case "Hybrid":
                return fuelConsumption * 15.0; // Approx 15.0g CO2 per liter
            case "Electric":
                return 0.0; // No direct emissions
            default:
                return fuelConsumption * 20.0; // Default average
        }
    }
}