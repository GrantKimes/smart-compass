package com.example.grant.compass;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
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
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DecimalFormat;

public class MainActivity
        extends AppCompatActivity
        implements SensorEventListener,
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener,
            LocationListener
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

    private float bearingMagNorth = 0f;
    private float bearingTrueNorth = 0f;
    private float bearingFromNorthToDestination = 0f;
    private float bearingToDestination = 0f;
    private float distanceToDestination = 0f;


    private GoogleApiClient googleApiClient;
    private Location myLocation;
    private Location destination;
    private double myLatitude;
    private double myLongitude;
    private double myAltitude;
    private long myTime;
    private LocationRequest myLocationRequest;






    @Override
    protected void onCreate(Bundle savedInstanceState) { // Implement onSaveInstanceState()
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

        // Create location request
        myLocationRequest = new LocationRequest();
        myLocationRequest.setInterval(2000).setFastestInterval(1000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Create location to point to
        destination = new Location("");
        destination.setLongitude(-88.146155);
        destination.setLatitude(42.144821);

        // Set button listener for screen switch
        Button nextScreenButton = (Button) findViewById(R.id.switch_view_button);
        nextScreenButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Log.d(TAG, "CLICKED BUTTON");
                Intent nextScreen = new Intent(getApplicationContext(), MapActivity.class);
                startActivity(nextScreen);
            }
        });

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

        // Orientation listeners
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

        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
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
        }
        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            smoothed = lowPassFilter(event.values, magReading);
            magReading[0] = smoothed[0];
            magReading[1] = smoothed[1];
            magReading[2] = smoothed[2];
        }

        if (myLocation != null) {
            updateOrientationAngles();
        }
    }

    private void updateOrientationAngles() {
        // Get sensor data
        sensorManager.getRotationMatrix(rotationMatrix, null, accelReading, magReading);
        sensorManager.getOrientation(rotationMatrix, orientationAngles);
        float azimuth = (float)Math.toDegrees(orientationAngles[0]);
        float pitch = (float)Math.toDegrees(orientationAngles[1]);
        float roll = (float)Math.toDegrees(orientationAngles[2]);

        // Calculate bearing to north based on updated sensor info
        bearingMagNorth = azimuth;

        // Add geomagnetic field to fix difference between true north and magnetic north
        GeomagneticField geoField = new GeomagneticField((float)myLatitude, (float)myLongitude, (float)myAltitude, myTime);
        bearingTrueNorth = azimuth + geoField.getDeclination();
        if (bearingTrueNorth < 0) bearingTrueNorth += 360;
        if (bearingMagNorth < 0) bearingMagNorth += 360;



        // Calculate bearing to destination based on updated bearing to north
        float newBearingToDestination = bearingTrueNorth - bearingFromNorthToDestination;
        if (newBearingToDestination < 0 ) newBearingToDestination += 360;

        // Output text to screen
        DecimalFormat df = new DecimalFormat("0");
        String text = "Magnetic north: " + df.format(bearingMagNorth) + "\n"
            + "True north: " + df.format(bearingTrueNorth) + "\n"
            + "Geo declination: " + df.format(geoField.getDeclination()) + "\n"
            + "Degrees from north to destination: " + df.format(bearingFromNorthToDestination) + "\n"
            + "Degrees to destination: " + df.format(newBearingToDestination) + "\n"
            + "Distance to destination: " + df.format(distanceToDestination) + " meters";
        tvHeading.setText(text);

        // Try matrix.postRotate()
        // Rotate image
        RotateAnimation ra = new RotateAnimation(
                -1 * bearingToDestination,  // *-1 because need to rotate image opposite direction the phone is turning
                -1 * newBearingToDestination,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        ra.setDuration(25);
        ra.setFillAfter(true);
        image.startAnimation(ra);

        // Save new bearing as current bearing
        bearingToDestination = newBearingToDestination;

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

    private void checkAppPermission(String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission denied");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                Log.d(TAG, "In shouldShowRequest");
            } else {
                Log.d(TAG, "Else from shouldShow");
                ActivityCompat.requestPermissions(this, new String[] {permission}, 1);
            }
        }

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "ON CONNECTED");

        // Get location permissions.
        checkAppPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, myLocationRequest, this);

        // Get location.
        myLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (myLocation != null) {
            myLatitude = myLocation.getLatitude();
            myLongitude = myLocation.getLongitude();
        }

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "ON CONNECTION SUSPENDED");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "ON CONNECTION FAILED");
    }

    @Override
    public void onLocationChanged(Location newLocation) {
        //Log.i(TAG, "LOCATION CHANGED");
        myLocation = newLocation;
        myLongitude = myLocation.getLongitude();
        myLatitude = myLocation.getLatitude();
        myAltitude = myLocation.getAltitude();
        myTime = myLocation.getTime();


        bearingFromNorthToDestination = myLocation.bearingTo(destination);
        distanceToDestination = myLocation.distanceTo(destination);

        showLocation();
    }

    private void showLocation() {
        //Log.d(TAG, "(LONG, LAT):" + String.valueOf(myLongitude) + ", " + String.valueOf(myLatitude) + ")");

        //double bearing = myLocation.bearingTo(destination);
        //Log.d(TAG, "bearing to destination: " + String.valueOf(bearing));
    }
}
