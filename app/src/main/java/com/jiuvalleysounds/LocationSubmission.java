package com.jiuvalleysounds;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;


public class LocationSubmission extends Activity implements LocationListener {
    Button backButton3;
    EditText locationName;
    EditText userName;
    EditText description;
    TextView coordinates;
    LocationManager locationManager;
    Double latitude;
    Double longitude;
    Location currLocation;
    String ACCESS_TOKEN = "";
    String bucketLink;
    Integer id;
    private static final int LOCATION_ACCESS = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location_submission);
        //Set an onclick for the back button
        backButton3 = (Button) findViewById(R.id.backButton3);
        backButton3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        //make the visual objects be variables
        locationName = (EditText) findViewById(R.id.names);
        userName = (EditText) findViewById(R.id.name);
        description = (EditText) findViewById(R.id.description);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //check to see if the location access is on
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(LocationSubmission.this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, LOCATION_ACCESS);
            return;
        }
        //listen for location changes
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

    }

    //do this if the location access is not on
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        //ask for permission to access location
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_ACCESS){
            //if not granted, tell them it needs permission
            if(grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(LocationSubmission.this, getString(R.string.locationPermission), Toast.LENGTH_SHORT);
            }
        }
    }
    //functionality required for get location to work
    @Override
    public void onLocationChanged(Location location){
        latitude = location.getLatitude();
        longitude = location.getLongitude();
    }

    //
    public void getLocation(View view) {
        coordinates = (TextView) findViewById(R.id.coordinates);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(LocationSubmission.this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, LOCATION_ACCESS);
            return;
        }
        //get location and set the text on screen as well as variables
        currLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        coordinates.setText(getString(R.string.latitude) + ": " + currLocation.getLatitude() + ", " + getString(R.string.longitude) + ": " + currLocation.getLongitude());
        latitude = currLocation.getLatitude();
        longitude = currLocation.getLongitude();
    }
    public void submitForm(View v){
        //If the description does not have at least 3 characters, error
        if(description.getText().length() <= 3){
            Toast.makeText(LocationSubmission.this, getString(R.string.descriptionError), Toast.LENGTH_SHORT);
            return;
        }
        //if there is not a location name, error
        if(locationName.getText().length() <= 1){
            Toast.makeText(LocationSubmission.this, getString(R.string.locationNameError), Toast.LENGTH_SHORT);
            return;
        }
        //create http client to get bucket link from zenodo
        AsyncHttpClient client = new AsyncHttpClient();
        //create content for request
        client.addHeader("Content-Type", "application/json");
        JSONObject jsonParams = new JSONObject();
        StringEntity entity = null;
        try {
            entity = new StringEntity(jsonParams.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String postUrl = "https://sandbox.zenodo.org/api/deposit/depositions?access_token=" + ACCESS_TOKEN;
        //carry out post request
        client.post(null, postUrl, entity, "application/json", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                //on resonse, pull out the bucket link and deposition id
                try {
                    bucketLink = response.getJSONObject("links").getString("bucket");
                    id = response.getInt("id");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                upload();
            }
            //if it errors with no JSON object then print status code
            @Override
            public void onFailure(int statusCode, Header[] headers, String string, Throwable throwable){
                System.out.println(statusCode);
            }
            //if it errors with json object print status code and message
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse){
                System.out.println("error " + errorResponse.names());
                try {
                    System.out.println(errorResponse.getString("status"));
                    System.out.println(errorResponse.getString("message"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    //upload a location
    public void upload(){

        String title = locationName.getText().toString();
        String name;
        //if the user put in a name, get it otherwise make the name anonymous
        if(userName.getText().toString().trim().length() != 0){
            name = userName.getText().toString();
        }
        else{
            name = getString(R.string.anonymousName);
        }
        String description1 = description.getText().toString();
        //start the python script
        if(!Python.isStarted()){
            Python.start(new AndroidPlatform(this));
        }
        Python instance = Python.getInstance();
        //zenodo requires a file upload, but all location data is coming from the metadata, therefore create a blank file to submit with it
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("soundsFile", Context.MODE_PRIVATE);
        File newFile = new File(directory.getAbsolutePath() + File.separator +  "blank.txt");
        try {
            newFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //run the python script
        PyObject currObj = instance.getModule("script");
        currObj.callAttr("sendway", bucketLink, title, description1, name, id.toString(), latitude, longitude, newFile.getAbsolutePath());
    }
}
