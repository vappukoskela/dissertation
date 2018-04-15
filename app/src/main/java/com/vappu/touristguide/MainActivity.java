package com.vappu.touristguide;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    // tag for logs
    private final String TAG = MainActivity.class.getSimpleName();

    ToggleButton toggleButton;

    // used for starting the location service as well as keeping on top of when it is running
    private String KEY_SERVICE = "service";
    private boolean mIsServiceRunning;
    private LocationService locationService = new LocationService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkingPermissions();

        Switch inSwitch = findViewById(R.id.switchIn);
        Switch outSwitch = findViewById(R.id.switchOut);

        inSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    Log.d(TAG, "onCheckedChanged: inswitch true");
                } else {
                    Log.d(TAG, "onCheckedChanged: inswitch false");
                }
            }
        });

        outSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    Log.d(TAG, "onCheckedChanged: outswitch true");
                } else {
                    Log.d(TAG, "onCheckedChanged: outswitch false");
                }
            }
        });

        /*
        toggleButton = findViewById(R.id.toggleButton);

        // find out if the service is already running
        if(savedInstanceState != null) {
            mIsServiceRunning = savedInstanceState.getBoolean(KEY_SERVICE);
        } else { mIsServiceRunning = false; } // if there was no saved instance, should start the service

        // if there is a service running set togglebutton to reflect state appropriately
        if(mIsServiceRunning){
            toggleButton.setChecked(true);
        } else { toggleButton.setChecked(false); }
        */
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        outState.putBoolean(KEY_SERVICE, mIsServiceRunning);
        super.onSaveInstanceState(outState);

    }

    public void openMap(View view) {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }

    /*
    public void startService(View view) {
        Log.d(TAG, "startService");
        Intent intent = new Intent(this, LocationService.class);
        if (!mIsServiceRunning){
            // toggle button has been clicked - start service
            Log.d(TAG, "Starting LocationService...");
           // startService(intent);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE );
            mIsServiceRunning = true;
        }
        else {
            // stop service if there is service running
            Log.d(TAG, "Stopping LocationService...");
            unbindService(serviceConnection);
            stopService(intent);
            mIsServiceRunning = false;
        }
    }
    */

    @Override
    public void onDestroy(){
        unbindService(serviceConnection);
        super.onDestroy();
    }

    private void checkingPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // need to ask permissions! permission has not been granted
            requestLocationPermission();
            Log.d(TAG, "checkingPermissions: if");
        }
        else {
            Log.d(TAG, "checkingPermissions: else");
            startService();
        }
    }

    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION  );
        }
        else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startService();

                } else {
                    requestLocationPermission();
                    // permission denied
                }
            }
        }
    }

    private void startService() {
        Intent intent = new Intent(this, LocationService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE );
        mIsServiceRunning = true;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            locationService = ((LocationService.LocalBinder) service).getService();
            Log.d(TAG, "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            locationService = null;
            Log.d(TAG, "onServiceDisconnected" );
        }
    };
}
