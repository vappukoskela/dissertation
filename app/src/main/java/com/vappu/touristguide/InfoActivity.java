package com.vappu.touristguide;

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

public class InfoActivity extends AppCompatActivity {

    String poiID = "";
    GeoDataClient mGeoDataClient;

    // Tag for debug purposes
    private static final String TAG = InfoActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        final TextView title = (TextView) findViewById(R.id.infoText);
        final TextView body = (TextView) findViewById(R.id.contentText);

        // initialise mGeoDataClient
        mGeoDataClient = Places.getGeoDataClient(this, null);

        // get extras with intent
        Bundle extras = getIntent().getExtras();

        WikiConnHandler wikiConnHandler = new WikiConnHandler("");
        body.setText(wikiConnHandler.getSummary());


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

                        title.setText(myPlace.getName());

                        // release PlaceBufferResponse object
                        places.release();
                    } else {
                        Log.e(TAG, "Place not found.");
                    }
                }
            });



        }

        // using https://en.wikipedia.org/api/rest_v1/#!/Page_content/get_page_summary_title
        // to get page summaries from the wikimedia API

    }


}
