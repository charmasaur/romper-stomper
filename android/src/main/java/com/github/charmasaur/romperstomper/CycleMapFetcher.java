package com.github.charmasaur.romperstomper;

import android.content.Context;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import java.util.ArrayList;
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
    void onFailure();
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
            callback.onFailure();
          }
        });
    // Add the request to the RequestQueue.
    queue.add(stringRequest);
  }

  private void parseResponse(String r) {
    List<MarkerInfo> markers = new ArrayList<>();
    JsonArray outer;
    try {
      outer = new JsonParser().parse(r).getAsJsonArray();
    } catch (IllegalStateException e) {
      Log.i(TAG, "Couldn't parse outer array", e);
      callback.onFailure();
      return;
    }
    for (JsonElement element : outer) {
      JsonArray a = element.getAsJsonArray();
      MarkerInfo newMarker;
      try {
        newMarker = new MarkerInfo(
            a.get(0).getAsDouble(),
            a.get(1).getAsDouble(),
            a.get(2).getAsLong());
      } catch (IllegalStateException e) {
        Log.i(TAG, "Couldn't parse element", e);
        continue;
      }
      markers.add(newMarker);
    }
    callback.onSuccess(markers);
  }
}