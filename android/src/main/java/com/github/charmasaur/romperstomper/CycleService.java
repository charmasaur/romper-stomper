package com.github.charmasaur.romperstomper;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public final class CycleService extends Service {
  /** Singleton to act as a broker between service and activity. */
  public static final class Broker {
    public static final Broker INSTANCE = new Broker();

    private final List<Runnable> listeners = new ArrayList<>();
    @Nullable private String token;

    private Broker() {}

    public void addListener(Runnable r) {
      listeners.add(r);
    }

    public void removeListener(Runnable r) {
      listeners.remove(r);
    }

    public boolean isStarted() {
      return token != null;
    }

    @Nullable
    public String getToken() {
      return token;
    }

    private void setToken(@Nullable String token) {
      this.token = token;
      for (Runnable r : listeners) {
        r.run();
      }
    }
  }

  private static final String TAG = CycleService.class.getSimpleName();
  private static final String NOTIFICATION_CHANNEL_ID = "cycle_service";
  private static final String QUIT_EXTRA = "quit";

  private LocationRequester locationRequester;
  private Sender sender;

  /**
   * These must be granted before starting or binding to the service.
   */
  public static  String[] REQUIRED_PERMISSIONS =
      new String[] {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.INTERNET};

  /**
   * The superset of permissions that might be used.
   */
  public static  String[] DESIRED_PERMISSIONS =
      new String[] {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.INTERNET};

  @Override
  public void onCreate() {
    super.onCreate();
    Log.i(TAG, "onCreate");
    locationRequester = new LocationRequester(this, locationRequesterCallback);
    sender = new Sender(this);
    createNotificationChannel();
  }

  @Override
  public IBinder onBind(Intent intent) {
   throw new IllegalStateException("CycleService does not support binding");
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.i(TAG, "onStartCommand");
    if (intent.hasExtra(QUIT_EXTRA)) {
      stopSelf();
    } else {
      maybeStart();
    }
    return START_NOT_STICKY;
  }

  @Override
  public void onDestroy() {
    maybeStop();
    super.onDestroy();
  }

  private void maybeStart() {
    if (Broker.INSTANCE.getToken() != null) {
      return;
    }
    Broker.INSTANCE.setToken(newToken());
    locationRequester.go();
    ServiceCompat.startForeground(
        this,
        /* id=*/ 1,
        new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Romping")
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    new Intent(this, CycleActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                PendingIntent.getService(
                  this,
                  1,
                  new Intent(this, CycleService.class).putExtra(QUIT_EXTRA, true),
                  PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
            .build(),
         ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
  }

  private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return;
    }
    CharSequence name = getString(R.string.cycle_service_channel_name);
    String description = getString(R.string.cycle_service_channel_description);
    int importance = NotificationManager.IMPORTANCE_LOW;
    NotificationChannel channel = new NotificationChannel(
        NOTIFICATION_CHANNEL_ID, name, importance);
    channel.setDescription(description);
    NotificationManager notificationManager = getSystemService(NotificationManager.class);
    notificationManager.createNotificationChannel(channel);
  }

  private void maybeStop() {
    if (Broker.INSTANCE.getToken() == null) {
      return;
    }
    Broker.INSTANCE.setToken(null);
    locationRequester.stop();
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
  }

  private final LocationRequester.Callback locationRequesterCallback =
      new LocationRequester.Callback() {
    @Override
    public void onLocation(double lat, double lng, double unc, long time) {
      SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
      sdf.setTimeZone(TimeZone.getDefault());
      String ts = sdf.format(new Date(time * 1000));
      sender.sendCycle(lat, lng, time, Broker.INSTANCE.getToken());
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
