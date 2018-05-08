package com.github.charmasaur.romperstomper.tracker;

/**
 * Shows the user's current location on the map when requested and allowed.
 */
public final class MyLocation {
  /**
   * Checks and requests the appropriate permissions.
   */
  public interface Permissions {
    void request();
    boolean has();
  }

  private final Permissions permissions;
  private final CycleMap cycleMap;

  private boolean isShowing;

  public MyLocation(Permissions permissions, CycleMap cycleMap) {
    this.permissions = permissions;
    this.cycleMap = cycleMap;
  }

  public void request(boolean show) {
    if (show) {
      requestShow();
      return;
    }
    requestHide();
  }

  public boolean isShowing() {
    return isShowing;
  }

  public void onPermissionMaybeGranted() {
    if (!permissions.has()) {
      return;
    }
    isShowing = true;
    cycleMap.showUserLocation();
  }

  private void requestShow() {
    if (isShowing) {
      return;
    }

    if (permissions.has()) {
      isShowing = true;
      cycleMap.showUserLocation();
      return;
    }
    permissions.request();
  }

  private void requestHide() {
    if (!isShowing) {
      return;
    }
    isShowing = false;
    cycleMap.hideUserLocation();
  }
}
