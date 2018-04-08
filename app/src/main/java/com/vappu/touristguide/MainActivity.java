package com.vappu.touristguide;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {

    // tag for logs
    private final String TAG = MainActivity.class.getSimpleName();

    ToggleButton toggleButton;

    // used for starting the location service as well as keeping on top of when it is running
    private String KEY_SERVICE = "service";
    private boolean mIsServiceRunning;
    private LocationService locationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toggleButton = findViewById(R.id.toggleButton);

        // find out if the service is already running
        if(savedInstanceState != null) {
            mIsServiceRunning = savedInstanceState.getBoolean(KEY_SERVICE);
        } else { mIsServiceRunning = false; } // if there was no saved instance, should start the service

        // if there is a service running set togglebutton to reflect state appropriately
        if(mIsServiceRunning){
            toggleButton.setChecked(true);
        } else { toggleButton.setChecked(false); }
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

    @Override
    public void onDestroy(){
     //   unbindService(serviceConnection);
        super.onDestroy();
    }


    // TODO create a prompt to ask user for permissions

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
