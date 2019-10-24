package com.gifhary.vehicleroutingsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class WelcomeActivity extends AppCompatActivity {
    private static final String PREFERENCES = "preferences";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);


        SharedPreferences settings = getSharedPreferences(PREFERENCES, 0);
        boolean hasLoggedIn = settings.getBoolean("hasLoggedIn", false);

        if (hasLoggedIn) {
            Intent loginSuccess = new Intent(WelcomeActivity.this, TripActivity.class);
            finish();
            startActivity(loginSuccess);

        } else {
            Intent toLogin = new Intent(WelcomeActivity.this, LoginActivity.class);
            finish();
            startActivity(toLogin);
        }

    }
}
