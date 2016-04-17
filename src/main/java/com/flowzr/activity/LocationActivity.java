/*******************************************************************************
 /*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Emmanuel Florent - initial port to Android Map V3 API
 ******************************************************************************/
package com.flowzr.activity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.flowzr.R;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.MyEntityManager;
import com.flowzr.model.MyLocation;
import com.flowzr.utils.MyPreferences;
import com.flowzr.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationActivity extends AppCompatActivity implements
        LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String LOCATION_ID_EXTRA = "locationId";
    private MyEntityManager em;
    private MyLocation orbLocation = new MyLocation();
    private GoogleMap mMap;
    private Marker mMarker;
    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private LocationRequest mLocationRequest;
    boolean mUpdatesRequested = true;
    private GoogleApiClient mGoogleApiClient;

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void initToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location);
        setUpMapIfNeeded();
        buildGoogleApiClient();
        DatabaseAdapter db = new DatabaseAdapter(this);
        db.open();
        em = db.em();
        TextView txtViewLocation = (TextView) findViewById(R.id.location);
        // These settings are the same as the settings for the map. They will in fact give you updates at
        // the maximal rates currently possible.
        mLocationRequest = LocationRequest.create()
                .setInterval(5000)         // 5 seconds
                .setFastestInterval(16)    // 16ms = 60fps
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //setup basic androids widgets
        Intent intent = getIntent();
        if (intent != null) {
            long locationId = intent.getLongExtra(LOCATION_ID_EXTRA, -1);
            if (locationId != -1) {
                orbLocation = em.load(MyLocation.class, locationId);
                EditText name = (EditText)findViewById(R.id.name);
                name.setText(orbLocation.name);
                if (orbLocation.resolvedAddress != null) {
                    txtViewLocation.setText(orbLocation.resolvedAddress);
                }
            }
        }
        Button bOK = (Button)findViewById(R.id.okButton);
        bOK.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                if (orbLocation!=null && mMarker!=null) {
                    EditText name = (EditText)findViewById(R.id.name);
                    orbLocation.latitude=mMarker.getPosition().latitude;
                    orbLocation.longitude=mMarker.getPosition().longitude;
                    if (Utils.checkEditText(name, "name", true, 100)) {
                        new SolveSaveLocationTask(getApplicationContext(),true).execute(orbLocation);
                    }
                }
            }
        });
    }

    /*
     * Called when the Activity is restarted, even before it becomes visible.
     */
    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Now set the map
        setUpMapIfNeeded();
        if (orbLocation.id == -1) {
            if (MyPreferences.isUseMyLocation(this)) {
                mUpdatesRequested=true;
            }
        } else {
            mMarker=mMap.addMarker(new MarkerOptions().position(new LatLng(orbLocation.latitude, orbLocation.longitude)).title(orbLocation.name).draggable(true));
            LatLng latLng = new LatLng(orbLocation.latitude, orbLocation.longitude);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng,15);
            mMap.animateCamera(cameraUpdate);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            stopPeriodicUpdates();
        }
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapview))
                    .getMap();
        }
    }

    private class SolveSaveLocationTask extends AsyncTask<MyLocation, Integer, String> {

        Context mContext;
        boolean doSave=true;

        public SolveSaveLocationTask(Context context,boolean pDoSave){
            super();
            mContext = context;
            doSave=pDoSave;
        }

        // Finding address using reverse geocoding
        @Override
        protected String doInBackground(MyLocation... params) {
            Geocoder geocoder = new Geocoder(mContext,Locale.getDefault());
            double latitude = params[0].latitude;
            double longitude = params[0].longitude;
            List<Address> addresses;
            String addressText="";
            try {
                addresses = geocoder.getFromLocation(latitude, longitude,1);
                Thread.sleep(500);
                if(addresses != null && addresses.size() > 0 ){
                    Address address = addresses.get(0);
                    String line0=address.getAddressLine(0);
                    if (line0!=null) {
                        if (line0.equals("null")) {
                            line0 = "";
                        }
                    }
                    String strLocality=address.getLocality();
                    if (strLocality!=null) {
                        if (strLocality.equals("null")) {
                            strLocality = "";
                        }
                    } else {
                        strLocality="";
                    }

                    String strCountry=address.getCountryName();
                    if (strCountry!=null) {
                        if (strCountry.equals("null")) {
                            strCountry = "";
                        }
                    } else {
                        strCountry="";
                    }

                    addressText = String.format("%s, %s, %s",
                            address.getMaxAddressLineIndex() > 0 ? line0 : "",
                            strLocality,
                            strCountry);
                }
            }
            catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            publishProgress(R.string.resolving_address);
            orbLocation.resolvedAddress = addressText;
            return addressText;
        }

        @Override
        protected void onPostExecute(String address) {
            ((TextView)findViewById(R.id.location)).setText(address);
            if (doSave) {
                EditText t = (EditText)findViewById(R.id.name);
                if (Utils.checkEditText(t, "name", true, 100)) {
                    //store and save object
                    orbLocation.name=Utils.text(t);
                    orbLocation.latitude = mMarker.getPosition().latitude;
                    orbLocation.longitude = mMarker.getPosition().longitude;
                    orbLocation.isPayee = true;
                    orbLocation.dateTime = System.currentTimeMillis();
                    long id = em.saveLocation(orbLocation);
                    //then return to location list
                    Intent data = new Intent();
                    data.putExtra(LOCATION_ID_EXTRA, id);
                    setResult(RESULT_OK, data);
                    finish();
                }
            }
        }

        @Override
        protected void onPreExecute() {
            if (doSave) {
                findViewById(R.id.name).setEnabled(false);
                findViewById(R.id.okButton).setEnabled(false);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            Toast toast = Toast.makeText(LocationActivity.this, values[0], Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
		  /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */
            } catch (IntentSender.SendIntentException e) {
                Toast.makeText(LocationActivity.this, R.string.gdocs_service_error, Toast.LENGTH_SHORT).show();
                // Log the error
                e.printStackTrace();
            }
        } else {
            // If no resolution is available, display a dialog to the user with the error.
            Toast.makeText(LocationActivity.this, connectionResult.toString(), Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onConnected(Bundle arg0) {
        if (mUpdatesRequested) {
            startPeriodicUpdates();
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    /**
     * Report location updates to the UI.
     *
     * @param location The updated location.
     */
    @Override
    public void onLocationChanged(Location location) {
        if (orbLocation.id == -1) {
            if (MyPreferences.isUseMyLocation(this)) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng,15);
                mMap.animateCamera(cameraUpdate);
                orbLocation.latitude=latLng.latitude;
                orbLocation.longitude=latLng.longitude;
                mMarker=mMap.addMarker(new MarkerOptions().position(latLng).draggable(true));
                mMarker.setPosition(latLng);


                new SolveSaveLocationTask(getBaseContext(), false).execute(orbLocation);
            }
            mUpdatesRequested=false;
            stopPeriodicUpdates();
        }

    }

    /**
     * In response to a request to start updates, send a request
     * to Location Services
     */
    private void startPeriodicUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);

    }

    /**
     * In response to a request to stop updates, send a request to
     * Location Services
     */
    private void stopPeriodicUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);

    }

}