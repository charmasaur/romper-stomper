package com.github.charmasaur.romperstomper.tracker;

import android.content.Context;
import android.util.Log;
import androidx.annotation.Nullable;
import com.google.common.base.Preconditions;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.maps.MapboxMap;

public final class MapboxLocationLayer {
  private static final String TAG = MapboxLocationLayer.class.getSimpleName();
  private final Context context;

  @Nullable private MapboxMap mapboxMap;
  private boolean started;

  @Nullable private LocationComponent locationComponent;

  public MapboxLocationLayer(Context context) {
    this.context = Preconditions.checkNotNull(context);
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

    if (locationComponent == null) {
      // We never started, so there's nothing to do.
      return;
    }

    if (started) {
      stopUpdates();
      // We could set started to false here, but who cares now that we're destroyed?
    }

    // Apparently we don't need to do any teardown of the location component, so just clear out the
    // # reference.
    locationComponent = null;
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
    locationComponent.setLocationComponentEnabled(true);
  }

  /**
   * Must only be called when stopped and initialized.
   */
  private void stopUpdates() {
    locationComponent.setLocationComponentEnabled(false);
  }

  /**
   * Ensures that {@link #locationEngine} and {@link #locationLayerPlugin} are non-null. Must only
   * be called when started and when {@link #mapboxMap} is set (because otherwise why would you
   * want to ensure initialization?).
   */
  private void ensureInitialized() {
    Preconditions.checkState(started);
    Preconditions.checkNotNull(mapboxMap);

    if (locationComponent != null) {
      return;
    }

    locationComponent = mapboxMap.getLocationComponent();
    Log.i(TAG, "Location component: " + locationComponent);
    locationComponent.activateLocationComponent(
        LocationComponentActivationOptions.builder(context, mapboxMap.getStyle())
          .useDefaultLocationEngine(true)
          .locationEngineRequest(
            new LocationEngineRequest.Builder(/* interval= */ 1000)
              .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
              .setFastestInterval(1000)
              .build())
          .build());
  }
}
