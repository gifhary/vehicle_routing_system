package com.gifhary.vehicleroutingsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NewTripActivity extends AppCompatActivity {

    private static final String PREFERENCES = "preferences";
    private static final String TAG = "New Trip Activity";

    private String startLocationLat;
    private String startLocationLong;
    private List<Address> startAddress;
    private Geocoder geocoder = new Geocoder(this, Locale.getDefault());

    private String destinationLat;
    private String destinationLong;
    private List<Address> destAddress;

    private SharedPreferences location;

    private EditText tripName;
    private EditText startPoint;
    private EditText destination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_trip);
        location = getSharedPreferences(PREFERENCES, 0);//initialing shared preferences

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//showing back arrow on toolbar

        tripName = findViewById(R.id.tripNameEditTV);
        tripName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                SharedPreferences.Editor editor = location.edit();
                String tripNameTxt = tripName.getText().toString();
                Log.d(TAG, "tripName : onFocusChange putString");
                //put trip name to shared preferences in mode private
                editor.putString("tripName", tripNameTxt);
                editor.apply();
            }
        });
        tripName.setText(location.getString("tripName", ""));

        //set on click listener for start point text field
        //to open location search
        startPoint = findViewById(R.id.startPointEditTV);
        startPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent setStartPoint = new Intent(NewTripActivity.this, StartPointActivity.class);
                startActivity(setStartPoint);
                finish();
            }
        });


        //getting start point coordinate from StartPointActivity.java
        startLocationLat = location.getString("startLocationLat", "");
        startLocationLong = location.getString("startLocationLong", "");

        if (!startLocationLat.equals("") && !startLocationLong.equals("")) {
            try {
                startAddress = geocoder.getFromLocation(Double.valueOf(startLocationLat), Double.valueOf(startLocationLong), 1);
                if (startAddress.size() != 0) {
                    //start point address from coordinate
                    String sAddress = startAddress.get(0).getAddressLine(0);

                    //put the address to start point field when location is set
                    startPoint.setText(sAddress);
                } else {
                    Toast.makeText(getApplicationContext(), "No Name for start location", Toast.LENGTH_LONG).show();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        //set on click listener for destination text field
        //to open location search
        destination = findViewById(R.id.destinationEditTv);
        destination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent openDestinationMap = new Intent(NewTripActivity.this, DestOnMapActivity.class);
                startActivity(openDestinationMap);
                finish();
            }
        });


        //getting destination coordinate from DestOnMapActivity.java
        destinationLat = location.getString("destLocationLat", "");
        destinationLong = location.getString("destLocationLong", "");

        if (!destinationLat.equals("") && !destinationLong.equals("")) {
            try {
                destAddress = geocoder.getFromLocation(Double.valueOf(destinationLat), Double.valueOf(destinationLong), 1);
                if (destAddress.size() != 0) {
                    //destination address from coordinate
                    String dAddress = destAddress.get(0).getAddressLine(0);
                    //put the address to destination field when location is set
                    destination.setText(dAddress);
                } else {
                    Toast.makeText(getApplicationContext(), "No Name for destination location", Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void findRoute(View view) {
        tripName = findViewById(R.id.tripNameEditTV);

        String tripText = tripName.getText().toString();
        String startText = startPoint.getText().toString();
        String destinationText = destination.getText().toString();

        if (!tripText.equals("") && !startText.equals("") && !destinationText.equals("")) {
            final ProgressBar progressBar = findViewById(R.id.saving);
            progressBar.setVisibility(View.VISIBLE);

            final String tripName = location.getString("tripName", "");
            final String startPoint = location.getString("startLocationLat", "") + "," + location.getString("startLocationLong", "");
            final String destination = location.getString("destLocationLat", "") + "," + location.getString("destLocationLong", "");

            String companyID = location.getString("companyID", "");
            if (!companyID.equals("")) {
                final DocumentReference documentReference = FirebaseFirestore.getInstance().document("users/" + companyID + "/trips/" + tripName);
                documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Toast.makeText(getApplicationContext(), "Trip name already exists", Toast.LENGTH_LONG).show();
                                progressBar.setVisibility(View.INVISIBLE);
                            } else {
                                Map<String, Object> tripToSave = new HashMap<>();

                                tripToSave.put("tripName", tripName);
                                tripToSave.put("startPoint", startPoint);
                                tripToSave.put("destination", destination);
                                documentReference.set(tripToSave);//save trip details to database
                                Toast.makeText(getApplicationContext(), "Trip saved", Toast.LENGTH_SHORT).show();

                                //open route activity
                                Intent calculateRout = new Intent(NewTripActivity.this, RouteActivity.class);
                                startActivity(calculateRout);
                                finish();

                            }
                        }
                    }
                });
            } else {
                Toast.makeText(getApplicationContext(), "Please login first", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Please fill all field", Toast.LENGTH_SHORT).show();
        }
    }

    public void saveBtn(View view) {
        tripName = findViewById(R.id.tripNameEditTV);

        String tripText = tripName.getText().toString();
        String startText = startPoint.getText().toString();
        String destinationText = destination.getText().toString();

        if (!tripText.equals("") && !startText.equals("") && !destinationText.equals("")) {
            final ProgressBar progressBar = findViewById(R.id.saving);
            progressBar.setVisibility(View.VISIBLE);

            final String tripName = location.getString("tripName", "");
            final String startPoint = location.getString("startLocationLat", "") + "," + location.getString("startLocationLong", "");
            final String destination = location.getString("destLocationLat", "") + "," + location.getString("destLocationLong", "");

            String companyID = location.getString("companyID", "");
            if (!companyID.equals("")) {
                final DocumentReference documentReference = FirebaseFirestore.getInstance().document("users/" + companyID + "/trips/" + tripName);
                documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Toast.makeText(getApplicationContext(), "Trip name already exists", Toast.LENGTH_LONG).show();
                                progressBar.setVisibility(View.INVISIBLE);

                            } else {
                                Map<String, Object> tripToSave = new HashMap<>();

                                tripToSave.put("tripName", tripName);
                                tripToSave.put("startPoint", startPoint);
                                tripToSave.put("destination", destination);
                                documentReference.set(tripToSave);//save trip details to database
                                Toast.makeText(getApplicationContext(), "Trip saved", Toast.LENGTH_SHORT).show();

                                //clear trip data in shared preferences
                                location.edit().remove("startLocationLat").apply();
                                location.edit().remove("startLocationLong").apply();
                                //
                                location.edit().remove("destLocationLat").apply();
                                location.edit().remove("destLocationLong").apply();
                                //
                                location.edit().remove("tripName").apply();//end clear trip

                                finish();

                            }
                        }
                    }
                });
            } else {
                Toast.makeText(getApplicationContext(), "Please login first", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Please fill all field", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        location.edit().remove("startLocationLat").apply();
        location.edit().remove("startLocationLong").apply();

        location.edit().remove("destLocationLat").apply();
        location.edit().remove("destLocationLong").apply();

        location.edit().remove("tripName").apply();
        super.onBackPressed();
    }

    //back arrow function
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            location.edit().remove("startLocationLat").apply();
            location.edit().remove("startLocationLong").apply();

            location.edit().remove("destLocationLat").apply();
            location.edit().remove("destLocationLong").apply();

            location.edit().remove("tripName").apply();
            finish();
        }
        return super.onOptionsItemSelected(menuItem);
    }

}
