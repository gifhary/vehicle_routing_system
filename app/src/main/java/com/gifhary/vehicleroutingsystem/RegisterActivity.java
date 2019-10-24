package com.gifhary.vehicleroutingsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {


    public static final String EMAIL = "email";
    public static final String COMPANY_NAME = "companyName";
    public static final String COMPANY_ID = "companyID";
    public static final String PASSWORD = "password";

    public static final String PREFERENCES = "preferences";

    private FirebaseAuth firebaseAuth;

    private static final String TAG = "RegusterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();

        //go to login page button
        TextView login = findViewById(R.id.loginOption);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent toLogin = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(toLogin);
                finish();
            }
        });

    }


    public void register(View view) {

        //getting text field input by id
        EditText emailTxt = findViewById(R.id.emailTxt);
        EditText CName = findViewById(R.id.companyNameTxt);
        EditText CID = findViewById(R.id.companyID);
        EditText pwd = findViewById(R.id.password);
        EditText cPwd = findViewById(R.id.cPassword);

        //assigning text from EditText to string variable
        final String email = emailTxt.getText().toString();
        final String companyName = CName.getText().toString();
        final String companyID = CID.getText().toString();
        final String password = pwd.getText().toString();
        String cPassword = cPwd.getText().toString();

        //check if everything is ok, then proceed to next step
        if (password.equals(cPassword) && !email.equals("") && !companyName.equals("")
                && !companyID.equals("") && !password.equals("") && !cPassword.equals("")) {

            final ProgressBar progressBar = findViewById(R.id.progressBar2);
            progressBar.setVisibility(View.VISIBLE);


            //write to database
            final DocumentReference documentReference = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(companyID);
            documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Toast.makeText(getApplicationContext(), "Company ID already exists!", Toast.LENGTH_LONG).show();
                            progressBar.setVisibility(View.INVISIBLE);
                        } else {

                            firebaseAuth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<AuthResult> task) {
                                            if (task.isSuccessful()) {
                                                Map<String, Object> accountToSave = new HashMap<>();

                                                accountToSave.put(EMAIL, email);
                                                accountToSave.put(COMPANY_NAME, companyName);
                                                accountToSave.put(COMPANY_ID, companyID);
                                                accountToSave.put(PASSWORD, encryptMD5(password));//encrypted password
                                                documentReference.set(accountToSave);//write to database
                                                Toast.makeText(getApplicationContext(), "Registration is success", Toast.LENGTH_SHORT).show();

                                                SharedPreferences settings = getSharedPreferences(PREFERENCES, 0);
                                                SharedPreferences.Editor editor = settings.edit();
                                                editor.putBoolean("hasLoggedIn", true);
                                                editor.putString("companyID", companyID);
                                                editor.putString("companyName", companyName);
                                                editor.apply();
                                                //auto logged in after register
                                                Intent loginSuccess = new Intent(RegisterActivity.this, TripActivity.class);
                                                startActivity(loginSuccess);
                                                finish();

                                            } else {
                                                Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                                Toast.makeText(RegisterActivity.this, "Please use different email",
                                                        Toast.LENGTH_SHORT).show();
                                                progressBar.setVisibility(View.INVISIBLE);

                                            }

                                        }
                                    });

                        }
                    }
                }
            });


            //error messages if user leaves any field empty
            //and if password and confirm password does not match
        } else if (!password.equals(cPassword)) {
            Toast.makeText(getApplicationContext(), "Password does not match!", Toast.LENGTH_LONG).show();

        } else if (email.equals("")) {
            Toast.makeText(getApplicationContext(), "Email cannot be empty!", Toast.LENGTH_LONG).show();

        } else if (companyName.equals("")) {
            Toast.makeText(getApplicationContext(), "Company name cannot be empty!", Toast.LENGTH_LONG).show();

        } else if (companyID.equals("")) {
            Toast.makeText(getApplicationContext(), "Company ID cannot be empty!", Toast.LENGTH_LONG).show();

        } else if (password.equals("")) {
            Toast.makeText(getApplicationContext(), "Password cannot be empty!", Toast.LENGTH_LONG).show();

        } else if (cPassword.equals("")) {
            Toast.makeText(getApplicationContext(), "Confirm password cannot be empty!", Toast.LENGTH_LONG).show();

        } else {
            Toast.makeText(getApplicationContext(), "Please fill all the field!", Toast.LENGTH_LONG).show();
        }
    }


    //encrypting password function
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
