package com.vappu.touristguide;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;

/**
 * An activity that displays a map showing the place at the device's current location.
 */
public class MapActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    // The core of the Activity's Location functionality provided by Google's Android tutorial
    // https://developers.google.com/maps/documentation/android-api/current-place-tutorial
    // This tutorial has been adapted to suit the purposes of the application

    // define TAG for logs
    private static final String TAG = MapActivity.class.getSimpleName();
    private static final int TYPE_PLACE = 1;
    private static final int TYPE_WIKI = 0;

    private GoogleMap mMap;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;

    // define default location. This will be used before mapactivity can locate itself
    // default location is lat 52.949591, long -1.154830, Nottingham Castle
    private LatLng mDefaultLocation = new LatLng(52.949591, -1.154830);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;

    // Keys for storing activity state.
    private static final String KEY_LOCATION = "location";

    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallBack;

    private HashMap<String, Marker> markerHashMap = new HashMap<String, Marker>();

    // service
    private LocationService mLocationService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mDefaultLocation = extras.getParcelable("location");
        }

        // bind to service
        bindService(new Intent(MapActivity.this, LocationService.class), serviceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter("placeEvent"));

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLastKnownLocation.getLatitude(),
                            mLastKnownLocation.getLongitude()), mMap.getCameraPosition().zoom));
        }

        GeoDataClient geoDataClient = Places.getGeoDataClient(this, null);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(100);
        mLocationRequest.setFastestInterval(10);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mLocationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if(!location.equals(mLastKnownLocation) && mLastKnownLocation != null) {
                        // move camera to center the user and keep current zoom level
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(mLastKnownLocation.getLatitude(),
                                        mLastKnownLocation.getLongitude()), mMap.getCameraPosition().zoom));
                    }
                    mLastKnownLocation = location;
                }
            }
        };


        // Build the map.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);

        if ( serviceConnection != null ){
            unbindService(serviceConnection);
            serviceConnection = null;
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String wikiID = intent.getStringExtra("wikiID");
            String placeID = intent.getStringExtra("placeID");
            LatLng latLng = intent.getParcelableExtra("latlng");
            String name = intent.getStringExtra("name");

            String id = "";
            int type = 0;
            if (wikiID != null ) {
                id = wikiID;
                type = TYPE_WIKI;
            }
            else if (placeID != null ) {
                id = placeID;
                type = TYPE_PLACE;
            }
            addMarker(name, latLng, id, type);
        }
    };

    private void addMarker(String title, LatLng pos, String id, int type){
        if ( !markerHashMap.containsKey(id) && pos != null) {
            Marker marker;
            if( type == TYPE_WIKI) {
                marker = mMap.addMarker(new MarkerOptions().position(pos).title(title));
            }
            else {
                marker = mMap.addMarker(new MarkerOptions().position(pos).title(title).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));
            }
            marker.setTag(id);
            markerHashMap.put(id, marker);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");

        if (mMap != null) {
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }


    // https://developers.google.com/maps/documentation/android-api/current-place-tutorial
    @Override
    public void onMapReady(GoogleMap map) {
        Log.d(TAG, "onMapReady");
        mMap = map;
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Intent intent = new Intent(MapActivity.this, InfoActivity.class);
                intent.putExtra("ID", marker.getTag().toString());

                String title = markerHashMap.get(marker.getTag()).getTitle();
                intent.putExtra("title", title);

                startActivity(intent);
            }
        });

        // Prompt the user for permission.
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map
        // this is the location button on the top right corner
        updateLocationUI();

        // Ask for location updates
        startLocationUpdates();
    }


    // https://developer.android.com/training/location/receive-location-updates.html
    private void startLocationUpdates() {

        try {
            if (mLocationPermissionGranted) {
                // start at the default position and obtain the default zoom
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallBack, null);
            } else {
                Log.d(TAG, "Current location is null. Using defaults.");
                mMap.moveCamera(CameraUpdateFactory
                        .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    // get permissions
    private void getLocationPermission() {
        Log.d(TAG, "getLocationPermission");

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    // handle the result of requesting permissions
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");

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

    private void updateLocationUI() {
        Log.d(TAG, "updateLocationUI");

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                    @Override
                    public boolean onMyLocationButtonClick() {
                        return false;
                    }
                });
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mLocationService = ((LocationService.LocalBinder) service).getService();
            Log.d(TAG, "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mLocationService = null;
            Log.d(TAG, "onServiceDisconnected");

        }
    };


    // Toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.backmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_close:
                // back button pressed
                finish();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }
}
