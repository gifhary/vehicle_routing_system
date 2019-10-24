package com.gifhary.vehicleroutingsystem;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ManageTripActivity extends AppCompatActivity implements OnMapReadyCallback {

    private String TAG = "ManageActivity";

    private static final String PREFERENCES = "preferences";

    private Geocoder geocoder = new Geocoder(this, Locale.getDefault());

    private String startPoint = null;
    private String destination = null;

    private String clickedTripName = null;

    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";
    private MapView mapView;
    private GoogleMap gMap;

    private String companyID;

    private ArrayList<String> driverListDB = new ArrayList<>();

    private String[] driverList;
    private boolean[] checkedItems;
    private ArrayList<Integer> userItems = new ArrayList<>();

    private ArrayList<LatLng> coordinate = new ArrayList<>();

    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_trip);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//showing back arrow on toolbar

        requestQueue = Volley.newRequestQueue(ManageTripActivity.this);//notification part

        SharedPreferences preferences = getSharedPreferences(PREFERENCES, 0);
        companyID = preferences.getString("companyID", "");//getting company name from sharedPreferences

        getDriverList();//getting driver list from database

        //Google map part
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }
        mapView = findViewById(R.id.mapViewManage);
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);//Google map part

        clickedTripName = getIntent().getExtras().getString("clickedTripName");
        Log.d(TAG, "Intent extra from Trip Activity : " + clickedTripName);

        TextView tripNameTV = findViewById(R.id.tripNameTV);
        tripNameTV.setText(clickedTripName);

        new Thread(new Runnable() {
            @Override
            public void run() {
                //accessing database
                FirebaseFirestore.getInstance().document("users/" + companyID + "/trips/" + clickedTripName).get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                startPoint = documentSnapshot.getString("startPoint");
                                destination = documentSnapshot.getString("destination");

                                //draw polyline
                                drawPolyLine(startPoint, destination);

                                TextView startPointTV = findViewById(R.id.startPointTV);
                                TextView destinationTV = findViewById(R.id.destinationTV);

                                if (startPoint != null && destination != null) {
                                    //set text on start point and destination text view using coordinateToAddress method
                                    startPointTV.setText(coordinateToAddress(startPoint));
                                    destinationTV.setText(coordinateToAddress(destination));
                                } else {
                                    startPointTV.setText("Database Error");
                                    destinationTV.setText("Database Error");
                                }
                            }
                        });
            }
        }).start();

        //track vehicle click listener
        ImageView trackVehicle = findViewById(R.id.trackBtn);
        trackVehicle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (coordinate.size() > 0) {
                    Intent goToTrackVehicle = new Intent(ManageTripActivity.this, TrackActivity.class);
                    goToTrackVehicle.putExtra("LatLngArrayList", coordinate);
                    goToTrackVehicle.putExtra("startPoint", startPoint);
                    goToTrackVehicle.putExtra("destination", destination);
                    goToTrackVehicle.putExtra("tripName", clickedTripName);
                    goToTrackVehicle.putExtra("driverList", driverList);
                    startActivity(goToTrackVehicle);
                } else {
                    Toast.makeText(getApplicationContext(), "Route is not set", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //select driver click listener
        ImageView selectDriver = findViewById(R.id.sDriverBtn);
        selectDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (driverListDB.size() > 0) {
                    if (coordinate.size() > 0) {
                        showDriverListDialog();
                    } else {
                        Toast.makeText(getApplicationContext(), "Route is not set", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "No registered driver", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //set route click listener
        ImageView setRoute = findViewById(R.id.setRouteBtn);
        setRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickedTripName != null && startPoint != null && destination != null) {
                    setRoute(clickedTripName, startPoint, destination);
                }
            }
        });

        //delete click listener
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ImageView delete = findViewById(R.id.deleteBtn);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //delete trip dialog box
                builder.setTitle("Delete trip");
                builder.setMessage("Are you sure want to delete " + clickedTripName + "?");

                //dialog box YES button, delete trip
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteDocument(companyID, clickedTripName);
                    }
                });

                //dialog box no button, do nothing
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });

    }

    private void showDriverListDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(ManageTripActivity.this);
        builder.setTitle("Send To Drivers");
        builder.setMultiChoiceItems(driverList, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                if (isChecked) {
                    userItems.add(which);
                } else {
                    userItems.remove(Integer.valueOf(which));
                }
            }
        });

        builder.setCancelable(false);

        builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                ArrayList<String> selectedDriverList = new ArrayList<>();//list of the driver to send the trip

                for (int i = 0; i < userItems.size(); i++) {
                    Log.d(TAG, "Checked Items : " + driverList[userItems.get(i)]);
                    selectedDriverList.add(driverList[userItems.get(i)]);
                }
                sendToDrivers(selectedDriverList);//send the trip to drivers database
                generateSelectedDriverToken(selectedDriverList);//generate selected driver's token

            }
        });

        builder.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void generateSelectedDriverToken(ArrayList<String> selectedDriverList) {
        for (String selectedDriver : selectedDriverList) {
            FirebaseFirestore.getInstance().document("drivers/" + selectedDriver)
                    .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot documentSnapshot = task.getResult();
                        String token = documentSnapshot.getString("token");

                        sendNotifToDriver(token);

                    } else {
                        Log.d(TAG, "generateSelectedDriverToken : failed getting driver's token");
                    }
                }
            });

        }


    }


    private void sendNotifToDriver(String token) {
        JSONObject json = new JSONObject();
        String URL = "https://fcm.googleapis.com/fcm/send";

        try {
            json.put("to", token);

            JSONObject notificationObj = new JSONObject();
            notificationObj.put("title", "New trip assigned to you");
            notificationObj.put("body", clickedTripName);
            notificationObj.put("sound", "default");

            json.put("notification", notificationObj);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, URL,
                    json,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {

                            Log.d("MUR", "onResponse: ");
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d("MUR", "onError: " + error.networkResponse);
                }
            }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> header = new HashMap<>();
                    header.put("Content-type", "application/json");
                    header.put("Authorization", "key=AIzaSyB4bIFZvfuYHe4_CHxAR_XiK_sWqDFRyJw");
                    return header;
                }
            };
            requestQueue.add(request);

        } catch (JSONException e) {
            Log.d(TAG, "JSON Error : " + e.getMessage());
        }
    }


    private void sendToDrivers(ArrayList<String> input) {

        WriteBatch batchTrip = FirebaseFirestore.getInstance().batch();

        DocumentReference[] sendingTrip = new DocumentReference[input.size()];

        Map<String, Object> tripToSend = new HashMap<>();

        tripToSend.put("tripName", clickedTripName);

        for (int i = 0; i < input.size(); i++) {
            sendingTrip[i] = FirebaseFirestore.getInstance()
                    .document("drivers/" + input.get(i) + "/trips/" + clickedTripName);
            batchTrip.set(sendingTrip[i], tripToSend);
        }
        batchTrip.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(getApplicationContext(), "Trip sent", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void getDriverList() {
        FirebaseFirestore.getInstance().collection("drivers").whereEqualTo("companyID", companyID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if (document.getId() != null) {
                                    Log.d(TAG, "Driver List : " + document.getId());
                                    driverListDB.add(document.getId());//get driver username where the companyID is same
                                }
                            }
                            driverList = driverListDB.toArray(new String[driverListDB.size()]);
                            checkedItems = new boolean[driverList.length];

                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    private void changeSetRouteBtn() {
        TextView setRoute = findViewById(R.id.setRouteTxt);
        setRoute.setText("Update Route");
    }

    private void drawPolyLine(final String startPoint, final String destination) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                //show existing route on map, if any
                FirebaseFirestore.getInstance().document("users/" + companyID + "/trips/" + clickedTripName + "/selectedRoute/route")
                        .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                changeSetRouteBtn();//change set route button name to update route if the route already set


                                List<GeoPoint> points = (List<GeoPoint>) document.get("route");
                                String distance = document.getString("distance");
                                String duration = document.getString("duration");

                                for (GeoPoint geoPoint : points) {
                                    Log.d(TAG, "Test : " + geoPoint.getLatitude() + "," + geoPoint.getLongitude());

                                    coordinate.add(new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude()));//coordinate for polyline
                                }
                                //draw polyline along the route coordinate
                                gMap.addPolyline(new PolylineOptions().addAll(coordinate).color(Color.CYAN));

                                //put start point marker
                                Marker startPointMarker = gMap.addMarker(new MarkerOptions()
                                        .position(stringToLatLng(startPoint))
                                        .title(distance)
                                        .snippet(duration)
                                );
                                startPointMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.start_marker));
                                startPointMarker.showInfoWindow();

                                //move map camera
                                CameraUpdate cameraUpdate = CameraUpdateFactory
                                        .newLatLngZoom(coordinate.get(coordinate.size() / 2), 9);
                                gMap.moveCamera(cameraUpdate);

                                //put destination marker
                                gMap.addMarker(new MarkerOptions().position(stringToLatLng(destination)));

                            } else {
                                Log.d(TAG, "No such document");
                                //set marker on start point and show no route has been set info window
                                Marker startPointMarker = gMap.addMarker(new MarkerOptions()
                                        .position(stringToLatLng(startPoint))
                                        .title("No route has been set")
                                );
                                startPointMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.start_marker));
                                startPointMarker.showInfoWindow();

                                //move map camera
                                CameraUpdate cameraUpdate = CameraUpdateFactory
                                        .newLatLngZoom(stringToLatLng(startPoint), 10);
                                gMap.animateCamera(cameraUpdate);

                                //put destination marker
                                gMap.addMarker(new MarkerOptions().position(stringToLatLng(destination)));
                            }
                        } else {
                            Log.d(TAG, "get failed with ", task.getException());
                        }
                    }
                });
            }
        });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;

    }


    //method for edit button, put trip name, start point and destination to shared preferences
    private void setRoute(final String tripName, final String startPoint, final String destination) {
        SharedPreferences settings = getSharedPreferences(PREFERENCES, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("editMode", true);

        //trip name
        editor.putString("tripNameEdit", tripName);

        //start point
        editor.putString("startPointEdit", startPoint);

        //destination
        editor.putString("destinationEdit", destination);

        editor.apply();

        Intent setRoute = new Intent(ManageTripActivity.this, EditTripActivity.class);
        startActivity(setRoute);
        finish();

    }


    private void deleteDocument(final String companyID, final String tripName) {
        ProgressBar progressBar = findViewById(R.id.progressBarDelete);
        progressBar.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {

                //Delete route if it exists in the trip
                final DocumentReference documentReference = FirebaseFirestore.getInstance()
                        .document("users/" + companyID + "/trips/" + clickedTripName + "/selectedRoute/route");
                documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            Log.d(TAG, "Delete document : Route exists in this trip : " + tripName);
                            documentReference.delete();
                        } else {
                            Log.d(TAG, "Delete document : Route does not exists in this trip : " + tripName);
                        }
                    }
                });

                FirebaseFirestore.getInstance().document("users/" + companyID + "/trips/" + tripName).delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "DocumentSnapshot successfully deleted!");
                                Toast.makeText(getApplicationContext(), "Trip successfully deleted", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error deleting trip", e);
                                Toast.makeText(getApplicationContext(), "Error deleting trip", Toast.LENGTH_SHORT).show();
                            }
                        });

                deleteTripInDriverDB(driverListDB, clickedTripName);
            }
        }).start();
    }

    private void deleteTripInDriverDB(final ArrayList<String> driverNameList, final String tripName) {

        WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (String driverNames : driverNameList) {
            DocumentReference delDriverTrip = FirebaseFirestore.getInstance()
                    .document("drivers/" + driverNames + "/trips/" + tripName);
            batch.delete(delDriverTrip);
        }
        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.w(TAG, "Trip deleted in driver's database");
                }
            }
        });

    }

    private LatLng stringToLatLng(String input) {
        LatLng output;
        String[] coordinate = input.split(",");
        double latitude = Double.parseDouble(coordinate[0]);
        double longitude = Double.parseDouble(coordinate[1]);

        output = new LatLng(latitude, longitude);

        return output;
    }

    private String coordinateToAddress(String input) {
        String output = null;
        String[] coordinate = input.split(",");
        double latitude = Double.parseDouble(coordinate[0]);
        double longitude = Double.parseDouble(coordinate[1]);

        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses.size() != 0) {
                output = addresses.get(0).getAdminArea();

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

    //back arrow function
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(menuItem);
    }
}
