package com.vappu.touristguide;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    // tag for logs
    private final String TAG = MainActivity.class.getSimpleName();

    // used for starting the location service as well as keeping on top of when it is running
    private String KEY_SERVICE = "service";
    private boolean mIsServiceRunning;

    private LocationService mLocationService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // find out if the service is already running
        if(savedInstanceState != null) {
            mIsServiceRunning = savedInstanceState.getBoolean(KEY_SERVICE);
        } else { mIsServiceRunning = false; } // if there was no saved instance, should start the service

        if(!mIsServiceRunning){
            Intent intent = new Intent(this, LocationService.class);
            startService(intent);
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        outState.putBoolean(KEY_SERVICE, mIsServiceRunning);
        super.onSaveInstanceState(outState);

    }

    public void openMap(View view) {
        // Placeholder button
        // tackling the location stuff first
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }

    // TODO create a prompt to ask user for permissions

}
