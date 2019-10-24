package com.gifhary.vehicleroutingsystem;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.ArrayList;

import javax.annotation.Nullable;

public class TripActivity extends AppCompatActivity

        implements NavigationView.OnNavigationItemSelectedListener {

    final String PREFERENCES = "preferences";
    final String TAG = "TripActivity";
    private ArrayList<String> tripList = new ArrayList();//store list of trip name

    private ListView tripListView;

    private FirebaseAuth firebaseAuth;

    private String companyID;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        firebaseAuth = FirebaseAuth.getInstance();//user authentication

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        SharedPreferences preferences = getSharedPreferences(PREFERENCES, 0);
        String companyName = preferences.getString("companyName", "");//getting company name from shared preferences

        View view = navigationView.getHeaderView(0);
        TextView textView = view.findViewById(R.id.name);
        textView.setText(companyName);//set company name in navigation drawer

        companyID = preferences.getString("companyID", "");//get companyID in shared preferences
        if (!companyID.equals("")) {

            generateTokenId(companyID);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    //Get trip list and listen to document addition in real time
                    FirebaseFirestore.getInstance().collection("users/" + companyID + "/trips")
                            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                @Override
                                public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException e) {
                                    if (e != null) {
                                        Log.w(TAG, "Listen failed.", e);
                                        return;
                                    }
                                    tripList.clear();//clear the ArrayList, because this method get all the document id instead of
                                    //the one that just created
                                    for (QueryDocumentSnapshot doc : value) {
                                        if (doc.getId() != null) {
                                            tripList.add(doc.getId());//store the document IDs to ArrayList

                                            Log.d(TAG, "Retrieved data: " + doc.getId());
                                        }
                                    }
                                    showTripList();//show trip name to listView
                                }
                            });
                }
            }).start();
        } else {
            Toast.makeText(getApplicationContext(), "Please login first", Toast.LENGTH_LONG).show();
        }

        tripListView = findViewById(R.id.tripList);
        tripListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "ListView : Clicked Text: " + tripList.get(position));
                Intent manageTrip = new Intent(TripActivity.this, ManageTripActivity.class);
                manageTrip.putExtra("clickedTripName", tripList.get(position));
                startActivity(manageTrip);
            }
        });

    }

    private void generateTokenId(final String companyID) {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }

                        String token = task.getResult().getToken();//get driver token Id
                        Log.d(TAG, "Token ID : " + token);

                        FirebaseFirestore.getInstance().document("users/" + companyID)
                                .update("token", token);
                    }
                });

    }


    @Override
    public void onStart() {
        super.onStart();
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(getApplicationContext(), "Please login first", Toast.LENGTH_SHORT).show();
            Intent notLoggedIn = new Intent(TripActivity.this, LoginActivity.class);
            startActivity(notLoggedIn);
            finish();
        }
    }

    public void showTripList() {
        tripListView = findViewById(R.id.tripList);
        TextView noTripTxt = findViewById(R.id.noTripTxt);

        if (tripList.size() != 0) {
            if (noTripTxt.getVisibility() == View.VISIBLE) {
                noTripTxt.setVisibility(View.GONE);
            }
            ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, tripList);
            tripListView.setAdapter(arrayAdapter);//show trip list on screen
        } else {
            tripListView.setAdapter(null);

            noTripTxt.setVisibility(View.VISIBLE);//show "no trip has been made yet" text to user

        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.addTrip) {
            Intent addNewTrip = new Intent(TripActivity.this, NewTripActivity.class);
            startActivity(addNewTrip);

        } else if (id == R.id.rejectedTrip) {
            Intent rejectedTrip = new Intent(TripActivity.this, RejectedTripActivity.class);
            startActivity(rejectedTrip);

        } else if (id == R.id.setting) {
            //TODO do something on setting button
        } else if (id == R.id.logout) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            //log out dialog box
            builder.setTitle("Log out");
            builder.setMessage("Are you sure want to log out?");

            //dialog box yes button for log out
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferences preferences = getSharedPreferences(PREFERENCES, 0);
                    preferences.edit().clear().apply();

                    firebaseAuth.signOut();
                    FirebaseFirestore.getInstance().document("users/" + companyID)
                            .update("token", FieldValue.delete());

                    Intent logout = new Intent(TripActivity.this, LoginActivity.class);
                    startActivity(logout);
                    finish();
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

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
