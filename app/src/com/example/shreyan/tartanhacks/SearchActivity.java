package com.example.shreyan.tartanhacks;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

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

public class SearchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        TextView searchText = (TextView) findViewById(R.id.result);
        searchText.setText("Hi Bing!");
        new BingAsyncTask().execute();
    }

    static String subscriptionKey = "3b63ff95-ada7-4614-a13f-7a8b180169b3";
    static String host = "https://api.cognitive.microsoft.com";
    static String path = "/bing/v7.0/search";
    static String searchTerm = "Ross O'Connell";

    public class BingAsyncTask extends AsyncTask<Void, Void, SearchResults> {
        @Override
        protected SearchResults doInBackground(Void... params) {
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
            TextView searchText = (TextView) findViewById(R.id.result);
            searchText.setText(searchResults.jsonResponse);
        }
    }

    // pretty-printer for JSON; uses GSON parser to parse and re-serialize
    public static String prettify(String json_text) throws JSONException {
        JSONObject json = new JSONObject(json_text);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(json);
    }

    public static SearchResults SearchWeb (String searchQuery) throws Exception {
        // construct URL of search request (endpoint + query string)
        URL url = new URL(host + path + "?q=" +  URLEncoder.encode(searchQuery, "UTF-8"));
        HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
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