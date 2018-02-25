package com.vappu.touristguide;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by vappu on 23/02/2018.
 */

public class WikiConnHandler {

    String TAG = WikiConnHandler.class.getSimpleName();
    String summary;

    public WikiConnHandler(String url){
        // New thread to handle network connection
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // define endpoint
                    URL wikiEndPoint = new URL("https://en.wikipedia.org/api/rest_v1/page/summary/Nottingham_Castle\n");
                    // open connection
                    HttpsURLConnection connection = (HttpsURLConnection) wikiEndPoint.openConnection();
                    // set up request headers
                    connection.setRequestProperty("User-Agent", "tourist-guide");
                    connection.setRequestMethod("GET");



                    // check if connected successfully
                    if(connection.getResponseCode() == 200){
                        // everything is fine!
                        Log.d(TAG, "response code 200");
                    }
                    else {
                        // error, log it
                        // TODO might need some error handling
                        Log.e(TAG, "response code " + connection.getResponseCode());
                    }

                    String result = null;

                    // Get JSON

                    InputStream inputStream = connection.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    StringBuilder stringBuilder = new StringBuilder();
                    String line = null;
                    while((line = bufferedReader.readLine()) != null ){
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();

                    // debug
                    result = stringBuilder.toString();
                    Log.d(TAG, result);

                    // close connection!
                    connection.disconnect();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public String getSummary() {
        return summary;
    }

    private void setSummary(String summary) {
        this.summary = summary;
    }
}
