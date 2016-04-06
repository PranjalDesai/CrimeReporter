package com.pranjal.crimereporter;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.firebase.client.ServerValue;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.util.HashMap;
import java.util.Map;

public class CrimeReport extends AppCompatActivity {

    //reference to firebase
    private static final String FIREBASE_URL = "https://crime-reporter.firebaseIO.com/";
    private static final String FIREBASE_ROOT_NODE = "Crimes";
    private Firebase mFirebase;

    Spinner spinner;
    ArrayAdapter<CharSequence> adapter;
    private String[] crimes= {"Select A Crime","Assault", "Battery", "Kidnapping", "Homicide", "Rape", "Robbery", "Burglary",
            "Arson", "Forgery"};

    private Button tButton, dButton, btnReport, btnPlace;
    private EditText inputDescription;

    static final int Dialog_IDT= 0;
    static final int Dialog_IDD= 1;
    private static final int REQUEST_PLACE_PICKER = 1;

    Map<String, Object> checkoutData;
    Place place;

    int hours, mins, globalPosition;
    int date=1, years =2016, month= 0;                                  //initial date
    String email, h,m;
    Boolean timeCheck= false, dateCheck= false, placeCheck= false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crime_report);

        //creates a new firebase object
        Firebase.setAndroidContext(this);
        mFirebase = new Firebase(FIREBASE_URL);

        //Info Message
        Snackbar.make(findViewById(android.R.id.content), "Please call 911 before reporting", Snackbar.LENGTH_LONG)
                .setAction("CALL", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:911"));
                        startActivity(intent);
                    }
                }).setActionTextColor(Color.RED).show();

        showTimePicker();
        showDatePicker();


        inputDescription = (EditText) findViewById(R.id.description);
        btnReport = (Button) findViewById(R.id.report);
        btnPlace = (Button) findViewById(R.id.slocation);
        btnPlace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                placePicker(view);
            }
        });
        checkoutData = new HashMap<>();


        SharedPreferences sp = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        email= sp.getString("email", email);


        btnReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitForm();
            }
        });

        //loads the spinner and saves the position index
        spinner= (Spinner) findViewById(R.id.spinner);
        adapter=ArrayAdapter.createFromResource(this,R.array.Crimes,android.R.layout.simple_list_item_1);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                globalPosition=position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }

        });
    }

    public void showTimePicker(){
        tButton= (Button) findViewById(R.id.t_picker);
        tButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showDialog(Dialog_IDT);
                    }
                }
        );
    }

    public void showDatePicker(){
        dButton= (Button) findViewById(R.id.d_picker);
        dButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showDialog(Dialog_IDD);
                    }
                }
        );
    }

    protected Dialog onCreateDialog(int id){
        if(id== Dialog_IDT)
        {
            return new TimePickerDialog(CrimeReport.this, timePicker, hours,mins, true);
        }
        if(id== Dialog_IDD)
        {
            return new DatePickerDialog(CrimeReport.this, datePicker, years, month, date);
        }
        return null;
    }

    //Time Picker
    protected TimePickerDialog.OnTimeSetListener timePicker=
            new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    hours = hourOfDay;
                    mins = minute;
                    h=Integer.toString(hours);
                    m=Integer.toString(mins);
                    if(hours<=9)
                    {
                        h= "0"+hours;
                    }
                    if(mins<=9)
                    {
                        m= "0"+mins;
                    }
                    tButton.setText(h + ":" + m);
                    timeCheck= true;
                }
            };

    //Date Picker
    protected DatePickerDialog.OnDateSetListener datePicker=
            new DatePickerDialog.OnDateSetListener(){
                @Override
                public  void onDateSet(DatePicker view, int year, int monthOfYear, int dateOfYear)
                {
                    date=dateOfYear;
                    years=year;
                    month=monthOfYear;
                    dButton.setText((month+1)+"/"+date+"/"+year);
                    dateCheck=true;
                }
            };


    @Override
    public void onBackPressed(){
        super.onBackPressed();
        Intent start = new Intent(CrimeReport.this, MapsActivity.class);
        start.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(start);
    }

    //checks if the entire form is filled and then creates a node in the firebase database
    private void submitForm() {
        if (!validateDescription()) {
            return;
        }
        if (globalPosition==0)
        {
            Snackbar.make(findViewById(android.R.id.content), "Select a Crime", Snackbar.LENGTH_LONG).show();
            return;
        }
        if(!timeCheck)
        {
            Snackbar.make(findViewById(android.R.id.content), "Select a relative Time", Snackbar.LENGTH_LONG).show();
            tButton.setBackgroundColor(Color.parseColor("#D32F2F"));
            return;
        }
        if(!dateCheck)
        {
            Snackbar.make(findViewById(android.R.id.content), "Select a Date", Snackbar.LENGTH_LONG).show();
            dButton.setBackgroundColor(Color.parseColor("#D32F2F"));
            return;
        }
        if(!placeCheck)
        {
            Snackbar.make(findViewById(android.R.id.content), "Select a Place", Snackbar.LENGTH_LONG).show();
            btnPlace.setBackgroundColor(Color.parseColor("#D32F2F"));
            return;
        }

        String time= h+":"+m;

        //adds more info into checkoutData (Node to be uploaded)
        checkoutData.put("email", email);
        checkoutData.put("description", inputDescription.getText().toString());
        checkoutData.put("reportedTime", (month+1)+"/"+date+"/"+ years+  "  at  "+time);
        checkoutData.put("crime", crimes[globalPosition]);

        //uploads the info in checkoutData to firebase
        mFirebase.child(FIREBASE_ROOT_NODE).child(place.getId()).setValue(checkoutData);

        SharedPreferences myPref= getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor= myPref.edit();
        editor.putString("report", "0");
        editor.apply();

        //takes the user back to the map
        Intent start = new Intent(CrimeReport.this, MapsActivity.class);
        start.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(start);
    }



    private boolean validateDescription()
    {
        if (inputDescription.getText().toString().trim().isEmpty()) {

            Snackbar.make(findViewById(android.R.id.content), "Enter a Description", Snackbar.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    //opens the places UI for the user to pick a place
    public void placePicker(View view)
    {
        try
        {
            PlacePicker.IntentBuilder intentBuilder = new PlacePicker.IntentBuilder();
            Intent intent = intentBuilder.build(this);
            startActivityForResult(intent, REQUEST_PLACE_PICKER);
        }
        catch (GooglePlayServicesRepairableException e)
        {
            GoogleApiAvailability.getInstance().getErrorDialog(this, e.getConnectionStatusCode(),
                    REQUEST_PLACE_PICKER);
        }
        catch (GooglePlayServicesNotAvailableException e)
        {
            Toast.makeText(this, "Please install Google Play Services!", Toast.LENGTH_LONG).show();
        }
    }

    //Saves the place info recieved when the user pics a place into checkoutData (Map Datastructure)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_PLACE_PICKER)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                place = PlacePicker.getPlace(data, this);

                checkoutData.put("time", ServerValue.TIMESTAMP);
                checkoutData.put("address", place.getAddress());
                checkoutData.put("placeName", place.getName());
                btnPlace.setText(place.getName());

                placeCheck=true;

            }
            else if (resultCode == PlacePicker.RESULT_ERROR)
            {
                Toast.makeText(this, "Places API failure! Check the API is enabled for your key",
                        Toast.LENGTH_LONG).show();
            }
        }
        else
        {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
