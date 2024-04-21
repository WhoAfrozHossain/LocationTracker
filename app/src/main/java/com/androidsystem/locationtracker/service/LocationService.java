package com.androidsystem.locationtracker.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.content.Context;
import android.util.Log;

import com.androidsystem.locationtracker.db.DBHelper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LocationService extends Service {

    private static DBHelper dbHelper;
    private static final String baseUrl = "http://89.116.134.237/";
    private LocationManager locationManager;
    private LocationListener listener;

    @Override
    public void onCreate() {
        super.onCreate();

        dbHelper = new DBHelper(LocationService.this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                // Log location or store in SQLite
                dbHelper.insertLocation(location.getLatitude(), location.getLongitude());
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(@NonNull String provider) {}

            @Override
            public void onProviderDisabled(@NonNull String provider) {}
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("locationService", "Location Service", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
            Notification notification = new Notification.Builder(this, "locationService")
                    .setContentTitle("Location Service")
                    .setContentText("Running")
                    .build();
            startForeground(1, notification);
        }

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000, 5, listener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            // Perform background tasks here
            sendDataToServer();
            handler.postDelayed(this, 60000); // Run every 1 minute
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler.post(runnable);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        locationManager.removeUpdates(listener);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendDataToServer() {
        Cursor cursor = dbHelper.getAllLocationAsc();

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") int columnId = cursor.getInt(cursor.getColumnIndex(DBHelper.COLUMN_ID));
                @SuppressLint("Range") double latitude = cursor.getDouble(cursor.getColumnIndex(DBHelper.COLUMN_LATITUDE));
                @SuppressLint("Range") double longitude = cursor.getDouble(cursor.getColumnIndex(DBHelper.COLUMN_LONGITUDE));
                @SuppressLint("Range") String datetime = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_CREATED_AT));

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

//                String locationString = "Latitude: " + latitude + ", Longitude: " + longitude + ", Time: " + datetime;
//                locationList.add(locationString);

                Date date;
                try {
                    date = dateFormat.parse(datetime);
                    assert date != null;
                    long adjustedTimeMillis = date.getTime() + (6 * 60 * 60 * 1000);
                    date.setTime(adjustedTimeMillis);
                    String formattedDatetime = dateFormat.format(date);

                    sendSingleDataToServer(columnId, latitude, longitude, formattedDatetime);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private static void sendSingleDataToServer(final int columnId, final double latitude, final double longitude, final String datetime) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    URL url = new URL(baseUrl + "hr/api/emp-location-tracking/");

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    // Create JSON payload
                    JSONObject jsonPayload = new JSONObject();
                    jsonPayload.put("user_id", 1);
                    jsonPayload.put("latitude", latitude);
                    jsonPayload.put("longitude", longitude);
                    jsonPayload.put("location_datetime", datetime);

                    // Write JSON payload to the output stream
                    OutputStream os = conn.getOutputStream();
                    os.write(jsonPayload.toString().getBytes());
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_CREATED) {
                        InputStream inputStream = conn.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();

                        dbHelper.deleteLocation(columnId);
                        Log.e("Location Data", "Response from server: " + response.toString());
//                        showToast("Response from server: " + response.toString());
                    } else {
//                        showToast("Server returned error: " + responseCode);
                        Log.e("Location Data", "Server returned error: " + responseCode);
                    }

                    // Close the connection
                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

//    private static void showToast(final String message) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(LocationService.this, message, Toast.LENGTH_LONG).show();
//            }
//        });
//    }

}
