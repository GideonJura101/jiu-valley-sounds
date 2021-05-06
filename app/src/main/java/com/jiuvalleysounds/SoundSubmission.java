package com.jiuvalleysounds;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.loopj.android.http.*;
import org.json.*;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class SoundSubmission extends Activity implements LocationListener{
    Button backButton2;
    private static final int PICK_SOUND_REQUEST = 9544;
    Uri selectedSound;
    TextView soundPath;
    EditText userName;
    EditText description;
    TextView coordinates;
    LocationManager locationManager;
    Double latitude;
    Double longitude;
    Location currLocation;
    String ACCESS_TOKEN = "uXYu0HAaakk0Krt6JYsXKBCbPHYMMoFpw4EeEVztAspjE3V5uQENCFtU6llz";
    String bucketLink;
    String uriPath;
    Integer id;
    private static final int LOCATION_ACCESS = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve the content view that renders the map.
        setContentView(R.layout.sound_submission);
        //make the visual objects be variables
        soundPath = (TextView) findViewById(R.id.soundPath);
        userName = (EditText) findViewById(R.id.name);
        description = (EditText) findViewById(R.id.description);
        //Set an onclick for the back button
        backButton2 = (Button) findViewById(R.id.backButton2);
        backButton2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        //Location stuff that should probably be abstracted
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //check to see if the location access is on
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(SoundSubmission.this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, LOCATION_ACCESS);
            return;
        }
        //listen for location changes
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    public void getBucket(String filePath) {
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
                upload(filePath);
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

    public void upload(String filepath){
        File file = new File(filepath);
        //start python
        if(!Python.isStarted()){
            Python.start(new AndroidPlatform(this));
        }
        Python instance = Python.getInstance();
        PyObject currObj = instance.getModule("script");
        //set the submission title
        String title = "Lat: " + latitude + "Long: " + longitude;
        String name;
        //if the user put in a name, get it otherwise make the name anonymous
        if(userName.getText().toString().trim().length() != 0){
            name = userName.getText().toString();
        }
        else{
            name = getString(R.string.anonymousName);
        }
        //run python script
        currObj.callAttr("send", bucketLink, filepath, file.getName(), title, description.getText().toString(), name, id.toString());

    }

    public void pick(View view) {
        //ask for permissions required to select files
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(SoundSubmission.this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE }, PICK_SOUND_REQUEST);
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/*");
            startActivityForResult(Intent.createChooser(intent, "Open Files"), PICK_SOUND_REQUEST);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //only accept audio files
        intent.setType("audio/*");
        //start the selection activity
        startActivityForResult(Intent.createChooser(intent, "Open Files"), PICK_SOUND_REQUEST);

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_SOUND_REQUEST) {
            if (resultCode == RESULT_OK) {
                //get the uri to the selected sound
                selectedSound = data.getData();
                //get the path from the uri
                uriPath = UriUtils.getPathFromUri(this, selectedSound);

            }
        }
    }
    //do this if the location access or sound access is not on
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        //ask for permission to access location
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_ACCESS){
            //if not granted, tell them it needs permission
            if(grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(SoundSubmission.this, getString(R.string.locationPermission), Toast.LENGTH_SHORT);
            }
        }
        if(requestCode == PICK_SOUND_REQUEST){
            //if not granted, tell them it needs permission
            if(grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(SoundSubmission.this, getString(R.string.soundPermission), Toast.LENGTH_SHORT);
            }
        }
    }

    //Location stuff that should probably be abstracted
    @Override
    public void onLocationChanged(@NonNull Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
    }

    public void getLocation(View view) {
        coordinates = (TextView) findViewById(R.id.coordinates);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(SoundSubmission.this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, LOCATION_ACCESS);
            return;
        }
        //get location and set the text on screen as well as variables
        currLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        coordinates.setText("Latitude:" + currLocation.getLatitude() + ", Longitude:" + currLocation.getLongitude());
        latitude = currLocation.getLatitude();
        longitude = currLocation.getLongitude();
    }
    //function ran when the submit button is clicked
    public void submitForm(View v){
        //If no sound is selected, error
        if(uriPath == null){
            Toast.makeText(SoundSubmission.this, getString(R.string.soundError), Toast.LENGTH_SHORT).show();
            return;
        }
        //If the description does not have at least 3 characters, error
        if(description.getText().length() < 3){
            Toast.makeText(SoundSubmission.this, getString(R.string.descriptionError), Toast.LENGTH_SHORT).show();
            return;
        }
        getBucket(uriPath);
    }
}
