package com.github.charmasaur.romperstomper.tracker;

import android.content.Context;
import android.location.Location;
import android.support.annotation.Nullable;
import android.util.Log;
import com.google.common.base.Preconditions;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;

public final class MapboxLocationLayer {
  private static final String TAG = MapboxLocationLayer.class.getSimpleName();
  private final Context context;
  private final MapView mapView;

  @Nullable private MapboxMap mapboxMap;
  private boolean started;

  @Nullable private LocationEngine locationEngine;
  @Nullable private LocationLayerPlugin locationLayerPlugin;

  public MapboxLocationLayer(Context context, MapView mapView) {
    this.context = Preconditions.checkNotNull(context);
    this.mapView = Preconditions.checkNotNull(mapView);
  }

  /**
   * Must have permission.
   */
  public void start() {
    Preconditions.checkState(!started);
    started = true;
    if (mapboxMap == null) {
      return;
    }
    startUpdates();
  }

  public void stop() {
    Preconditions.checkState(started);
    started = false;
    if (mapboxMap == null) {
      return;
    }
    stopUpdates();
  }

  /**
   * Stops if currently started, and does any necessary cleanup.
   */
  public void destroy() {
    // Whatever happens we can now clean out the mapboxMap reference.
    mapboxMap = null;

    if (locationEngine == null) {
      // We never started, so there's nothing to do.
      return;
    }

    if (started) {
      stopUpdates();
      // We could set started to false here, but who cares now that we're destroyed?
    }

    // At this point we're stopped, but still need to tear down the location engine and layer.
    // Clear out the location engine because I don't trust the API at all.
    locationLayerPlugin.setLocationEngine(null);
    locationLayerPlugin = null;

    locationEngine.removeLocationEngineListener(updateRequester);
    locationEngine.deactivate();
    locationEngine = null;
  }

  /**
   * Must only be called once.
   */
  public void setMapboxMap(MapboxMap mapboxMap) {
    Preconditions.checkState(this.mapboxMap == null);
    this.mapboxMap = Preconditions.checkNotNull(mapboxMap);

    if (started) {
      startUpdates();
    }
  }

  /**
   * Must only be called when started and {@link #mapboxMap} is non-null.
   */
  private void startUpdates() {
    ensureInitialized();
    updateRequester.requestLocationUpdatesWhenConnected();
    locationLayerPlugin.setLocationLayerEnabled(true);
  }

  /**
   * Must only be called when stopped and initialized.
   */
  private void stopUpdates() {
    locationLayerPlugin.setLocationLayerEnabled(false);
    updateRequester.removeLocationUpdatesWhenConnected();
  }

  /**
   * Ensures that {@link #locationEngine} and {@link #locationLayerPlugin} are non-null. Must only
   * be called when started and when {@link #mapboxMap} is set (because otherwise why would you
   * want to ensure initialization?).
   */
  private void ensureInitialized() {
    Preconditions.checkState(started);
    Preconditions.checkNotNull(mapboxMap);

    if (locationEngine != null) {
      return;
    }

    locationEngine = new LocationEngineProvider(context).obtainBestLocationEngineAvailable();
    Log.i(TAG, "Location engine: " + locationEngine);
    locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
    locationEngine.setFastestInterval(1000);
    locationEngine.activate();
    locationEngine.addLocationEngineListener(updateRequester);

    locationLayerPlugin = new LocationLayerPlugin(mapView, mapboxMap, locationEngine);
  }

  private final RequestUpdatesWhenConnectedLocationEngineListener updateRequester =
    new RequestUpdatesWhenConnectedLocationEngineListener();

  /**
   * It appears that calls to {@link LocationEngine#requestLocationUpdates} are no-ops prior to
   * getting the {@link LocationEngineListener#onConnected} callback. This makes the API kind of
   * awkward to use. This class provides a little wrapper API that will accept requests and
   * removals of location updates, and actually send them through to the engine when it connects.
   */
  private final class RequestUpdatesWhenConnectedLocationEngineListener
      implements LocationEngineListener {
    private boolean wantLocationUpdatesWhenConnected;
    private boolean connected;

    @Override
    public void onConnected() {
      // I really hope this is on the main thread...
      connected = true;
      if (wantLocationUpdatesWhenConnected) {
        locationEngine.requestLocationUpdates();
      }
    }

    @Override
    public void onLocationChanged(Location location) {}

    public void requestLocationUpdatesWhenConnected() {
      Preconditions.checkState(!wantLocationUpdatesWhenConnected);
      wantLocationUpdatesWhenConnected = true;
      if (connected) {
        locationEngine.requestLocationUpdates();
      }
    }

    public void removeLocationUpdatesWhenConnected() {
      Preconditions.checkState(wantLocationUpdatesWhenConnected);
      wantLocationUpdatesWhenConnected = false;
      if (connected) {
        locationEngine.removeLocationUpdates();
      }
    }
  }
}
