package com.example.shreyan.tartanhacks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SearchActivity extends AppCompatActivity {

    private TextView resultTextView;
    private ImageView resultImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        resultTextView = (TextView) this.findViewById(R.id.result);
        resultImageView = (ImageView) this.findViewById(R.id.imageView);

        BingAsyncTask getNewsUpdate = new BingAsyncTask();
        getNewsUpdate.execute();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class BingAsyncTask extends AsyncTask<Void, Void, Bitmap> {

        //private String APILink = "https://api.datamarket.azure.com/Bing/Search/v1/";
        private String APILink = "https://api.datamarket.azure.com/Bing/Search/v1/Image?Query=%27xbox%27&Market=%27en-US%27&Adult=%27Moderate%27&ImageFilters=%27Size%3ASmall%27&$format=json&$top=1";
        private String API_KEY = "OnU2Mxti1LltKV0xiBXh0wvlN3DbXwXngWfcHZ+1tME";
        private String[] SECTION = {"image"};

        @Override
        protected Bitmap doInBackground(Void... params) {
            String result = "";
            //For some reason post method doesn't work.
            //Only Get request work for this API.
            //Prepare Post request.


            HttpClient httpClient = new DefaultHttpClient();


            //Add all array list
            //ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            //nameValuePairs.add(new BasicNameValuePair("Query", "'xbox'"));
            //nameValuePairs.add(new BasicNameValuePair("Market", "'en-us'"));
            //nameValuePairs.add(new BasicNameValuePair("ImageFilters", "'Size:Small'"));
            //String paramsString = URLEncodedUtils.format(nameValuePairs, "UTF-8");


            //Log.e("Get link result ", APILink + SECTION[0] + "?" + paramsString);
            //Build Link
            HttpGet httpget = new HttpGet(APILink);
            //HttpGet httpget = new HttpGet(APILink + SECTION[0] + "?" + paramsString);
            String auth = API_KEY + ":" + API_KEY;
            String encodedAuth = Base64.encodeToString(auth.getBytes(), Base64.NO_WRAP);
            Log.e("", encodedAuth);
            httpget.addHeader("Authorization", "Basic " + encodedAuth);


            //Execute and get the response.
            HttpResponse response = null;
            try {
                response = httpClient.execute(httpget);
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                try {
                    inputStream = entity.getContent();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                try {
                    while ((line = bufferedReader.readLine()) != null) {
                        result += line;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //Extract link from JSON
            //String to Json
            JSONObject jsonObject = null;
            if (JSONValue.isValidJson(result)) {
                jsonObject = (JSONObject) JSONValue.parse(result);
            }
            ;

            jsonObject = (JSONObject) jsonObject.get("d");
            jsonObject = (JSONObject) ((JSONArray) jsonObject.get("results")).get(0);
            jsonObject = (JSONObject) jsonObject.get("Thumbnail");
            Log.e(". ", jsonObject.toString() + " . ");
            String url = (String) jsonObject.get("MediaUrl");
            Log.e(". ", url + " . ");

            Bitmap bitmap = null;
            try {
                bitmap = downloadBitmap(url);
            } catch (IOException e) {
                e.printStackTrace();
            }


            return bitmap;
        }

        private Bitmap downloadBitmap(String url) throws IOException {
            HttpUriRequest request = new HttpGet(url.toString());
            HttpClient httpClient = new DefaultHttpClient();
            HttpResponse response = httpClient.execute(request);

            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                byte[] bytes = EntityUtils.toByteArray(entity);

                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0,
                        bytes.length);
                return bitmap;
            } else {
                throw new IOException("Download failed, HTTP response code "
                        + statusCode + " - " + statusLine.getReasonPhrase());
            }


        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            SearchActivity.this.resultImageView.setImageBitmap(bitmap);
        }
    }
}
