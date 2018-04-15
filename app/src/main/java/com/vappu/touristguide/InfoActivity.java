package com.vappu.touristguide;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBufferResponse;
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

import javax.net.ssl.HttpsURLConnection;

import me.xdrop.fuzzywuzzy.FuzzySearch;

public class InfoActivity extends AppCompatActivity {

    LatLng poiLatLng;
    GeoDataClient mGeoDataClient;

    // Tag for debug purposes
    private static final String TAG = InfoActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        // initialise mGeoDataClient
        mGeoDataClient = Places.getGeoDataClient(this, null);

        // get extras with intent
        Bundle extras = getIntent().getExtras();

        // check if null
        if (extras != null) {
            // TODO remove name from identifiers - get this from placeID

            // get the latitude and longitude of the place as a LatLng Object
            poiLatLng = extras.getParcelable("placeLatLng");
            String name = extras.getString("placeName");
            String placeID = extras.getString("placeID");

            Log.d(TAG, "onCreate: " + name + placeID);

            TextView title = findViewById(R.id.infoText);
            title.setText(name);

            FetchTaskParams paramsKey = new FetchTaskParams(poiLatLng.latitude, poiLatLng.longitude, name, placeID);
            new FetchInfoTask().execute(paramsKey);

            final TextView gTextPrice = findViewById(R.id.gTextPrice);
            final TextView gTextAddress = findViewById(R.id.gTextAddress);
            mGeoDataClient.getPlaceById(placeID).addOnCompleteListener(new OnCompleteListener<PlaceBufferResponse>() {
                @Override
                public void onComplete(@NonNull Task<PlaceBufferResponse> task) {
                    PlaceBufferResponse places = task.getResult();
                    Place thisPlace = places.get(0);
                    int pricelevel = thisPlace.getPriceLevel();
                    String address = (String) thisPlace.getAddress();

                    // TODO bug here
                    if (pricelevel >= 0){
                        gTextPrice.setText(pricelevel);
                    }
                    if (address != null){
                        gTextAddress.setText(address);
                    }
                    places.release();
                }
            });


        } else {
            Log.d(TAG, "onCreate: Nothing passed in extras!");
        }
    }

    // wrapper class for parameters to be used in the AsyncTask
    private static class FetchTaskParams {
        double latitude;
        double longitude;
        String placeName;
        String placeID;


        FetchTaskParams(double lat, double lon, String name, String id) {
            Log.d(TAG, "FetchTaskParams");
            this.latitude = lat;
            this.longitude = lon;
            this.placeName = name;
            this.placeID = id;
        }
    }

    // using https://en.wikipedia.org/api/rest_v1/#!/Page_content/get_page_summary_title
    // to get page summaries from the wikimedia API
    @SuppressLint("StaticFieldLeak")
    private class FetchInfoTask extends AsyncTask<FetchTaskParams, Integer, String> {

        ProgressBar progressBar = findViewById(R.id.progressBar3);
        String key;
        String name;
        String searchQueryType;

        // JSON passed as a String, parse it and return the content of the desired identifier
        private String parseJSON(String jsonString, String key) {
            Log.d(TAG, "parseJSON");
            String result = "";

            if(key.equals("error")){ return jsonString; }

            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(jsonString);
                JSONArray jsonArray = null;
                if(key.equals("extract")) {
                    result = jsonObject.get("extract").toString();
                }
                else {
                    JSONObject queryObject = jsonObject.getJSONObject("query");

                    if (searchQueryType.equals("geo")) {
                        jsonArray = queryObject.getJSONArray("geosearch");
                    } else if (searchQueryType.equals("search")) {
                        jsonArray = queryObject.getJSONArray("search");
                    }
                    if (jsonArray != null) {
                        jsonObject = jsonArray.getJSONObject(0);
                    }
                    result = jsonObject.getString("title");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "parseJSON: result " + result);
            return result;
        }

        @Override
        protected void onPostExecute(String resultString) {
            super.onPostExecute(resultString);
            progressBar.setVisibility(View.INVISIBLE);
            TextView body = findViewById(R.id.contentText);
            body.setText(resultString);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
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

                key = "error";
                return "Unable to connect. Please check your connection";
            }
            Log.d(TAG, "queryApi: result is " + result);
            return result;
        }

        // use FuzzyWuzzy to check whether the latlong search wiki article is what we want!
        // parameters wTitle = title from wikipedia
        // gPlaceName = what google thinks the place is called
        private boolean isFuzzyCorrect(String wTitle, String gPlaceName){
            int fuz = FuzzySearch.weightedRatio(wTitle, gPlaceName);
            Log.d(TAG, "isFuzzyCorrect: " + fuz);
            return fuz > 70;
        }

        // fetch
        @Override
        protected String doInBackground(FetchTaskParams... fetchTaskParams) {
            Log.d(TAG, "doInBackground");

            name = fetchTaskParams[0].placeName;

            // fetch coordinates
            double lat = fetchTaskParams[0].latitude;
            double lon = fetchTaskParams[0].longitude;

            // Different queries to try on wiki
            searchQueryType = "geo";
            String geoQueryResult = parseJSON(queryApi("https://en.wikipedia.org/w/api.php?action=query&list=geosearch" +
                    "&gscoord=" + lat + "|" + lon + "&gsradius=500&gslimit=5&format=json"), "title");

            searchQueryType = "search";
            String searchQueryResult = parseJSON(queryApi("https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=" + name + "&format=json"), "title");

            String summaryText = "";

            if(isFuzzyCorrect(geoQueryResult, name)){
                summaryText = parseJSON(queryApi("https://en.wikipedia.org/api/rest_v1/page/summary/" + geoQueryResult), "extract");
            }
            else if(isFuzzyCorrect(searchQueryResult, name)){
                summaryText = parseJSON(queryApi("https://en.wikipedia.org/api/rest_v1/page/summary/" + searchQueryResult), "extract");
            }
            else {
                Log.d(TAG, "doInBackground: Cannot find wiki page");
            }
            return summaryText;
        }
    }
}