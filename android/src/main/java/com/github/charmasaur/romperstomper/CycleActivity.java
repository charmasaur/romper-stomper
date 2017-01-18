package com.github.charmasaur.romperstomper;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

  private LocationRequester requester;
  private Sender sender;

  @Nullable
  private String token;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.i(TAG, "onCreate");

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_cycle);

    button = (Button) findViewById(R.id.start_stop_button);
    adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1);
    ((ListView) findViewById(R.id.list)).setAdapter(adapter);

    requester = new LocationRequester(this, requesterCallback);
    sender = new Sender(this, senderCallback);

    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (token != null) {
          stop();
        } else {
          start();
        }
        invalidateOptionsMenu();
        updateButtonText();
      }
    });

    updateButtonText();

    getPermission();
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
        startActivity(Intent.createChooser(getShareIntent(), "Share via..."));
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    menu.findItem(R.id.menu_share).setVisible(token != null);
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public void onDestroy() {
    if (token != null) {
      stop();
    }
    requester.destroy();
    super.onDestroy();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grants) {
    Log.i(TAG, "Got result");
    requester.onPermissions();
  }

  private void start() {
    if (token != null) {
      throw new RuntimeException("Started when started");
    }
    token = newToken();
    CycleService.start(CycleActivity.this);
    requester.go();
  }

  private void stop() {
    if (token == null) {
      throw new RuntimeException("Stopped when stopped");
    }
    requester.stop();
    CycleService.stop(CycleActivity.this);
    token = null;
  }

  private Intent getShareIntent() {
    Intent intent = new Intent();
    intent.setAction(Intent.ACTION_SEND);
    intent.putExtra(Intent.EXTRA_SUBJECT, "Follow my progress at...");
    intent.putExtra(
        Intent.EXTRA_TEXT,
        "Follow my progress at http://romper-stomper.appspot.com/cycler?token=" + token);
    intent.setType("text/plain");
    return intent;
  }

  private void getPermission() {
    String[] them = new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.INTERNET};
    if (!checkThePermissions(them)) {
      ActivityCompat.requestPermissions(this, them, PERMISSION_CODE);
    } else {
      requester.onPermissions();
    }
  }

  private boolean checkThePermissions(String [] its) {
    for (String it : its) {
      if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  private void updateButtonText() {
    button.setText(token == null ? "Start" : "Stop");
  }

  private static String newToken() {
    byte[] randomBytes = new byte[12];
    new SecureRandom().nextBytes(randomBytes);
    // Use NO_WRAP since otherwise this won't get saved correctly to shared preferences (if we ever
    // wanted to do that).
    return Base64.encodeToString(randomBytes, Base64.URL_SAFE | Base64.NO_WRAP);
  }

  private final Sender.Callback senderCallback = new Sender.Callback() {
    @Override
    public void onResults(List<String> results) {
    }

    @Override
    public void onStatus(String status) {
      adapter.add("Status: " + status);
    }
  };

  private final LocationRequester.Callback requesterCallback = new LocationRequester.Callback() {
    @Override
    public void onLocation(double lat, double lng, double acc, long time) {
      SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
      sdf.setTimeZone(TimeZone.getDefault());
      String ts = sdf.format(new Date(time * 1000));
      adapter.add("(" + lat + "," + lng + ")" + " at " + ts);
      sender.sendCycle(lat, lng, time, token);
    }
  };
}
