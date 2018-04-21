package com.vappu.touristguide;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

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
        String[] params = new String[1];

        // check if null
        if (extras != null) {
            String wikiId = extras.getString("wikiID");
            String title = extras.getString("title");

            TextView titleView = findViewById(R.id.infoText);
            titleView.setText(title);

            params[0] = title;
            new FetchInfoTask().execute(params);

        } else {
            Log.d(TAG, "onCreate: Nothing passed in extras!");
        }
    }


    // using https://en.wikipedia.org/api/rest_v1/#!/Page_content/get_page_summary_title
    // to get page summaries from the wikimedia API
    @SuppressLint("StaticFieldLeak")
    private class FetchInfoTask extends AsyncTask<String, Integer, String> {

        ProgressBar progressBar = findViewById(R.id.progressBar3);

        // JSON passed as a String, parse it and return the content of the desired identifier
        private String parseJSON(String jsonString) {
            Log.d(TAG, "parseJSON");
            String result = "";
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(jsonString);
                result = jsonObject.get("extract").toString();

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
                return "Unable to connect. Please check your connection";
            }
            Log.d(TAG, "queryApi: result is " + result);
            return result;
        }

        // fetch
        @Override
        protected String doInBackground(String... strings) {
            Log.d(TAG, "doInBackground");

            String title = strings[0];
            String summaryText = "";
            summaryText = parseJSON(queryApi("https://en.wikipedia.org/api/rest_v1/page/summary/" + title));
            return summaryText;
        }
    }

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