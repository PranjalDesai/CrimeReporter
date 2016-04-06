package com.pranjal.crimereporter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

public class DisplayCrime extends AppCompatActivity {

    private String placeID;
    private static final String FIREBASE_URL = "https://crime-reporter.firebaseIO.com/";
    private Firebase mFirebase;
    String address="";
    String cType= "Crime";
    TextView dispTime, crimesType, place, descrip;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_crime);
        SharedPreferences sp = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        placeID= sp.getString("placeID", placeID);

        //creates a new firebase object
        Firebase.setAndroidContext(this);
        mFirebase = new Firebase(FIREBASE_URL);

        dispTime = (TextView) findViewById(R.id.textView8);
        crimesType = (TextView) findViewById(R.id.textView7);
        place =  (TextView) findViewById(R.id.textView6);
        descrip = (TextView) findViewById(R.id.textView5);

        crimeType();
        location();
        description();
        time();

    }

    //grabs the location data from the database for a specific crime
    public void location()
    {
        mFirebase.child("Crimes/"+placeID+"/address").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                address=snapshot.getValue().toString();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
            }
        });
        mFirebase.child("Crimes/"+placeID+"/placeName").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                place.setText(address+"\n"+snapshot.getValue().toString());
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
            }
        });
    }

    //grabs the crime type data from the database for a specific crime
    public void crimeType(){
        mFirebase.child("Crimes/"+placeID+"/crime").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                cType=snapshot.getValue().toString();
                crimesType.setText(cType);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
            }
        });
    }

    //grabs the description data from the database for a specific crime
    public void description(){
        mFirebase.child("Crimes/"+placeID+"/description").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                descrip.setText(snapshot.getValue().toString());
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
            }
        });
    }

    //grabs the time data from the database for a specific crime
    public void time(){
        mFirebase.child("Crimes/"+placeID+"/reportedTime").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                dispTime.setText(snapshot.getValue().toString());
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
            }
        });
    }

    public void done(View view){
        Intent start = new Intent(DisplayCrime.this, MapsActivity.class);
        start.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(start);
    }
}
