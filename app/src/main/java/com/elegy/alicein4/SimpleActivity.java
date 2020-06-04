package com.elegy.alicein4;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.snackbar.Snackbar;

import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IAOrientationListener;
import com.indooratlas.android.sdk.IAOrientationRequest;
import com.indooratlas.android.sdk.IARegion;
import com.indooratlas.android.sdk.IARoute;
import com.indooratlas.android.sdk.IAWayfindingListener;
import com.indooratlas.android.sdk.IAWayfindingRequest;
import com.indooratlas.android.sdk.resources.IAFloorPlan;
import com.indooratlas.android.sdk.resources.IALatLng;
import com.indooratlas.android.sdk.resources.IALocationListenerSupport;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class SimpleActivity extends FragmentActivity implements GoogleMap.OnMapClickListener, OnMapReadyCallback
{
    // AR fragment part
    // The minimum required version of OpenGL
    private static final double MIN_OPENGL_VERSION = 3.0;

    float[] accValues = new float[3];
    float[] magValues = new float[3];
    float northDegree;
    private AutoArFragment arFragment;
    private SensorManager sensorManager;
    private Sensor aSensor;
    private Sensor mSensor;
    private SensorEventListener sensorEventListener = new SensorEventListener()
    {
        @Override
        public void onSensorChanged(SensorEvent event)
        {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                accValues = event.values;
            else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                magValues = event.values;
            northDegree = calculateOrientation();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}

        private float calculateOrientation()
        {
            float[] values = new float[3];
            float[] R = new float[9];
            SensorManager.getRotationMatrix(R, null, accValues, magValues);
            SensorManager.getOrientation(R, values);
            float degree = (float) Math.toDegrees(values[0]);
            return degree;
        }
    };

    public float getNorthDegree()
    {
        return  northDegree;
    }

    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 42;
    private static final int MAX_DIMENSION = 2048;      // The threshold which used to decide when bitmap should be downscaled
    private static final String TAG = "IndoorMapFragment";

    private GoogleMap mMap;
    private Circle mCircle;     // The Circle which represent the area contains the user's position
    private GroundOverlay mGroundOverlay = null;    // The ground overlay (floor plane graph) cover the google map
    private boolean mCameraPositionNeedsUpdating = true; // The camera position for map, update on first location
    private Marker mDestinationMarker;      // Mark for the destination
    private Marker mHeadingMarker;          // Mark for the user's position
    private List<Polyline> mPolylines = new ArrayList<>();  // Line marks on the Google map

    private Target mLoadTarget;     // Picasso target for drawing the overlay

    private double currentDirect = 0;
    private int mFloor;         // The floor ID in IndoorAtlas
    private IALocationManager mIALocationManager;
    private IARegion mOverlayFloorPlan = null;
    private IARoute mCurrentRoute;
    private IAWayfindingRequest mWayfindingDestination;
    private IAWayfindingListener mWayfindingListener = new IAWayfindingListener()
    {
        @Override
        public void onWayfindingUpdate(IARoute route)
        {
            mCurrentRoute = route;
            if (hasArrivedToDestination(route))
            {
                // stop wayfinding
                showInfo("You're there!");
                mCurrentRoute = null;
                mWayfindingDestination = null;
                mIALocationManager.removeWayfindingUpdates();
            }
            updateRouteVisualization();     // Draw the route on the Google Map

            //currentDirect = mCurrentRoute.getLegs().get(0).getDirection();   // Get the current route direction.
            //double length = mCurrentRoute.getLegs().get(0).getLength();
            //Toast.makeText(SimpleActivity.this, message, Toast.LENGTH_LONG).show();
        }
    };

    private IAOrientationListener mOrientationListener = new IAOrientationListener()
    {
        @Override
        public void onHeadingChanged(long timestamp, double heading)
        {
            updateHeading(heading);
        }

        @Override
        public void onOrientationChange(long timestamp, double[] quaternion) {}
    };

    // Listener that handles location change events.
    private IALocationListener mListener = new IALocationListenerSupport()
    {
        // Location changed, move marker and camera position.
        @Override
        public void onLocationChanged(IALocation location)
        {
            Log.d(TAG, "new location received with coordinates: " + location.getLatitude() + "," + location.getLongitude());
            final LatLng center = new LatLng(location.getLatitude(), location.getLongitude());      // User's position
            final int newFloor = location.getFloorLevel();
            // location received before map is initialized, ignoring update here
            if (mMap == null) { return; }
            if (mFloor != newFloor) { updateRouteVisualization(); }
            mFloor = newFloor;
            showLocationCircle(center, location.getAccuracy());
            if (mCameraPositionNeedsUpdating)       // Camera position needs updating if location has significantly changed
            {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 17.5f));
                mCameraPositionNeedsUpdating = false;
            }
        }
    };

    // Listener that changes overlay if needed
    private IARegion.Listener mRegionListener = new IARegion.Listener()
    {
        @Override
        public void onEnterRegion(IARegion region)
        {
            if (region.getType() == IARegion.TYPE_FLOOR_PLAN)
            {
                Log.d(TAG, "enter floor plan " + region.getId());
                mCameraPositionNeedsUpdating = true; // Entering new fp, need to move camera
                if (mGroundOverlay != null)
                {
                    mGroundOverlay.remove();        // Remove the old overlay
                    mGroundOverlay = null;
                }
                mOverlayFloorPlan = region;         // overlay will be this (unless error in loading)
                fetchFloorPlanBitmap(region.getFloorPlan());
            }
        }

        @Override
        public void onExitRegion(IARegion region) { }
    };

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;
        mMap.setMyLocationEnabled(false);       // do not show Google's outdoor location
        mMap.setOnMapClickListener(this);
    }

    // Add the circle on th Google Map
    private void showLocationCircle(LatLng center, double accuracyRadius)
    {
        if (mCircle == null)
        {
            // location can received before map is initialized, ignoring those updates
            if (mMap != null)
            {
                // Add the circle which represents the possibility area
                mCircle = mMap.addCircle(new CircleOptions()
                        .center(center)
                        .radius(accuracyRadius)
                        .fillColor(0x201681FB)
                        .strokeColor(0x500A78DD)
                        .zIndex(1.0f)
                        .visible(true)
                        .strokeWidth(5.0f));

                mHeadingMarker = mMap.addMarker(new MarkerOptions()
                        .position(center)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_blue_dot))
                        .anchor(0.5f, 0.5f)
                        .flat(true));
            }
        }
        else
        {   // Move existing markers position to received location
            mCircle.setCenter(center);
            mHeadingMarker.setPosition(center);
            mCircle.setRadius(accuracyRadius);
        }
    }

    // Change the heading of the mark of user position based on the user's phone heading
    private void updateHeading(double heading)
    {
        if (mHeadingMarker != null)
        {
            mHeadingMarker.setRotation((float)heading);
        }
    }

    public double getCurrentDirect()
    {
        return currentDirect;
    }

    // Download floor plan using Picasso library.
    private void fetchFloorPlanBitmap(final IAFloorPlan floorPlan)
    {
        final String url = floorPlan.getUrl();
        if (floorPlan == null)
        {
            Log.e(TAG, "null floor plan in fetchFloorPlanBitmap");
            return;
        }
        Log.d(TAG, "loading floor plan bitmap from "+url);
        mLoadTarget = new Target()
        {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from)
            {
                Log.d(TAG, "onBitmap loaded with dimensions: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                if (mOverlayFloorPlan != null && floorPlan.getId().equals(mOverlayFloorPlan.getId()))
                {
                    Log.d(TAG, "showing overlay");
                    setupGroundOverlay(floorPlan, bitmap);
                }
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {}

            @Override
            public void onBitmapFailed(Drawable placeHolderDrawable)
            {
                showInfo("Failed to load bitmap");
                mOverlayFloorPlan = null;
            }
        };

        RequestCreator request = Picasso.with(this).load(url);
        final int bitmapWidth = floorPlan.getBitmapWidth();
        final int bitmapHeight = floorPlan.getBitmapHeight();

        if (bitmapHeight > MAX_DIMENSION)
        {
            request.resize(0, MAX_DIMENSION);
        }
        else if (bitmapWidth > MAX_DIMENSION)
        {
            request.resize(MAX_DIMENSION, 0);
        }
        request.into(mLoadTarget);
    }

    // Sets bitmap of floor plan as ground overlay on Google Maps
    private void setupGroundOverlay(IAFloorPlan floorPlan, Bitmap bitmap)
    {
        if (mGroundOverlay != null)
        {
            mGroundOverlay.remove();
        }
        if (mMap != null)
        {
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
            IALatLng iaLatLng = floorPlan.getCenter();
            LatLng center = new LatLng(iaLatLng.latitude, iaLatLng.longitude);
            GroundOverlayOptions fpOverlay = new GroundOverlayOptions()
                    .image(bitmapDescriptor)
                    .zIndex(0.0f)
                    .position(center, floorPlan.getWidthMeters(), floorPlan.getHeightMeters())
                    .bearing(floorPlan.getBearing());

            mGroundOverlay = mMap.addGroundOverlay(fpOverlay);
        }
    }

    @Override
    public void onMapClick(LatLng point)
    {
        if (mMap != null)
        {
            mWayfindingDestination = new IAWayfindingRequest.Builder()      //The position clicked is the destination
                    .withFloor(mFloor)
                    .withLatitude(point.latitude)
                    .withLongitude(point.longitude)
                    .build();

            mIALocationManager.requestWayfindingUpdates(mWayfindingDestination, mWayfindingListener);

            if (mDestinationMarker == null)
            {
                mDestinationMarker = mMap.addMarker(new MarkerOptions()
                        .position(point)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            }
            else
            {
                mDestinationMarker.setPosition(point);
            }
            Log.d(TAG, "Set destination: (" + mWayfindingDestination.getLatitude() + ", " + mWayfindingDestination.getLongitude() + "), floor=" + mWayfindingDestination.getFloor());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple);
        if (!checkIsSupportedDeviceOrFinish(this))
        {
            return;
        }
        // prevent the screen going to sleep while app is on foreground
        findViewById(android.R.id.content).setKeepScreenOn(true);
        mIALocationManager = IALocationManager.create(this);    // instantiate IALocationManager
        mIALocationManager.lockIndoors(true);       // disable indoor-outdoor detection (assume we're indoors)

        // Try to obtain the google map from the SupportMapFragment.
        ((SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map))
                .getMapAsync(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        aSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(sensorEventListener, aSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(sensorEventListener, mSensor, SensorManager.SENSOR_DELAY_GAME);

        arFragment = (AutoArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        arFragment.setActivity(this);

    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mIALocationManager.destroy();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        // start receiving location updates & monitor region changes
        mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mListener);
        mIALocationManager.registerRegionListener(mRegionListener);
        mIALocationManager.registerOrientationListener(new IAOrientationRequest(1, 0), mOrientationListener);  // update if heading changes by 1 degrees or more
        if (mWayfindingDestination != null)
        {
            mIALocationManager.requestWayfindingUpdates(mWayfindingDestination, mWayfindingListener);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        sensorManager.unregisterListener(sensorEventListener);
        // unregister location & region changes
        mIALocationManager.removeLocationUpdates(mListener);
        mIALocationManager.unregisterRegionListener(mRegionListener);
        mIALocationManager.unregisterOrientationListener(mOrientationListener);
        if (mWayfindingDestination != null)
        {
            mIALocationManager.removeWayfindingUpdates();
        }
    }

    private void showInfo(String text)
    {
        final Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), text, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.button_close, new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                snackbar.dismiss();
            }
        });
        snackbar.show();
    }

    private boolean hasArrivedToDestination(IARoute route)
    {   // empty routes are only returned when there is a problem, for example, missing or disconnected routing graph
        if (route.getLegs().size() == 0)
        {
            return false;
        }
        final double FINISH_THRESHOLD_METERS = 3.0;     // The distance threshold for the terminal and position right now.
        double routeLength = 0;
        for (IARoute.Leg leg : route.getLegs()) routeLength += leg.getLength();     // Sum up all route in the list so that we can get the total routeLength.
        return routeLength < FINISH_THRESHOLD_METERS;                       //  If the routeLength shorter than 3.0
    }

    // Visualize the IndoorAtlas Wayfinding route on top of the Google Maps.
    private void updateRouteVisualization()
    {
        clearRouteVisualization();
        if (mCurrentRoute == null)
        {
            return;
        }

        int count = 0;
        for (IARoute.Leg leg : mCurrentRoute.getLegs())
        {
            if (leg.getEdgeIndex() == null)
            {
                // Legs without an edge index are, in practice, the last and first legs of the
                // route. They connect the destination or current location to the routing graph.
                // All other legs travel along the edges of the routing graph.

                // Omitting these "artificial edges" in visualization can improve the aesthetics
                // of the route. Alternatively, they could be visualized with dashed lines.
                continue;
            }

            PolylineOptions opt = new PolylineOptions();    // Draw the line from the start to the end
            opt.add(new LatLng(leg.getBegin().getLatitude(), leg.getBegin().getLongitude()));
            opt.add(new LatLng(leg.getEnd().getLatitude(), leg.getEnd().getLongitude()));

            // Here wayfinding path in different floor than current location is visualized in a semi-transparent color
            if (leg.getBegin().getFloor() == mFloor && leg.getEnd().getFloor() == mFloor)
            {
                opt.color(0xFF0000FF);
                if (count == 0)
                {
                    opt.color(0xffff0000);
                    currentDirect = leg.getDirection();
                    Toast.makeText(SimpleActivity.this, String.valueOf((int) currentDirect) + "degrees", Toast.LENGTH_SHORT).show();
                }
                count += 1;
            }
            else
            {
                opt.color(0x300000FF);
            }
            mPolylines.add(mMap.addPolyline(opt));
        }
    }

    // Clear the visualizations for the wayfinding paths
    private void clearRouteVisualization()
    {
        for (Polyline pl : mPolylines)
        {
            pl.remove();
        }
        mPolylines.clear();
    }

    public double getDegree(double latSource, double lngSource, double latDestination, double lngDestination)
    {
        double lat1 = latSource / 180 * Math.PI;
        double lng1 = lngSource / 180 * Math.PI;
        double lat2 = latDestination / 180 * Math.PI;
        double lng2 = lngDestination / 180 * Math.PI;

        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lng2 - lng1);
        double y = Math.sin(lng2 - lng1) * Math.cos(lat2);

        double tan2 = Math.atan2(y, x);
        double degree = tan2 * 180 / Math.PI;
        if (degree < 0)
            return  degree + 360;
        return degree;
    }

    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity)
    {
        if (ActivityCompat.checkSelfPermission(activity, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_ACCESS_FINE_LOCATION);
            Toast.makeText(activity, "Google Map requires access fine location", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
        {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString = ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                .getDeviceConfigurationInfo()
                .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION)
        {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        return true;
    }
}
