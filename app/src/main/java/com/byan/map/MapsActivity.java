package com.byan.map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity {

    private GoogleMap gMap; // Might be null if Google Play services APK is not available.

    private static final LatLng ITB = new LatLng(-6.89148, 107.6106591);
    private static final LatLng UNISBA = new LatLng(-6.9034436, 107.6083965);
    private static final LatLng BEC = new LatLng(-6.9080755,107.6077688);

    protected LocationManager locationManager;
    private Location myLocation;
    LatLng myLatLng;

    final String TAG = "PathGoogleMapActivity";

    private void drawDestionation(LatLng fromDst, LatLng toDst){
        String url = getMapsApiDirectionsUrl(fromDst, toDst);
        requestUrl(url);
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fromDst,
                11));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        final Spinner spTo = (Spinner) findViewById(R.id.selectTo);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.cities, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTo.setAdapter(adapter);

        Button buttonGo = (Button) findViewById(R.id.goBtn);
        buttonGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gMap.clear();
                String spinnerVal = spTo.getSelectedItem().toString();
                switch (spinnerVal){
                    case "ITB" :
                        drawDestionation(myLatLng, ITB);
                        addMarkers(spinnerVal,myLatLng,ITB);
                        break;
                    case "UNISBA" :
                        drawDestionation(myLatLng, UNISBA);
                        addMarkers(spinnerVal, myLatLng, UNISBA);
                        break;
                    case "BEC" :
                        drawDestionation(myLatLng, BEC);
                        addMarkers(spinnerVal,myLatLng,BEC);
                        break;
                }
            }
        });

         /* Get Location using Network GPS */
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                myLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        }

        myLatLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
        SupportMapFragment fm = (SupportMapFragment)getSupportFragmentManager()
                .findFragmentById(R.id.map);

        gMap = fm.getMap();
        gMap.addMarker(new MarkerOptions().position(myLatLng).title("My Location"));
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng,
                10));
    }

    private void requestUrl(String url){
        // Inflate the layout for this fragment
        Response.Listener<JSONObject> successListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                // Check to make sure that the activity hasn't been destroyed while the call was in flight.
                if (!isFinishing()) {
                    System.out.println("data json : " + data.toString());
                    new ParserTask().execute(data.toString());
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("error stream registry");
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(getApplication());
        Map<String, String> params = new HashMap<String, String>();
        GetRequest requestRS = new GetRequest(Request.Method.GET, url , params, successListener, errorListener);
        requestQueue.add(requestRS);
    }

    private void addMarkers(String toDstTitle, LatLng fromDst, LatLng toDst) {
        if (gMap != null) {
            gMap.addMarker(new MarkerOptions().position(fromDst)
                    .title("My Location"));
            gMap.addMarker(new MarkerOptions().position(toDst)
                    .title(toDstTitle));
        }
    }

    private String getMapsApiDirectionsUrl(LatLng fromDestination, LatLng toDestination) {
        String waypoints = "waypoints=optimize:true" +
                "|" + fromDestination.latitude + "," + fromDestination.longitude + "|" +
                "|" + toDestination.latitude + "," + toDestination.longitude + "|";

        String sensor = "sensor=false";

        String origin = "origin=" + fromDestination.latitude + "," + fromDestination.longitude;
        String destination = "destination=" + toDestination.latitude + "," + toDestination.longitude;
        String params = origin + "&" + destination + "&%20" + waypoints + "&" + sensor;

        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/"
                + output + "?" + params;
        return url;
    }

    private class ParserTask extends
            AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(
                String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                PathJSONParser parser = new PathJSONParser();
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> routes) {
            ArrayList<LatLng> points = null;
            PolylineOptions polyLineOptions = null;

            // traversing through routes
            for (int i = 0; i < routes.size(); i++) {
                points = new ArrayList<LatLng>();
                polyLineOptions = new PolylineOptions();
                List<HashMap<String, String>> path = routes.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                polyLineOptions.addAll(points);
                polyLineOptions.width(5);
                polyLineOptions.color(Color.BLUE);
            }

            gMap.addPolyline(polyLineOptions);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #gMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (gMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            gMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (gMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #gMap} is not null.
     */
    private void setUpMap() {
        gMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }
}
