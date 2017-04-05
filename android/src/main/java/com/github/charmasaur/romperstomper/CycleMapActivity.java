package com.github.charmasaur.romperstomper;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.List;

public class CycleMapActivity extends FragmentActivity {
  private static final String TAG = CycleMapActivity.class.getSimpleName();

  private CycleMapFetcher fetcher;
  @Nullable private GoogleMap googleMap;
  @Nullable private List<CycleMapFetcher.MarkerInfo> markers;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_cycle_map);
    SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager()
      .findFragmentById(R.id.map_fragment);
    fragment.getMapAsync(onMapReadyCallback);
    String url =
      getIntent().getData().buildUpon().appendQueryParameter("native", "true").build().toString();
    fetcher = new CycleMapFetcher(this, url, fetcherCallback);
    Log.i(TAG, "Created CycleMapActivity with URL: " + url);
  }

  @Override
  public void onStart() {
    super.onStart();
    refresh();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_cycle_map, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_refresh:
        refresh();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  private void refresh() {
    fetcher.fetch();
  }

  private void maybeUpdateMarkers() {
    if (googleMap == null || markers == null) {
      return;
    }
    for (CycleMapFetcher.MarkerInfo marker : markers) {
      googleMap.addMarker(
          new MarkerOptions()
              .position(new LatLng(marker.lat, marker.lng))
              .title("" + marker.timestamp));
    }
  }

  private final CycleMapFetcher.Callback fetcherCallback = new CycleMapFetcher.Callback() {
    @Override
    public void onSuccess(List<CycleMapFetcher.MarkerInfo> markers) {
      CycleMapActivity.this.markers = markers;
      maybeUpdateMarkers();
    }
  };

  private final OnMapReadyCallback onMapReadyCallback = new OnMapReadyCallback() {
    @Override
    public void onMapReady(GoogleMap googleMap) {
      CycleMapActivity.this.googleMap = googleMap;
      maybeUpdateMarkers();
    }
  };
}
