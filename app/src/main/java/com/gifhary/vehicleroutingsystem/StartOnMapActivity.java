package com.gifhary.vehicleroutingsystem;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class StartOnMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";
    private MapView mapView;
    private GoogleMap gMap;

    private LatLng userLoc;

    private ConstraintLayout addressOkGroup;
    private Geocoder geocoder = new Geocoder(this, Locale.getDefault());

    private FusedLocationProviderClient fusedLocationClient;

    private static final String PREFERENCES = "preferences";

    private double selectedLocationLat;
    private double selectedLocationLong;

    private double searchLat;
    private double searchLong;

    private boolean editMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_on_map);

        SharedPreferences settings = getSharedPreferences(PREFERENCES, 0);
        editMode = settings.getBoolean("editMode", false);

        //Google map part
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);//Google map part

        //hide address name and ok button layout on first open
        addressOkGroup = findViewById(R.id.addressOk);
        addressOkGroup.setVisibility(View.GONE);

    }

    //set location button for start point
    public void setLocation(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences settings = getSharedPreferences(PREFERENCES, 0);
                SharedPreferences.Editor editor = settings.edit();

                if (editMode) {
                    //put selected start point on map to shared preferences in mode private
                    editor.putString("startPointEdit", String.valueOf(selectedLocationLat) + "," + String.valueOf(selectedLocationLong));
                    editor.apply();

                    Intent setStartPointByMap = new Intent(StartOnMapActivity.this, EditTripActivity.class);
                    startActivity(setStartPointByMap);
                    finish();

                } else {
                    //put selected start point on map to shared preferences in mode private
                    editor.putString("startLocationLat", String.valueOf(selectedLocationLat));
                    editor.putString("startLocationLong", String.valueOf(selectedLocationLong));
                    editor.apply();

                    Intent setStartPointByMap = new Intent(StartOnMapActivity.this, NewTripActivity.class);
                    startActivity(setStartPointByMap);
                    finish();
                }
            }
        }).start();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;

        //get user location on map displayed and and move camera to user position
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "Please enable location", Toast.LENGTH_LONG).show();
        } else {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        userLoc = new LatLng(location.getLatitude(), location.getLongitude());
                        CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(userLoc, 15);
                        gMap.moveCamera(yourLocation);//move camera to user position
                    } else {
                        Toast.makeText(getApplicationContext(), "Cannot access your location", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
        gMap.setMyLocationEnabled(true);

        gMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                gMap.clear();
                Marker marker = gMap.addMarker(new MarkerOptions().position(latLng));
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));

                try {
                    selectedLocationLat = latLng.latitude;
                    selectedLocationLong = latLng.longitude;

                    List<Address> geoAddress = geocoder.getFromLocation(selectedLocationLat, selectedLocationLong, 1);
                    if (geoAddress.size() != 0) {
                        String clickedAddress = geoAddress.get(0).getAddressLine(0);
                        //show address name and ok button to continue
                        addressOkGroup = findViewById(R.id.addressOk);
                        addressOkGroup.setVisibility(View.VISIBLE);

                        EditText selectedLoc = findViewById(R.id.selectedLoc);
                        selectedLoc.setText(clickedAddress);
                    } else {
                        if (addressOkGroup.getVisibility() == View.VISIBLE) {
                            addressOkGroup.setVisibility(View.GONE);
                            Toast.makeText(getApplicationContext(), "Please select proper location", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Please select proper location", Toast.LENGTH_SHORT).show();
                        }

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        final EditText searchTxt = findViewById(R.id.searchTxt);
        //Search image (As button) function
        ImageView search = findViewById(R.id.searchBtn);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Address> addresses;

                String text = searchTxt.getText().toString();
                if (!text.equals("")) {
                    try {
                        addresses = geocoder.getFromLocationName(text, 1);
                        if (addresses.size() > 0) {
                            searchLat = addresses.get(0).getLatitude();
                            searchLong = addresses.get(0).getLongitude();

                            LatLng searchCoordinate = new LatLng(searchLat, searchLong);
                            CameraUpdate searchLocationCameraMove = CameraUpdateFactory.newLatLngZoom(searchCoordinate, 13);
                            gMap.animateCamera(searchLocationCameraMove);


                        } else {
                            Toast.makeText(getApplicationContext(), "No result", Toast.LENGTH_LONG).show();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onBackPressed() {
        Intent selectOnMap = new Intent(StartOnMapActivity.this, StartPointActivity.class);
        startActivity(selectOnMap);
        finish();
        super.onBackPressed();
    }
}
