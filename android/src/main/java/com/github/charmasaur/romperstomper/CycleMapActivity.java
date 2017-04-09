package com.github.charmasaur.romperstomper;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.List;
import java.util.Calendar;
import java.text.SimpleDateFormat;

public class CycleMapActivity extends FragmentActivity {
  private static final String TAG = CycleMapActivity.class.getSimpleName();

  private CycleMapFetcher fetcher;
  @Nullable private GoogleMap googleMap;
  @Nullable private List<CycleMapFetcher.MarkerInfo> markers;

  @Nullable private Toast toast;

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
    toast("Fetching");
  }

  private void maybeUpdateMarkers() {
    if (googleMap == null || markers == null) {
      return;
    }
    googleMap.clear();
    LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
    PolylineOptions options = new PolylineOptions();
    LatLng prev = null;
    for (CycleMapFetcher.MarkerInfo marker : markers) {
      LatLng latLng = new LatLng(marker.lat, marker.lng);
      googleMap.addMarker(
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
    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
  }

  private String formatTimestamp(long timestamp) {
    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(timestamp * 1000);
    return formatter.format(calendar.getTime());
  }

  private void toast(String message) {
    if (toast != null) {
      toast.cancel();
    }
    toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
    toast.show();
  }

  private final CycleMapFetcher.Callback fetcherCallback = new CycleMapFetcher.Callback() {
    @Override
    public void onSuccess(List<CycleMapFetcher.MarkerInfo> markers) {
      CycleMapActivity.this.markers = markers;
      maybeUpdateMarkers();
      toast("Success");
    }
    @Override
    public void onFailure() {
      toast("Failure");
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