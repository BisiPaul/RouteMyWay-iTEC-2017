package com.routemyway.ceapata;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.*;
import java.lang.*;
import java.io.*;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
    {

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 100;
    private static final String TAG = MapsActivity.class.getSimpleName();
    private CameraPosition mCameraPosition;
    private static final float DEFAULT_ZOOM = 10.0f; // Can take values between 2.0 and 21.0
    private GoogleMap mMap;
    private boolean mLocationPermissionGranted;
    private Location mLastKnownLocation;
    private Location firstKnownLocation;
    private GoogleApiClient mGoogleApiClient;
    private final LatLng mDefaultLocation = new LatLng(45.7489, 21.2087);

    private double Distance = 0;
    private double Time = 0;

    private String auxLongitude;
    private String auxLatitude;
    private String auxTime;

    public static final String USER_CHILD = "users";
    public static final String ANONYMOUS = "anonymous";
    private String mUsername;
    private User user;

        // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mFirebaseDatabaseReference;


        //This function converts decimal degrees to radians
        private double deg2rad(double deg) {
            return (deg * Math.PI / 180.0);
        }

        //This function converts radians to decimal degrees
        private double rad2deg(double rad) {
            return (rad * 180 / Math.PI);
        }

        private double getDistance(double lat1, double lon1, double lat2, double lon2) {

            double theta = lon1 - lon2;
            double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
            dist = Math.acos(dist);
            dist = rad2deg(dist);
            dist = dist * 60 * 1.1515;

            //pt a fi expr in km
            dist = dist * 1.609344;

            return dist;

        }

        public void showDistance(View view){
            String displayedText;
            double meterDist = Distance;
            displayedText=String.valueOf(meterDist)+" Meters";
            Toast toast = Toast.makeText(this, displayedText, Toast.LENGTH_LONG);
            toast.show();
        }


        //regex pt timp
        private double getTime(String str){
            //String [] delimiter={":"," "};
            String []time;
            int i;
            double seconds, minutes,hours;
            time= str.split(":|\\s");

            seconds=Double.parseDouble(time[2])/60;
            minutes=Double.parseDouble(time[1])+seconds;
            minutes=minutes/60;
            hours=Double.parseDouble(time[0])+minutes;
            return hours;
        }

        public void showTime(View view){//query din BD
            String displayedText;
            double minuteTime = Time*60;
            displayedText=String.valueOf(minuteTime)+" Minutes";
            Toast toast = Toast.makeText(this, displayedText,Toast.LENGTH_LONG);
            toast.show();
        }

        public void showAvrgSpeed(View view){
            String displayedText;
            double result;
            result = (Distance/1000)/Time;
            displayedText=String.valueOf(result)+" Km/h";
            Toast toast = Toast.makeText(this, displayedText,Toast.LENGTH_LONG);
            toast.show();
        }


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this /* FragmentActivity */,
                            this /* OnConnectionFailedListener */)
                    .addConnectionCallbacks(this)
                    .addApi(LocationServices.API)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .build();
        mGoogleApiClient.connect();

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Set default username is anonymous.
        mUsername = ANONYMOUS;


        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mUsername = mFirebaseUser.getDisplayName();
        mFirebaseDatabaseReference =  FirebaseDatabase.getInstance().getReference();

        Thread timer = new Thread() {

            public void run() {
                boolean firstEntry = true;


                for (; ; ) {
                    // reads the latitude,longitude and timestamp and sends it to the server
                    try {
                        String latitudeText, longitudeText;
                        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            mLastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                            if (mLastKnownLocation != null) {
                                if (firstEntry == true) {
                                    String initLatitudeText = String.valueOf(mLastKnownLocation.getLatitude());
                                    String initLongitudeText = String.valueOf(mLastKnownLocation.getLongitude());
                                    String initMytime = java.text.DateFormat.getTimeInstance().format(Calendar.getInstance().getTime());

                                    user = new User(mUsername, initLongitudeText, initLatitudeText, initMytime, initMytime);
                                    mFirebaseDatabaseReference.child(USER_CHILD).push().setValue(user);

                                    firstEntry = false;
                                } else {
                                    auxLongitude = user.getLongitude();
                                    auxLatitude = user.getLatitude();
                                    auxTime = user.getPresentTimestamp();

                                    String mytime = java.text.DateFormat.getTimeInstance().format(Calendar.getInstance().getTime());
                                    user.setPresentTimestamp(mytime);

                                    latitudeText = String.valueOf(mLastKnownLocation.getLatitude());
                                    user.setLatitude(latitudeText);

                                    longitudeText = String.valueOf(mLastKnownLocation.getLongitude());
                                    user.setLongitude(latitudeText);

                                    mFirebaseDatabaseReference.child(USER_CHILD).child(user.getUsername()).addListenerForSingleValueEvent(
                                            new ValueEventListener() {
                                                @Override
                                                public void onDataChange(DataSnapshot dataSnapshot) {
                                                    User aux = dataSnapshot.getValue(User.class);
                                                    //Distance = Distance + getDistance(Double.parseDouble(aux.getLatitude())
                                                    //        ,Double.parseDouble(aux.getLongitude())
                                                    //        ,Double.parseDouble(auxLatitude)
                                                    //        ,Double.parseDouble(auxLongitude));
                                                    float[] results = new float[1];
                                                    Location.distanceBetween(Double.parseDouble(auxLatitude), Double.parseDouble(auxLongitude) ,
                                                              Double.parseDouble(aux.getLatitude()), Double.parseDouble(aux.getLongitude()),  results);
                                                    if(results[0]<100.0)
                                                        Distance = Distance + results[0];

                                                }

                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {

                                                }
                                            });

                                    mFirebaseDatabaseReference.child(USER_CHILD).child(user.getUsername()).setValue(user);
                                    Time = getTime(user.getPresentTimestamp()) - getTime((user.getInitTimestamp()));
                                    Log.d("TIIIIIIMP:", String.valueOf(Time));
                                    Log.d("DIIIIIISTANTA:", String.valueOf(Distance));


                                    Log.d("LATITUDE:", latitudeText);
                                    Log.d("LONGITUDE:", longitudeText);
                                    Log.d("LONGITUDE:", mytime);
                                }
                            }
                        }
                        Thread.sleep(3000);    // sleep for 3 seconds
                    } catch (InterruptedException e) {
                        Log.d("ERROR:", e.getMessage());
                    }
                }
            }
        };
        timer.start();


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

        // Do other setup activities here too, as described elsewhere in this tutorial.

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();

    }

    /**
    * Builds the map when the Google Play services client is successfully connected.
    */
    @Override
    public void onConnected(Bundle connectionHint) {

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }


    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
    }


    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }

    /*
     * Request location permission, so that we can get the location of the
     * device. The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (mLocationPermissionGranted) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            mMap.setMyLocationEnabled(false);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mLastKnownLocation = null;
        }
    }

    private void getDeviceLocation() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (mLocationPermissionGranted) {
            mLastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }

        // Set the map's camera position to the current location of the device.
        //GoogleMap mMap;
        if (mCameraPosition != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));
        } else if (mLastKnownLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLastKnownLocation.getLatitude(),
                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
        } else {
            Log.d(TAG, "Current location is null. Using defaults.");

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }
}
