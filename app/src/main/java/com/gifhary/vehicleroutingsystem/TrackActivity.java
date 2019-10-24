package com.gifhary.vehicleroutingsystem;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;

public class TrackActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "TrackActivity";

    private static final String PREFERENCES = "preferences";

    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";
    private MapView mapView;
    private GoogleMap gMap;

    private ArrayList<LatLng> polylineCoordinate = new ArrayList<>();
    private String startPoint = null;
    private String destination = null;

    private String[] driverList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);

        polylineCoordinate = (ArrayList<LatLng>) getIntent().getSerializableExtra("LatLngArrayList");
        startPoint = getIntent().getExtras().getString("startPoint");
        destination = getIntent().getExtras().getString("destination");
        String tripName = getIntent().getExtras().getString("tripName");
        driverList = getIntent().getExtras().getStringArray("driverList");

        SharedPreferences preferences = getSharedPreferences(PREFERENCES, 0);//initialing shared preferences
        String companyID = preferences.getString("companyID", "");

        //Google map part
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }
        mapView = findViewById(R.id.trackMap);
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);//Google map part


        final Marker[] driverMarker = new Marker[driverList.length];//The magic start here
        final DocumentReference docRef = FirebaseFirestore.getInstance()
                .document("users/" + companyID + "/trips/" + tripName);
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }
                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, "Current data: " + snapshot.getData());

                    int i = 0;
                    for (String list : driverList) {
                        String locations = snapshot.getString(list);//get driver location by driver's username
                        Log.d(TAG, "Drivers Location List : " + locations);


                        if (driverMarker[i] != null) {
                            driverMarker[i].remove();//Magic happen here, don't ask me how it work
                        }

                        if (locations != null) {
                            driverMarker[i] = gMap.addMarker(new MarkerOptions()
                                    .position(stringToLatLng(locations))
                                    .title(list)
                            );
                            driverMarker[i].setIcon(BitmapDescriptorFactory.fromResource(R.drawable.truck_blank));
                            driverMarker[i].showInfoWindow();
                            i++;
                        }
                    }

                } else {
                    Log.d(TAG, "Current data: null");
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


    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;

        if (polylineCoordinate.size() > 0) {
            gMap.addPolyline(new PolylineOptions().addAll(polylineCoordinate).color(Color.CYAN));
        }

        if (startPoint != null && destination != null) {
            //put start point marker
            Marker startPointMarker = gMap.addMarker(new MarkerOptions().position(stringToLatLng(startPoint)));
            startPointMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.start_marker));
            startPointMarker.showInfoWindow();

            //put destination marker
            gMap.addMarker(new MarkerOptions().position(stringToLatLng(destination)));
        }

        //move map camera
        CameraUpdate cameraUpdate = CameraUpdateFactory
                .newLatLngZoom(polylineCoordinate.get(polylineCoordinate.size() / 2), 9);
        gMap.moveCamera(cameraUpdate);

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

}
