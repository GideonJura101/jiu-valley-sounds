package com.jiuvalleysounds;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class MarkerActivity extends AppCompatActivity {
    private static final int PICK_SOUND_REQUEST = 9544;
    Double latitude;
    Double longitude;
    String ACCESS_TOKEN = "";
    String bucketLink;
    Integer id;
    EditText userName;
    TextView description;
    TextView placeName;
    Intent intent;
    Button backButton4;
    String uriPath;
    Uri selectedSound;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve the content view that renders the map.
        setContentView(R.layout.marker);
        intent = getIntent();
        //get data sent over from map
        longitude = intent.getDoubleExtra("longitude", 0);
        latitude = intent.getDoubleExtra("latitude", 0);
        //make the visual objects be variables
        description = (TextView) findViewById(R.id.description);
        placeName = (TextView) findViewById(R.id.placeName);
        userName = (EditText) findViewById(R.id.name);
        description.setText(intent.getStringExtra("description"));
        placeName.setText(intent.getStringExtra("name"));
        //Set an onclick for the back button
        backButton4 = (Button) findViewById(R.id.backButton4);
        backButton4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });
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
        currObj.callAttr("send", bucketLink, filepath, file.getName(), title, "Submitted through Waypoint Form", name, id.toString());
    }

    public void pick(View view) {
        //ask for permissions required to select files
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MarkerActivity.this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE }, PICK_SOUND_REQUEST);
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
    //do this if the sound access is not on
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        //ask for permission to access location
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PICK_SOUND_REQUEST){
            //if not granted, tell them it needs permission
            if(grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(MarkerActivity.this, getString(R.string.soundPermission), Toast.LENGTH_SHORT);
            }
        }
    }
    // Method to get the absolute path of the selected sound from its URI
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
    public void submitForm(View v){
        //If no sound is selected, error
        if(uriPath == null){
            Toast.makeText(MarkerActivity.this, getString(R.string.soundError), Toast.LENGTH_SHORT).show();
            return;
        }
        getBucket(uriPath);

    }
}
