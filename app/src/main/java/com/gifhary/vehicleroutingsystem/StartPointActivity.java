package com.gifhary.vehicleroutingsystem;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class StartPointActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;

    private double lastLocationLat;
    private double lastLocationLong;

    private static final String PREFERENCES = "preferences";

    private boolean editMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_point);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//showing back arrow on toolbar

        SharedPreferences settings = getSharedPreferences(PREFERENCES, 0);
        editMode = settings.getBoolean("editMode", false);

        //set click listener on "Your location" text
        TextView yourLocation = findViewById(R.id.yourLocationTxt);
        yourLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editMode) {
                    getLastLocationEditMode();//get last location for edit mode true
                } else {
                    getLastLocation();
                }
            }
        });

        //set click listener on "Select on map" text
        TextView selectOnMap = findViewById(R.id.selectOnMapTxt);
        selectOnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent selectOnMap = new Intent(StartPointActivity.this, StartOnMapActivity.class);
                startActivity(selectOnMap);
                finish();
            }
        });

    }

    private void getLastLocationEditMode() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "Please enable location", Toast.LENGTH_LONG).show();
        } else {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {

                    if (location != null) {
                        lastLocationLat = location.getLatitude();
                        lastLocationLong = location.getLongitude();

                        SharedPreferences settings = getSharedPreferences(PREFERENCES, 0);
                        SharedPreferences.Editor editor = settings.edit();

                        //store "Your location" coordinate in SharedPreferences
                        editor.putString("startPointEdit", String.valueOf(lastLocationLat) + "," + String.valueOf(lastLocationLong));
                        editor.apply();

                        Intent setStartPoint = new Intent(StartPointActivity.this, EditTripActivity.class);
                        startActivity(setStartPoint);
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(), "Cannot access location", Toast.LENGTH_LONG).show();
                    }

                }
            });
        }

    }

    private void getLastLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "Please enable location", Toast.LENGTH_LONG).show();
        } else {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {

                    if (location != null) {
                        lastLocationLat = location.getLatitude();
                        lastLocationLong = location.getLongitude();

                        SharedPreferences settings = getSharedPreferences(PREFERENCES, 0);
                        SharedPreferences.Editor editor = settings.edit();

                        //store "Your location" coordinate in SharedPreferences
                        editor.putString("startLocationLat", String.valueOf(lastLocationLat));
                        editor.putString("startLocationLong", String.valueOf(lastLocationLong));
                        editor.apply();

                        Intent setStartPoint = new Intent(StartPointActivity.this, NewTripActivity.class);
                        startActivity(setStartPoint);
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(), "Cannot access location", Toast.LENGTH_LONG).show();
                    }

                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (editMode){
            Intent setStartPoint = new Intent(StartPointActivity.this, EditTripActivity.class);
            startActivity(setStartPoint);
        }else {
            Intent setStartPoint = new Intent(StartPointActivity.this, NewTripActivity.class);
            startActivity(setStartPoint);
        }
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            if (editMode){
                Intent setStartPoint = new Intent(StartPointActivity.this, EditTripActivity.class);
                startActivity(setStartPoint);
            }else {
                Intent setStartPoint = new Intent(StartPointActivity.this, NewTripActivity.class);
                startActivity(setStartPoint);
            }
            finish();
        }
        return super.onOptionsItemSelected(menuItem);
    }

}
