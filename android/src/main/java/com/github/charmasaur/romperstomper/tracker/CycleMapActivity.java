package com.github.charmasaur.romperstomper.tracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.github.charmasaur.romperstomper.R;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;

/**
 * Activity for showing a map of romped locations.
 */
public final class CycleMapActivity extends FragmentActivity {
  private static final int PERMISSION_CODE = 1339;
  private static final String USING_LOCATION_KEY = "using_location";

  private CycleMap cycleMap;
  private CycleMapFetcher fetcher;
  private MyLocation myLocation;

  @Nullable private Toast toast;
  @Nullable private String url;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    cycleMap = new MapboxCycleMap(getLayoutInflater(), getSupportFragmentManager());
    cycleMap.onCreate(savedInstanceState);
    setContentView(cycleMap.getView());

    myLocation = new MyLocation(myLocationPermissions, cycleMap);

    fetcher = new CycleMapFetcher(this, fetcherCallback);

    if (savedInstanceState != null) {
      myLocation.request(savedInstanceState.getBoolean(USING_LOCATION_KEY, false));
    }
    handleIntent();
  }

  @Override
  public void onStart() {
    super.onStart();
    cycleMap.onStart();
  }

  @Override
  public void onResume() {
    super.onResume();
    cycleMap.onResume();
  }

  @Override
  public void onPause() {
    cycleMap.onPause();
    super.onPause();
  }

  @Override
  public void onStop() {
    cycleMap.onStop();
    super.onStop();
  }

  @Override
  public void onDestroy() {
    cycleMap.onDestroy();
    super.onDestroy();
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
        invalidateOptionsMenu();
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
    cycleMap.onSaveInstanceState(bundle);
    bundle.putBoolean(USING_LOCATION_KEY, myLocation.isShowing());
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grants) {
    myLocation.onPermissionMaybeGranted();
    invalidateOptionsMenu();
  }

  private void handleIntent() {
    if (getIntent().getData() == null) {
      return;
    }

    url =
      getIntent().getData().buildUpon().appendQueryParameter("native", "true").build().toString();
    cycleMap.setMarkers(ImmutableSet.<CycleMapFetcher.MarkerInfo>of());
    refresh();
  }

  private void refresh() {
    fetcher.fetch(url);
    toast("Fetching");
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
    public void onSuccess(ImmutableList<CycleMapFetcher.MarkerInfo> markers) {
      cycleMap.setMarkers(ImmutableSet.<CycleMapFetcher.MarkerInfo>copyOf(markers));
      toast("Success");
    }
    @Override
    public void onFailure() {
      toast("Failure");
    }
  };

  private final MyLocation.Permissions myLocationPermissions = new MyLocation.Permissions() {
    @Override
    public void request() {
      ActivityCompat.requestPermissions(
          CycleMapActivity.this,
          new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
          PERMISSION_CODE);
    }

    @Override
    public boolean has() {
      return ContextCompat.checkSelfPermission(
              CycleMapActivity.this,
              Manifest.permission.ACCESS_FINE_LOCATION)
          == PackageManager.PERMISSION_GRANTED;
    }
  };
}
