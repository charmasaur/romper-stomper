package com.github.charmasaur.romperstomper.tracker;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import com.github.charmasaur.romperstomper.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.common.collect.ImmutableList;
import java.util.Calendar;
import java.text.SimpleDateFormat;

/**
 * Implementation of {@link CycleMap} using the Google Maps API.
 */
public final class GoogleMapsCycleMap implements CycleMap {
  private final View view;

  private ImmutableList<CycleMapFetcher.MarkerInfo> markers = ImmutableList.of();
  private boolean hasSetInitialViewport;
  private boolean showingLocation;

  @Nullable private GoogleMap googleMap;

  public GoogleMapsCycleMap(
      LayoutInflater layoutInflater,
      FragmentManager fragmentManager) {
    view = layoutInflater.inflate(R.layout.cycle_map_google_maps, /* root= */ null);

    SupportMapFragment fragment =
        (SupportMapFragment) fragmentManager.findFragmentById(R.id.google_maps_map_fragment);
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
    if (googleMap != null) {
      updateMarkersOnMap();
    }
  }

  private void updateMarkersOnMap() {
    googleMap.clear();
    if (markers.isEmpty()) {
      return;
    }
    LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
    PolylineOptions options = new PolylineOptions();
    LatLng prev = null;
    Marker lastMarker = null;
    for (CycleMapFetcher.MarkerInfo marker : markers) {
      LatLng latLng = new LatLng(marker.lat, marker.lng);
      lastMarker = googleMap.addMarker(
          new MarkerOptions()
              .position(latLng)
              .title(formatTimestamp(marker.timestamp)));
      boundsBuilder.include(latLng);
      if (prev != null) {
        options.add(prev, latLng);
      }
      prev = latLng;
    }
    googleMap.addPolyline(options);
    if (!hasSetInitialViewport) {
      googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
      hasSetInitialViewport = true;
    }
    if (lastMarker != null) {
      lastMarker.showInfoWindow();
    }
  }

  @Override
  public void showUserLocation() {
    showingLocation = true;
    if (googleMap != null) {
      updateMyLocationLayer();
    }
  }

  @Override
  public void hideUserLocation() {
    showingLocation = false;
    if (googleMap != null) {
      updateMyLocationLayer();
    }
  }

  private void updateMyLocationLayer() {
    googleMap.setMyLocationEnabled(showingLocation);
  }

  private final OnMapReadyCallback onMapReadyCallback = new OnMapReadyCallback() {
    @Override
    public void onMapReady(GoogleMap googleMap) {
      GoogleMapsCycleMap.this.googleMap = googleMap;
      updateMyLocationLayer();
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
