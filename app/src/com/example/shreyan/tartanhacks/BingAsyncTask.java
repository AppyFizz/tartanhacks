package com.example.shreyan.tartanhacks;

import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

import static com.example.shreyan.tartanhacks.MainActivity.coolMap;

/**
 * Created by Shreyan on 2/10/2018.
 */
@RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
public class BingAsyncTask extends AsyncTask<String, String, SearchResults> {
    static String subscriptionKey = "512105a5848a43b29f4dd8078d3390f3";
    static String host = "https://api.cognitive.microsoft.com";
    static String path = "/bing/v7.0/search";
    @Override
    protected SearchResults doInBackground(String... params) {
        String searchTerm = params[0];
        if (coolMap.containsKey(searchTerm)) return null;
        if (subscriptionKey.length() != 32) {
            Log.e("Search", "Invalid Bing Search API subscription key!");
            Log.e("Search", "Please paste yours into the source code.");
            return null;
        }
        SearchResults result;
        try {
            Log.e("Search", "Searching the Web for: " + searchTerm);
            result = SearchWeb(searchTerm);
            Log.e("Search", "\nRelevant HTTP Headers:\n");
            for (String header : result.relevantHeaders.keySet())
                System.out.println(header + ": " + result.relevantHeaders.get(header));
            Log.e("Search", "\nJSON Response:\n");
            Log.e("Search", prettify(result.jsonResponse));
        } catch (Exception e) {
            Log.e("Search", e.getMessage());
            return null;
        }
        return result;
    }
    @Override
    protected void onPostExecute(SearchResults searchResults) {
        super.onPostExecute(searchResults);
        if (searchResults == null) return;
        try {
            JSONObject json = new JSONObject(searchResults.jsonResponse);
            String query = json.getJSONObject("queryContext").getString("originalQuery");
            String url = ((JSONObject) json.getJSONObject("webPages").
                    getJSONArray("value").get(0)).getString("url");
            coolMap.put(query, url);
        } catch (JSONException e) {
            Log.e("SearchActivity", e.getMessage());
        }
    }
    //    @Override
    //    protected void onPostExecute(SearchResults searchResults) {
    //        super.onPostExecute(searchResults);
    //        try {
    //            this.publishProgress(prettify(searchResults.jsonResponse.toString()));
    //        } catch (JSONException | NullPointerException e) {
    //            Log.e("Search", e.getMessage());
    //        }
    //    }
    // pretty-printer for JSON; uses GSON parser to parse and re-serialize
    private static String prettify(String json_text) throws JSONException {
        JSONObject json = new JSONObject(json_text);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(json);
    }
    private static SearchResults SearchWeb(String searchQuery) throws Exception {
        // construct URL of search request (endpoint + query string)
        URL url = new URL(host + path + "?q=" + URLEncoder.encode(searchQuery, "UTF-8"));
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestProperty("Ocp-Apim-Subscription-Key", subscriptionKey);

        // receive JSON body
        InputStream stream = connection.getInputStream();
        String response = new Scanner(stream).useDelimiter("\\A").next();

        // construct result object for return
        SearchResults results = new SearchResults(new HashMap<String, String>(), response);

        // extract Bing-related HTTP headers
        Map<String, List<String>> headers = connection.getHeaderFields();
        for (String header : headers.keySet()) {
            if (header == null) continue;      // may have null key
            if (header.startsWith("BingAPIs-") || header.startsWith("X-MSEdge-")) {
                results.relevantHeaders.put(header, headers.get(header).get(0));
            }
        }
        stream.close();
        return results;
    }
}
