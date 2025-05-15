package com.example.maps4u;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.StringReader;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private boolean isFullScreen = false;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private static final int REQUEST_ACTIVITY_RECOGNITION_PERMISSION = 1001;
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private int previousStepCount = 0;
    private TextView tvStepCount;
    private Handler stepHandler = new Handler();
    private Runnable stepRunnable;

    private int dailySteps = 0;
    private String currentDate = "";
    private Handler dailyStepHandler = new Handler();
    private Runnable dailyStepRunnable;


    private void getCoordinatesFromAddress(String address, Consumer<Location> callback) {
        executor.execute(() -> {
            Geocoder geocoder = new Geocoder(HomeActivity.this);
            try {
                List<Address> addresses = geocoder.getFromLocationName(address, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address locationAddress = addresses.get(0);
                    double latitude = locationAddress.getLatitude();
                    double longitude = locationAddress.getLongitude();

                    Location location = new Location("");
                    location.setLatitude(latitude);
                    location.setLongitude(longitude);

                    runOnUiThread(() -> callback.accept(location));
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(HomeActivity.this, "Address not found", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(HomeActivity.this, "Geocoding failed", Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_home);

        tvStepCount = findViewById(R.id.tvStepCount);

        currentDate = getCurrentDateString();

        loadDailySteps();

        startDailyStepChecker();


        loadImageFromSQLite();

        Button button_loc = findViewById(R.id.button_loc);
        Button button_desired = findViewById(R.id.button_desired);
        ImageView circleImage = findViewById(R.id.profile_image);

        Button toggleButton = findViewById(R.id.toggleMapButton);
        LinearLayout topLayout = findViewById(R.id.topLayout);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this); //for loc

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000); // 5secs
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        FirebaseApp.initializeApp(this);
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("Auth", "Authentific succesful");
                    } else {
                        Log.e("Auth", "Error auth", task.getException());
                    }
                });

        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
        StorageReference storageReference = firebaseStorage.getReference();

        String userId = getIntent().getStringExtra("USERNAME");
        if (userId == null) {
            Toast.makeText(this, "User ID is null. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String role = getIntent().getStringExtra("ROLE");
        if (userId != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

            userRef.child("profileImage").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String imageUrl = dataSnapshot.getValue(String.class);
                    if (imageUrl != null) {
                        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
                        StorageReference imageRef = storageReference.child("profile_images").child(userId + ".jpg");

                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            Glide.with(HomeActivity.this)
                                    .load(uri)
                                    .into((ImageView) findViewById(R.id.profile_image));
                        }).addOnFailureListener(e -> {
                            Toast.makeText(HomeActivity.this, "Failed to get image URL", Toast.LENGTH_SHORT).show();
                        });
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(HomeActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "User ID is null. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
        }
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);


        userRef.child("profileImage").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String imageUrl = dataSnapshot.getValue(String.class);
                if (imageUrl != null) {
                    StorageReference storageReference = FirebaseStorage.getInstance().getReference();
                    StorageReference imageRef = storageReference.child("profile_images").child(userId + ".jpg");

                    Glide.with(HomeActivity.this)
                            .load(imageRef)
                            .into((ImageView) findViewById(R.id.profile_image));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(HomeActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    updateMapWithLocation(location);
                }
            }
        };

        //map
        toggleButton.setOnClickListener(v -> {
            if (isFullScreen) {
                topLayout.setVisibility(View.VISIBLE);
                mapFragment.getView().getLayoutParams().height = 1150;
            } else {
                topLayout.setVisibility(View.GONE);
                mapFragment.getView().getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            }

            mapFragment.getView().post(() -> {
                mapFragment.getView().requestLayout();
                if (mMap != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mMap.getCameraPosition().target, 14));
                }
            });

            isFullScreen = !isFullScreen;
        });



        circleImage.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(HomeActivity.this, circleImage);
            popupMenu.getMenuInflater().inflate(R.menu.menu_tab, popupMenu.getMenu());

            popupMenu.getMenu().add("History");

            if (role != null && role.equals("admin")) {
                popupMenu.getMenu().add("Admin Settings");
            }

            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.logout) {
                    startActivity(new Intent(HomeActivity.this, MainActivity.class));
                    return true;
                } else if (item.getItemId() == R.id.upload_image) {
                    openImageChooser();
                    return true;
                } else if (item.getItemId() == R.id.engine_choice) {
                    openEngineChoiceDialog();
                    return true;
                } else if (item.getTitle().equals("Admin Settings")) {
                    Intent intent = new Intent(HomeActivity.this, AdminSettingsActivity.class);
                    startActivity(intent);
                    return true;
                } else if (item.getTitle().equals("History")) {
                    Intent intent = new Intent(HomeActivity.this, HistoryActivity.class);
                    intent.putExtra("USERNAME", userId);
                    intent.putExtra("ROLE", role);
                    startActivity(intent);
                    return true;
                }else if (item.getItemId() == R.id.my_rofile) {
                    Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
                    intent.putExtra("USERNAME", userId);
                    intent.putExtra("ROLE", role);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
            popupMenu.show();
        });

        button_loc.setOnClickListener(v -> getCurrentLocation());

        button_desired.setOnClickListener(v -> {
            String currentLocation = ((EditText) findViewById(R.id.locinput)).getText().toString();
            String desiredLocation = ((EditText) findViewById(R.id.desiredinput)).getText().toString();

            if (currentLocation.isEmpty() || desiredLocation.isEmpty()) {
                Toast.makeText(HomeActivity.this, "Please fill in both locations", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userId != null) {
                DatabaseHelper dbHelper = new DatabaseHelper(HomeActivity.this);
                String carData = dbHelper.getCarData(userId);

                if (carData == null) {
                    Toast.makeText(this, "No car selected. Please choose a car first.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            getCoordinatesFromAddress(currentLocation, currentLoc -> {
                getCoordinatesFromAddress(desiredLocation, desiredLoc -> {
                    Intent intent = new Intent(HomeActivity.this, MapActivity.class);
                    intent.putExtra("origin_lat", currentLoc.getLatitude());
                    intent.putExtra("origin_lng", currentLoc.getLongitude());
                    intent.putExtra("dest_lat", desiredLoc.getLatitude());
                    intent.putExtra("dest_lng", desiredLoc.getLongitude());
                    intent.putExtra("origin_address", currentLocation);
                    intent.putExtra("destination_address", desiredLocation);
                    intent.putExtra("USERNAME", userId);
                    startActivity(intent);
                });
            });
        });

    }


    //dafgdfdfggshsghdfsdghsdghsdghf



    private void openEngineChoiceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Car Option");

        String[] options = {"Select from Database", "Create Custom Profile", "Select from Your Profiles"};
        builder.setItems(options, (dialog, which) -> {
            String userId = getIntent().getStringExtra("USERNAME");
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            int userIdInt = dbHelper.getUserId(userId);

            switch (which) {
                case 0: // Select from Database
                    String[] carMakes = {"Toyota", "Ford", "Honda", "BMW", "Audi"};
                    AlertDialog.Builder makeBuilder = new AlertDialog.Builder(this);
                    makeBuilder.setTitle("Choose a Car Make");
                    makeBuilder.setItems(carMakes, (makeDialog, makeWhich) -> {
                        String selectedMake = carMakes[makeWhich];
                        fetchCarsFromAPI(selectedMake);
                    });
                    makeBuilder.show();
                    break;

                case 1: // Create Custom Profile
                    Intent intent = new Intent(HomeActivity.this, CreateCarProfileActivity.class);
                    intent.putExtra("USERNAME", userId);
                    startActivity(intent);
                    break;

                case 2: // Select from Your Profiles
                    List<Car> customProfiles = dbHelper.getCustomCarProfiles(userIdInt);
                    if (customProfiles.isEmpty()) {
                        Toast.makeText(this, "You don't have any custom profiles yet", Toast.LENGTH_SHORT).show();
                    } else {
                        showCustomProfilesDialog(customProfiles);
                    }
                    break;
            }
        });
        builder.show();
    }

    private void showCustomProfilesDialog(List<Car> profiles) {
        String[] profileNames = new String[profiles.size()];
        for (int i = 0; i < profiles.size(); i++) {
            profileNames[i] = profiles.get(i).getModel() + " (" + profiles.get(i).getYear() + ")";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Your Custom Profiles");
        builder.setItems(profileNames, (dialog, which) -> {
            Car selectedCar = profiles.get(which);
            saveCarToDatabase(selectedCar);
            Toast.makeText(this, "Selected: " + selectedCar.getModel(), Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }

    private void fetchCarsFromAPI(String make) {
        OkHttpClient client = new OkHttpClient();

        String url = "https://vpic.nhtsa.dot.gov/api/vehicles/getmodelsformake/" + make + "?format=json";

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("Error", "Failed to fetch cars: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    List<Car> cars = parseCarsFromResponse(responseData);

                    for (Car car : cars) {
                        fetchFuelDataFromFuelEconomyAPI(car);
                    }
                    runOnUiThread(() -> displayCarsInDialog(cars));
                }
            }
        });
    }

    private List<Car> parseCarsFromResponse(String jsonResponse) {
        List<Car> cars = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray results = jsonObject.getJSONArray("Results");

            for (int i = 0; i < results.length(); i++) {
                JSONObject carJson = results.getJSONObject(i);
                String make = carJson.getString("Make_Name");
                String model = carJson.getString("Model_Name");
                String year = carJson.optString("Model_Year", "N/A");

                // Create car with default fuel type (you might get this from API later)
                Car car = new Car(make, model, year, 0, 0, "Gasoline");
                cars.add(car);
            }
        } catch (JSONException e) {
            Log.e("Error", "Failed to parse JSON: " + e.getMessage());
        }
        return cars;
    }

    private void displayCarsInDialog(List<Car> cars) {
        String[] carOptions = new String[cars.size()];
        for (int i = 0; i < cars.size(); i++) {
            Car car = cars.get(i);
            carOptions[i] = car.getMake() + " " + car.getModel() + " (" + car.getYear() + ")";
        }

        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Choose a Car");
            builder.setItems(carOptions, (dialog, which) -> {
                Car selectedCar = cars.get(which);
                saveCarToDatabase(selectedCar);
                calculateAndDisplayFuelConsumption(selectedCar);
            });
            builder.show();
        });
    }

    private void saveCarToDatabase(Car car) {
        String userId = getIntent().getStringExtra("USERNAME");
        if (userId != null) {
            Gson gson = new Gson();
            String carData = gson.toJson(car); //Car to JSON

            DatabaseHelper dbHelper = new DatabaseHelper(HomeActivity.this);
            boolean isSaved = dbHelper.saveCar(userId, carData);
            if (isSaved) {
                Toast.makeText(this, "Car saved: " + car.getModel(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to save car", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void calculateAndDisplayFuelConsumption(Car car) {
        Intent intent = new Intent(HomeActivity.this, MapActivity.class);
        intent.putExtra("CAR_DATA", new Gson().toJson(car)); // send Car obj as JSON
        //startActivity(intent);
    }

    private void fetchFuelDataFromFuelEconomyAPI(Car car) {
        if (car.getYear().equals("N/A")) {
            return;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();

        String url = "https://www.fueleconomy.gov/ws/rest/vehicle/menu/options?year=" +
                car.getYear() + "&make=" + car.getMake() + "&model=" + car.getModel();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("Error", "Failed to fetch fuel data: " + e.getMessage());
                // You might want to set default values here
                car.setFuelConsumption(8);
                car.setCo2Emissions(10);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        if (responseData.startsWith("<?xml")) {
                            parseFuelDataFromXML(responseData, car);
                        } else {
                            parseFuelDataFromResponse(responseData, car);
                        }
                    } catch (Exception e) {
                        Log.e("Error", "Failed to parse fuel data: " + e.getMessage());
                        car.setFuelConsumption(10);
                        car.setCo2Emissions(12);
                    }
                } else {
                    Log.e("Error", "Failed to fetch fuel data: HTTP " + response.code());
                    car.setFuelConsumption(14);
                    car.setCo2Emissions(20);
                }
            }
        });
    }

    private void parseFuelDataFromXML(String xmlData, Car car) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xmlData));

            String comb08 = null;
            String co2TailpipeGpm = null;

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tagName = parser.getName();
                    if ("comb08".equals(tagName)) {
                        comb08 = parser.nextText();
                    } else if ("co2TailpipeGpm".equals(tagName)) {
                        co2TailpipeGpm = parser.nextText();
                    }
                }
                eventType = parser.next();
            }

            if (comb08 != null && co2TailpipeGpm != null) {
                double fuelConsumption = Double.parseDouble(comb08);
                double co2Emissions = Double.parseDouble(co2TailpipeGpm);

                // Convert from miles to km /100km
                double fuelConsumptionLitersPer100Km = 235.214583 / fuelConsumption;
                // Convert from g/mile to g/km
                double co2EmissionsGPerKm = co2Emissions / 1.60934;

                car.setFuelConsumption(fuelConsumptionLitersPer100Km);
                car.setCo2Emissions(co2EmissionsGPerKm);
            }
        } catch (Exception e) {
            Log.e("Error", "Failed to parse XML fuel data: " + e.getMessage());
        }
    }

    private void parseFuelDataFromResponse(String jsonResponse, Car car) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray menuItems = jsonObject.getJSONArray("menuItems");

            if (menuItems.length() > 0) {
                JSONObject firstItem = menuItems.getJSONObject(0);

                double fuelConsumption = firstItem.getDouble("comb08");
                double co2Emissions = firstItem.getDouble("co2TailpipeGpm");

                // from miles to km /100km
                double fuelConsumptionLitersPer100Km = 235.214583 / fuelConsumption;

                // from g/mile to g/km
                double co2EmissionsGPerKm = co2Emissions / 1.60934;

                car.setFuelConsumption(fuelConsumptionLitersPer100Km);
                car.setCo2Emissions(co2EmissionsGPerKm);
            }
        } catch (JSONException e) {
            Log.e("Error", "Failed to parse fuel data: " + e.getMessage());
        }
    }


    //fgdghjodfghhjigbdffgdhiujbdfgkhjifdg
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            ImageView profileImageView = findViewById(R.id.profile_image);
            profileImageView.setImageURI(imageUri);

            uploadImageToFirebase(); // save imag url and in firebase
        }
    }

    private void uploadImageToFirebase() {
        if (!isInternetAvailable()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    Log.e("Error", "File not found");
                    Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
                    return;
                }
                inputStream.close();
            } catch (IOException e) {
                Log.e("Error", "File error: " + e.getMessage());
                Toast.makeText(this, "File error", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = getIntent().getStringExtra("USERNAME");
            if (userId == null) {
                Log.e("Error", "User ID is null");
                return;
            }

            StorageReference fileReference = FirebaseStorage.getInstance()
                    .getReference("profile_images")
                    .child(userId + ".jpg");

            fileReference.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            saveImageUrlToDatabase(imageUrl);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Error", "Image upload failed: " + e.getMessage());
                        Toast.makeText(HomeActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.e("Error", "Image URI is null");
        }
    }

    private void saveImageUrlToDatabase(String imageUrl) {
        String userId = getIntent().getStringExtra("USERNAME");

        if (userId != null) {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference userRef = database.getReference("users").child(userId);
            userRef.child("profileImage").setValue(imageUrl)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(HomeActivity.this, "Image URL saved to Firebase", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(HomeActivity.this, "Failed to save image URL to Firebase", Toast.LENGTH_SHORT).show();
                    });

            DatabaseHelper dbHelper = new DatabaseHelper(HomeActivity.this);
            dbHelper.insertImageUrl(userId, imageUrl);
            Toast.makeText(HomeActivity.this, "Image URL saved to SQLite", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadImageFromSQLite() {
        String userId = getIntent().getStringExtra("USERNAME");

        if (userId != null) {
            DatabaseHelper dbHelper = new DatabaseHelper(HomeActivity.this);
            String imageUrl = dbHelper.getImageUrl(userId); // get url from sql

            if (imageUrl != null) {
                Glide.with(HomeActivity.this)
                        .load(imageUrl)
                        .into((ImageView) findViewById(R.id.profile_image));
            }
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        String newDate = getCurrentDateString();
        if (!newDate.equals(currentDate)) {
            saveDailySteps();
            currentDate = newDate;
            dailySteps = 0;
            previousStepCount = 0;
            updateStepDisplay();
        }
        if (stepSensor != null) {
            registerStepSensor();
        }
        if (stepHandler != null && stepRunnable != null) {
            stepHandler.post(stepRunnable);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

        if (!isInternetAvailable()) {
            showAlert("Network connection isn't available right now, please check your wifi.");
        }

        if (!isLocationEnabled()) {
            showAlert("Location services are not activated. Activate the location to use the map.");
        }
        }
    }

    private void initStepCounter() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }

        if (stepSensor == null) {
            Toast.makeText(this, "Step counter sensor not available", Toast.LENGTH_SHORT).show();
        } else {
            checkActivityRecognitionPermission();
        }
    }

    private void checkActivityRecognitionPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                    REQUEST_ACTIVITY_RECOGNITION_PERMISSION);
        } else {
            registerStepSensor();
        }
    }

    private void registerStepSensor() {
        sensorManager.registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private String getCurrentDateString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private void loadDailySteps() {
        String userId = getIntent().getStringExtra("USERNAME");
        if (userId != null) {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            int userIdInt = dbHelper.getUserId(userId);
            dailySteps = dbHelper.getDailySteps(userIdInt, currentDate);
            updateStepDisplay();
        }
    }

    private void startDailyStepChecker() {
        dailyStepRunnable = new Runnable() {
            @Override
            public void run() {
                checkDateChange();
                dailyStepHandler.postDelayed(this, 3600000); // Verifică la fiecare oră
            }
        };
        dailyStepHandler.post(dailyStepRunnable);
    }

    private void checkDateChange() {
        String newDate = getCurrentDateString();
        if (!newDate.equals(currentDate)) {
            // Zi nouă, salvează și resetează
            saveDailySteps();
            currentDate = newDate;
            dailySteps = 0;
            previousStepCount = 0; // Resetăm și contorul de pași pentru recalcularea corectă
            updateStepDisplay();
        }
    }

    private void updateStepDisplay() {
        runOnUiThread(() -> {
            tvStepCount.setText("Steps: " + dailySteps);
            saveDailySteps(); // Salvăm periodic
        });
    }

    private void saveDailySteps() {
        String userId = getIntent().getStringExtra("USERNAME");
        if (userId != null) {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            int userIdInt = dbHelper.getUserId(userId);
            dbHelper.saveDailySteps(userIdInt, currentDate, dailySteps);
        }
    }


    private SensorEventListener stepListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                int totalSteps = (int) event.values[0];

                if (previousStepCount == 0) {
                    // Prima citire după boot
                    previousStepCount = totalSteps;
                } else {
                    int stepsSinceLastUpdate = totalSteps - previousStepCount;
                    previousStepCount = totalSteps;
                    dailySteps += stepsSinceLastUpdate;
                    updateStepDisplay();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };






    @Override
    protected void onPause() {
        super.onPause();
        saveDailySteps();
        fusedLocationClient.removeLocationUpdates(locationCallback);
        if (sensorManager != null) {
            sensorManager.unregisterListener(stepListener);
        }
        if (stepHandler != null) {
            stepHandler.removeCallbacks(stepRunnable);
        }
    }


    private void updateMapWithLocation(Location location) {
        if (mMap == null) return;

        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.clear();

        mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Current Location"));

        mMap.addCircle(new CircleOptions()
                .center(currentLatLng)
                .radius(50)  // 50m
                .strokeWidth(2)
                .strokeColor(Color.BLUE)
                .fillColor(Color.argb(50, 0, 0, 255)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    Location location = task.getResult();
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    LatLng currentLatLng = new LatLng(latitude, longitude);
                    mMap.clear();
                    mMap.addMarker(new MarkerOptions().position(currentLatLng).title("My location"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                } else {
                    Toast.makeText(HomeActivity.this, "Could not find the location", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }


    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        executor.execute(() -> processLocation(location));
                    } else {
                        requestLocationUpdates();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Eroare la obținerea locației: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void processLocation(Location location) {
        try {
            Geocoder geocoder = new Geocoder(this);
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1);

            runOnUiThread(() -> {
                if (addresses != null && !addresses.isEmpty()) {
                    String address = addresses.get(0).getAddressLine(0);
                    ((EditText)findViewById(R.id.locinput)).setText(address);
                } else {
                    Toast.makeText(this, "Adresa nu a fost găsită", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            runOnUiThread(() ->
                    Toast.makeText(this, "Eroare geocoding: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void requestLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    processLocation(locationResult.getLastLocation());
                    fusedLocationClient.removeLocationUpdates(this);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ACTIVITY_RECOGNITION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                registerStepSensor();
            } else {
                Toast.makeText(this, "Step counting requires activity recognition permission", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission not accepted", Toast.LENGTH_SHORT).show();
            }

        }


    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            Network activeNetwork = cm.getActiveNetwork();
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
            return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        return false;
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null &&
                (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    private void showAlert(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Warning")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }


}

