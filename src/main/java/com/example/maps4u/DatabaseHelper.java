package com.example.maps4u;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Users.db";
    private static final int DATABASE_VERSION = 15; // Incrementat versiunea
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_IMAGE_URL = "image_url";
    private static final String COLUMN_CAR_DATA = "car_data";

    private static final String TABLE_HISTORY = "history";
    private static final String COLUMN_HISTORY_ID = "history_id";
    private static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_ORIGIN = "origin";
    public static final String COLUMN_DESTINATION = "destination";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_TRANSPORT_MODE = "transport_mode";

    private static final String TABLE_BIOMETRICS = "user_biometrics";
    private static final String COLUMN_USER_ID_FK = "user_id";
    private static final String COLUMN_HEIGHT = "height";
    private static final String COLUMN_WEIGHT = "weight";
    private static final String COLUMN_AGE = "age";
    private static final String COLUMN_GENDER = "gender";
    private static final String COLUMN_STEP_COUNT = "step_count";

    private static final String TABLE_DAILY_STEPS = "daily_steps";
    private static final String COLUMN_DAILY_STEPS_ID = "id";
    private static final String COLUMN_STEPS_DATE = "date";
    private static final String COLUMN_STEPS_COUNT = "steps";

    private static final String TABLE_CAR_PROFILES = "car_profiles";
    private static final String COLUMN_CAR_PROFILE_ID = "id";
    private static final String COLUMN_CAR_PROFILE_USER_ID = "user_id";
    private static final String COLUMN_CAR_NAME = "car_name";
    private static final String COLUMN_CAR_YEAR = "year";
    private static final String COLUMN_FUEL_CONSUMPTION = "fuel_consumption";
    private static final String COLUMN_CO2_EMISSIONS = "co2_emissions";
    private static final String COLUMN_FUEL_TYPE = "fuel_type";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USERNAME + " TEXT, " +
                    COLUMN_PASSWORD + " TEXT, " +
                    COLUMN_IMAGE_URL + " TEXT, " +
                    COLUMN_CAR_DATA + " TEXT, " +
                    "role TEXT DEFAULT 'user');";

    // Update CREATE_TABLE_HISTORY to include transport mode
    private static final String CREATE_TABLE_HISTORY =
            "CREATE TABLE " + TABLE_HISTORY + " (" +
                    COLUMN_HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USER_ID + " INTEGER, " +
                    COLUMN_ORIGIN + " TEXT NOT NULL, " +
                    COLUMN_DESTINATION + " TEXT NOT NULL, " +
                    COLUMN_TRANSPORT_MODE + " TEXT, " +  // Added this line
                    COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " +
                    TABLE_USERS + "(" + COLUMN_ID + ") ON DELETE CASCADE);";

    private static final String CREATE_TABLE_BIOMETRICS =
            "CREATE TABLE " + TABLE_BIOMETRICS + " (" +
                    COLUMN_USER_ID_FK + " INTEGER PRIMARY KEY, " +
                    COLUMN_HEIGHT + " REAL, " +
                    COLUMN_WEIGHT + " REAL, " +
                    COLUMN_AGE + " INTEGER, " +
                    COLUMN_GENDER + " TEXT, " +
                    COLUMN_STEP_COUNT + " INTEGER DEFAULT 0, " +
                    "FOREIGN KEY(" + COLUMN_USER_ID_FK + ") REFERENCES " +
                    TABLE_USERS + "(" + COLUMN_ID + ") ON DELETE CASCADE);";

    private static final String CREATE_TABLE_DAILY_STEPS =
            "CREATE TABLE " + TABLE_DAILY_STEPS + " (" +
                    COLUMN_DAILY_STEPS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USER_ID + " INTEGER, " +
                    COLUMN_STEPS_DATE + " TEXT, " +
                    COLUMN_STEPS_COUNT + " INTEGER, " +
                    "UNIQUE(" + COLUMN_USER_ID + ", " + COLUMN_STEPS_DATE + ") ON CONFLICT REPLACE);";

    private static final String CREATE_TABLE_CAR_PROFILES =
            "CREATE TABLE " + TABLE_CAR_PROFILES + " (" +
                    COLUMN_CAR_PROFILE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_CAR_PROFILE_USER_ID + " INTEGER, " +
                    COLUMN_CAR_NAME + " TEXT, " +
                    COLUMN_CAR_YEAR + " TEXT, " +
                    COLUMN_FUEL_CONSUMPTION + " REAL, " +
                    COLUMN_CO2_EMISSIONS + " REAL, " +
                    COLUMN_FUEL_TYPE + " TEXT, " +
                    "FOREIGN KEY(" + COLUMN_CAR_PROFILE_USER_ID + ") REFERENCES " +
                    TABLE_USERS + "(" + COLUMN_ID + ") ON DELETE CASCADE);";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("PRAGMA foreign_keys=ON;");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
        db.execSQL(CREATE_TABLE_HISTORY);
        db.execSQL(CREATE_TABLE_BIOMETRICS);
        db.execSQL(CREATE_TABLE_DAILY_STEPS);
        db.execSQL(CREATE_TABLE_CAR_PROFILES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 12) {
            db.execSQL(CREATE_TABLE_BIOMETRICS);
        }
        if (oldVersion < 13) {
            db.execSQL(CREATE_TABLE_DAILY_STEPS);
        }
        if (oldVersion < 14) {
            db.execSQL(CREATE_TABLE_CAR_PROFILES);
        }
        if (oldVersion < 15) {
            db.execSQL("ALTER TABLE " + TABLE_HISTORY + " ADD COLUMN " +
                    COLUMN_TRANSPORT_MODE + " TEXT DEFAULT 'car';");
        }
    }


    // Metode pentru gestionarea profilelor auto
    public boolean saveCustomCarProfile(int userId, Car car) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CAR_PROFILE_USER_ID, userId);
        values.put(COLUMN_CAR_NAME, car.getModel());
        values.put(COLUMN_CAR_YEAR, car.getYear());
        values.put(COLUMN_FUEL_CONSUMPTION, car.getFuelConsumption());
        values.put(COLUMN_CO2_EMISSIONS, car.getCo2Emissions());
        values.put(COLUMN_FUEL_TYPE, car.getFuelType());

        long result = db.insert(TABLE_CAR_PROFILES, null, values);
        return result != -1;
    }

    public List<Car> getCustomCarProfiles(int userId) {
        List<Car> profiles = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CAR_PROFILES,
                new String[]{COLUMN_CAR_NAME, COLUMN_CAR_YEAR, COLUMN_FUEL_CONSUMPTION,
                        COLUMN_CO2_EMISSIONS, COLUMN_FUEL_TYPE},
                COLUMN_CAR_PROFILE_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex(COLUMN_CAR_NAME));
                @SuppressLint("Range") String year = cursor.getString(cursor.getColumnIndex(COLUMN_CAR_YEAR));
                @SuppressLint("Range") double consumption = cursor.getDouble(cursor.getColumnIndex(COLUMN_FUEL_CONSUMPTION));
                @SuppressLint("Range") double emissions = cursor.getDouble(cursor.getColumnIndex(COLUMN_CO2_EMISSIONS));
                @SuppressLint("Range") String fuelType = cursor.getString(cursor.getColumnIndex(COLUMN_FUEL_TYPE));

                Car car = new Car("Custom", name, year, consumption, emissions, fuelType);
                profiles.add(car);
            }
            cursor.close();
        }
        return profiles;
    }



    public boolean saveDailySteps(int userId, String date, int steps) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_STEPS_DATE, date);
        values.put(COLUMN_STEPS_COUNT, steps);

        try {
            long result = db.insertWithOnConflict(TABLE_DAILY_STEPS, null, values,
                    SQLiteDatabase.CONFLICT_REPLACE);
            return result != -1;
        } catch (SQLException e) {
            Log.e("DB_ERROR", "Error saving daily steps: " + e.getMessage());
            return false;
        }
    }

    public int getDailySteps(int userId, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_DAILY_STEPS,
                new String[]{COLUMN_STEPS_COUNT},
                COLUMN_USER_ID + " = ? AND " + COLUMN_STEPS_DATE + " = ?",
                new String[]{String.valueOf(userId), date},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            @SuppressLint("Range") int steps = cursor.getInt(cursor.getColumnIndex(COLUMN_STEPS_COUNT));
            cursor.close();
            return steps;
        }
        return 0;
    }

    public List<Pair<String, Integer>> getStepHistory(int userId) {
        List<Pair<String, Integer>> history = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_DAILY_STEPS,
                new String[]{COLUMN_STEPS_DATE, COLUMN_STEPS_COUNT},
                COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null, null,
                COLUMN_STEPS_DATE + " DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") String date = cursor.getString(cursor.getColumnIndex(COLUMN_STEPS_DATE));
                @SuppressLint("Range") int steps = cursor.getInt(cursor.getColumnIndex(COLUMN_STEPS_COUNT));
                history.add(new Pair<>(date, steps));
            }
            cursor.close();
        }
        return history;
    }

    public boolean updateStepCount(int userId, int stepCount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STEP_COUNT, stepCount);

        int rowsAffected = db.update(TABLE_BIOMETRICS, values,
                COLUMN_USER_ID_FK + " = ?",
                new String[]{String.valueOf(userId)});
        return rowsAffected > 0;
    }


    public boolean saveCar(String username, String carData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_CAR_DATA, carData);

        int rowsAffected = db.update(TABLE_USERS, contentValues, COLUMN_USERNAME + " = ?", new String[]{username});
        return rowsAffected > 0;
    }
    public boolean insertUser(String username, String password, String imageUrl, String role) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, password);
        values.put("role", role);

        if (imageUrl == null || imageUrl.isEmpty()) {
            imageUrl = "https://firebasestorage.googleapis.com/v0/b/maps4u-d0355.firebasestorage.app/o/profile_images%2Fguest.jpg?alt=media&token=1564162d-9bbf-4b02-87d6-b473cef4ce30";
        }
        values.put(COLUMN_IMAGE_URL, imageUrl);

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public String checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_ID, "role"};
        String selection = COLUMN_USERNAME + " = ? AND " + COLUMN_PASSWORD + " = ?";
        String[] selectionArgs = {username, password};

        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            @SuppressLint("Range") String role = cursor.getString(cursor.getColumnIndex("role"));
            cursor.close();
            return role;
        }
        return null;
    }

    public boolean insertImageUrl(String username, String imageUrl) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_IMAGE_URL, imageUrl);

        int rowsAffected = db.update(TABLE_USERS, contentValues, COLUMN_USERNAME + " = ?", new String[]{username});
        return rowsAffected > 0;
    }

    public String getImageUrl(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String imageUrl = null;

        try {
            cursor = db.query(
                    TABLE_USERS,
                    new String[]{COLUMN_IMAGE_URL},
                    COLUMN_USERNAME + " = ?",
                    new String[]{username},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(COLUMN_IMAGE_URL);
                if (columnIndex >= 0) {
                    imageUrl = cursor.getString(columnIndex);
                } else {

                    Log.e("DatabaseHelper", "Column " + COLUMN_IMAGE_URL + " not found in cursor");
                }
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting image URL: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return imageUrl;
    }

    public String getCarData(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_CAR_DATA}, COLUMN_USERNAME + " = ?", new String[]{username}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            @SuppressLint("Range") String carData = cursor.getString(cursor.getColumnIndex(COLUMN_CAR_DATA));
            cursor.close();
            return carData;
        }
        return null;
    }

    public boolean updateUser(String username, String newPassword, String newRole) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_PASSWORD, newPassword);
        contentValues.put("role", newRole);

        int rowsAffected = db.update(TABLE_USERS, contentValues, COLUMN_USERNAME + " = ?", new String[]{username});
        return rowsAffected > 0;
    }

    public boolean deleteUser(String username) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_USERS, COLUMN_USERNAME + " = ?", new String[]{username});
        return rowsAffected > 0;
    }
    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, null, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String username = cursor.getString(cursor.getColumnIndex(COLUMN_USERNAME));
                @SuppressLint("Range") String password = cursor.getString(cursor.getColumnIndex(COLUMN_PASSWORD));
                @SuppressLint("Range") String role = cursor.getString(cursor.getColumnIndex("role"));
                userList.add(new User(username, password, role));
            } while (cursor.moveToNext());
            cursor.close();
        }

        return userList;
    }
    public String getUserPassword(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_PASSWORD}, COLUMN_USERNAME + " = ?", new String[]{username}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            @SuppressLint("Range") String password = cursor.getString(cursor.getColumnIndex(COLUMN_PASSWORD));
            cursor.close();
            return password;
        }
        return null;
    }

    public boolean addRouteToHistory(int userId, String origin, String destination, String transportMode) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_ORIGIN, origin);
        values.put(COLUMN_DESTINATION, destination);
        values.put(COLUMN_TRANSPORT_MODE, transportMode);

        try {
            long result = db.insertOrThrow(TABLE_HISTORY, null, values);
            return result != -1;
        } catch (SQLException e) {
            Log.e("DB_ERROR", "Error saving history: " + e.getMessage());
            return false;
        }
    }

    public Cursor getUserHistory(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_HISTORY,
                new String[]{COLUMN_HISTORY_ID, COLUMN_ORIGIN, COLUMN_DESTINATION,
                        COLUMN_TRANSPORT_MODE, COLUMN_TIMESTAMP},
                COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null, null,
                COLUMN_TIMESTAMP + " DESC");
    }
    public boolean deleteUserHistory(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_HISTORY, COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)}) > 0;
    }

    public Cursor getRecentHistory(int userId, int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_HISTORY,
                new String[]{COLUMN_ORIGIN, COLUMN_DESTINATION, COLUMN_TIMESTAMP},
                COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null, null,
                COLUMN_TIMESTAMP + " DESC",
                String.valueOf(limit));
    }

    public boolean hasHistory(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_HISTORY,
                new String[]{COLUMN_HISTORY_ID},
                COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null, null, null, "1");
        boolean hasRecords = cursor != null && cursor.getCount() > 0;
        if (cursor != null) cursor.close();
        return hasRecords;
    }
    public int getUserId(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COLUMN_ID},
                COLUMN_USERNAME + " = ?",
                new String[]{username},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
            cursor.close();
            return id;
        }
        return -1; // user not found
    }
}

