package com.gifhary.vehicleroutingsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

import javax.annotation.Nullable;

public class RejectedTripActivity extends AppCompatActivity {
    final String PREFERENCES = "preferences";
    final String TAG = "RejectedTripActivity";

    private String companyID;

    private ArrayList<String> rTripList = new ArrayList<>();
    private ListView rTripListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rejected_trip);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//showing back arrow on toolbar

        SharedPreferences preferences = getSharedPreferences(PREFERENCES, 0);
        companyID = preferences.getString("companyID", "");//getting company ID from sharedPreferences

        listenToUpdate();//listen to rejected trip update

        rTripListView = findViewById(R.id.rTripList);
        rTripListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "ListView : Clicked Text: " + rTripList.get(position));
                Intent openRTrip = new Intent(RejectedTripActivity.this, RTripDetailActivity.class);
                openRTrip.putExtra("clickedRejectedTrip", rTripList.get(position));
                startActivity(openRTrip);
            }
        });
    }

    private void listenToUpdate() {
        if (!companyID.equals("") && companyID != null) {
            //Get trip list and listen to document addition in real time
            FirebaseFirestore.getInstance().collection("users/" + companyID + "/rejectedTrip")
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException e) {
                            if (e != null) {
                                Log.w(TAG, "Listen failed.", e);
                                return;
                            }
                            rTripList.clear();//clear the ArrayList, because this method get all the document id instead of
                            //the one that just created
                            for (QueryDocumentSnapshot doc : value) {
                                if (doc.getId() != null) {
                                    rTripList.add(doc.getId());//store the document IDs to ArrayList

                                    Log.d(TAG, "Rejected Trip : " + doc.getId());
                                }
                            }
                            showRTripList();//show trip name to listView
                        }
                    });
        }
    }

    public void showRTripList() {
        TextView noRTripTxt = findViewById(R.id.noRTripTxt);

        if (rTripList.size() > 0) {
            if (noRTripTxt.getVisibility() == View.VISIBLE) {
                noRTripTxt.setVisibility(View.GONE);
            }
            ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, rTripList);
            rTripListView.setAdapter(arrayAdapter);//show trip list on screen
        } else {
            rTripListView.setAdapter(null);

            noRTripTxt.setVisibility(View.VISIBLE);//show "no trip has been made yet" text to user

        }
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
