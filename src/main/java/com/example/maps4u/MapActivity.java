package com.example.maps4u;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng origin;
    private LatLng destination;
    private TextView distanceText;
    private TextView durationText;
    private TextView fuelConsumptionText;
    private TextView co2EmissionsText;
    private Car selectedCar;
    private TextView co2RatingText;
    private View progressIndicator;

    // Stats views
    private TextView walkingDurationText;
    private TextView walkingDistanceText;
    private TextView caloriesBurnedText;
    private TextView healthBenefitsText;

    private TextView bicycleDurationText;
    private TextView bicycleDistanceText;
    private TextView bicycleCaloriesText;
    private TextView bicycleHealthText;

    // Layout containers
    private LinearLayout carStatsView;
    private LinearLayout walkingStatsView;
    private LinearLayout bicycleStatsView;

    // Route data
    private double distanceInKm;
    private String duration;

    // Transport selection
    private Button selectButton;
    private String selectedTransportMode = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Initialize views
        Button backButton = findViewById(R.id.backButton);
        distanceText = findViewById(R.id.distanceText);
        durationText = findViewById(R.id.durationText);
        fuelConsumptionText = findViewById(R.id.fuelConsumptionText);
        co2EmissionsText = findViewById(R.id.co2EmissionsText);
        co2RatingText = findViewById(R.id.co2RatingText);
        progressIndicator = findViewById(R.id.progressIndicator);
        selectButton = findViewById(R.id.selectButton);

        // Walking stats
        walkingDurationText = findViewById(R.id.walkingDurationText);
        walkingDistanceText = findViewById(R.id.walkingDistanceText);
        caloriesBurnedText = findViewById(R.id.caloriesBurnedText);
        healthBenefitsText = findViewById(R.id.healthBenefitsText);

        // Bicycle stats
        bicycleDurationText = findViewById(R.id.bicycleDurationText);
        bicycleDistanceText = findViewById(R.id.bicycleDistanceText);
        bicycleCaloriesText = findViewById(R.id.bicycleCaloriesText);
        bicycleHealthText = findViewById(R.id.bicycleHealthText);

        // Layout containers
        carStatsView = findViewById(R.id.carStatsView);
        walkingStatsView = findViewById(R.id.walkingStatsView);
        bicycleStatsView = findViewById(R.id.bicycleStatsView);

        String userId = getIntent().getStringExtra("USERNAME");
        if (userId == null) {
            Toast.makeText(this, "User ID is null. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load car data if exists
        if (userId != null) {
            DatabaseHelper dbHelper = new DatabaseHelper(MapActivity.this);
            String carData = dbHelper.getCarData(userId);
            if (carData != null) {
                Gson gson = new Gson();
                selectedCar = gson.fromJson(carData, Car.class);
            }
        }

        // Get route coordinates
        Intent intent = getIntent();
        origin = new LatLng(intent.getDoubleExtra("origin_lat", 0), intent.getDoubleExtra("origin_lng", 0));
        destination = new LatLng(intent.getDoubleExtra("dest_lat", 0), intent.getDoubleExtra("dest_lng", 0));

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Back button
        backButton.setOnClickListener(v -> {
            Intent back = new Intent(MapActivity.this, HomeActivity.class);
            back.putExtra("USERNAME", userId);
            startActivity(back);
            finish();
        });

        // Setup tab listener (tabs are defined in XML)
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (selectedTransportMode != null) {
                    // If transport mode is already selected, don't allow changing tabs
                    tabLayout.selectTab(tabLayout.getTabAt(getTransportModeTabIndex(selectedTransportMode)));
                    return;
                }

                switch (tab.getPosition()) {
                    case 0: // Car tab
                        showCarStats();
                        break;
                    case 1: // Walking tab
                        showWalkingStats();
                        break;
                    case 2: // Bicycle tab
                        showBicycleStats();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Select button
        selectButton.setOnClickListener(v -> {
            int selectedTab = tabLayout.getSelectedTabPosition();
            switch (selectedTab) {
                case 0:
                    selectedTransportMode = "car";
                    break;
                case 1:
                    selectedTransportMode = "walking";
                    break;
                case 2:
                    selectedTransportMode = "bicycle";
                    break;
            }

            // Save to history
            String originAddress = getIntent().getStringExtra("origin_address");
            String destinationAddress = getIntent().getStringExtra("destination_address");

            if (originAddress != null && destinationAddress != null && userId != null) {
                DatabaseHelper dbHelper = new DatabaseHelper(MapActivity.this);
                int userIdInt = dbHelper.getUserId(userId);
                boolean saved = dbHelper.addRouteToHistory(userIdInt, originAddress, destinationAddress, selectedTransportMode);

                if (saved) {
                    Toast.makeText(this, "Route saved to history", Toast.LENGTH_SHORT).show();
                }
            }

            // Hide select button and lock tabs
            selectButton.setVisibility(View.GONE);
            for (int i = 0; i < tabLayout.getTabCount(); i++) {
                tabLayout.getTabAt(i).view.setEnabled(false);
            }
        });

        // Show initial tab content
        showCarStats();
    }

    private int getTransportModeTabIndex(String transportMode) {
        switch (transportMode) {
            case "car": return 0;
            case "walking": return 1;
            case "bicycle": return 2;
            default: return 0;
        }
    }

    private void showCarStats() {
        carStatsView.setVisibility(View.VISIBLE);
        walkingStatsView.setVisibility(View.GONE);
        bicycleStatsView.setVisibility(View.GONE);
        updateCarDetails();
    }

    private void showWalkingStats() {
        carStatsView.setVisibility(View.GONE);
        walkingStatsView.setVisibility(View.VISIBLE);
        bicycleStatsView.setVisibility(View.GONE);
        updateWalkingStats();
    }

    private void showBicycleStats() {
        carStatsView.setVisibility(View.GONE);
        walkingStatsView.setVisibility(View.GONE);
        bicycleStatsView.setVisibility(View.VISIBLE);
        updateBicycleStats();
    }

    private void updateCarDetails() {
        if (selectedCar != null) {
            double fuelConsumptionTotal = (selectedCar.getFuelConsumption() * distanceInKm) / 100;
            double co2EmissionsTotal = (selectedCar.getCo2Emissions() * distanceInKm);

            fuelConsumptionText.setText(getString(R.string.fuel_consumption, String.format("%.2f", fuelConsumptionTotal)));
            co2EmissionsText.setText(getString(R.string.co2_emissions, String.format("%.2f", co2EmissionsTotal)));

            updateEcoRatingIndicator(co2EmissionsTotal);
        } else {
            fuelConsumptionText.setText(R.string.fuel_consumption_na);
            co2EmissionsText.setText(R.string.co2_emissions_na);
        }
    }

    private void updateWalkingStats() {
        double walkingSpeedKmH = 5.0;
        double walkingDurationHours = distanceInKm / walkingSpeedKmH;
        int walkingMinutes = (int) (walkingDurationHours * 60);
        double caloriesPerKm = 65;
        double caloriesBurned = distanceInKm * caloriesPerKm;

        walkingDistanceText.setText(String.format(getString(R.string.walking_distance), distanceInKm));
        walkingDurationText.setText(getString(R.string.walking_duration, walkingMinutes));
        caloriesBurnedText.setText(String.format(getString(R.string.calories_burned), caloriesBurned));

        if (distanceInKm < 1) {
            healthBenefitsText.setText(R.string.health_benefits_short);
        } else if (distanceInKm < 3) {
            healthBenefitsText.setText(R.string.health_benefits_moderate);
        } else {
            healthBenefitsText.setText(R.string.health_benefits_long);
        }
    }

    private void updateBicycleStats() {
        double cyclingSpeedKmH = 15.0;
        double cyclingDurationHours = distanceInKm / cyclingSpeedKmH;
        int cyclingMinutes = (int) (cyclingDurationHours * 60);
        double caloriesPerKm = 35;
        double caloriesBurned = distanceInKm * caloriesPerKm;

        bicycleDistanceText.setText(String.format(getString(R.string.bicycle_distance), distanceInKm));
        bicycleDurationText.setText(getString(R.string.bicycle_duration, cyclingMinutes));
        bicycleCaloriesText.setText(String.format(getString(R.string.bicycle_calories), caloriesBurned));

        if (distanceInKm < 5) {
            bicycleHealthText.setText(R.string.bicycle_health_short);
        } else if (distanceInKm < 15) {
            bicycleHealthText.setText(R.string.bicycle_health_moderate);
        } else {
            bicycleHealthText.setText(R.string.bicycle_health_long);
        }
    }

    private void updateEcoRatingIndicator(double co2EmissionsTotal) {
        double co2PerKm = co2EmissionsTotal / distanceInKm;

        String rating;
        int color;
        float progressPosition;

        if (co2PerKm < 100) {
            rating = getString(R.string.eco_rating_excellent);
            color = Color.GREEN;
            progressPosition = 0.1f;
        } else if (co2PerKm < 120) {
            rating = getString(R.string.eco_rating_good);
            color = Color.parseColor("#4CAF50");
            progressPosition = 0.3f;
        } else if (co2PerKm < 150) {
            rating = getString(R.string.eco_rating_average);
            color = Color.parseColor("#FFC107");
            progressPosition = 0.6f;
        } else {
            rating = getString(R.string.eco_rating_poor);
            color = Color.RED;
            progressPosition = 0.9f;
        }

        co2RatingText.setText(getString(R.string.eco_rating, rating));
        co2RatingText.setTextColor(color);

        // Get the parent view width
        View parent = (View) progressIndicator.getParent();
        int parentWidth = parent.getWidth();

        // Calculate the new position
        int newPosition = (int) (parentWidth * progressPosition);

        // Update layout params
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) progressIndicator.getLayoutParams();
        params.leftMargin = newPosition - (progressIndicator.getWidth() / 2);
        progressIndicator.setLayoutParams(params);
        progressIndicator.setBackgroundColor(color);

        // Force layout update
        progressIndicator.requestLayout();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add validation
        if (origin.latitude == 0 && origin.longitude == 0 ||
                destination.latitude == 0 && destination.longitude == 0) {
            Toast.makeText(this, "Invalid coordinates", Toast.LENGTH_LONG).show();
            return;
        }
        mMap.addMarker(new MarkerOptions().position(origin).title("Starting Point"));
        mMap.addMarker(new MarkerOptions().position(destination).title("Destination"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origin, 10));
        getDirectionData();
    }

    private void getDirectionData() {
        String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&key=...";

        Log.d("DirectionsAPI", "Request URL: " + url); // Add this line

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("DirectionsAPI", "Failed to fetch directions", e); // Add this
                runOnUiThread(() ->
                        Toast.makeText(MapActivity.this, "Failed to fetch directions.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    // Store the response body first
                    String responseData = response.body().string();
                    Log.d("DirectionsAPI", "Response: " + responseData);

                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonObject = new JSONObject(responseData);
                            JSONArray routes = jsonObject.getJSONArray("routes");

                            if (routes.length() > 0) {
                                JSONObject route = routes.getJSONObject(0);
                                JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                                String encodedPolyline = overviewPolyline.getString("points");

                                JSONObject legs = route.getJSONArray("legs").getJSONObject(0);
                                final String distance = legs.getJSONObject("distance").getString("text");
                                duration = legs.getJSONObject("duration").getString("text");
                                distanceInKm = legs.getJSONObject("distance").getDouble("value") / 1000;

                                runOnUiThread(() -> {
                                    distanceText.setText(getString(R.string.distance) + ": " + distance);
                                    durationText.setText(getString(R.string.duration) + ": " + duration);
                                    updateCarDetails();

                                    // Add polyline to map
                                    List<LatLng> decodedPath = decodePolyline(encodedPolyline);
                                    if (decodedPath != null && !decodedPath.isEmpty()) {
                                        mMap.addPolyline(new PolylineOptions()
                                                .addAll(decodedPath)
                                                .width(10)
                                                .color(Color.BLUE));
                                    }
                                });
                            } else {
                                runOnUiThread(() ->
                                        Toast.makeText(MapActivity.this, "No routes found.", Toast.LENGTH_SHORT).show());
                            }
                        } catch (JSONException e) {
                            Log.e("DirectionsAPI", "JSON parsing error", e);
                            runOnUiThread(() ->
                                    Toast.makeText(MapActivity.this, "Error parsing directions data", Toast.LENGTH_LONG).show());
                        }
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(MapActivity.this, "API request failed", Toast.LENGTH_SHORT).show());
                    }
                } catch (IOException e) {
                    Log.e("DirectionsAPI", "IO error", e);
                    runOnUiThread(() ->
                            Toast.makeText(MapActivity.this, "Network error", Toast.LENGTH_SHORT).show());
                } finally {
                    response.close(); 
                }
            }
        });
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> polyline = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            polyline.add(new LatLng((lat / 1E5), (lng / 1E5)));
        }
        return polyline;
    }
}
