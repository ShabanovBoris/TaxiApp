package com.example.taxiapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.taxiapp.signin.DriverSignIn;
import com.example.taxiapp.signin.PassengerSignIn;
import com.google.firebase.auth.FirebaseAuth;

public class ChoosingModActivity extends AppCompatActivity {

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choosing_mode);


    }

    public void goToPassengerSignIn(View view) {
        startActivity(new Intent(this, PassengerSignIn.class));
    }

    public void goToDriverSignIn(View view) {

        startActivity(new Intent(this, DriverSignIn.class));
    }
}