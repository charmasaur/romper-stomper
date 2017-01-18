package com.github.charmasaur.romperstomper;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

// TODO: THis should probably control the location requester. Also blah blah torn down blah blah.
public final class CycleService extends Service {
  private static final String TAG = CycleService.class.getSimpleName();
  @Override
  public void onCreate() {
    Log.i(TAG, "onCreate");
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.i(TAG, "onStartCommand");
    startForeground(1, new Notification.Builder(this)
        .setContentTitle("Romping")
        .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, CycleActivity.class), Intent.FLAG_ACTIVITY_NEW_TASK))
        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
        .build());
    return START_STICKY;
  }

  public static void start(Context context) {
    context.startService(new Intent(context, CycleService.class));
  }

  public static void stop(Context context) {
    context.stopService(new Intent(context, CycleService.class));
  }
};
