package com.github.charmasaur.romperstomper;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity {
  private static final String TAG = MainActivity.class.getSimpleName();
  private static final int PERMISSION_CODE = 1337;

  private TextView text;
  private TextView lastUpdatedText;
  private SwipeRefreshLayout swipeRefreshLayout;

  private LocationRequester requester;
  private Sender sender;

  private ArrayAdapter<String> adapter;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.i(TAG, "onCreate");

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1);

    text = (TextView) findViewById(R.id.text);
    lastUpdatedText = (TextView) findViewById(R.id.last_updated);
    ((ListView) findViewById(R.id.list)).setAdapter(adapter);
    swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);

    requester = new LocationRequester(this, requesterCallback);
    sender = new Sender(this, senderCallback);

    ((Switch) findViewById(R.id.button)).setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton view, boolean isChecked) {
            if (isChecked) {
              requester.go();
            } else {
              requester.stop();
            }
          }
        });

    findViewById(R.id.update_button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        text.setText("Refreshing");
        sender.update();
      }
    });

    swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
      @Override
      public void onRefresh() {
        text.setText("Refreshing");
        sender.update();
      }
    });

    getPermission();
  }

  @Override
  public void onDestroy() {
    requester.destroy();
    super.onDestroy();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grants) {
    Log.i(TAG, "Got result");
    requester.onPermissions();
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

  private final Sender.Callback senderCallback = new Sender.Callback() {
    @Override
    public void onResults(List<String> results) {
      adapter.clear();
      adapter.addAll(results);
      lastUpdatedText.setText("Last updated: " + DateFormat.getTimeInstance().format(new Date()));
    }

    @Override
    public void onStatus(String status) {
      text.setText(status);
      swipeRefreshLayout.setRefreshing(false);
    }
  };

  private final LocationRequester.Callback requesterCallback = new LocationRequester.Callback() {
    @Override
    public void onLocation(double lat, double lng, double acc, long time) {
      text.setText("Location: " + lat + " " + lng + " " + acc + " " + time);
      sender.send(lat, lng, acc, time);
    }
  };
}
