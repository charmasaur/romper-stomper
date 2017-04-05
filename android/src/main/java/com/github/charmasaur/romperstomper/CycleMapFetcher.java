package com.github.charmasaur.romperstomper;

import android.content.Context;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import java.util.Arrays;
import java.util.List;

public class CycleMapFetcher {
  private static final String TAG = CycleMapFetcher.class.getSimpleName();

  public static final class MarkerInfo {
    public final double lat;
    public final double lng;
    public final long timestamp;
    public MarkerInfo(double lat, double lng, long timestamp) {
      this.lat = lat;
      this.lng = lng;
      this.timestamp = timestamp;
    }
  }

  public interface Callback {
    void onSuccess(List<MarkerInfo> markers);
  }

  private final Callback callback;
  private final String url;
  private final RequestQueue queue;

  public CycleMapFetcher(Context context, String url, Callback callback) {
    this.url = url;
    this.callback = callback;

    queue = Volley.newRequestQueue(context);
  }

  public void fetch() {
    StringRequest stringRequest = new StringRequest(
        Request.Method.GET,
        url,
        new Response.Listener<String>() {
          @Override
          public void onResponse(String response) {
            Log.i(TAG, "Got response: " + response);
            parseResponse(response);
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

  private void parseResponse(String r) {
    callback.onSuccess(Arrays.asList(new MarkerInfo(-33.86, 151.2, 0)));
  }
}
