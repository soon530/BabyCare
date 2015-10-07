package tw.tasker.babysitter.utils;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.parse.ParseGeoPoint;

import static tw.tasker.babysitter.utils.LogUtils.LOGD;
import static tw.tasker.babysitter.utils.LogUtils.makeLogTag;

public class MyLocation implements
        OnConnectionFailedListener, ConnectionCallbacks {
    static final float METERS_PER_FEET = 0.3048f;
    static final int METERS_PER_KILOMETER = 1000;
    static final int RADIUS = 250;
    private static final String TAG = makeLogTag(MyLocation.class);
    // Initial offset for calculating the map bounds
    private static final double OFFSET_CALCULATION_INIT_DIFF = 1.0;
    // Accuracy for calculating the map bounds
    private static final float OFFSET_CALCULATION_ACCURACY = 0.01f;
    private GoogleApiClient mLocationClient;
    private Location mCurrentLocation;
    private Context mContext;
    private LatLngBounds mBounds;

    private GetLocation mGetLocation;

    public MyLocation(Context mapActivity, GetLocation getLocation) {
        mContext = mapActivity;
        mGetLocation = getLocation;

        //mLocationClient = new LocationClient(mapActivity, this, this);

        mLocationClient = new GoogleApiClient.Builder(mapActivity)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        connect();
    }

    public LatLngBounds getmBounds() {
        return mBounds;
    }

    public Location getCurLocation() {
        return mCurrentLocation;
    }

    public void connect() {
        mLocationClient.connect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        mCurrentLocation = getLocation();

        if (mCurrentLocation == null) {
            mCurrentLocation = new Location("taiwan");
            mCurrentLocation.setLatitude(22.885127);
            mCurrentLocation.setLongitude(120.589881);
        }

        if (mGetLocation != null) {
            double latitude = mCurrentLocation.getLatitude();
            double longitude = mCurrentLocation.getLongitude();
            ParseGeoPoint parseGeoPoint = new ParseGeoPoint(latitude, longitude);
            mGetLocation.done(parseGeoPoint);
        }

        //updateZoom();
    }

    private Location getLocation() {
        // If Google Play Services is available
        if (servicesConnected()) {
            // Get the current location
            Log.i("vic", "getLocation()");

            Location loc = LocationServices.FusedLocationApi.getLastLocation(mLocationClient);

            return loc;
        } else {
            return null;
        }
    }

    private void updateZoom() {
        if (mCurrentLocation == null)
            return;


        double latitude = mCurrentLocation.getLatitude();
        double longitude = mCurrentLocation.getLongitude();
        LatLng myLatLng = new LatLng(latitude, longitude);
        mBounds = calculateBoundsWithCenter(myLatLng);
    }

    public LatLngBounds calculateBoundsWithCenter(LatLng myLatLng) {
        // Create a bounds
        LatLngBounds.Builder builder = LatLngBounds.builder();

        // Calculate east/west points that should to be included
        // in the bounds
        double lngDifference = calculateLatLngOffset(myLatLng, false);
        LatLng east = new LatLng(myLatLng.latitude, myLatLng.longitude
                + lngDifference);
        builder.include(east);
        LatLng west = new LatLng(myLatLng.latitude, myLatLng.longitude
                - lngDifference);
        builder.include(west);

        // Calculate north/south points that should to be included
        // in the bounds
        double latDifference = calculateLatLngOffset(myLatLng, true);
        LatLng north = new LatLng(myLatLng.latitude + latDifference,
                myLatLng.longitude);
        builder.include(north);
        LatLng south = new LatLng(myLatLng.latitude - latDifference,
                myLatLng.longitude);
        builder.include(south);

        return builder.build();
    }

    public void disconnect() {
        mLocationClient.disconnect();
    }

    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(mContext);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            LOGD(TAG, "Google play services available");
            return true;
            // Google Play services was not available for some reason
        } else {
            // Display an error dialog

            // Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode,
            // mContext, 0);
            // if (dialog != null) {
            // ErrorDialogFragment errorFragment = new ErrorDialogFragment();
            // errorFragment.setDialog(dialog);
            // errorFragment.show(mContext.getSupportFragmentManager(),
            // "tasker");
            // }

            return false;
        }
    }

//	@Override
//	public void onDisconnected() {
//	}

//	@Override
//	public void onConnectionFailed(ConnectionResult result) {
//	}

    private double calculateLatLngOffset(LatLng myLatLng, boolean bLatOffset) {
        // The return offset, initialized to the default difference
        double latLngOffset = OFFSET_CALCULATION_INIT_DIFF;
        // Set up the desired offset distance in meters
        float desiredOffsetInMeters = RADIUS * METERS_PER_FEET;
        // Variables for the distance calculation
        float[] distance = new float[1];
        boolean foundMax = false;
        double foundMinDiff = 0;
        // Loop through and get the offset
        do {
            // Calculate the distance between the point of interest
            // and the current offset in the latitude or longitude direction
            if (bLatOffset) {
                Location.distanceBetween(myLatLng.latitude, myLatLng.longitude,
                        myLatLng.latitude + latLngOffset, myLatLng.longitude,
                        distance);
            } else {
                Location.distanceBetween(myLatLng.latitude, myLatLng.longitude,
                        myLatLng.latitude, myLatLng.longitude + latLngOffset,
                        distance);
            }
            // Compare the current difference with the desired one
            float distanceDiff = distance[0] - desiredOffsetInMeters;
            if (distanceDiff < 0) {
                // Need to catch up to the desired distance
                if (!foundMax) {
                    foundMinDiff = latLngOffset;
                    // Increase the calculated offset
                    latLngOffset *= 2;
                } else {
                    double tmp = latLngOffset;
                    // Increase the calculated offset, at a slower pace
                    latLngOffset += (latLngOffset - foundMinDiff) / 2;
                    foundMinDiff = tmp;
                }
            } else {
                // Overshot the desired distance
                // Decrease the calculated offset
                latLngOffset -= (latLngOffset - foundMinDiff) / 2;
                foundMax = true;
            }
        } while (Math.abs(distance[0] - desiredOffsetInMeters) > OFFSET_CALCULATION_ACCURACY);
        return latLngOffset;
    }

    public double getLat() {
        return mCurrentLocation.getLatitude();
    }

    public double getLng() {
        return mCurrentLocation.getLongitude();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // TODO Auto-generated method stub

    }

}