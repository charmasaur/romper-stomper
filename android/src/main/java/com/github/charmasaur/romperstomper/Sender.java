package com.github.charmasaur.romperstomper;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class Sender {
  private static final String TAG = Sender.class.getSimpleName();
  private static final String SCHEME = "https";
  private static final String AUTHORITY = "romper.charmasaur.com";
  private static final String CYCLE_PATH = "cycle_submit";

  private final RequestQueue queue;

  public Sender(Context context) {
    queue = Volley.newRequestQueue(context);
  }

  public void sendCycle(double lat, double lng, long time, String thistoken) {
    Uri.Builder uriBuilder = new Uri.Builder()
        .scheme(SCHEME)
        .authority(AUTHORITY)
        .path(CYCLE_PATH)
        .appendQueryParameter("token", thistoken)
        .appendQueryParameter("lat", Double.toString(lat))
        .appendQueryParameter("lng", Double.toString(lng))
        .appendQueryParameter("tim", Long.toString(time));
    StringRequest stringRequest = new StringRequest(
        Request.Method.GET,
        uriBuilder.build().toString(),
        new Response.Listener<String>() {
          @Override
          public void onResponse(String response) {
            Log.i(TAG, "Got response: " + response);
          }
        },
        new Response.ErrorListener() {
          @Override
          public void onErrorResponse(VolleyError error) {
            Log.i(TAG, "Got error: " + error);
          }
        });
    // Add the request to the RequestQueue.
    queue.add(stringRequest);
  }
}
