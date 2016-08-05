package com.example.grant.compass;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.text.DecimalFormat;

public class MainActivity
        extends AppCompatActivity
        implements SensorEventListener,
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener
{

    private static final String TAG = "MainActivity";
    private ImageView image;
    private float currDegree = 0;
    private SensorManager sensorManager;
    private TextView tvHeading;

    private Sensor sensorAccelerometer;
    private Sensor sensorMagneticField;

    private float[] accelReading = new float[3];
    private float[] magReading = new float[3];
    private float[] smoothed = new float[3];

    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];

    private float currBearing = 0f;


    private GoogleApiClient googleApiClient;
    private Location myLocation;
    private double myLatitude;
    private double myLongitude;






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "CREATE");

        setContentView(R.layout.activity_main);

        image = (ImageView) findViewById(R.id.imageViewCompass);
        tvHeading = (TextView) findViewById(R.id.tvHeading);

        // Orientation and sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMagneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);



        // Connect to google API
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "START");

        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "STOP");
        googleApiClient.disconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "RESUME");


        sensorManager.registerListener(
                this,
                sensorAccelerometer,
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(
                this,
                sensorMagneticField,
                SensorManager.SENSOR_DELAY_GAME);
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "PAUSE");
        sensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.v(TAG, "SENSOR CHANGED");
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            smoothed = lowPassFilter(event.values, accelReading);
            accelReading[0] = smoothed[0];
            accelReading[1] = smoothed[1];
            accelReading[2] = smoothed[2];
            Log.v(TAG, "SMOOTHED: " + smoothed[0]);
        }
        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            smoothed = lowPassFilter(event.values, magReading);
            magReading[0] = smoothed[0];
            magReading[1] = smoothed[1];
            magReading[2] = smoothed[2];
        }

        updateOrientationAngles();
    }

    private void updateOrientationAngles() {
        sensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                accelReading,
                magReading);
        sensorManager.getOrientation(
                rotationMatrix,
                orientationAngles);

        float azimuth = orientationAngles[0];
        float pitch = orientationAngles[1];
        float roll = orientationAngles[2];

        double bearing = Math.toDegrees(azimuth);
        // Add geomagnetic field to fix difference between true north and magnetic north
        if (bearing < 0) bearing += 360;

        DecimalFormat df = new DecimalFormat("#.0");
        tvHeading.setText("Heading: " + df.format(bearing));

        RotateAnimation ra = new RotateAnimation(
                currBearing,
                (float)-bearing,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
        );
        ra.setDuration(200);
        ra.setFillAfter(true);
        image.startAnimation(ra);

        currBearing = (float)-bearing;



    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.v(TAG, "ACCURACY CHANGED");
        // Not used
    }

    public float[] lowPassFilter(float[] newArr, float[] existingArr) {
        float alpha = .95f;
        for (int i = 0; i < existingArr.length; i++) {
            existingArr[i] = alpha * existingArr[i] + (1 - alpha) * newArr[i];
        }


        /*float smoothingFactor = 5;

        for (int i = 0; i < existingArr.length; i++) {
            existingArr[i] += ((newArr[i] - existingArr[i]) / smoothingFactor);
        }
        */
        return existingArr;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "ON CONNECTED");

        // Get my location.
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission denied");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.d(TAG, "In shouldShowRequest");
            } else {
                Log.d(TAG, "Else from shouldShow");
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }

        myLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (myLocation != null) {
            myLatitude = myLocation.getLatitude();
            myLongitude = myLocation.getLongitude();
        }

        Log.i(TAG, "LAT:" + String.valueOf(myLatitude));
        Log.i(TAG, "LONG: " + String.valueOf(myLongitude));


        // Create location to point to
        Location destination = new Location("");
        destination.setLatitude(50.0);
        destination.setLongitude(-90.0);

        double bearing = myLocation.bearingTo(destination);
        Log.d(TAG, String.valueOf(bearing));


    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "ON CONNECTION SUSPENDED");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "ON CONNECTION FAILED");
    }
}
