package com.github.charmasaur.romperstomper.tracker;

public final class MyLocationLayer implements MyLocation.LayerEnabler {
  private boolean enabled;

  @Override
  public void enable() {
    enabled = true;
  }

  @Override
  public void disable() {
    enabled = false;
  }

  // TODO: Actually implement.
}
