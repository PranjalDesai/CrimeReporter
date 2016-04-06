package com.pranjal.crimereporter;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewTreeObserver;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener, ChildEventListener {

    private GoogleMap mMap;
    private LatLngBounds.Builder mBounds = new LatLngBounds.Builder();
    private GoogleApiClient mGoogleApiClient;

    private static final String FIREBASE_URL = "https://crime-reporter.firebaseIO.com/";
    private static final String FIREBASE_ROOT_NODE = "Crimes";
    private Firebase mFirebase;
    private static final int MY_LOCATION_REQUEST_CODE = 1;

    private static final long DELETER=1814400000;               //x(semi secret sauce) number of days
    private String reportChecker= "";
    private Boolean check= false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Places.GEO_DATA_API)
                .build();
        mGoogleApiClient.connect();

        //creates a new firebase object
        Firebase.setAndroidContext(this);
        mFirebase = new Firebase(FIREBASE_URL);
        mFirebase.child(FIREBASE_ROOT_NODE).addChildEventListener(this);


        SharedPreferences sp = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        reportChecker= sp.getString("report", reportChecker);
        if(reportChecker.equals("0"))
        {
            Snackbar.make(findViewById(android.R.id.content), "Crime Reported!", Snackbar.LENGTH_LONG).show();
            SharedPreferences myPref= getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor= myPref.edit();
            editor.putString("report", "1");
            editor.apply();
        }

    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        final FloatingActionButton button = (FloatingActionButton) findViewById(R.id.Reportfab);
        button.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mMap.setPadding(0, button.getHeight(), 0, 0);
                    }
                }
        );
        button.setBackgroundTintList(getResources().getColorStateList(R.color.red));

        //asks for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            check =true;
        }
        else{
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_LOCATION_REQUEST_CODE);
            // Show rationale and request permission.
        }

        mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                addPointToViewPort(ll);
                // we only want to grab the location once, to allow the user to pan and zoom freely.
                mMap.setOnMyLocationChangeListener(null);
            }
        });
    }
    private void addPointToViewPort(LatLng newPoint) {
        mBounds.include(newPoint);

        //sets a certain map view
        if(check)
        {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
            if (location != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(location.getLatitude(), location.getLongitude()), 13));

                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to location user
                        .zoom(17)                   // Sets the zoom
                        .bearing(90)                // Sets the orientation of the camera to east
                        .tilt(20)                   // Sets the tilt of the camera to 30 degrees
                        .build();                   // Creates a CameraPosition from the builder
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        }
        else{
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(mBounds.build(),
                    findViewById(R.id.Reportfab).getHeight()));
        }
    }


    @Override
    public void onChildRemoved(DataSnapshot snapshot)
    {

    }
    @Override
    public void onChildChanged(DataSnapshot snapshot, String previousChildName)
    {

    }

    @Override
    public void onCancelled(FirebaseError error)
    {

    }

    @Override
    public void onChildMoved(DataSnapshot snapshot, String previousChildName)
    {

    }

    //to authentication page if the permission is granted
    public void report(View view) {
        if(check)
        {
            Intent start = new Intent(MapsActivity.this, FacebookActivity.class);
            start.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(start);
        }
        else
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_LOCATION_REQUEST_CODE);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MY_LOCATION_REQUEST_CODE) {
            if (permissions.length == 1 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                check =true;
                mMap.setMyLocationEnabled(true);
                //onMapReady(mMap);
            } else {
                Snackbar.make(findViewById(android.R.id.content), "Error!! Error!!", Snackbar.LENGTH_LONG).show();
                // Permission was denied. Display an error message.
            }
        }
    }


    //grabs all the data from firebase and displays it as a Marker on a the map
    @Override
    public void onChildAdded(final DataSnapshot dataSnapshot, String s) {
        final String placeId = dataSnapshot.getKey();
        final String crimeTypeFinal;
        String crimeType= "";
        String time= "";


        for(DataSnapshot child: dataSnapshot.getChildren())
        {
            if(child.getKey().equals("crime"))
            {
                crimeType= child.getValue().toString();
            }
            if(child.getKey().equals("time"))
            {
                time= child.getValue().toString();
                long x= Long.parseLong(time);
                long currentTime= System.currentTimeMillis();

                if((currentTime-x)>= DELETER)
                {
                    mFirebase.child("Crimes/" + placeId).removeValue();
                }
            }

        }
        crimeTypeFinal= crimeType;
        if (placeId != null) {
            Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId)
                    .setResultCallback(new ResultCallback<PlaceBuffer>() {
                                           @Override
                                           public void onResult(PlaceBuffer places) {
                                               LatLng location = places.get(0).getLatLng();
                                               addPointToViewPort(location);
                                               mMap.addMarker(new MarkerOptions()
                                                       .position(location)
                                                       .title(crimeTypeFinal)
                                                       .snippet("Click for more info\n(Crime ID: " + placeId + ")"));
                                               places.release();
                                           }
                                       }
                    );

            mMap.setOnInfoWindowClickListener(this);
        }
    }

    //Shows info about the crime and displays more info if clicked
    @Override
    public void onInfoWindowClick(Marker marker) {
        String pID= marker.getSnippet().substring(31,marker.getSnippet().length()-1);
        SharedPreferences myPref= getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor= myPref.edit();
        editor.putString("placeID", pID);
        editor.apply();
        Intent start = new Intent(MapsActivity.this, DisplayCrime.class);
        start.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(start);
    }
}

