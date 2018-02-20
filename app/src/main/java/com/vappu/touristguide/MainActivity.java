package com.vappu.touristguide;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void openMap(View view) {
        // Placeholder button
        // tackling the location stuff first
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }

    // TODO create a prompt to ask user for permissions

}
