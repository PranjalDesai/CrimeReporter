package com.pranjal.crimereporter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class FacebookActivity extends AppCompatActivity {

    private String facebookEmail;

    AccessTokenTracker tracker;
    ProfileTracker profileTracker;
    private CallbackManager callbackManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //initializes the facebook sdk
        FacebookSdk.sdkInitialize(getApplicationContext());

        setContentView(R.layout.activity_facebook);

        tracker= new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {

            }
        };
        profileTracker= new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {

            }
        };

        tracker.startTracking();
        profileTracker.startTracking();


        LoginButton mButtonLogin = (LoginButton) findViewById(R.id.login_button);
        mButtonLogin.setReadPermissions(Arrays.asList("public_profile", "email"));
        callbackManager = CallbackManager.Factory.create();

        //authenticates user with facebook and saves their email in shared preferences
        mButtonLogin.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {

                GraphRequest request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {

                        JSONObject json = response.getJSONObject();
                        try {
                            if (json != null) {

                                facebookEmail = json.getString("email");


                                //saves the user email in SharedPreferences
                                SharedPreferences myPref= getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor= myPref.edit();
                                editor.putString("email", facebookEmail);
                                editor.apply();

                                //authentication is passed and takes the user to the next screen
                                Intent start = new Intent(FacebookActivity.this, CrimeReport.class);
                                start.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(start);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "email");
                request.setParameters(parameters);
                request.executeAsync();

            }

            @Override
            public void onCancel() {
                Snackbar.make(findViewById(android.R.id.content), "Log In cancelled", Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onError(FacebookException e) {
                Snackbar.make(findViewById(android.R.id.content), e.getMessage(), Snackbar.LENGTH_LONG).show();

            }
        });

        //User is logged-in so we can move to the next page and skip authentication
        if(isLoggedIn())
        {
            Intent start = new Intent(FacebookActivity.this, CrimeReport.class);
            start.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(start);
        }
    }

    public boolean isLoggedIn()
    {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        return accessToken != null;
    }


    @Override
    public void onStop()
    {
        super.onStop();
        tracker.stopTracking();
        profileTracker.stopTracking();

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}

