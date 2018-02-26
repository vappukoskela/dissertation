package com.vappu.touristguide;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class InfoActivity extends AppCompatActivity {

    String poiID = "";
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


        // set parameters for the tasks
        FetchTaskParams paramsKey = new FetchTaskParams("title", "Nottingham_Castle");
        FetchTaskParams paramsSummary = new FetchTaskParams("extract", "Nottingham_Castle");

        // start background tasks for fetching the title and the summary
        new FetchInfoTask().execute(paramsKey);
        new FetchInfoTask().execute(paramsSummary);


        // TODO get rid of this
        // This fetches the place name by Google's place ID
        // a smarter way to do this to enable the use of the wiki API would be to search by name
        // although names can sometimes be tricky!

        // check if null
        if (extras != null) {
            poiID = extras.getString("poiID");

            mGeoDataClient.getPlaceById(poiID).addOnCompleteListener(new OnCompleteListener<PlaceBufferResponse>() {
                @Override
                public void onComplete(@NonNull Task<PlaceBufferResponse> task) {
                    if (task.isSuccessful()) {
                        PlaceBufferResponse places = task.getResult();
                        Place myPlace = places.get(0);
                        Log.i(TAG, "Place found: " + myPlace.getName());

                        //title.setText(myPlace.getName());

                        // release PlaceBufferResponse object
                        places.release();
                    } else {
                        Log.e(TAG, "Place not found.");
                    }
                }
            });
        }
    }

    // wrapper class for parameters to be used in the AsyncTask
    private static class FetchTaskParams{
        String key;
        String page;

        FetchTaskParams(String key, String page){
            Log.d(TAG, "FetchTaskParams");
            this.key = key;
            this.page = page;
        }
    }

    // using https://en.wikipedia.org/api/rest_v1/#!/Page_content/get_page_summary_title
    // to get page summaries from the wikimedia API
    @SuppressLint("StaticFieldLeak")
    private class FetchInfoTask extends AsyncTask<FetchTaskParams, Integer, String> {

        private String key;

        // JSON passed as a String, parse it and return the content of the desired identifier
        private String parseJSON(String jsonString, String key) {
            Log.d(TAG, "parseJSON");
            String result = null;
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(jsonString);
                result = jsonObject.get(key).toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //Log.d(TAG, result); // debug
            return result;
        }

        @Override
        protected void onPostExecute(String resultString) {
            super.onPostExecute(resultString);
            Log.d(TAG, "onPostExecute");

            // after executed, set the text on body or title to be the parsed string
            switch (key){
                case "extract":
                    TextView body = (TextView) findViewById(R.id.contentText);
                    body.setText(resultString);
                    break;
                case "title":
                    TextView title = (TextView) findViewById(R.id.infoText);
                    title.setText(resultString);
                    break;
                default:
                    // invalid key, does not match either of the required options
                    Log.e(TAG, "Invalid key onPostExecute");
                    break;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            Log.d(TAG, "onProgressUpdate");
            super.onProgressUpdate(values);
        }

        // fetch
        @Override
        protected String doInBackground(FetchTaskParams... fetchTaskParams) {
            Log.d(TAG, "doInBackground");
            String result = null;
            try {
                // for onProgressUpdate
                int i = 0;
                publishProgress(i);

                // key is the identifier want to fetch e.g. summary ("extract"), title, etc.
                key = fetchTaskParams[0].key;

                // page to be used to select the correct wiki page
                String page = fetchTaskParams[0].page;

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
        }
    }
}