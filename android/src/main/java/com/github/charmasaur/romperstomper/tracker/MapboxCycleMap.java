package com.github.charmasaur.romperstomper.tracker;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import com.github.charmasaur.romperstomper.R;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.SupportMapFragment;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Calendar;
import java.text.SimpleDateFormat;

/**
 * Activity for showing a map of romped locations.
 */
public final class MapboxCycleMap implements CycleMap {
  private final View view;

  private ImmutableList<CycleMapFetcher.MarkerInfo> markers = ImmutableList.of();
  private boolean hasSetInitialViewport;

  @Nullable private MapboxMap mapboxMap;

  public MapboxCycleMap(
      LayoutInflater layoutInflater,
      FragmentManager fragmentManager) {
    Mapbox.getInstance(
        layoutInflater.getContext(),
        layoutInflater.getContext().getResources().getString(R.string.mapbox_key));
    view = layoutInflater.inflate(R.layout.cycle_map_mapbox, /* root= */ null);

    SupportMapFragment fragment =
        (SupportMapFragment) fragmentManager.findFragmentById(R.id.mapbox_map_fragment);
    fragment.getMapAsync(onMapReadyCallback);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {}
  @Override
  public void onStart() {}
  @Override
  public void onResume() {}
  @Override
  public void onPause() {}
  @Override
  public void onStop() {}
  @Override
  public void onDestroy() {}
  @Override
  public void onSaveInstanceState(Bundle outState) {}

  @Override
  public View getView() {
    return view;
  }

  @Override
  public void setMarkers(ImmutableList<CycleMapFetcher.MarkerInfo> markers) {
    if (markers.isEmpty()) {
      hasSetInitialViewport = false;
    }
    this.markers = markers;
    if (mapboxMap == null) {
      return;
    }
    updateMarkersOnMap();
  }

  private void updateMarkersOnMap() {
    mapboxMap.clear();
    LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
    PolylineOptions options = new PolylineOptions();
    LatLng prev = null;
    Marker lastMarker = null;
    for (CycleMapFetcher.MarkerInfo marker : markers) {
      LatLng latLng = new LatLng(marker.lat, marker.lng);
      lastMarker = mapboxMap.addMarker(
          new MarkerOptions()
              .position(latLng)
              .title(formatTimestamp(marker.timestamp)));
      boundsBuilder.include(latLng);
      if (prev != null) {
        options.add(prev, latLng);
      }
      prev = latLng;
    }
    mapboxMap.addPolyline(options);

    // TODO: It'd be cool to just detect gestures rather than doing this "first markers" thing.
    if (hasSetInitialViewport) {
      mapboxMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
      hasSetInitialViewport = true;
    }
    // TODO: Can we show the most recent info window?
  }

  @Override
  public void showUserLocation() {
    // TODO: Implement.
  }

  @Override
  public void hideUserLocation() {
    // TODO: Way ahead of you.
  }

  private final OnMapReadyCallback onMapReadyCallback = new OnMapReadyCallback() {
    @Override
    public void onMapReady(MapboxMap mapboxMap) {
      MapboxCycleMap.this.mapboxMap = mapboxMap;

      //locationEngine =
      //    new LocationEngineProvider(CycleMapActivity.this).obtainBestLocationEngineAvailable();
      //locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
      //locationEngine.setFastestInterval(1000);

      //locationLayerPlugin = new LocationLayerPlugin(mapView, mapboxMap, locationEngine);
      //locationLayerPlugin.addOnLocationClickListener(this);
      //locationLayerPlugin.addOnCameraTrackingChangedListener(this);
      //locationLayerPlugin.setCameraMode(cameraMode);

      //locationEngine.activate();

      updateMarkersOnMap();
    }
  };

  private static String formatTimestamp(long timestamp) {
    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(timestamp * 1000);
    return formatter.format(calendar.getTime());
  }
}
