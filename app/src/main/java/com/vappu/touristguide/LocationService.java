package com.vappu.touristguide;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import static android.support.v4.app.NotificationCompat.PRIORITY_HIGH;


// LocationService, start a new service that tracks the user's location and lets them know of interesting places
// place detection would go here too

public class LocationService extends Service {

    // define TAG for logs
    private static final String TAG = LocationService.class.getSimpleName();

    // the threshold which determines whether to send notification or not
    private static final double PLACELIKELIHOODTHRESHOLD = 0.25;

    // default location which will be used if location unavailable
    private final LatLng mDefaultLocation = new LatLng(52.949591, -1.154830);

    private boolean mLocationPermissionGranted = false;

    private PlaceDetectionClient mPlaceDetectionClient;
    private LocationCallback mLocationCallBack;
    private Location mLastKnownLocation;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private NotificationManager notificationManager;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        //TODO delete
        setLocationPermissionGranted(true);

        mLastKnownLocation = new Location(String.valueOf(mDefaultLocation));
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    Log.d(TAG, "location " + location.toString());

                    mLastKnownLocation = location;

                    if(location.toString() != null){
                        Log.d(TAG, "Location not null");
                        checkLikelyPlaces(location);
                    }

                    /*
                    THIS SHOULD BE USED IN MAPACTIVITY BUT CALL LOCATION FROM HERE

                    // move camera to center the user and keep current zoom level
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(mLastKnownLocation.getLatitude(),
                                    mLastKnownLocation.getLongitude()), mMap.getCameraPosition().zoom));

                    */
                }
            }
        };

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        createInfoNotification("hi");
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        try {
            if(mLocationPermissionGranted) {
                mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallBack, null);
            }
            else {
                Log.e(TAG, "Current location is null. Using defaults.");
            }
        }
        catch ( SecurityException e ){
            Log.e("Exception: %s", e.getMessage());
        }

    }


    // adapted from https://developers.google.com/places/android-api/current-place#get-current
    @SuppressLint("MissingPermission")
    private void checkLikelyPlaces(Location location) {
        Log.d(TAG, "checkLikelyPlaces " + location);
        if(mLocationPermissionGranted) {
            // TODO add filters
            Task<PlaceLikelihoodBufferResponse> placeResult = mPlaceDetectionClient.getCurrentPlace(null);
            placeResult.addOnCompleteListener(new OnCompleteListener<PlaceLikelihoodBufferResponse>() {
                @Override
                public void onComplete(@NonNull Task<PlaceLikelihoodBufferResponse> task) {
                    PlaceLikelihoodBufferResponse likelyPlaces = task.getResult();

                    // get the top result of the likely places
                    // i.e. the most likely place we are near of
                    PlaceLikelihood placeLikelihood = likelyPlaces.get(0);
                    Log.i(TAG, String.format("place '%s' has likelihood %g", placeLikelihood.getPlace().getName(),
                            placeLikelihood.getLikelihood()));
                    if ( placeLikelihood.getLikelihood() > 0.25 ){
                        createInfoNotification(String.valueOf(placeLikelihood.getPlace().getName()));
                    }


                    /* don't necessarily need all of them!
                    // keep this until sure won't need all
                    for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                        Log.i(TAG, String.format("Place '%s' has likelihood: %g",
                                placeLikelihood.getPlace().getName(),
                                placeLikelihood.getLikelihood()));

                    }
                    */

                    // release PlaceLikelihoodBufferResponse
                    likelyPlaces.release();
                    likelyPlaces.close();
                }
            });
        }
    }


    public boolean isLocationPermissionGranted() {
        return mLocationPermissionGranted;
    }

    public void setLocationPermissionGranted(boolean mLocationPermissionGranted) {
        this.mLocationPermissionGranted = mLocationPermissionGranted;
    }


    public class LocalBinder extends Binder {
        LocationService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LocationService.this;
        }
    }

    public void createNotificationChannel(){
        // check the sdk version so that only doing this if sdk 8.0 and higher
        // since channels are a newer concept
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // tODO these strings should be in /res/
            CharSequence name = "guideChannel";
            String description = "channel for tourist guide proximity notifications";
            String CHANNEL_ID = "channelID";

            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mChannel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = (NotificationManager) getSystemService(
                    NOTIFICATION_SERVICE);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(mChannel);
        }

    }

    public void createInfoNotification(String placeName){

        createNotificationChannel();

        Log.d(TAG, "service createNotification");
        Intent notificationIntent = new Intent(this, InfoActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(this, "channelID")
                .setTicker(("message"))
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setContentTitle("Interesting Place Detected")
                .setContentText(placeName)
                .setContentIntent(pi)
                .setPriority(PRIORITY_HIGH)
                .setAutoCancel(false)
                .build();


        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(0, notification);
        }
    }



    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "service onBind");
        return new LocalBinder();

    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "service onRebind");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "service onUnbind");



        // TODO figure out how to stop it

        return super.onUnbind(intent);
    }
}