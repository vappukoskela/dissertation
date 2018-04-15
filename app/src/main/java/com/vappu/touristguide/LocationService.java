package com.vappu.touristguide;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static android.support.v4.app.NotificationCompat.PRIORITY_HIGH;
import static android.support.v4.app.NotificationCompat.PRIORITY_MIN;


// LocationService, start a new service that tracks the user's location and lets them know of interesting places
// place detection would go here too

public class LocationService extends Service {

    // define TAG for logs
    private static final String TAG = LocationService.class.getSimpleName();

    // the threshold which determines whether to send notification or not
    private static final double PLACELIKELIHOODTHRESHOLD = 0.0;

    // default location which will be used if location unavailable
    private final LatLng mDefaultLocation = new LatLng(52.949591, -1.154830);

    private PlaceDetectionClient mPlaceDetectionClient;
    private LocationCallback mLocationCallBack;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private NotificationManager notificationManager;
    private ArrayList<Integer> mTypesList;
    private ArrayList<Integer> mIndoorsList;
    private ArrayList<Integer> mOutdoorsList;
    private String[] mPreviousArray = new String[2];


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        mTypesList = new ArrayList<>();
        mIndoorsList = new ArrayList<>();
        mOutdoorsList = new ArrayList<>();
        updateFilters();

        // Create Notification Channels
        int importance;
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

        createNotification();

        mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    Log.d(TAG, "location " + location.toString());
                    if (location.toString() != null) {
                        Log.d(TAG, "Location not null");
                        checkLikelyPlaces(location);
                    }
                }
            }
        };

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(15000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        startLocationUpdates();
    }

    private void updateFilters(){
        mTypesList.clear();
        mTypesList.addAll(mOutdoorsList);
        mTypesList.addAll(mIndoorsList);
    }

    // modify the list containing categories for filtering
    public void filterIndoors(boolean isIndoorChecked) {
        if (isIndoorChecked) {
            mIndoorsList.add(Place.TYPE_ART_GALLERY);
            mIndoorsList.add(Place.TYPE_MUSEUM);
            mIndoorsList.add(Place.TYPE_LIBRARY);
        }
        else { mIndoorsList.clear(); }
        updateFilters();
    }

    public void filterOutdoors(boolean isOutdoorChecked){
        if(isOutdoorChecked){
            mOutdoorsList.add(Place.TYPE_AMUSEMENT_PARK);
            mOutdoorsList.add(Place.TYPE_ZOO);
            mOutdoorsList.add(Place.TYPE_MUSEUM);
            mOutdoorsList.add(Place.TYPE_PARK);
            mOutdoorsList.add(Place.TYPE_STADIUM);
            mOutdoorsList.add(Place.TYPE_UNIVERSITY);
            mOutdoorsList.add(Place.TYPE_CEMETERY);
            mOutdoorsList.add(Place.TYPE_PLACE_OF_WORSHIP);
            mOutdoorsList.add(Place.TYPE_CITY_HALL);
            mOutdoorsList.add(Place.TYPE_EMBASSY);
        } else { mOutdoorsList.clear(); }
        updateFilters();
    }

    private void startLocationUpdates() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "startLocationUpdates: Permissions not enabled");
                return;
            } else {
                mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallBack, null);
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void checkLikelyPlaces(Location location) {
        Log.d(TAG, "checkLikelyPlaces " + location);
        @SuppressLint("MissingPermission") Task<PlaceLikelihoodBufferResponse> placeResult = mPlaceDetectionClient.getCurrentPlace(null);
            placeResult.addOnCompleteListener(new OnCompleteListener<PlaceLikelihoodBufferResponse>() {
                @Override
                public void onComplete(@NonNull Task<PlaceLikelihoodBufferResponse> task) {
                    if (task.isSuccessful()) {
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

                            // if the place was in previous 3 places, then do not send notification for it
                            boolean isDuplicate = false;
                            for (String previousId : mPreviousArray) {
                                if (Objects.equals(id, previousId)) {
                                    isDuplicate = true;
                                }
                            }


                            if(!typeList.isEmpty()) {

                                // TODO add all latlngs to a list
                                LatLng latLng = placeLikelihood.getPlace().getLatLng();

                                if (!isDuplicate && placeLikelihood.getLikelihood() > PLACELIKELIHOODTHRESHOLD) {

                                    Log.d(TAG, "onComplete: Passed, create notification");
                                    createInfoNotification(placeLikelihood.getPlace());

                                    // shift to the right
                                    System.arraycopy(mPreviousArray, 0, mPreviousArray, 1, mPreviousArray.length - 1);
                                    mPreviousArray[0] = id;
                                    Log.d(TAG, "onComplete: " + Arrays.toString(mPreviousArray));
                                    break;
                                }
                            }
                        }
                        likelyPlaces.release();
                    }
                }
            });
    }

    private void createNotificationChannel(int importance, CharSequence name, String description, String channelID) {
        // check the sdk version so that only doing this if sdk 8.0 and higher
        // since channels are a newer concept
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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

    public void createNotification(){

        Log.d(TAG, "service createNotification");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(this, "channelID")
                .setTicker(("message"))
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setContentTitle("Tourist guide")
                .setContentText("Running in the background")
                .setOngoing(true)
                .setContentIntent(pi)
                .setPriority(PRIORITY_MIN)
                .setAutoCancel(false)
                .build();


        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(0, notification);
        }
    }

    public void createInfoNotification(Place place){

        String placeName = (String) place.getName();

        Intent intent = new Intent(this, InfoActivity.class);
        intent.putExtra("placeID", place.getId());
        intent.putExtra("placeName", place.getName());
        intent.putExtra("placeLatLng", place.getLatLng());

        Log.d(TAG, "service createNotification " + placeName + place.getId());
        Intent notificationIntent = new Intent(intent);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this,0 , notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(this, "channelID")
                .setTicker(("message"))
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setContentTitle("Interesting Place Detected")
                .setContentText(placeName)
                .setContentIntent(pi)
                .setPriority(PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            // create a unique id for the notification
            notificationManager.notify( 1, notification);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (notificationManager != null) {
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