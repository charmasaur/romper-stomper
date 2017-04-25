package com.github.charmasaur.romperstomper;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

public class CycleActivity extends Activity {
  private static final String TAG = CycleActivity.class.getSimpleName();
  private static final int PERMISSION_CODE = 1338;

  private Button button;
  private Button stopButton;
  private Button shareButton;
  private Button showButton;
  private boolean havePermissions;

  // Valid iff serviceBinder is non-null.
  @Nullable private String token;
  private boolean started;

  @Nullable
  private CycleService.Binder serviceBinder;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.i(TAG, "onCreate");

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_cycle);

    button = (Button) findViewById(R.id.start_stop_button);
    stopButton = (Button) findViewById(R.id.stop_button);
    shareButton = (Button) findViewById(R.id.share_button);
    showButton = (Button) findViewById(R.id.show_button);

    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (serviceBinder == null) {
          throw new RuntimeException("Start button clicked when not bound");
        } else if (!havePermissions) {
          getPermissions();
        } else if (started) {
          throw new RuntimeException("Start button clicked when started");
        } else {
          serviceBinder.start();
        }
      }
    });

    stopButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (serviceBinder == null) {
          throw new RuntimeException("Stop button clicked when not bound");
        } else if (!started) {
          throw new RuntimeException("Stop button clicked when not started");
        } else {
          serviceBinder.stop();
        }
      }
    });

    shareButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (serviceBinder == null) {
          throw new RuntimeException("Share button clicked when not bound");
        } else if (!started) {
          throw new RuntimeException("Share button clicked when not started");
        }
        startActivity(Intent.createChooser(getShareIntent(token), "Share via..."));
      }
    });

    showButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (serviceBinder == null) {
          throw new RuntimeException("Show button clicked when not bound");
        } else if (!started) {
          throw new RuntimeException("Show button clicked when not started");
        }
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getUrl(token))));
      }
    });

    havePermissions = checkThePermissions();
    if (!havePermissions) {
      getPermissions();
    }
    updateAll();

    if (!bindService(new Intent(this, CycleService.class), connection, BIND_AUTO_CREATE)) {
      throw new RuntimeException("Failed to bind to service");
    }
  }

  @Override
  public void onDestroy() {
    if (serviceBinder != null) {
      serviceBinder.removeListener(binderListener);
    }
    unbindService(connection);
    super.onDestroy();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grants) {
    Log.i(TAG, "Got result");
    havePermissions = checkThePermissions();
    updateAll();
  }

  private void getPermissions() {
    ActivityCompat.requestPermissions(this, CycleService.REQUIRED_PERMISSIONS, PERMISSION_CODE);
  }

  private boolean checkThePermissions() {
    for (String it : CycleService.REQUIRED_PERMISSIONS) {
      if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  private void updateButtonText() {
    if (serviceBinder == null) {
      button.setText("Loading...");
    } else if (!havePermissions) {
      button.setText("Waiting for permissions...");
    } else {
      button.setText("Start");
    }
  }

  private void updateButtonEnabled() {
    button.setEnabled(serviceBinder != null && !started);
    stopButton.setEnabled(serviceBinder != null && started);
  }

  private void updateShareButtonEnabled() {
    boolean enabled = serviceBinder != null && started;
    shareButton.setEnabled(enabled);
    showButton.setEnabled(enabled);
  }

  private static Intent getShareIntent(String token) {
    Intent intent = new Intent();
    intent.setAction(Intent.ACTION_SEND);
    intent.putExtra(Intent.EXTRA_SUBJECT, "Follow my progress at...");
    intent.putExtra(Intent.EXTRA_TEXT, "Follow my progress at " + getUrl(token));
    intent.setType("text/plain");
    return intent;
  }

  private static String getUrl(String token) {
    return "http://romper-stomper.appspot.com/cycler?token=" + token;
  }

  private void updateAll() {
    updateButtonText();
    updateButtonEnabled();
    updateShareButtonEnabled();
  }

  private final Runnable binderListener = new Runnable() {
    @Override
    public void run() {
      started = serviceBinder.isStarted();
      token = serviceBinder.getToken();
      updateAll();
    }
  };

  private final ServiceConnection connection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder binder) {
      Log.i(TAG, "onServiceConnected");
      serviceBinder = (CycleService.Binder) binder;
      serviceBinder.addListener(binderListener);
      binderListener.run();
      updateAll();
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
      Log.i(TAG, "onServiceDisconnected");
      serviceBinder = null;
      updateAll();
    }
  };
}
