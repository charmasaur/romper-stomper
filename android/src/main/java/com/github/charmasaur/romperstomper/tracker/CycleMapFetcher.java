package com.github.charmasaur.romperstomper.tracker;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;

/**
 * Fetches target locations.
 */
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
    void onSuccess(ImmutableList<MarkerInfo> markers);
  }

  private final Callback callback;
  private final RequestQueue queue;

  @Nullable private StringRequest lastRequest;

  public CycleMapFetcher(Context context, Callback callback) {
    this.callback = callback;

    queue = Volley.newRequestQueue(context);
  }

  public void fetch(String url) {
    if (lastRequest != null) {
      lastRequest.cancel();
      lastRequest = null;
    }
    lastRequest = new StringRequest(
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
    queue.add(lastRequest);
  }

  private void parseResponse(String r) {
    JsonArray outer;
    try {
      outer = new JsonParser().parse(r).getAsJsonArray();
    } catch (IllegalStateException e) {
      Log.i(TAG, "Couldn't parse outer array", e);
      callback.onFailure();
      return;
    }
    ImmutableList.Builder<MarkerInfo> markersBuilder = new ImmutableList.Builder<>();
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
      markersBuilder.add(newMarker);
    }
    callback.onSuccess(markersBuilder.build());
  }
}
