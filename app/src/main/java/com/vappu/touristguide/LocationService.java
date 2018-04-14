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
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static android.support.v4.app.NotificationCompat.PRIORITY_HIGH;


// LocationService, start a new service that tracks the user's location and lets them know of interesting places
// place detection would go here too

public class LocationService extends Service {

    public ServiceState state;

    public enum ServiceState{
        RUNNING,
        STOPPED
    }

    public ServiceState getState(){
        return this.state;
    }

    // define TAG for logs
    private static final String TAG = LocationService.class.getSimpleName();

    // the threshold which determines whether to send notification or not
    private static final double PLACELIKELIHOODTHRESHOLD = 0.0;

    // default location which will be used if location unavailable
    private final LatLng mDefaultLocation = new LatLng(52.949591, -1.154830);

    private boolean mLocationPermissionGranted = false;

    private PlaceDetectionClient mPlaceDetectionClient;
    private LocationCallback mLocationCallBack;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private NotificationManager notificationManager;
    private String mPreviousPlaceId = "";
    private ArrayList<Object> mTypesList;
    private ArrayList<Object> mIndoorsList;
    private ArrayList<Object> mShopsLists;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        createFilters();

        //TODO delete
        setLocationPermissionGranted(true);

        // Create Notification Channels
        int importance = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            // Create notification channel for place detection alerts
            importance = NotificationManager.IMPORTANCE_DEFAULT;
            CharSequence name = "guideChannel";
            String description = "channel for tourist guide proximity notifications";
            String channelID = "channelID";
            createNotificationChannel(importance, name, description, channelID);

            // Create Notification Channel for notification keeping service alive
            importance = NotificationManager.IMPORTANCE_MIN;
            name = "serviceChannel";
            description = "channel for notification keeping service alive";
            channelID = "serviceID";
            createNotificationChannel(importance, name, description, channelID);
        }

        mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    Log.d(TAG, "location " + location.toString());

                    if(location.toString() != null){
                        Log.d(TAG, "Location not null");
                        checkLikelyPlaces(location);
                    }
                }
            }
        };

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        startLocationUpdates();
    }

    private void createFilters() {

        mTypesList = new ArrayList<>();

        // filter grouping 1
        //mTypesList.add(Place.TYPE_POINT_OF_INTEREST);

        // activities and places
        mTypesList.add(Place.TYPE_AMUSEMENT_PARK);
        mTypesList.add(Place.TYPE_ART_GALLERY);
        mTypesList.add(Place.TYPE_ZOO);
        mTypesList.add(Place.TYPE_MUSEUM);
        mTypesList.add(Place.TYPE_PARK);
        mTypesList.add(Place.TYPE_STADIUM);
        mTypesList.add(Place.TYPE_UNIVERSITY);
        mTypesList.add(Place.TYPE_SPA);
        mTypesList.add(Place.TYPE_LOCALITY);
        mTypesList.add(Place.TYPE_SUBLOCALITY);

        // religious places
        mTypesList.add(Place.TYPE_CEMETERY);
        mTypesList.add(Place.TYPE_PLACE_OF_WORSHIP);
        mTypesList.add(Place.TYPE_CHURCH);
        mTypesList.add(Place.TYPE_HINDU_TEMPLE);
        mTypesList.add(Place.TYPE_MOSQUE);
        mTypesList.add(Place.TYPE_SYNAGOGUE);

        // places of authority
        mTypesList.add(Place.TYPE_CITY_HALL);
        mTypesList.add(Place.TYPE_EMBASSY);


        // indoors places, not shops
        mIndoorsList = new ArrayList<>();
        mIndoorsList.add(Place.TYPE_ART_GALLERY);
        mIndoorsList.add(Place.TYPE_MUSEUM);
        mIndoorsList.add(Place.TYPE_SPA);
        mIndoorsList.add(Place.TYPE_LIBRARY);


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

    private void checkNearbyPlaces(Location location) {

    }

    @SuppressLint("MissingPermission")
    private void checkLikelyPlaces(Location location) {
        Log.d(TAG, "checkLikelyPlaces " + location);
        if(mLocationPermissionGranted) {
                Task<PlaceLikelihoodBufferResponse> placeResult = mPlaceDetectionClient.getCurrentPlace(null);
                placeResult.addOnCompleteListener(new OnCompleteListener<PlaceLikelihoodBufferResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<PlaceLikelihoodBufferResponse> task) {
                        if(task.isSuccessful()) {
                            PlaceLikelihoodBufferResponse likelyPlaces = task.getResult();

                            // keep this until sure won't need all
                            for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                                Log.i(TAG, String.format("Place '%s' has likelihood: %g",
                                        placeLikelihood.getPlace().getName(),
                                        placeLikelihood.getLikelihood()));

                                String id = placeLikelihood.getPlace().getId();

                                Log.d(TAG, "PlaceTypes originally " + placeLikelihood.getPlace().getPlaceTypes());
                                List<Integer> typeList = placeLikelihood.getPlace().getPlaceTypes();
                                typeList.retainAll(mTypesList);
                                Log.d(TAG, "PlaceTypes after retainall: " + typeList);

                                // is not the same place as previously notified
                                // proximity over threshold
                                if (!(Objects.equals(id, mPreviousPlaceId))
                                        && placeLikelihood.getLikelihood() > PLACELIKELIHOODTHRESHOLD
                                        && !typeList.isEmpty()) {
                                    Log.d(TAG, "onComplete: Passed, create notification");
                                    createInfoNotification(placeLikelihood.getPlace());
                                    mPreviousPlaceId = id;
                                    break;
                                }
                            }
                            // release PlaceLikelihoodBufferResponse
                            likelyPlaces.release();
                        }
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


    public void createNotificationChannel(int importance, CharSequence name, String description, String channelID){
        // check the sdk version so that only doing this if sdk 8.0 and higher
        // since channels are a newer concept
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            //int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel mChannel = new NotificationChannel(channelID, name, importance);
            mChannel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = (NotificationManager) getSystemService(
                    NOTIFICATION_SERVICE);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(mChannel);
        }

    }
/*
    public void createNotification(String channelID, int priority){

        Log.d(TAG, "service createNotification");
        Intent notificationIntent = new Intent(this, InfoActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(this, "channelID")
                .setTicker(("message"))
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setContentTitle("Tourist guide")
                .setContentText("running in the background")
                .setContentIntent(pi)
                .setPriority(PRIORITY_MIN)
                .setAutoCancel(false)
                .build();


        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(0, notification);
        }
    }
*/
    public void createInfoNotification(Place place){

        String placeName = (String) place.getName();

        Intent intent = new Intent(this, InfoActivity.class);
        intent.putExtra("placeID", place.getId());
        intent.putExtra("placeName", place.getName());
        intent.putExtra("placeLatLng", place.getLatLng());


        Log.d(TAG, "service createNotification");
        Intent notificationIntent = new Intent(intent);
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
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        if (notificationManager != null){
            notificationManager.cancelAll();
        }
    }


    public class LocalBinder extends Binder {
        LocationService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LocationService.this;
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
        stopSelf();
        return super.onUnbind(intent);
    }
}