package com.jiuvalleysounds;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * An activity that displays a Google map with a marker (pin) to indicate a particular location.
 */
public class MapsMarkerActivity extends AppCompatActivity
        implements OnMapReadyCallback {
    Button menuButton;
    ImageButton helpButton;
    //this url sets the type to other, which should only be waypoints and returns the first 10000 entries (the maximum for one page)
    String url = "https://sandbox.zenodo.org/api/records/?type=other&communities=jiu-valley-sounds&size=10000";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_maps);

        //Set an onclick for the back button
        menuButton = (Button) findViewById(R.id.menuButton);
        menuButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent myIntent = new Intent(v.getContext(), Menu.class);
                startActivityForResult(myIntent, 0);
            }
        });
        //Set an onclick for the help button
        helpButton = (ImageButton) findViewById(R.id.helpButton);
        helpButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent myIntent = new Intent(v.getContext(), Help.class);
                startActivityForResult(myIntent, 0);
            }
        });

        // Get the SupportMapFragment and request notification when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map when it's available.
     * The API invokes this callback when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user receives a prompt to install
     * Play services inside the SupportMapFragment. The API invokes this method after the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        //start client for request
        AsyncHttpClient client = new AsyncHttpClient();
        //set content for request
        client.addHeader("Content-Type", "application/json");
        RequestParams params = new RequestParams();
        //start get request
        client.get(url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                //on success run addMarkers
                try {
                    addMarkers(response, googleMap);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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
        LatLng petrilaMine = new LatLng(45.438, 23.375);
        //These are default waypoints, that should be deleted when there are other waypoints. THese do not have dedicated screens associated with them
        googleMap.addMarker(new MarkerOptions()
                .position(petrilaMine)
                .title("Planeta Petrila Mine")
                .snippet("Petrila Mine"));
        LatLng petrosaniMiningMuseum = new LatLng(45.416684, 23.371461);
        googleMap.addMarker(new MarkerOptions()
                .position(petrosaniMiningMuseum)
                .title("Petrosani Mining Museum").snippet("Petrosani Mining Museum"));
        LatLng loneaMine = new LatLng(45.449744, 23.430998);
        googleMap.addMarker(new MarkerOptions()
                .position(loneaMine)
                .title("Lonea Mine").snippet("Lonea Mine"));
        //sets default zoom
        googleMap.moveCamera(CameraUpdateFactory.zoomTo(11));
        //This will center the camera on the petrilaMine LatLng object, so keep that object or set new one
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(petrilaMine));

    }
    public void addMarkers(JSONObject response, GoogleMap googleMap) throws JSONException {

        Integer numMarks = 0;
        //get the number of waypoint entries
        numMarks = response.getJSONObject("hits").getInt("total");
        //cycle through each waypoint and add a marker with its data
        for(int i = 0; i < numMarks; i++){
            googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(response.getJSONObject("hits").getJSONArray("hits").getJSONObject(i).getJSONObject("metadata").getJSONArray("locations").getJSONObject(0).getDouble("lat"),
                            response.getJSONObject("hits").getJSONArray("hits").getJSONObject(i).getJSONObject("metadata").getJSONArray("locations").getJSONObject(0).getDouble("lon")))
                    .title(response.getJSONObject("hits").getJSONArray("hits").getJSONObject(i).getJSONObject("metadata").getJSONArray("locations").getJSONObject(0).getString("place"))
                    .snippet(response.getJSONObject("hits").getJSONArray("hits").getJSONObject(i).getJSONObject("metadata").getString("description")));

        }
        //set an onclick for markers that opens the marker screen
        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Intent intentMain = new Intent(MapsMarkerActivity.this ,
                        MarkerActivity.class);
                //send over relevant data to the marker screen
                intentMain.putExtra("latitude", marker.getPosition().latitude);
                intentMain.putExtra("longitude", marker.getPosition().longitude);
                intentMain.putExtra("name", marker.getTitle());
                intentMain.putExtra("description", marker.getSnippet());
                MapsMarkerActivity.this.startActivity(intentMain);
            }
        });
    }

}