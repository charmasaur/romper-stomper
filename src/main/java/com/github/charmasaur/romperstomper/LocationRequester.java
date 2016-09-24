package com.github.charmasaur.romperstomper;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;

public class LocationRequester {
  private static final String TAG = LocationRequester.class.getSimpleName();
  private static final long INTERVAL_MS = 60 * 1000;

  public interface Callback {
    void onLocation(double lat, double lng, double unc, long time);
  }

  private final GoogleApiClient client;
  private final Context context;
  private final Callback callback;

  @Nullable
  private LocationRequest request;

  private boolean isConnected;
  private boolean going;

  private boolean requesting;

  public LocationRequester(Context context, Callback callback) {
    this.context = context;
    this.callback = callback;
    client = new GoogleApiClient.Builder(context)
        .addConnectionCallbacks(connectionCallback)
        .addOnConnectionFailedListener(failCallback)
        .addApi(LocationServices.API)
        .build();
    client.connect();
  }

  public void destroy() {
    client.disconnect();
  }

  public void onPermissions() {
    Log.i(TAG, "Setting request");
    request = new LocationRequest();
    request.setInterval(INTERVAL_MS);
    request.setFastestInterval((long)((float)INTERVAL_MS * 0.9f));
    request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
  }

  public void go() {
    going = true;
    maybeDo();
  }

  public void stop() {
    going = false;
    maybeDo();
  }

  public void maybeDo() {
    boolean shouldRequesting = isConnected && request != null && going;
    if (requesting && !shouldRequesting) {
      Log.i(TAG, "Undoing it");
      LocationServices.FusedLocationApi.removeLocationUpdates(client, locationListener);
      requesting = false;
    } else if (!requesting && shouldRequesting) {
      Log.i(TAG, "Doing it");
      LocationServices.FusedLocationApi.requestLocationUpdates(client, request, locationListener);
      requesting = true;
    }
  }

  private final LocationListener locationListener = new LocationListener() {
    @Override
    public void onLocationChanged(Location location) {
      callback.onLocation(location.getLatitude(), location.getLongitude(),
          location.hasAccuracy() ? location.getAccuracy() : 13.37, location.getTime() / 1000);
    }
  };

  private final GoogleApiClient.ConnectionCallbacks connectionCallback =
    new GoogleApiClient.ConnectionCallbacks() {
      @Override
      public void onConnected(Bundle hint) {
        Log.i(TAG, "Connected");
        isConnected = true;
        maybeDo();
      }

      @Override
      public void onConnectionSuspended(int cause) {
        Log.i(TAG, "Connection suspended: " + cause);
      }
    };

  private final GoogleApiClient.OnConnectionFailedListener failCallback =
    new GoogleApiClient.OnConnectionFailedListener() {
      @Override
      public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: " + result);
      }
    };
}
