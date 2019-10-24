package com.gifhary.vehicleroutingsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginActivity extends AppCompatActivity {
    private String companyID;
    private String password;

    public static final String PREFERENCES = "preferences";

    private static final String TAG = "LoginActivity";

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();

        TextView register = findViewById(R.id.registerOption);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent toRegister = new Intent(LoginActivity.this, RegisterActivity.class);
                finish();
                startActivity(toRegister);
            }
        });
    }

    public void login(View view) {
        final EditText CID = findViewById(R.id.companyID);
        final EditText pwd = findViewById(R.id.password);

        companyID = CID.getText().toString();
        password = pwd.getText().toString();

        if (companyID.equals("")) {
            Toast.makeText(getApplicationContext(), "Company ID cannot be empty!", Toast.LENGTH_LONG).show();

        } else if (password.equals("")) {
            Toast.makeText(getApplicationContext(), "Password cannot be empty!", Toast.LENGTH_LONG).show();

        } else {
            final ProgressBar progressBar = findViewById(R.id.progressBar);
            progressBar.setVisibility(View.VISIBLE);
            final DocumentReference documentReference = FirebaseFirestore.getInstance().collection("users").document(companyID);
            documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    String dbCompanyID = documentSnapshot.getString("companyID");
                    String dbPassword = documentSnapshot.getString("password");
                    final String dbCompanyName = documentSnapshot.getString("companyName");
                    String dbEmail = documentSnapshot.getString("email");


                    if (companyID.equals(dbCompanyID)) {
                        if (encryptMD5(password).equals(dbPassword)) {

                            assert dbEmail != null;
                            firebaseAuth.signInWithEmailAndPassword(dbEmail, password)
                                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<AuthResult> task) {
                                            if (task.isSuccessful()){

                                                SharedPreferences settings = getSharedPreferences(PREFERENCES, 0);
                                                SharedPreferences.Editor editor = settings.edit();
                                                editor.putBoolean("hasLoggedIn", true);
                                                editor.putString("companyID", companyID);
                                                editor.putString("companyName", dbCompanyName);
                                                editor.apply();

                                                Intent loginSuccess = new Intent(LoginActivity.this, TripActivity.class);
                                                startActivity(loginSuccess);
                                                finish();

                                            }else {
                                                // If sign in fails, display a message to the user.
                                                Log.w(TAG, "signInUserWithEmail:failure", task.getException());
                                                Toast.makeText(LoginActivity.this, "Authentication failed",
                                                        Toast.LENGTH_SHORT).show();
                                                progressBar.setVisibility(View.INVISIBLE);
                                            }
                                        }
                                    });

                        } else {
                            Toast.makeText(getApplicationContext(), "Wrong password!", Toast.LENGTH_LONG).show();
                            progressBar.setVisibility(View.INVISIBLE);
                        }

                    } else {
                        Toast.makeText(getApplicationContext(), "Invalid account!", Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                }
            });
        }

    }


    public String encryptMD5(String input) {
        String output = null;

        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(input.getBytes());
            byte[] bytes = md5.digest();

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }

            output = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return output;
    }

}
