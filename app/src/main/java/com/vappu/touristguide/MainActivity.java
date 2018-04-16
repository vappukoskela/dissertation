package com.vappu.touristguide;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final String APIKEY = "9e80e9560584debd18b3d5ddd160c524";
    // tag for logs
    private final String TAG = MainActivity.class.getSimpleName();

    // used for starting the location service as well as keeping on top of when it is running
    private String KEY_SERVICE = "service";
    private boolean mIsServiceRunning;
    private LocationService locationService;
    private LatLng mCurrentLocation;
    private boolean isTimeToUpdate;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkingPermissions();
        isTimeToUpdate = true;
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter("locationEvent"));
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(isTimeToUpdate) {
                mCurrentLocation = intent.getParcelableExtra("latlng");
                WeatherTaskParams weatherTaskParams = new WeatherTaskParams(mCurrentLocation.latitude, mCurrentLocation.longitude);
                new WeatherTask().execute(weatherTaskParams);
            }
        }
    };


    private void updateWeatherUI(String place, String temp, String weatherDesc) {
        TextView locationTV = findViewById(R.id.locationText);
        TextView temperatureTV = findViewById(R.id.tempText);
        TextView weatherTV = findViewById(R.id.weatherText);

        locationTV.setText(place);
        temperatureTV.setText(temp);
        weatherTV.setText(weatherDesc);
    }

    // requires the service to be up and running
    private void switchButtonOperations(){
        Switch inSwitch = findViewById(R.id.switchIn);
        Switch outSwitch = findViewById(R.id.switchOut);

        inSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                locationService.filterIndoors(isChecked);
            }
        });

        outSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                locationService.filterOutdoors(isChecked);
            }
        });

        // set the switch buttons to initially be true
        inSwitch.setChecked(true);
        outSwitch.setChecked(true);
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
 
    @Override
    public void onDestroy(){
        unbindService(serviceConnection);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    private void checkingPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // need to ask permissions! permission has not been granted
            requestLocationPermission();
        }
        else {
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

            // the buttons require the service to be connected
            switchButtonOperations();

        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            locationService = null;
            Log.d(TAG, "onServiceDisconnected" );
        }
    };

    // wrapper class for parameters to be used in the AsyncTask
    private static class WeatherTaskParams {
        double latitude;
        double longitude;


        WeatherTaskParams(double lat, double lon) {
            this.latitude = lat;
            this.longitude = lon;
        }
    }

    private class WeatherTask extends AsyncTask<WeatherTaskParams, Integer, String> {
        @Override
        protected void onPostExecute(String result) {
            parseResult(result);
        }

        private void parseResult(String result) {
            try {
                JSONObject jsonObject = new JSONObject(result);
                String place = jsonObject.getString("name");
                JSONObject tempObj = jsonObject.getJSONObject("main");
                int celsius = (int) tempObj.getDouble("temp"); // temperature

                String temp = "" + celsius + (char) 0x00B0;

                JSONArray descArr = jsonObject.getJSONArray("weather");
                JSONObject descObj = descArr.getJSONObject(0);
                String weatherDesc = descObj.getString("description");

                Log.d(TAG, "parseResult: place " + place + temp + weatherDesc);

                updateWeatherUI(place, temp, weatherDesc);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected String doInBackground(WeatherTaskParams... weatherTaskParams) {
            double lat = weatherTaskParams[0].latitude;
            double lon = weatherTaskParams[0].longitude;

            String url = "https://api.openweathermap.org/data/2.5/weather?" + "lat=" + lat + "&lon=" + lon +
                    "&units=metric&appid=" + APIKEY;

            String result = "";

            try {
                URL titleEndPoint = new URL(url);
                HttpsURLConnection conn = (HttpsURLConnection) titleEndPoint.openConnection();
                conn.setRequestProperty("User-Agent", "tourist-guide");
                conn.setRequestMethod("GET");


                if (conn.getResponseCode() == 200) {
                    Log.d(TAG, "response code 200");
                    // Get JSON
                    InputStream inputStream = conn.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    Log.d(TAG, stringBuilder.toString());
                    result = stringBuilder.toString();

                } else {
                    Log.e(TAG, "response code " + conn.getResponseCode());
                }
                conn.disconnect();

            } catch (IOException e) {
                e.printStackTrace();
                result = "Unable to connect. Please check your connection";
            }
            return result;
        }
    }


}
