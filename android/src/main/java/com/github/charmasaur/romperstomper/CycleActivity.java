package com.github.charmasaur.romperstomper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.Button;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewGroupCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;

public class CycleActivity extends FragmentActivity {
  private static final String TAG = CycleActivity.class.getSimpleName();

  private int permissionCode = 0;
  private Button button;
  private Button stopButton;
  private Button shareButton;
  private Button showButton;
  private boolean havePermissions;

  @Nullable private String token;
  private boolean started;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.i(TAG, "onCreate");

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_cycle);

    button = (Button) findViewById(R.id.start_stop_button);
    stopButton = (Button) findViewById(R.id.stop_button);
    shareButton = (Button) findViewById(R.id.share_button);
    showButton = (Button) findViewById(R.id.show_button);

    View rootView = findViewById(R.id.main);
    ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
      Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

      MarginLayoutParams mlp = (MarginLayoutParams) v.getLayoutParams();
      mlp.topMargin = insets.top;
      mlp.bottomMargin = insets.bottom;
      v.setLayoutParams(mlp);

      return WindowInsetsCompat.CONSUMED;
    });

    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (!havePermissions) {
          showPermissionsEducation();
        } else if (started) {
          throw new RuntimeException("Start button clicked when started");
        } else {
          startForegroundService(new Intent(CycleActivity.this, CycleService.class));
        }
      }
    });

    stopButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (!started) {
          throw new RuntimeException("Stop button clicked when not started");
        } else {
          stopService(new Intent(CycleActivity.this, CycleService.class));
        }
      }
    });

    shareButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (!started) {
          throw new RuntimeException("Share button clicked when not started");
        }
        startActivity(Intent.createChooser(getShareIntent(token), "Share via..."));
      }
    });

    showButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (!started) {
          throw new RuntimeException("Show button clicked when not started");
        }
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getUrl(token))));
      }
    });

    havePermissions = checkThePermissions();

    CycleService.Broker.INSTANCE.addListener(binderListener);
    binderListener.run();
  }

  @Override
  public void onDestroy() {
    CycleService.Broker.INSTANCE.removeListener(binderListener);
    super.onDestroy();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grants) {
    Log.i(TAG, "Got result");
    havePermissions = checkThePermissions();
    updateAll();
  }

  private void showPermissionsEducation() {
    new PermissionEducationDialogFragment().show(
        getSupportFragmentManager(), "PERMISSION_EDUCATION_DIALOG");
  }

  private void getPermissions() {
    ActivityCompat.requestPermissions(this, CycleService.DESIRED_PERMISSIONS, permissionCode++);
  }

  private boolean checkThePermissions() {
    for (String it : CycleService.REQUIRED_PERMISSIONS) {
      if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  private void updateButtonEnabled() {
    button.setEnabled(!started);
    stopButton.setEnabled(started);
  }

  private void updateShareButtonEnabled() {
    boolean enabled = started;
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
    return "https://romper.charmasaur.com/cycler?token=" + token;
  }

  private void updateAll() {
    updateButtonEnabled();
    updateShareButtonEnabled();
  }

  private final Runnable binderListener = new Runnable() {
    @Override
    public void run() {
      started = CycleService.Broker.INSTANCE.isStarted();
      token = CycleService.Broker.INSTANCE.getToken();
      updateAll();
    }
  };

  public static final class PermissionEducationDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setMessage(R.string.permission_education)
        .setPositiveButton(
            R.string.permission_education_yes,
            (dialog, id) -> ((CycleActivity) getActivity()).getPermissions())
      .setNegativeButton(
          R.string.permission_education_no, (dialog, id) -> {});
      return builder.create();
    }
  }
}
