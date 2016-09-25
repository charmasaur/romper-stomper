package com.github.charmasaur.romperstomper;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Sender {
  private static final String TAG = Sender.class.getSimpleName();
  private static final String SCHEME = "http";
  private static final String AUTHORITY = "romper-stomper.appspot.com";
  private static final String PATH = "here";
  private static final String PREFS_NAME = "SENDER";
  private static final String PREFS_KEY_TOKEN = "TOKEN";

  public interface Callback {
    // TODO: Should probably have onError as well.
    void onResults(List<String> data);
    void onStatus(String status);
  }

  private final Callback callback;
  private final RequestQueue queue;
  private final String token;

  public Sender(Context context, Callback callback) {
    this.callback = callback;

    queue = Volley.newRequestQueue(context);

    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
    String storedToken = prefs.getString(PREFS_KEY_TOKEN, null);
    if (storedToken != null) {
      token = storedToken;
    } else {
      token = generateToken();
      prefs.edit().putString(PREFS_KEY_TOKEN, token).apply();
    }
  }

  public void send(double lat, double lng, double acc, long time) {
    sendIt(lat, lng, acc, time, false);
  }

  public void update() {
    sendIt(0., 0., 0., 0, true);
  }

  private void sendIt(double lat, double lng, double acc, long time, boolean just_request) {
    Uri.Builder uriBuilder = new Uri.Builder()
        .scheme(SCHEME)
        .authority(AUTHORITY)
        .path(PATH)
        .appendQueryParameter("token", token);
    if (!just_request) {
      uriBuilder.appendQueryParameter("lat", Double.toString(lat))
          .appendQueryParameter("lng", Double.toString(lng))
          .appendQueryParameter("acc", Double.toString(acc))
          .appendQueryParameter("tim", Long.toString(time));
    }
    StringRequest stringRequest = new StringRequest(
        Request.Method.GET,
        uriBuilder.build().toString(),
        new Response.Listener<String>() {
          @Override
          public void onResponse(String response) {
            Log.i(TAG, "Got response: " + response);
            parseResponse(response);
            callback.onStatus("Last query successful");
          }
        },
        new Response.ErrorListener() {
          @Override
          public void onErrorResponse(VolleyError error) {
            Log.i(TAG, "Got error: " + error);
            callback.onStatus("Last query failed: " + error);
          }
        });
    // Add the request to the RequestQueue.
    queue.add(stringRequest);
  }

  private void parseResponse(String r) {
    callback.onResults(Arrays.asList(r.split("\\|")));
  }

  private static String generateToken() {
    byte[] randomBytes = new byte[10];
    new SecureRandom().nextBytes(randomBytes);
    // Use NO_WRAP since otherwise this won't get saved correctly to shared preferences.
    return Base64.encodeToString(randomBytes, Base64.URL_SAFE | Base64.NO_WRAP);
  }
}
