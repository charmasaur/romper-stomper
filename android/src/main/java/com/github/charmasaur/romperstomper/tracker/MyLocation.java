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
  /**
   * Enables or disables the location layer.
   *
   * <p>The layer is initially assumed disabled.
   */
  public interface LayerEnabler {
    /**
     * Guaranteed only to be called when the appropriate permissions are granted.
     *
     * <p>Will only be called when the layer is currently disabled.
     */
    void enable();
    /** Will only be called when the layer is currently disabled. */
    void disable();
  }

  private final Permissions permissions;
  private final LayerEnabler layerEnabler;

  private boolean isShowing;

  public MyLocation(Permissions permissions, LayerEnabler layerEnabler) {
    this.permissions = permissions;
    this.layerEnabler = layerEnabler;
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
    layerEnabler.enable();
  }

  private void requestShow() {
    if (isShowing) {
      return;
    }

    if (permissions.has()) {
      isShowing = true;
      layerEnabler.enable();
      return;
    }
    permissions.request();
  }

  private void requestHide() {
    if (!isShowing) {
      return;
    }
    isShowing = false;
    layerEnabler.disable();
  }
}
