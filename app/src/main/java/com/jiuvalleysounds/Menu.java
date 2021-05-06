package com.jiuvalleysounds;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;



public class Menu extends Activity {
    Button backButton;
    Button soundSubmitButton;
    Button locationSubmitButton;
    Button adminButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve the content view that renders the map.
        setContentView(R.layout.menu);

        //Set an onclick for the back button
        backButton = (Button) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        //Set an onclick for the submit sound button
        soundSubmitButton = (Button) findViewById(R.id.soundSubmitButton);
        soundSubmitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent myIntent = new Intent(v.getContext(), SoundSubmission.class);
                startActivityForResult(myIntent, 0);
            }
        });
        //set an onclick for the location submit button
        locationSubmitButton = (Button) findViewById(R.id.locationButton);
        locationSubmitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent myIntent = new Intent(v.getContext(), LocationSubmission.class);
                startActivityForResult(myIntent, 0);
            }
        });

    }

}