package com.github.charmasaur.romperstomper;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public final class CycleService extends Service {
  private static final String TAG = CycleService.class.getSimpleName();
  /**
   * Extra to be sent with startService when the service should quit.
   *
   * <p>This is kind of interesting. If we bind with BIND_AUTO_CREATE then it isn't an option to
   * use only stopSelf to stop, because that won't do anything. Instead we need to do the teardown
   * manually and call stopSelf. But to do the teardown we either need to be bound or in
   * onStartCommand, and being bound isn't really an option if we're doing it from a notification
   * action (can't bind in a BroadcastReceiver... I guess we could start a new service-stopper
   * service and bind from that, but in that case we'd be using onStartCommand just from a
   * different service). That then implies that we need to do it like it is at the moment -- use
   * onStartCommand to stop too.
   *
   * <p>HOWEVER if we bind without BIND_AUTO_CREATE, calling stopSelf will terminate the bindings.
   * So if we did it that way then we'd remove start/stop from the binder, and use
   * startService/stopService to start/stop.
   */
  private static final String QUIT_EXTRA = "quit";
  private final List<Runnable> listeners = new ArrayList<>();
  private LocationRequester locationRequester;
  private Sender sender;

  @Nullable
  private String token;

  public static  String[] REQUIRED_PERMISSIONS =
      new String[] {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.INTERNET};

  public interface Binder {
    /** Don't call until permissions are granted. */
    void start();
    void stop();
    boolean isStarted();
    @Nullable String getToken();
    void addListener(Runnable listener);
    void removeListener(Runnable listener);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    Log.i(TAG, "onCreate");
    locationRequester = new LocationRequester(this, locationRequesterCallback);
    sender = new Sender(this, senderCallback);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.i(TAG, "onStartCommand");
    if (intent.hasExtra(QUIT_EXTRA)) {
      maybeStop();
    } else {
      maybeStart();
    }
    return START_NOT_STICKY;
  }

  @Override
  public void onDestroy() {
    // It's possible somebody else is killing us. That's a bit weird, but we can at least clean up.
    maybeStop();
    locationRequester.destroy();
    super.onDestroy();
  }

  private void maybeStart() {
    if (token != null) {
      return;
    }
    token = newToken();
    // TODO: Handle permissions properly.
    locationRequester.onPermissions();
    locationRequester.go();
    startForeground(1, new NotificationCompat.Builder(this)
        .setContentTitle("Romping")
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                new Intent(this, CycleActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT))
        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
        .addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Stop",
            PendingIntent.getService(
              this,
              1,
              new Intent(this, CycleService.class).putExtra(QUIT_EXTRA, true),
              PendingIntent.FLAG_UPDATE_CURRENT))
        .build());
    for (Runnable r : listeners) {
      r.run();
    }
  }

  private void maybeStop() {
    if (token == null) {
      return;
    }
    token = null;
    locationRequester.stop();
    stopForeground(true);
    stopSelf();
    for (Runnable r : listeners) {
      r.run();
    }
  }

  private final LocationRequester.Callback locationRequesterCallback =
      new LocationRequester.Callback() {
    @Override
    public void onLocation(double lat, double lng, double unc, long time) {
      SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
      sdf.setTimeZone(TimeZone.getDefault());
      String ts = sdf.format(new Date(time * 1000));
      sender.sendCycle(lat, lng, time, token);
    }
  };

  private final Sender.Callback senderCallback = new Sender.Callback() {
    @Override
    public void onResults(List<String> results) {}

    @Override
    public void onStatus(String status) {}
  };

  private final android.os.Binder binder = new BinderImpl();
  private final class BinderImpl extends android.os.Binder implements Binder {
    @Override
    public void start() {
      startService(new Intent(CycleService.this, CycleService.class));
    }

    @Override
    public void stop() {
      startService(new Intent(CycleService.this, CycleService.class).putExtra(QUIT_EXTRA, true));
    }

    @Override
    public boolean isStarted() {
      return token != null;
    }

    @Override
    @Nullable
    public String getToken() {
      return token;
    }

    @Override
    public void addListener(Runnable r) {
      listeners.add(r);
    }

    @Override
    public void removeListener(Runnable r) {
      listeners.remove(r);
    }
  };

  private static String newToken() {
    byte[] randomBytes = new byte[12];
    new SecureRandom().nextBytes(randomBytes);
    // Use NO_WRAP since otherwise this won't get saved correctly to shared preferences (if we ever
    // wanted to do that).
    return Base64.encodeToString(randomBytes, Base64.URL_SAFE | Base64.NO_WRAP);
  }

}
