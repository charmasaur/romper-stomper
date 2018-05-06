package com.github.charmasaur.romperstomper.tracker;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.GoogleMap;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.SupportMapFragment;
import java.util.List;
import java.util.Calendar;
import java.text.SimpleDateFormat;

public class CycleMapActivity extends FragmentActivity {
  private static final String TAG = CycleMapActivity.class.getSimpleName();
  private static final int PERMISSION_CODE = 1339;
  private static final String USING_LOCATION_KEY = "using_location";

  private final MyLocation myLocation = new MyLocation(myLocationPermissions, myLocationLayer);
  private final MyLocation.Permissions myLocationPermissions = new MyLocation.Permissions() {
    @Override
    public void request() {
      ActivityCompat.requestPermissions(
          CycleMapActivity.this, CycleService.REQUIRED_PERMISSIONS, PERMISSION_CODE);
    }

    @Override
    public void has() {
      return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
          == PackageManager.PERMISSION_GRANTED;
    }
  };
  private final MyLocationLayer myLocationLayer = new MyLocationLayer();

  private CycleMapFetcher fetcher;
  @Nullable private MapboxMap mapboxMap;
  @Nullable private List<CycleMapFetcher.MarkerInfo> markers;
  @Nullable private Toast toast;
  @Nullable private String url;

  private boolean hasSetInitialViewport;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_cycle_map);
    SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager()
      .findFragmentById(R.id.map_fragment);
    fragment.getMapAsync(onMapReadyCallback);
    fetcher = new CycleMapFetcher(this, fetcherCallback);
    myLocation = new MyLocation();

    if (savedInstanceState != null) {
      myLocation.request(savedInstanceState.getBoolean(USING_LOCATION_KEY, false));
    }
    handleIntent();
  }

  @Override
  public void onStart() {
    super.onStart();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_cycle_map, menu);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    menu.findItem(R.id.menu_location).setIcon(
        myLocation.isShowing()
            ? android.R.drawable.ic_menu_mylocation
            : android.R.drawable.ic_menu_help);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_refresh:
        refresh();
        return true;
      case R.id.menu_location:
        myLocation.request(!myLocation.isShowing());
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onNewIntent(Intent intent) {
    setIntent(intent);
    handleIntent();
  }

  @Override
  public void onSaveInstanceState(Bundle bundle) {
    super.onSaveInstanceState(bundle);
    bundle.putBoolean(USING_LOCATION_KEY, myLocation.isShowing());
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grants) {
    myLocation.onPermissionMaybeGranted();
  }

  private void handleIntent() {
    if (getIntent().getData() == null) {
      return;
    }

    url =
      getIntent().getData().buildUpon().appendQueryParameter("native", "true").build().toString();
    hasSetInitialViewport = false;
    markers = null;
    if (mapboxMap != null) {
      mapboxMap.clear();
    }
    refresh();
  }

  private void refresh() {
    fetcher.fetch(url);
    toast("Fetching");
  }

  private void maybeUpdateMarkers() {
    if (mapboxMap == null || markers == null) {
      return;
    }
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
    if (!hasSetInitialViewport) {
      mapboxMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
      hasSetInitialViewport = true;
    }
    if (lastMarker != null) {
      lastMarker.showInfoWindow();
    }
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
    public void onMapReady(MapboxMap mapboxMap) {
      CycleMapActivity.this.mapboxMap = mapboxMap;

      locationEngine =
          new LocationEngineProvider(CycleMapActivity.this).obtainBestLocationEngineAvailable();
      locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
      locationEngine.setFastestInterval(1000);

      locationLayerPlugin = new LocationLayerPlugin(mapView, mapboxMap, locationEngine);
      locationLayerPlugin.addOnLocationClickListener(this);
      locationLayerPlugin.addOnCameraTrackingChangedListener(this);
      locationLayerPlugin.setCameraMode(cameraMode);

      locationEngine.activate();

      maybeUpdateMarkers();
    }
  };
}
