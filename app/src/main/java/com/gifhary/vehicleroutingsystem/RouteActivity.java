package com.gifhary.vehicleroutingsystem;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.TravelMode;

import org.joda.time.DateTime;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RouteActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleMap.OnPolylineClickListener {
    private static final String TAG = "Route Activity";
    private static final String PREFERENCES = "preferences";

    private String startPoint = "";
    private String destination = "";

    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";
    private MapView mapView;
    private GoogleMap gMap;

    private boolean editMode = false;

    private ArrayList<PolylineData> mPolyLinesData = new ArrayList<>();
    private ArrayList<RouteInfo> routeInfos = new ArrayList<>();

    private ListView listViewRoute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);


        //Google map part
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);//Google map part

        new Thread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences locations = getSharedPreferences(PREFERENCES, 0);
                editMode = locations.getBoolean("editMode", false);

                if (editMode) {
                    String startPointCoordinate = locations.getString("startPointEdit", "");
                    String destinationCoordinate = locations.getString("destinationEdit", "");

                    startPoint = startPointCoordinate;
                    destination = destinationCoordinate;

                    if (startPointCoordinate != "" && destinationCoordinate != "") {
                        requestDirections(startPointCoordinate, destinationCoordinate);
                    } else {
                        Log.d(TAG, "Edit Mode : Empty locations in shared preferences");
                    }

                } else {
                    String startLat = locations.getString("startLocationLat", "");
                    String startLong = locations.getString("startLocationLong", "");

                    String destLat = locations.getString("destLocationLat", "");
                    String destLong = locations.getString("destLocationLong", "");

                    startPoint = startLat + "," + startLong;
                    destination = destLat + "," + destLong;

                    requestDirections(startPoint, destination);//call calculateDirection function
                }
            }
        }).start();

        listViewRoute = findViewById(R.id.listViewRoute);
        listViewRoute.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.e(TAG, "List View Item Click : Route ID : " + routeInfos.get(position).getRouteID());
                for (final PolylineData polylineData : mPolyLinesData) {

                    if (polylineData.getPolyline().getId().equals(routeInfos.get(position).getRouteID())) {
                        final SharedPreferences preferences = getSharedPreferences(PREFERENCES, 0);
                        AlertDialog.Builder builder = new AlertDialog.Builder(RouteActivity.this);

                        final String distance = routeInfos.get(position).getDistance();
                        final String duration = routeInfos.get(position).getDuration();
                        final String durationInTraffic = routeInfos.get(position).getDurationInTraffic();
                        //set route dialog box
                        builder.setTitle("Set New Route");
                        builder.setMessage("Set \"" + routeInfos.get(position).getRouteName() + "\" to the trip?"
                                + "\nDistance : " + distance
                                + "\nDuration : " + duration
                                + "\nDuration In Current Traffic : " + durationInTraffic);

                        //dialog box yes button for log out
                        builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                List<LatLng> points = polylineData.getPolyline().getPoints();
                                List<GeoPoint> geoPoints = new ArrayList<>();

                                for (int i = 0; i < points.size(); i++) {
                                    geoPoints.add(latLngToGeoPoint(points.get(i)));
                                }

                                if (editMode) {
                                    String tripName = preferences.getString("tripNameEdit", "");

                                    //if there is any changes in startPoint or destination, it will updated in editMode on
                                    updateTrip();
                                    //call saveDirection function with list parameter contains polyline coordinate
                                    saveDirection(tripName, geoPoints, distance, duration);
                                    try {
                                        Thread.sleep(1000);//set 1 second gap before closing this activity
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    Intent backToManageTrip = new Intent(RouteActivity.this, ManageTripActivity.class);
                                    backToManageTrip.putExtra("clickedTripName", tripName);
                                    startActivity(backToManageTrip);
                                    finish();

                                } else {
                                    String tripName = preferences.getString("tripName", "");

                                    //call saveDirection function with list parameter contains polyline coordinate
                                    saveDirection(tripName, geoPoints, distance, duration);
                                    try {
                                        Thread.sleep(1000);//set 1 second gap before closing this activity
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    finish();
                                }


                            }
                        });

                        //dialog box no button, do nothing
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        AlertDialog alert = builder.create();
                        alert.show();
                    }
                }
            }
        });

    }

    private GeoPoint latLngToGeoPoint(LatLng latLng) {
        GeoPoint geoPoint = new GeoPoint(latLng.latitude, latLng.longitude);

        return geoPoint;
    }

    private void updateTrip() {

        final SharedPreferences preferences = getSharedPreferences(PREFERENCES, 0);

        String companyID = preferences.getString("companyID", "");
        String tripName = preferences.getString("tripNameEdit", "");

        final DocumentReference documentReference = FirebaseFirestore.getInstance()
                .document("users/" + companyID + "/trips/" + tripName);
        documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    Map<String, Object> tripUpdate = new HashMap<>();
                    tripUpdate.put("startPoint", startPoint);
                    tripUpdate.put("destination", destination);

                    documentReference.update(tripUpdate);
                }
            }
        });

    }

    private void saveDirection(String tripName, final List<GeoPoint> geoPointList, final String distance, final String duration) {
        final SharedPreferences preferences = getSharedPreferences(PREFERENCES, 0);

        String companyID = preferences.getString("companyID", "");

        final DocumentReference documentReference = FirebaseFirestore.getInstance()
                .document("users/" + companyID + "/trips/" + tripName + "/selectedRoute/route");
        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    Map<String, Object> routeToSave = new HashMap<>();
                    routeToSave.put("distance", distance);
                    routeToSave.put("duration", duration);
                    routeToSave.put("route", geoPointList);

                    documentReference.set(routeToSave);//save the route to firestore

                    Toast.makeText(getApplicationContext(), "Route is updated successfully", Toast.LENGTH_SHORT).show();

                    preferences.edit().remove("editMode").apply();
                    preferences.edit().remove("startPointEdit").apply();
                    preferences.edit().remove("destinationEdit").apply();
                    preferences.edit().remove("tripNameEdit").apply();

                    preferences.edit().remove("startLocationLat").apply();
                    preferences.edit().remove("startLocationLong").apply();
                    preferences.edit().remove("destLocationLat").apply();
                    preferences.edit().remove("destLocationLong").apply();
                    preferences.edit().remove("tripName").apply();//end clear trip
                }

            }
        });

    }


    private void requestDirections(final String origin, final String destination) {
        String apiKey = getResources().getString(R.string.API_KEY);

        GeoApiContext geoApiContext = new GeoApiContext.Builder().apiKey(apiKey).build();
        DirectionsApiRequest directions = new DirectionsApiRequest(geoApiContext);

        directions.alternatives(true);

        DateTime now = new org.joda.time.DateTime();
        directions.departureTime(now);

        directions.origin(origin);
        directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                addPolyLineToMap(result);
            }

            @Override
            public void onFailure(Throwable e) {
                Log.e(TAG, "calculateDirections: Failed getting directions: " + e.getMessage());
                Toast.makeText(getApplicationContext(), "Failed getting directions", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void addPolyLineToMap(final DirectionsResult result) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: result routes: " + result.routes.length);

                if (mPolyLinesData.size() > 0) {
                    for (PolylineData polylineData : mPolyLinesData) {
                        polylineData.getPolyline().remove();
                    }
                    mPolyLinesData.clear();
                    mPolyLinesData = new ArrayList<>();
                }

                for (DirectionsRoute route : result.routes) {
                    Log.d(TAG, "run: leg: " + route.legs[0].toString());
                    List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(route.overviewPolyline.getEncodedPath());

                    List<LatLng> newDecodedPath = new ArrayList<>();

                    // This loops through all the LatLng coordinates of ONE polyline.
                    for (com.google.maps.model.LatLng latLng : decodedPath) {
                        newDecodedPath.add(new LatLng(
                                latLng.lat,
                                latLng.lng
                        ));
                    }
                    Polyline polyline = gMap.addPolyline(new PolylineOptions()
                            .addAll(newDecodedPath)
                            .color(Color.GRAY));

                    polyline.setClickable(true);

                    mPolyLinesData.add(new PolylineData(polyline, route.legs[0]));
                }
                showTripList();
            }
        });

    }

    private void showTripList() {
        listViewRoute = findViewById(R.id.listViewRoute);

        int i = 1;
        for (PolylineData polylineData : mPolyLinesData) {
            String routeID = polylineData.getPolyline().getId();
            String distance = polylineData.getLeg().distance.toString();
            String duration = polylineData.getLeg().duration.toString();
            String durationInTraffic = polylineData.getLeg().durationInTraffic.toString();

            routeInfos.add(new RouteInfo("Route " + i, routeID, distance, duration, durationInTraffic));
            Log.d(TAG, "Show Trip List : Route ID : " + polylineData.getPolyline().getId());
            i++;
        }

        RouteInfoAdapter routeInfoAdapter = new RouteInfoAdapter(RouteActivity.this, R.layout.adapter_layout, routeInfos);
        listViewRoute.setAdapter(routeInfoAdapter);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;

        //move camera focus to star point of route
        if (!startPoint.equals(",") && !startPoint.equals("")) {
            Log.d(TAG, "onMapReady: move camera, startPoint value : " + startPoint);

            String[] latLong = startPoint.split(",");
            LatLng startLatLng = new LatLng(Double.parseDouble(latLong[0]), Double.parseDouble(latLong[1]));
            Marker marker = gMap.addMarker(new MarkerOptions().position(startLatLng));
            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.start_marker));

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(startLatLng, 10);
            gMap.animateCamera(cameraUpdate);
        }

        if (!destination.equals(",") && !destination.equals("")) {
            String[] latLong = destination.split(",");
            LatLng destLatLng = new LatLng(Double.parseDouble(latLong[0]), Double.parseDouble(latLong[1]));

            gMap.addMarker(new MarkerOptions().position(destLatLng));
        }

        gMap.setOnPolylineClickListener(this);

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
        if (editMode) {
            Intent back = new Intent(RouteActivity.this, EditTripActivity.class);
            startActivity(back);
        }
        super.onBackPressed();
    }

    @Override
    public void onPolylineClick(Polyline polyline) {
        Log.d(TAG, "onPolylineClick: ID : " + polyline.getId());

        int i = 1;
        for (PolylineData polylineData : mPolyLinesData) {
            Log.d(TAG, "onPolylineClick: toString: " + polylineData.toString());

            if (polyline.getId().equals(polylineData.getPolyline().getId())) {

                polylineData.getPolyline().setColor(Color.CYAN);
                polylineData.getPolyline().setZIndex(1);

                LatLng endLocation = new LatLng(
                        polylineData.getLeg().endLocation.lat,
                        polylineData.getLeg().endLocation.lng
                );

                Marker marker = gMap.addMarker(new MarkerOptions()
                        .position(endLocation)
                        .title("Route : " + i)
                        .snippet("Duration : " + polylineData.getLeg().duration)
                );
                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.transparent));
                marker.showInfoWindow();

            } else {
                polylineData.getPolyline().setColor(Color.GRAY);
                polylineData.getPolyline().setZIndex(0);
            }
            i++;
        }
    }
}
