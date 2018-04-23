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
import android.media.RingtoneManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

import static android.support.v4.app.NotificationCompat.PRIORITY_HIGH;
import static android.support.v4.app.NotificationCompat.PRIORITY_MIN;


// LocationService, start a new service that tracks the user's location and lets them know of interesting places
// place detection would go here too

public class LocationService extends Service {

    // define TAG for logs
    private static final String TAG = LocationService.class.getSimpleName();
    public static final String STOP_SERVICE = "stop_service";
    public static final String START_SERVICE = "start_service";

    // default location which will be used if location unavailable
    public final LatLng mDefaultLocation = new LatLng(52.949591, -1.154830);
    private final int NOTIFID = 3;

    private PlaceDetectionClient mPlaceDetectionClient;
    private LocationCallback mLocationCallBack;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private LatLng mCurrentLocation;

    private NotificationManager notificationManager;
    private ArrayList<Integer> mTypesList;
    private ArrayList<MarkerObject> markerObjectArrayList = new ArrayList<>();

    private String[] mPreviousArray = new String[10];
    private ArrayList<Integer> mFoodList;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: ");

        if (Objects.equals(intent.getAction(), STOP_SERVICE)){
            Log.d(TAG, "onStartCommand: stop");
            stopForeground(true);
            notificationManager.cancelAll();
            stopSelf();
            return START_NOT_STICKY;
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        mTypesList = new ArrayList<>();
        mFoodList = new ArrayList<>();

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
        startForeground(NOTIFID, createNotification());

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
                        LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
                        Intent intent = new Intent("locationEvent");
                        intent.putExtra("latlng", latlng);
                        LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(intent);
                    }
                }
            }
        };

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(20000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        startLocationUpdates();
    }

    private void updateFilters() {
        mTypesList.clear();
        mTypesList.addAll(mFoodList);
    }


    public void filterFood(boolean isChecked) {
        if (isChecked) {
            mFoodList.add(Place.TYPE_BAR);
            mFoodList.add(Place.TYPE_CAFE);
            mFoodList.add(Place.TYPE_RESTAURANT);
        } else {
            mFoodList.clear();
        }
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
        @SuppressLint("MissingPermission") Task<PlaceLikelihoodBufferResponse> placeResult = mPlaceDetectionClient.getCurrentPlace(null);
        placeResult.addOnCompleteListener(new OnCompleteListener<PlaceLikelihoodBufferResponse>() {
            @Override
            public void onComplete(@NonNull Task<PlaceLikelihoodBufferResponse> task) {
                if (task.isSuccessful()) {
                    PlaceLikelihoodBufferResponse likelyPlaces = task.getResult();
                    for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                        Log.i(TAG, String.format("Place '%s' has likelihood: %g",
                                placeLikelihood.getPlace().getName(),
                                placeLikelihood.getLikelihood()));

                        List<Integer> typeList = placeLikelihood.getPlace().getPlaceTypes();
                        typeList.retainAll(mTypesList);
                        if (!typeList.isEmpty()){
                            createMarkerObject(null, placeLikelihood.getPlace().getId(),
                                    placeLikelihood.getPlace().getLatLng(),
                                    placeLikelihood.getPlace().getName().toString(),
                                    0.0);
                        }
                    }
                    likelyPlaces.release();
                }
            }
        });

        // check nearby wiki articles
        WikiTaskParams wikiTaskParams = new WikiTaskParams(location.getLatitude(), location.getLongitude());
        new FindNearbyWikiTextTask().execute(wikiTaskParams);

        Log.d(TAG, "checkLikelyPlaces: END");
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

    public Notification createNotification() {
        Log.d(TAG, "service createNotification");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 , notificationIntent, 0);

        Intent stopServiceIntent = new Intent(this, LocationService.class);
        stopServiceIntent.setAction(STOP_SERVICE);
        PendingIntent stopServicePendingIntent = PendingIntent.getService(this, 0, stopServiceIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, "channelID")
                .setTicker(("message"))
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setContentTitle("Tourist guide")
                .setContentText("Running in the background")
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setPriority(PRIORITY_MIN)
                .setAutoCancel(false)
                .addAction(R.drawable.ic_close, "Close", stopServicePendingIntent)
                .build();

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFID, notification);
        }
        return notification;
    }

    private boolean isDuplicate(String placeID) {
        for (String previousId : mPreviousArray) {
            if (placeID.equals(previousId)) {
                return true;
            }
        }
        return false;
    }

    private void updateMPreviousArray(String placeID) {
        System.arraycopy(mPreviousArray, 0, mPreviousArray, 1, mPreviousArray.length - 1);
        mPreviousArray[0] = placeID;
    }

    public void createInfoNotification(String placeID, String title) {
        // if the place was in previous 10 places, then do not send notification for it

        if (!isDuplicate(placeID)) {
            Intent intent = new Intent(this, InfoActivity.class);
            intent.putExtra("ID", placeID);
            intent.putExtra("title", title);
            Intent notificationIntent = new Intent(intent);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Notification notification = new NotificationCompat.Builder(this, "channelID")
                    .setTicker(("message"))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentTitle("Interesting Place Detected")
                    .setContentText(title)
                    .setContentIntent(pi)
                    .setPriority(PRIORITY_HIGH)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setAutoCancel(true)
                    .build();

            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                // each notification has a unique id to allow multiple notifications on the same channel
                notificationManager.notify(1, notification);
            }
            updateMPreviousArray(placeID);
        }
    }

    private MarkerObject createMarkerObject(String pageID, String placeID, LatLng latLng, String title, double distance){
        // create the object for the place
        MarkerObject markerObject = new MarkerObject();
        if (pageID!= null) {
            markerObject.setWikiID(pageID);
        }
        else if( placeID != null ){
            markerObject.setPlaceID(placeID);
        }
        markerObject.setLatLng(latLng);
        markerObject.setTitle(title);
        markerObject.setDistance(distance);

        Intent intent = new Intent("placeEvent");
        intent.putExtra("wikiID", pageID);
        intent.putExtra("placeID", placeID);
        intent.putExtra("latlng", latLng);
        intent.putExtra("name", title);
        LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(intent);
        return markerObject;
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
        return super.onUnbind(intent);
    }

    // wrapper class for parameters to be used in the AsyncTask
    private static class WikiTaskParams {
        double latitude;
        double longitude;

        WikiTaskParams(double lat, double lon) {
            Log.d(TAG, "FetchTaskParams");
            this.latitude = lat;
            this.longitude = lon;
        }
    }

    // using https://en.wikipedia.org/api/rest_v1/#!/Page_content/get_page_summary_title
    // to get page summaries from the wikimedia API
    @SuppressLint("StaticFieldLeak")
    private class FindNearbyWikiTextTask extends AsyncTask<WikiTaskParams, Integer, String> {

        private double lat;
        private double lon;

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            for ( int i = 0; i < markerObjectArrayList.size(); i++ ){

                // notify user about a nearby sight
                if ( markerObjectArrayList.get(i).getDistance() < 100 ){
                    createInfoNotification(markerObjectArrayList.get(i).getWikiID(),
                            markerObjectArrayList.get(i).getTitle());
                }
            }
        }

        private boolean passesFilter(String title){
            // discard titles with numbers in them, these are often dates
            // wikipedia's dated titles are often fairly recent events
            // such as 2011 Anti-cut protest, 2012 Summer Olympics Triathlon etc

            return !title.matches(".*\\d+.*");
        }

        // JSON passed as a String, parse the content of the desired identifier
        private String parseJSON(String jsonString) {
            Log.d(TAG, "parseJSON");

            JSONObject jsonObject;
            try {
                markerObjectArrayList.clear();

                jsonObject = new JSONObject(jsonString);
                JSONArray pagesArray = jsonObject.getJSONObject("query").getJSONArray("geosearch");

                for (int i = 0; i < pagesArray.length(); i++){
                    JSONObject pageObject = pagesArray.getJSONObject(i);
                    final String pageID = pageObject.getString("pageid");
                    final String title = pageObject.getString("title");
                    double latitude = pageObject.getDouble("lat");
                    double longitude = pageObject.getDouble("lon");
                    final double distance = pageObject.getDouble("dist");

                    final LatLng latLng = new LatLng(latitude, longitude);

                    Log.d(TAG, "parseJSON: " + pageObject);

                    if(passesFilter(title)) {
                        markerObjectArrayList.add(createMarkerObject(pageID, null, latLng,title,distance));
                    }
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
            return "success";
        }


        private String queryApi(String query) {
            String result = "";
            try {
                URL titleEndPoint = new URL(query);
                HttpsURLConnection conn = (HttpsURLConnection) titleEndPoint.openConnection();
                conn.setRequestProperty("User-Agent", "tourist-guide");
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    // everything is fine!
                    Log.d(TAG, "response code 200");
                } else {
                    // error, log it
                    Log.e(TAG, "response code " + conn.getResponseCode());
                }

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
                Log.d(TAG, "queryApi: " + stringBuilder.toString());
                result = stringBuilder.toString();

                // close connection!
                conn.disconnect();

            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "queryApi: result is " + result);
            return result;
        }

        @Override
        protected String doInBackground(WikiTaskParams... wikiTaskParams) {
            Log.d(TAG, "doInBackground");

            // fetch coordinates
            lat = wikiTaskParams[0].latitude;
            lon = wikiTaskParams[0].longitude;

            String locationQuery = "https://en.wikipedia.org/w/api.php?action=query&format=json&" +
                    "list=geosearch&gscoord=" +
                    lat +
                    "|" +
                    lon +
                    "&gsradius=500|";

            Log.d(TAG, "doInBackground: locationquery " + locationQuery);

            return parseJSON(queryApi(locationQuery));
        }
    }
}