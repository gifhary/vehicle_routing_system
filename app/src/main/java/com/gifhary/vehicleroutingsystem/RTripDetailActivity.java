package com.gifhary.vehicleroutingsystem;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class RTripDetailActivity extends AppCompatActivity {

    final String TAG = "RTripDetailActivity";
    final String PREFERENCES = "preferences";

    private String clickedRejectedTrip = null;
    private String companyID;

    private TextView tripNameTxt, driverNameTxt, reasonTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rtrip_detail);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//showing back arrow on toolbar

        clickedRejectedTrip = getIntent().getExtras().getString("clickedRejectedTrip");
        Log.d(TAG, "Intent extra from Rejected Trip Activity : " + clickedRejectedTrip);

        SharedPreferences preferences = getSharedPreferences(PREFERENCES, 0);
        companyID = preferences.getString("companyID", "");//getting company ID from sharedPreferences

        if (!companyID.equals("") && clickedRejectedTrip != null) {
            accessDatabase(companyID, clickedRejectedTrip);
        }

        tripNameTxt = findViewById(R.id.tripNameTxt);

        driverNameTxt = findViewById(R.id.driverNameTxt);

        reasonTxt = findViewById(R.id.reasonTxt);
        reasonTxt.setMovementMethod(new ScrollingMovementMethod());

        //delete button
        ImageView deleteIV = findViewById(R.id.deleteImage);
        deleteIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!companyID.equals("") && clickedRejectedTrip != null) {
                    //delete trip dialog box
                    AlertDialog.Builder builder = new AlertDialog.Builder(RTripDetailActivity.this);
                    builder.setTitle("Delete");
                    builder.setMessage("Are you sure want to delete?");

                    //dialog box YES button, delete trip
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteRejectedTrip(companyID, clickedRejectedTrip);
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
            }
        });

    }

    private void accessDatabase(String companyID, String tripName) {
        FirebaseFirestore.getInstance().document("users/" + companyID + "/rejectedTrip/" + tripName)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            Log.d(TAG, "Database : " + documentSnapshot.getString("tripName"));
                            Log.d(TAG, "Database : " + documentSnapshot.getString("username"));
                            Log.d(TAG, "Database : " + documentSnapshot.getString("reason"));

                            tripNameTxt.setText(documentSnapshot.getString("tripName"));
                            driverNameTxt.setText(documentSnapshot.getString("username"));
                            reasonTxt.setText(documentSnapshot.getString("reason"));

                        }
                    }
                });

    }

    private void deleteRejectedTrip(String companyID, String tripName) {

        FirebaseFirestore.getInstance().document("users/" + companyID + "/rejectedTrip/" + tripName).delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully deleted!");
                        Toast.makeText(getApplicationContext(), "Deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error deleting Rejected Trip Notification", e);
                        Toast.makeText(getApplicationContext(), "Delete failed", Toast.LENGTH_SHORT).show();
                    }
                });
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
