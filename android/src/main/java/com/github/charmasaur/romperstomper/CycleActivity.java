package com.github.charmasaur.romperstomper;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
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
  private ArrayAdapter<String> adapter;
  private boolean havePermissions;

  @Nullable
  private CycleService.Binder serviceBinder;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.i(TAG, "onCreate");

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_cycle);

    button = (Button) findViewById(R.id.start_stop_button);
    adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1);
    ((ListView) findViewById(R.id.list)).setAdapter(adapter);

    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (serviceBinder == null) {
          throw new RuntimeException("onClick called when not bound");
        } else if (!havePermissions) {
          getOrUpdatePermissions();
        } else if (serviceBinder.isStarted()) {
          serviceBinder.stop();
        } else {
          serviceBinder.start();
        }
        updateAll();
      }
    });

    getOrUpdatePermissions();
    updateAll();

    if (!bindService(new Intent(this, CycleService.class), connection, BIND_AUTO_CREATE)) {
      throw new RuntimeException("Failed to bind to service");
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_cycle, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_share:
        startActivity(Intent.createChooser(getShareIntent(serviceBinder.getToken()), "Share via..."));
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    menu.findItem(R.id.menu_share).setVisible(serviceBinder != null && serviceBinder.isStarted());
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public void onDestroy() {
    unbindService(connection);
    super.onDestroy();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grants) {
    Log.i(TAG, "Got result");
    havePermissions = checkThePermissions();
    updateAll();
  }

  private void getOrUpdatePermissions() {
    havePermissions = checkThePermissions();
    if (!havePermissions) {
      ActivityCompat.requestPermissions(this, CycleService.REQUIRED_PERMISSIONS, PERMISSION_CODE);
    }
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
      button.setText(serviceBinder.isStarted() ? "Stop" : "Start");
    }
  }

  private void updateButtonEnabled() {
    button.setEnabled(serviceBinder != null);
  }

  private static Intent getShareIntent(String token) {
    Intent intent = new Intent();
    intent.setAction(Intent.ACTION_SEND);
    intent.putExtra(Intent.EXTRA_SUBJECT, "Follow my progress at...");
    intent.putExtra(
        Intent.EXTRA_TEXT,
        "Follow my progress at http://romper-stomper.appspot.com/cycler?token=" + token);
    intent.setType("text/plain");
    return intent;
  }

  private void updateAll() {
    updateButtonText();
    updateButtonEnabled();
    invalidateOptionsMenu();
  }


  private final ServiceConnection connection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder binder) {
      serviceBinder = (CycleService.Binder) binder;
      updateAll();
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
      serviceBinder = null;
      updateAll();
    }
  };
}
