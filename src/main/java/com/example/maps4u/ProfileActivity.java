package com.example.maps4u;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        String userId = getIntent().getStringExtra("USERNAME");

        ImageView profileImage = findViewById(R.id.profileImage);
        TextView tvUsername = findViewById(R.id.tvUsername);
        WebView routesChartWebView = findViewById(R.id.routesChartWebView);
        WebView consumptionChartWebView = findViewById(R.id.consumptionChartWebView);

        tvUsername.setText(userId);

        loadProfileImage(userId, profileImage);

        setupRoutesChart(routesChartWebView, userId);
        setupConsumptionChart(consumptionChartWebView, userId);

        Button backButton = findViewById(R.id.back_button1);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);

            intent.putExtra("USERNAME", getIntent().getStringExtra("USERNAME"));
            intent.putExtra("ROLE", getIntent().getStringExtra("ROLE"));

            startActivity(intent);
            finish();
        });
    }

    private void loadProfileImage(String userId, ImageView imageView) {
        StorageReference imageRef = FirebaseStorage.getInstance()
                .getReference("profile_images")
                .child(userId + ".jpg");

        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(imageView);
        }).addOnFailureListener(e -> {
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupRoutesChart(WebView webView, String userId) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        int userIdInt = dbHelper.getUserId(userId);
        Cursor historyCursor = dbHelper.getRecentHistory(userIdInt, 5);

        Map<String, Integer> routeCounts = new HashMap<>();
        if (historyCursor != null && historyCursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String origin = historyCursor.getString(historyCursor.getColumnIndex(DatabaseHelper.COLUMN_ORIGIN));
                @SuppressLint("Range") String destination = historyCursor.getString(historyCursor.getColumnIndex(DatabaseHelper.COLUMN_DESTINATION));
                String routeKey = origin + " → " + destination;
                routeCounts.put(routeKey, routeCounts.getOrDefault(routeKey, 0) + 1);
            } while (historyCursor.moveToNext());
            historyCursor.close();
        }

        try {
            JSONArray dataArray = new JSONArray();
            for (Map.Entry<String, Integer> entry : routeCounts.entrySet()) {
                JSONObject dataPoint = new JSONObject();
                dataPoint.put("route", entry.getKey());
                dataPoint.put("count", entry.getValue());
                dataArray.put(dataPoint);
            }

            String html = getRouteChartHtml(dataArray.toString());
            webView.getSettings().setJavaScriptEnabled(true);
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupConsumptionChart(WebView webView, String userId) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        String carData = dbHelper.getCarData(userId);

        try {
            JSONArray dataArray = new JSONArray();

            JSONObject cityData = new JSONObject();
            cityData.put("type", "City");
            cityData.put("consumption", 8.5);
            dataArray.put(cityData);

            JSONObject highwayData = new JSONObject();
            highwayData.put("type", "Highway");
            highwayData.put("consumption", 6.2);
            dataArray.put(highwayData);

            JSONObject mixedData = new JSONObject();
            mixedData.put("type", "Mixed");
            mixedData.put("consumption", 7.3);
            dataArray.put(mixedData);

            String html = getConsumptionChartHtml(dataArray.toString());
            webView.getSettings().setJavaScriptEnabled(true);
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String getRouteChartHtml(String jsonData) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>\n" +
                "  <script type=\"text/javascript\">\n" +
                "    google.charts.load('current', {'packages':['corechart']});\n" +
                "    google.charts.setOnLoadCallback(drawChart);\n" +
                "\n" +
                "    function drawChart() {\n" +
                "      var data = new google.visualization.DataTable();\n" +
                "      data.addColumn('string', 'Route');\n" +
                "      data.addColumn('number', 'Count');\n" +
                "\n" +
                "      var jsonData = " + jsonData + ";\n" +
                "      jsonData.forEach(function(row) {\n" +
                "        data.addRow([row.route, row.count]);\n" +
                "      });\n" +
                "\n" +
                "      var options = {\n" +
                "        title: 'Routes Taken',\n" +
                "        colors: ['#3F51B5'],\n" +
                "        legend: { position: 'none' },\n" +
                "        bar: { groupWidth: '75%' },\n" +
                "        hAxis: { \n" +
                "          title: 'Route',\n" +
                "          slantedText: true,\n" +
                "          slantedTextAngle: 45\n" +
                "        },\n" +
                "        vAxis: { title: 'Count' }\n" +
                "      };\n" +
                "\n" +
                "      var chart = new google.visualization.ColumnChart(document.getElementById('chart_div'));\n" +
                "      chart.draw(data, options);\n" +
                "    }\n" +
                "  </script>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div id=\"chart_div\" style=\"width: 100%; height: 100%;\"></div>\n" +
                "</body>\n" +
                "</html>";
    }

    private String getConsumptionChartHtml(String jsonData) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>\n" +
                "  <script type=\"text/javascript\">\n" +
                "    google.charts.load('current', {'packages':['corechart']});\n" +
                "    google.charts.setOnLoadCallback(drawChart);\n" +
                "\n" +
                "    function drawChart() {\n" +
                "      var data = new google.visualization.DataTable();\n" +
                "      data.addColumn('string', 'Type');\n" +
                "      data.addColumn('number', 'Consumption (L/100km)');\n" +
                "\n" +
                "      var jsonData = " + jsonData + ";\n" +
                "      jsonData.forEach(function(row) {\n" +
                "        data.addRow([row.type, row.consumption]);\n" +
                "      });\n" +
                "\n" +
                "      var options = {\n" +
                "        title: 'Fuel Consumption',\n" +
                "        colors: ['#4CAF50'],\n" +
                "        legend: { position: 'none' },\n" +
                "        bar: { groupWidth: '75%' },\n" +
                "        hAxis: { title: 'Driving Type' },\n" +
                "        vAxis: { \n" +
                "          title: 'L/100km',\n" +
                "          minValue: 0\n" +
                "        }\n" +
                "      };\n" +
                "\n" +
                "      var chart = new google.visualization.ColumnChart(document.getElementById('chart_div'));\n" +
                "      chart.draw(data, options);\n" +
                "    }\n" +
                "  </script>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div id=\"chart_div\" style=\"width: 100%; height: 100%;\"></div>\n" +
                "</body>\n" +
                "</html>";
    }
}