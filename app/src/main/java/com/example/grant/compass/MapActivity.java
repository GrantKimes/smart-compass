package com.example.grant.compass;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by grant on 8/8/16.
 */
public class MapActivity
        extends AppCompatActivity
        implements OnMapReadyCallback, GoogleMap.OnMapClickListener
{

    private static final String TAG = "MapActivity";
    private GoogleMap map;

    private double latitude;
    private double longitude;

    private Marker destination;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.google_maps);

        Intent i = getIntent();
        latitude = i.getDoubleExtra("Latitude", 0);
        longitude = i.getDoubleExtra("Longitude", 0);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);

        findViewById(R.id.startNavigationButton).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Log.i(TAG, "CLICKED NAVIGATE");
                Bundle data = new Bundle();

                if (destination != null) {
                    data.putSerializable("Latitude", destination.getPosition().latitude);
                    data.putSerializable("Longitude", destination.getPosition().longitude);
                    data.putSerializable("DESTINATION_SELECTED", true);
                } else {
                    data.putSerializable("DESTINATION_SELECTED", false);
                }
                Intent intent = new Intent();
                intent.putExtras(data);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap newMap) {
        Log.i(TAG, "ON MAP READY");

        map = newMap;

        float zoomLevel = 16.0f;
        LatLng point = new LatLng(latitude, longitude);


        map.moveCamera(CameraUpdateFactory.newLatLngZoom(point, zoomLevel));
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        }
        else {
            Log.w(TAG, "Location access not enabled");
        }
        map.setOnMapClickListener(this);

    }

    @Override
    public void onMapClick(LatLng clicked) {
        Log.i(TAG, clicked.toString());

        if (destination != null) destination.remove();
        destination = map.addMarker(new MarkerOptions()
                .position(clicked)
                .title("Destination"));
        destination.showInfoWindow();
    }
}
