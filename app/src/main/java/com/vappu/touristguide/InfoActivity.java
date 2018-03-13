package com.vappu.touristguide;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

public class InfoActivity extends AppCompatActivity {

    // String poiID = "";
    LatLng poiLatLng;
    GeoDataClient mGeoDataClient;

    // Tag for debug purposes
    private static final String TAG = InfoActivity.class.getSimpleName();
    private String summary;
    //    private TextView body;
    private TextView title;

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
            // get the latitude and longitude of the place as a LatLng Object
            poiLatLng = extras.getParcelable("poiLatLng");
            Log.d(TAG, "onCreate: " + poiLatLng.toString());

            // set parameters for the tasks
            // key: "title" for title, "extract" for the summary.
            FetchTaskParams paramsKey = new FetchTaskParams(poiLatLng.latitude, poiLatLng.longitude, "title");
            FetchTaskParams paramsSummary = new FetchTaskParams(poiLatLng.latitude, poiLatLng.longitude, "extract");

            // start background tasks for fetching the title and the summary
            new FetchInfoTask().execute(paramsKey);
            new FetchInfoTask().execute(paramsSummary);
        } else {
            Log.d(TAG, "onCreate: Nothing passed in extras!");
        }
    }

    // wrapper class for parameters to be used in the AsyncTask
    private static class FetchTaskParams {
        double latitude;
        double longitude;
        String key;


        FetchTaskParams(double lat, double lon, String key) {
            Log.d(TAG, "FetchTaskParams");
            this.latitude = lat;
            this.longitude = lon;
            this.key = key;
        }
    }

    // using https://en.wikipedia.org/api/rest_v1/#!/Page_content/get_page_summary_title
    // to get page summaries from the wikimedia API
    @SuppressLint("StaticFieldLeak")
    private class FetchInfoTask extends AsyncTask<FetchTaskParams, Integer, String> {

        String key;

        // JSON passed as a String, parse it and return the content of the desired identifier
        private String parseJSON(String jsonString, String key) {
            Log.d(TAG, "parseJSON");
            String result = null;
            JSONObject jsonObject;
            try {
                switch (key) {
                    case "title":
                        jsonObject = new JSONObject(jsonString);
                        JSONObject queryObject = jsonObject.getJSONObject("query");
                        JSONArray jsonArray = queryObject.getJSONArray("geosearch");
                        jsonObject = jsonArray.getJSONObject(0);
                        result = jsonObject.getString(key);
                        break;
                    case "extract":
                        jsonObject = new JSONObject(jsonString);
                        result = jsonObject.get(key).toString();
                        break;
                    default:
                        Log.d(TAG, "parseJSON: Issue with key " + key);
                        break;
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.d(TAG, jsonString); // debug
            return result;
        }

        @Override
        protected void onPostExecute(String resultString) {
            super.onPostExecute(resultString);
            Log.d(TAG, "onPostExecute");

            // after executed, set the text on body or title to be the parsed string
            switch (key) {
                case "title":
                    TextView title = (TextView) findViewById(R.id.infoText);
                    title.setText(resultString);
                    break;
                case "extract":
                    TextView body = (TextView) findViewById(R.id.contentText);
                    body.setText(resultString);
                    break;
                default:
                    break;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            Log.d(TAG, "onProgressUpdate");
            super.onProgressUpdate(values);
        }

        private String queryApi(String query, String key) {
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

                // return the title by using the JSON as a string and passing "extract" key
                result = parseJSON(stringBuilder.toString(), key);

                // close connection!
                conn.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        // fetch
        @Override
        protected String doInBackground(FetchTaskParams... fetchTaskParams) {
            Log.d(TAG, "doInBackground");

            // for onProgressUpdate
            int i = 0;
            publishProgress(i);

            key = fetchTaskParams[0].key;

            // fetch coordinates
            double lat = fetchTaskParams[0].latitude;
            double lon = fetchTaskParams[0].longitude;


            // debug query
            //String titleQuery = "https://en.wikipedia.org/w/api.php?action=query&list=geosearch&gscoord=37.786952%7C-122.399523&gsradius=10000&gslimit=10&format=json";

            // find the title used by wiki
            String titleQuery = "https://en.wikipedia.org/w/api.php?action=query&list=geosearch" +
                    "&gscoord=" + lat + "|" + lon + "&gsradius=500&gslimit=1&format=json";
            Log.d(TAG, "doInBackground: " + titleQuery);

            String title = queryApi(titleQuery, "title");
            Log.d(TAG, "doInBackground: " + title);

            // if looking for just title then return here
            if (Objects.equals(key, "title")) {
                return title;
            }
            // if not, carry on and get the summary

            // use the obtained page title from wiki to find the summary
            String summaryQuery = "https://en.wikipedia.org/api/rest_v1/page/summary/" + title;

            return queryApi(summaryQuery, key);


                /*
                // define endpoint
                URL wikiEndPoint = new URL("https://en.wikipedia.org/api/rest_v1/page/summary/Nottingham_Castle\n");
                // open connection
                HttpsURLConnection connection = (HttpsURLConnection) wikiEndPoint.openConnection();
                // set up request headers
                connection.setRequestProperty("User-Agent", "tourist-guide");
                connection.setRequestMethod("GET");

                // check if connected successfully
                if (connection.getResponseCode() == 200) {
                    // everything is fine!
                    Log.d(TAG, "response code 200");
                } else {
                    // error, log it
                    Log.e(TAG, "response code " + connection.getResponseCode());

                    // display error message for the user. Most often the error would result from
                    // the lack of internet connection (can't connect to the API
                    if(key.equals("extract")){
                        result = "Error: " + connection.getResponseCode() + ", please check your internet connection";
                    }
                }

                // Get JSON
                InputStream inputStream = connection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();

                // return the summary by using the JSON as a string and passing "extract" key
                result = parseJSON(stringBuilder.toString(), key);

                // close connection!
                connection.disconnect();

            } catch (IOException e) {
                e.printStackTrace();
            }

            //Log.d(TAG, result);                     //debug

            // return the parsed string
            // null if failed
            return result;

        }*/

        }
    }
}