package com.github.charmasaur.romperstomper;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.FusedLocationProviderClient;

public class LocationRequester {
  private static final String TAG = LocationRequester.class.getSimpleName();
  private static final long INTERVAL_MS = 60 * 1000;

  public interface Callback {
    void onLocation(double lat, double lng, double unc, long time);
  }

  private final FusedLocationProviderClient client;
  private final Context context;
  private final Callback callback;

  public LocationRequester(Context context, Callback callback) {
    this.context = context;
    this.callback = callback;
    client = LocationServices.getFusedLocationProviderClient(context);
  }

  public void go() {
    LocationRequest request = new LocationRequest();
    request.setInterval(INTERVAL_MS);
    request.setFastestInterval((long)((float)INTERVAL_MS * 0.9f));
    request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    client.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
  }

  public void stop() {
    client.removeLocationUpdates(locationCallback);
  }

  private final LocationCallback locationCallback = new LocationCallback() {
    @Override
    public void onLocationAvailability(LocationAvailability locationAvailability) {
      if (!locationAvailability.isLocationAvailable()) {
        Log.i(TAG, "Location not available");
      }
    }

    @Override
    public void onLocationResult(LocationResult locationResult) {
      Location location = locationResult.getLastLocation();
      callback.onLocation(location.getLatitude(), location.getLongitude(),
          location.hasAccuracy() ? location.getAccuracy() : 13.37, location.getTime() / 1000);
    }
  };
}
