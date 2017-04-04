package com.github.charmasaur.romperstomper;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

public final class CycleService extends Service {
  private static final String TAG = CycleService.class.getSimpleName();
  private LocationRequester locationRequester;
  private Sender sender;

  @Nullable
  private String token;

  public interface Binder {
    /** Don't call until permissions are granted. */
    void start();
    void stop();
    boolean isStarted();
    @Nullable String getToken();
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
    return START_NOT_STICKY;
  }

  @Override
  public void onDestroy() {
    // It's possible somebody else is killing us. That's a bit weird, but we can at least clean up.
    if (token != null) {
      locationRequester.stop();
    }
    locationRequester.destroy();
    super.onDestroy();
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
      if (token != null) {
        return;
      }
      token = newToken();
      // TODO: Handle permissions properly.
      locationRequester.onPermissions();
      locationRequester.go();
      startService(new Intent(CycleService.this, CycleService.class));
      startForeground(1, new Notification.Builder(CycleService.this)
          .setContentTitle("Romping")
          .setContentIntent(
              PendingIntent.getActivity(
                  CycleService.this,
                  0,
                  new Intent(CycleService.this, CycleActivity.class),
                  Intent.FLAG_ACTIVITY_NEW_TASK))
          .setSmallIcon(android.R.drawable.ic_menu_mylocation)
          .build());
    }

    @Override
    public void stop() {
      if (token == null) {
        return;
      }
      token = null;
      locationRequester.stop();
      stopForeground(true);
      stopSelf();
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
  };

  private static String newToken() {
    byte[] randomBytes = new byte[12];
    new SecureRandom().nextBytes(randomBytes);
    // Use NO_WRAP since otherwise this won't get saved correctly to shared preferences (if we ever
    // wanted to do that).
    return Base64.encodeToString(randomBytes, Base64.URL_SAFE | Base64.NO_WRAP);
  }

}
