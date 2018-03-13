package com.vappu.touristguide;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PointOfInterest;

/**
 * An activity that displays a map showing the place at the device's current location.
 */
public class MapActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleMap.OnPoiClickListener {

    // The core of the Activity's Location functionality provided by Google's Android tutorial
    // https://developers.google.com/maps/documentation/android-api/current-place-tutorial
    // This tutorial has been adapted to suit the purposes of the application

    // define TAG for logs
    private static final String TAG = MapActivity.class.getSimpleName();

    private GoogleMap mMap;
    private CameraPosition mCameraPosition;

    // The entry points to the Places API.
    private GeoDataClient mGeoDataClient;
    private PlaceDetectionClient mPlaceDetectionClient;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;

    // define default location
    // default location is lat 52.949591, long -1.154830, Nottingham Castle
    private final LatLng mDefaultLocation = new LatLng(52.949591, -1.154830);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallBack;

    // service
    private LocationService mLocationService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // bind to service
        //bindService(new Intent(MapActivity.this, LocationService.class), serviceConnection, Context.BIND_AUTO_CREATE);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        // Construct a GeoDataClient.
        mGeoDataClient = Places.getGeoDataClient(this, null);
        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);


        // todo wont need these
        // Construct a PlaceDetectionClient.
  //      mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);



        mLocationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    //Log.d(TAG, "location " + location.toString());
                    mLastKnownLocation = location;

                    // move camera to center the user and keep current zoom level
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(mLastKnownLocation.getLatitude(),
                                    mLastKnownLocation.getLongitude()), mMap.getCameraPosition().zoom));
                }
            }
        };

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Build the map.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


    }

    // todo wont need this
    /*
    // adapted from https://developers.google.com/places/android-api/current-place#get-current
    @SuppressLint("MissingPermission")
    private void checkLikelyPlaces(Location location) {
        Log.d(TAG, "checkLikelyPlaces " + location);
        if (mLocationPermissionGranted) {
            // TODO add filters
            Task<PlaceLikelihoodBufferResponse> placeResult = mPlaceDetectionClient.getCurrentPlace(null);
            placeResult.addOnCompleteListener(new OnCompleteListener<PlaceLikelihoodBufferResponse>() {
                @Override
                public void onComplete(@NonNull Task<PlaceLikelihoodBufferResponse> task) {
                    PlaceLikelihoodBufferResponse likelyPlaces = task.getResult();
                    for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                        Log.i(TAG, String.format("Place '%s' has likelihood: %g",
                                placeLikelihood.getPlace().getName(),
                                placeLikelihood.getLikelihood()));
                        if (placeLikelihood.getLikelihood() > 0.7) {
                            Toast.makeText(MapActivity.this, "You are at " + placeLikelihood.getPlace().getName(), Toast.LENGTH_LONG).show();
                        }
                    }
                    // release PlaceLikelihoodBufferResponse
                    likelyPlaces.release();
                    likelyPlaces.close();
                }
            });
        }
    }
    */

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
    }
/*
    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        if ( serviceConnection != null ){
            unbindService(serviceConnection);
            serviceConnection = null;
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        Log.d(TAG, "onPause");
        if ( serviceConnection != null ){
            unbindService(serviceConnection);
            serviceConnection = null;
        }
    }
    */

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");

        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }


    // https://developers.google.com/maps/documentation/android-api/current-place-tutorial
    @Override
    public void onMapReady(GoogleMap map) {
        Log.d(TAG, "onMapReady");
        mMap = map;

        // Prompt the user for permission.
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map
        // this is the location button on the top right corner
        updateLocationUI();

        // Ask for location updates
        startLocationUpdates();

        // listen for clicks at Points of Interests allowing to display info that user wants to see
        mMap.setOnPoiClickListener(this);
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


    // todo can get this from elsewhere
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

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        Log.d(TAG, "updateLocationUI");
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
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

    @Override
    public void onPoiClick(PointOfInterest pointOfInterest) {
        Toast.makeText(this, "clicked " + pointOfInterest.name, Toast.LENGTH_SHORT).show();

        // once clicking point of interest, open the info activity for that POI
        Intent intent = new Intent(this, InfoActivity.class);

        // TODO clean up
    //    intent.putExtra("poiID", pointOfInterest.placeId);

        // pass the coordinates of the place
        intent.putExtra("poiLatLng", pointOfInterest.latLng);
        startActivity(intent);
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


}

