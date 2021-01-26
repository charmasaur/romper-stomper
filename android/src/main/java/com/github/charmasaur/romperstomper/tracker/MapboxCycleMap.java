package com.github.charmasaur.romperstomper.tracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.Nullable;
import com.github.charmasaur.romperstomper.R;
import com.google.common.base.Preconditions;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Calendar;
import java.text.SimpleDateFormat;

/**
 * Implementation of {@link CycleMap} using Mapbox.
 */
public final class MapboxCycleMap implements CycleMap {
  private final View view;
  private final MapView mapView;
  private final MapboxLocationLayer locationLayer;

  private ImmutableList<CycleMapFetcher.MarkerInfo> markers = ImmutableList.of();
  private boolean hasSetInitialViewport;

  @Nullable private MapboxMap mapboxMap;

  public MapboxCycleMap(LayoutInflater layoutInflater) {
    Mapbox.getInstance(
        layoutInflater.getContext(),
        layoutInflater.getContext().getResources().getString(R.string.mapbox_key));
    view = layoutInflater.inflate(R.layout.cycle_map_mapbox, /* root= */ null);

    mapView = (MapView) view.findViewById(R.id.mapbox_map);
    locationLayer = new MapboxLocationLayer(layoutInflater.getContext());
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(onMapReadyCallback);
  }

  @Override
  public void onStart() {
    mapView.onStart();
  }

  @Override
  public void onResume() {
    mapView.onResume();
  }

  @Override
  public void onPause() {
    mapView.onPause();
  }

  @Override
  public void onStop() {
    mapView.onStop();
  }

  @Override
  public void onDestroy() {
    locationLayer.destroy();
    mapView.onDestroy();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    mapView.onSaveInstanceState(outState);
  }

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
    if (markers.isEmpty()) {
      return;
    }
    PolylineOptions options = new PolylineOptions();
    LatLng prev = null;
    Marker lastMarker = null;
    for (CycleMapFetcher.MarkerInfo marker : markers) {
      LatLng latLng = getMarkerLatLng(marker);
      lastMarker = mapboxMap.addMarker(
          new MarkerOptions()
              .position(latLng)
              .title(formatTimestamp(marker.timestamp)));
      if (prev != null) {
        options.add(prev, latLng);
      }
      prev = latLng;
    }
    mapboxMap.addPolyline(options);

    // TODO: It'd be cool to just detect gestures rather than doing this "first markers" thing.
    if (!hasSetInitialViewport) {
      setDefaultCameraPosition();
      hasSetInitialViewport = true;
    }

    mapboxMap.selectMarker(lastMarker);
  }

  /**
   * {@link #markers} must be non-empty.
   */
  private void setDefaultCameraPosition() {
    ImmutableList<CycleMapFetcher.MarkerInfo> markers = this.markers;
    Preconditions.checkState(markers.size() > 0);

    ImmutableSet.Builder<LatLng> markerLatLngsBuilder = new ImmutableSet.Builder<>();

    for (CycleMapFetcher.MarkerInfo marker : markers) {
      markerLatLngsBuilder.add(getMarkerLatLng(marker));
    }
    ImmutableSet<LatLng> markerLatLngs = markerLatLngsBuilder.build();

    if (markerLatLngs.size() == 1) {
      mapboxMap.moveCamera(
          CameraUpdateFactory.newLatLngZoom(markerLatLngs.iterator().next(), 18.));
      return;
    }
    mapboxMap.moveCamera(
        CameraUpdateFactory.newLatLngBounds(
            new LatLngBounds.Builder().includes(markerLatLngs.asList()).build(),
            100));
  }

  @Override
  public void showUserLocation() {
    locationLayer.start();
  }

  @Override
  public void hideUserLocation() {
    locationLayer.stop();
  }

  private final OnMapReadyCallback onMapReadyCallback = new OnMapReadyCallback() {
    @Override
    public void onMapReady(MapboxMap mapboxMap) {
      mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
        @Override
        public void onStyleLoaded(Style style) {
          MapboxCycleMap.this.mapboxMap = mapboxMap;
          updateMarkersOnMap();

          locationLayer.setMapboxMap(mapboxMap);
        }
      });
    }
  };

  private static LatLng getMarkerLatLng(CycleMapFetcher.MarkerInfo marker) {
    return new LatLng(marker.lat, marker.lng);
  }

  private static String formatTimestamp(long timestamp) {
    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(timestamp * 1000);
    return formatter.format(calendar.getTime());
  }
}
