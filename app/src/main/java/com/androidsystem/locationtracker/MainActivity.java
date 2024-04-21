package com.androidsystem.locationtracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.androidsystem.locationtracker.adapter.CustomListAdapter;
import com.androidsystem.locationtracker.db.DBHelper;
import com.androidsystem.locationtracker.service.LocationService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST = 100;

    private DBHelper dbHelper;

    private SwipeRefreshLayout swipeRefreshLayout;
    private ListView locationListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        checkPermissions();

        dbHelper = new DBHelper(this);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_location_list);
        locationListView = findViewById(R.id.location_list_view);

        displayLocationData();

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Refresh the list here
                displayLocationData();
                // Make sure you call swipeRefreshLayout.setRefreshing(false) once the data is updated.
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void displayLocationData() {
        ArrayList<String> latitudeList = new ArrayList<>();
        ArrayList<String> longitudeList = new ArrayList<>();
        ArrayList<String> timeList = new ArrayList<>();
        Cursor cursor = dbHelper.getAllLocations();

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") double latitude = cursor.getDouble(cursor.getColumnIndex(DBHelper.COLUMN_LATITUDE));
                @SuppressLint("Range") double longitude = cursor.getDouble(cursor.getColumnIndex(DBHelper.COLUMN_LONGITUDE));
                @SuppressLint("Range") String datetime = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_CREATED_AT));

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                Date date;
                try {
                    date = dateFormat.parse(datetime);
                    assert date != null;
                    long adjustedTimeMillis = date.getTime() + (6 * 60 * 60 * 1000);
                    date.setTime(adjustedTimeMillis);
                    String formattedDatetime = dateFormat.format(date);

                    latitudeList.add("Latitude: " + latitude);
                    longitudeList.add("Longitude: " + longitude);
                    timeList.add("Time: " + formattedDatetime);

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        CustomListAdapter adapter = new CustomListAdapter(this, latitudeList, longitudeList, timeList);
        locationListView.setAdapter(adapter);
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request foreground location permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request background location permission separately
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    PERMISSIONS_REQUEST);
        } else {
            // All necessary permissions are granted
            startLocationService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length > 0) {
                boolean allPermissionsGranted = true;
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGranted = false;
                        Toast.makeText(this, "Permission denied for: " + permissions[i], Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                if (allPermissionsGranted) {
                    // Check if background permission needs to be requested
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q &&
                            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                PERMISSIONS_REQUEST);
                    } else {
                        startLocationService();
                    }
                }
            }
        }
    }


    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
}