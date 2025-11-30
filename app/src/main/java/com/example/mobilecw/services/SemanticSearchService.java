package com.example.mobilecw.services;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Service to handle semantic search API calls to the backend server.
 */
public class SemanticSearchService {

    private static final String TAG = "SemanticSearchService";
    private static final String BASE_URL = "http://206.189.93.77:8000";
    
    public interface SearchCallback {
        void onSuccess(List<SearchResult> results);
        void onError(String error);
    }

    public static class SearchResult {
        public String id;
        public String type; // "hike" or "observation"
        public double score;
        public String name;
        public String location;
        public String description;
        public String observationText;
        public Integer hikeId;

        public SearchResult(JSONObject json) throws JSONException {
            this.id = json.getString("id");
            this.type = json.getString("type");
            this.score = json.getDouble("score");
            this.name = json.optString("name", null);
            this.location = json.optString("location", null);
            this.description = json.optString("description", null);
            this.observationText = json.optString("observation_text", null);
            if (json.has("hike_id") && !json.isNull("hike_id")) {
                this.hikeId = json.getInt("hike_id");
            }
        }
    }

    /**
     * Perform semantic search using the backend API.
     */
    public static void search(Context context, String query, String firebaseUid, 
                             String searchType, int topK, SearchCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            callback.onError("Query cannot be empty");
            return;
        }

        if (firebaseUid == null || firebaseUid.isEmpty()) {
            callback.onError("User not logged in");
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(context.getApplicationContext());
        String url = BASE_URL + "/search";

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("query", query);
            requestBody.put("firebase_uid", firebaseUid);
            requestBody.put("search_type", searchType != null ? searchType : "hikes");
            requestBody.put("top_k", topK);
        } catch (JSONException e) {
            callback.onError("Failed to create request: " + e.getMessage());
            return;
        }
        
        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.POST,
            url,
            requestBody,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        List<SearchResult> results = parseSearchResponse(response);
                        callback.onSuccess(results);
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to parse search response", e);
                        callback.onError("Failed to parse response: " + e.getMessage());
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    String errorMessage = "Search failed";
                    if (error.networkResponse != null) {
                        errorMessage += " (Code: " + error.networkResponse.statusCode + ")";
                    }
                    if (error.getMessage() != null) {
                        errorMessage += ": " + error.getMessage();
                    }
                    Log.e(TAG, errorMessage, error);
                    callback.onError(errorMessage);
                }
            }
        );

        queue.add(request);
    }

    private static List<SearchResult> parseSearchResponse(JSONObject response) throws JSONException {
        List<SearchResult> results = new ArrayList<>();
        
        // Handle new API response format: { success, status_code, message, data: { results, total_found } }
        JSONArray resultsArray = null;
        
        if (response.has("data")) {
            // New format
            JSONObject data = response.getJSONObject("data");
            if (data.has("results")) {
                resultsArray = data.getJSONArray("results");
            }
        } else if (response.has("results")) {
            // Old format (direct results array)
            resultsArray = response.getJSONArray("results");
        }
        
        if (resultsArray == null) {
            return results;
        }

        for (int i = 0; i < resultsArray.length(); i++) {
            try {
                JSONObject resultJson = resultsArray.getJSONObject(i);
                SearchResult result = new SearchResult(resultJson);
                results.add(result);
            } catch (JSONException e) {
                // Skip invalid results
            }
        }

        return results;
    }
}

