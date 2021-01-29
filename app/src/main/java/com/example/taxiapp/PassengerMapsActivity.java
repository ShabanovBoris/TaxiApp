package com.example.taxiapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.taxiapp.signin.PassengerSignIn;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PassengerMapsActivity extends FragmentActivity implements OnMapReadyCallback {

    public static final int CHECK_SETTINGS_CODE = 111;
    private static final int REQUSET_LOCATION_PERMISSION = 222;

    private FloatingActionButton stopLocationUpdateButton;
    private ExtendedFloatingActionButton settingButton, signOutButton;
    private Chip bookTaxiButton;

    private FusedLocationProviderClient fusedLocationClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;
    private Location currentLocation;

    private boolean isLocationUpdatesActive;

    private GoogleMap mMap;
    private Marker driverMarker;

    private int searchRadius = 1;
    private boolean isDriverFound = false;
    private String nearestDriverID;

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference geoDrivers;
    private DatabaseReference nearestDriverLocation;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pasenger_maps);

        settingButton = findViewById(R.id.settingButton);
        signOutButton = findViewById(R.id.signOutButton);
        bookTaxiButton = findViewById(R.id.bookTaxiButton);
        stopLocationUpdateButton = findViewById(R.id.floatingActionButtonStopLocation);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        geoDrivers = FirebaseDatabase.getInstance().getReference().child("geoDrivers");

        stopLocationUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopLocationUpdates();
                recreate();


            }
        });

        bookTaxiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopLocationUpdateButton.setVisibility(View.VISIBLE);
                bookTaxiButton.setText("Getting your taxi...");
                firstLocation();
                gettingNearestTaxi();
            }
        });
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOutPassenger();
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);

        buildLocationRequest();
        buildLocationCallback();
        buildLocationSettingsRequest();

        startLocationUpdates();

    }

    private void gettingNearestTaxi() {


        GeoFire geoFire = new GeoFire(geoDrivers);

        GeoQuery geoQuery = geoFire.queryAtLocation(
                new GeoLocation(currentLocation.getLatitude(), currentLocation.getLongitude()),
                searchRadius);

        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!isDriverFound) {
                    isDriverFound = true;
                    nearestDriverID = key;

                    getNearestDriverLocation();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!isDriverFound) {
                    searchRadius++;
                    mMap.animateCamera(CameraUpdateFactory.zoomBy(-2));
                    gettingNearestTaxi();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });


    }

    private void getNearestDriverLocation() {
        bookTaxiButton.setText("Getting location...");
        nearestDriverLocation = FirebaseDatabase.getInstance().getReference()
                .child("geoDrivers")
                .child(nearestDriverID)
                .child("l");

        nearestDriverLocation.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {
                    List<Object> driverLocationParameters = (List<Object>) snapshot.getValue();
                    double latitude = 0;
                    double longitude = 0;

                    if (driverLocationParameters.get(0) != null) {

                        latitude = Double.parseDouble(driverLocationParameters.get(0).toString());
                        longitude = Double.parseDouble(driverLocationParameters.get(1).toString());
                    }

                    LatLng driverLatLng = new LatLng(latitude, longitude);
                    if (driverMarker != null) {
                        driverMarker.remove();
                    }

                    Location driverLocation = new Location("");
                    driverLocation.setLatitude(latitude);
                    driverLocation.setLongitude(longitude);
                    float distance = driverLocation.distanceTo(currentLocation);

                    bookTaxiButton.setText("Distance to driver  " + distance);

                    driverMarker = mMap.addMarker(new MarkerOptions()
                            .position(driverLatLng)
                            .title("Your driver is here"));

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    private void signOutPassenger() {


        String passengerUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference database = FirebaseDatabase.getInstance().getReference()
                .child("geoPassengers");

        GeoFire geoFire = new GeoFire(database);
        geoFire.removeLocation(passengerUserId);
        Intent intent = new Intent(PassengerMapsActivity.this, PassengerSignIn.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        auth.signOut();
        finish();


    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        if (currentLocation != null) {

            LatLng passengerLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

            mMap.moveCamera(CameraUpdateFactory.newLatLng(passengerLocation));
        }
    }

    private void stopLocationUpdates() {
        if (!isLocationUpdatesActive) {
            return;
        } else {
            fusedLocationClient.removeLocationUpdates(locationCallback)
                    .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            isLocationUpdatesActive = false;

                        }
                    });
        }
    }

    private void startLocationUpdates() {
        isLocationUpdatesActive = true;


        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint ("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        fusedLocationClient.requestLocationUpdates(
                                locationRequest,
                                locationCallback,
                                Looper.myLooper()
                        );

                        updateLocationUI();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                try {
                                    ResolvableApiException resolvableApiException =
                                            (ResolvableApiException) e;
                                    resolvableApiException.startResolutionForResult(
                                            PassengerMapsActivity.this, CHECK_SETTINGS_CODE);
                                } catch (IntentSender.SendIntentException intentSender) {
                                    intentSender.printStackTrace();

                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String message = "Adjust location settings on your device";
                                Toast.makeText(PassengerMapsActivity.this, message, Toast.LENGTH_LONG).show();
                                isLocationUpdatesActive = false;
                        }

                        updateLocationUI();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data); // TODO: 25.01.2021 Delete, if dont anything work

        switch (requestCode) {
            case CHECK_SETTINGS_CODE:
                switch (resultCode) {
                    case RESULT_OK:   // TODO: 25.01.2021 or Activity.RESULT_OK
                        Log.d("MainActivity", "User has agreed to change location settings");
                        startLocationUpdates();
                        break;
                    case RESULT_CANCELED:
                        Log.d("MainActivity", "User has not agreed to change location settings");
                        isLocationUpdatesActive = false;
                        updateLocationUI();
                        break;

                }
                break;
        }
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder();

        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();

    }

    private void buildLocationCallback() {


        locationCallback = new LocationCallback() {


            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                currentLocation = locationResult.getLastLocation();

                updateLocationUI();


            }


        };
    }

    private void updateLocationUI() {


        if (currentLocation != null) {





            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mMap.setMyLocationEnabled(true);


            String passengerUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference databaseGeoPassengers = FirebaseDatabase.getInstance().getReference()
                    .child("geoPassengers");

            DatabaseReference databaseDrivers = FirebaseDatabase.getInstance().getReference()
                    .child("passengers");
            databaseDrivers.setValue("Checking");



            GeoFire geoFire = new GeoFire(databaseGeoPassengers);
            geoFire.setLocation(passengerUserId, new GeoLocation(currentLocation.getLatitude(), currentLocation.getLongitude()));


          /*  locationUpdateTimeTextView.setText(
                    DateFormat.getTimeInstance().format(new Date())
            );*/
        }
    }


    private void firstLocation() {

            LatLng passengerLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(passengerLatLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

    }

    private void buildLocationRequest() {

        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isLocationUpdatesActive && checkLocationPermission()) {

            startLocationUpdates();


        } else if (!checkLocationPermission()) {

            requestLocationPermission();

        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopLocationUpdates();
    }

    private void requestLocationPermission() {

        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (shouldProvideRationale) {
            showSnackBar(
                    "Location Permission is needed for app functionality",
                    "OK",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(PassengerMapsActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUSET_LOCATION_PERMISSION);
                        }
                    }
            );
        } else {
            ActivityCompat.requestPermissions(PassengerMapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUSET_LOCATION_PERMISSION);

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUSET_LOCATION_PERMISSION) {

            if (grantResults.length <= 0) {
                Log.d("onReqPermissionsResult", "Request was canceled");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isLocationUpdatesActive) {
                    startLocationUpdates();
                }
            } else {
                showSnackBar("Turn on location settings",
                        "Settings",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts(
                                        "package",
                                        Build.ID,  // TODO: 25.01.2021 BuildConfig.ID on sample
                                        null
                                );
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);

                            }
                        });
            }


        }

    }

    private void showSnackBar(final String mainText, final String action, View.OnClickListener listener) {

        Snackbar.make(findViewById(android.R.id.content),
                mainText, BaseTransientBottomBar.LENGTH_INDEFINITE)
                .setAction(action, listener).show();


    }

    private boolean checkLocationPermission() {


        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }


}