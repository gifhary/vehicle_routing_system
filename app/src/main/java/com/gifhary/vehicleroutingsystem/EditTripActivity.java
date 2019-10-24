package com.gifhary.vehicleroutingsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class EditTripActivity extends AppCompatActivity {
    private static final String PREFERENCES = "preferences";
    private static final String TAG = "EditTripActivity";

    private SharedPreferences location;

    private String tripName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_trip);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//showing back arrow on toolbar
        location = getSharedPreferences(PREFERENCES, 0);//initialing shared preferences

        EditText tripNameEdit = findViewById(R.id.tripNameEditTV);
        tripNameEdit.setText(location.getString("tripNameEdit", ""));
        tripNameEdit.setEnabled(false);//disable trip name editing

        tripName = location.getString("tripNameEdit", "");

        String startPointCoordinate = location.getString("startPointEdit", "");
        EditText startPointEdit = findViewById(R.id.startPointEditTV);
        startPointEdit.setText(coordinateToAddress(startPointCoordinate));
        startPointEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //click listener for start point text field
                Intent changeStartPoint = new Intent(EditTripActivity.this, StartPointActivity.class);
                startActivity(changeStartPoint);
                finish();
            }
        });

        String destinationCoordinate = location.getString("destinationEdit", "");
        EditText destinationEdit = findViewById(R.id.destinationEditTv);
        destinationEdit.setText(coordinateToAddress(destinationCoordinate));
        destinationEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //click listener for destination text field
                Intent changeDestination = new Intent(EditTripActivity.this, DestOnMapActivity.class);
                startActivity(changeDestination);
                finish();
            }
        });


    }

    public void findRouteEdit(View view){
        //open route activity
        Intent calculateRout = new Intent(EditTripActivity.this, RouteActivity.class);
        startActivity(calculateRout);
        finish();

    }

    private String coordinateToAddress(String input) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        String output = null;
        String[] coordinate = input.split(",");
        double latitude = Double.parseDouble(coordinate[0]);
        double longitude = Double.parseDouble(coordinate[1]);

        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses.size() != 0) {
                output = addresses.get(0).getAddressLine(0);

            } else {
                Toast.makeText(getApplicationContext(), "No Name for location", Toast.LENGTH_SHORT).show();
                output = input;
            }
        } catch (IOException e) {
            Log.d(TAG, "coordinateToAddress : OOOPS! one error for ya! : " + e.getMessage());
        }

        return output;
    }

    @Override
    public void onBackPressed(){
        location.edit().remove("editMode").apply();
        location.edit().remove("tripNameEdit").apply();
        location.edit().remove("startPointEdit").apply();
        location.edit().remove("destinationEdit").apply();

        Intent backToManage = new Intent(EditTripActivity.this, ManageTripActivity.class);
        backToManage.putExtra("clickedTripName", tripName);
        startActivity(backToManage);

        super.onBackPressed();
    }

    //back arrow function
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            location.edit().remove("editMode").apply();
            location.edit().remove("tripNameEdit").apply();
            location.edit().remove("startPointEdit").apply();
            location.edit().remove("destinationEdit").apply();

            finish();
        }
        return super.onOptionsItemSelected(menuItem);
    }
}
