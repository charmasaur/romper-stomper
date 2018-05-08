package com.github.charmasaur.romperstomper.tracker;

import android.os.Bundle;
import android.view.View;
import com.google.common.collect.ImmutableList;

/**
 * Map that can show a collection of locations.
 */
public interface CycleMap {
  void onCreate(Bundle savedInstanceState);
  void onStart();
  void onResume();
  void onPause();
  void onStop();
  void onDestroy();
  void onSaveInstanceState(Bundle outState);
  
  /**
   * Returns the {@link View} into which this map draws.
   */
  View getView();

  /**
   * Shows the given list of markers.
   */
  void setMarkers(ImmutableList<CycleMapFetcher.MarkerInfo> markers);

  /**
   * Shows the user location.
   *
   * <p>Must have the ACCESS_FINE_LOCATION permission.
   */
  void showUserLocation();

  /**
   * Hides the user location.
   */
  void hideUserLocation();
}
