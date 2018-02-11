/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license.
 * //
 * Project Oxford: http://ProjectOxford.ai
 * //
 * ProjectOxford SDK GitHub:
 * https://github.com/Microsoft/ProjectOxford-ClientSDK
 * //
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 * //
 * MIT License:
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * //
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * //
 * THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.example.shreyan.tartanhacks;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.bing.speech.SpeechClientStatus;
import com.microsoft.cognitive.textanalytics.model.request.RequestDoc;
import com.microsoft.cognitive.textanalytics.model.request.RequestDocIncludeLanguage;
import com.microsoft.cognitive.textanalytics.model.request.keyphrases_sentiment.TextRequest;
import com.microsoft.cognitive.textanalytics.model.request.language.LanguageRequest;
import com.microsoft.cognitive.textanalytics.model.response.keyphrases.KeyPhrasesResponse;
import com.microsoft.cognitive.textanalytics.model.response.language.LanguageResponse;
import com.microsoft.cognitive.textanalytics.model.response.sentiment.SentimentResponse;
import com.microsoft.cognitive.textanalytics.retrofit.ServiceCall;
import com.microsoft.cognitive.textanalytics.retrofit.ServiceCallback;
import com.microsoft.cognitive.textanalytics.retrofit.ServiceRequestClient;
import com.microsoft.cognitiveservices.speechrecognition.DataRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.MicrophoneRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionStatus;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionMode;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class MainActivity extends Activity implements ISpeechRecognitionServerEvents, View.OnClickListener
{
    private String mSubscriptionKey;

    public static HashMap<String, String> coolMap;
    private static final String TAG = MainActivity.class.getSimpleName();

    // UI
    private TextInputEditText mTextInput;
    private TextView mDetectedLanguage;
    private TextView mKeyPhrases;
    private TextView mSentimentScore;
    private ProgressDialog mProgressDialog;
    private ImageButton mClearButton;

    // Network request
    private ServiceRequestClient mRequest;
    private RequestDoc mDocument;
    private LanguageRequest mLanguageRequest;       // request for language detection
    private RequestDocIncludeLanguage mDocIncludeLanguage;
    private TextRequest mTextIncludeLanguageRequest;               // request for key phrases and sentiment analysis


    private ServiceCall mLanguageServiceCall;
    private ServiceCallback mLanguageCallback;

    private ServiceCall mKeyPhrasesCall;
    private ServiceCallback mKeyPhrasesCallback;

    private ServiceCall mSentimentCall;
    private ServiceCallback mSentimentCallback;
    int m_waitSeconds = 0;
    DataRecognitionClient dataClient = null;
    MicrophoneRecognitionClient micClient = null;
    FinalResponseStatus isReceivedResponse = FinalResponseStatus.NotReceived;
    EditText _logText2;
    EditText _logText;
    ImageButton _startButton;
    ImageButton _stopButton;


    public enum FinalResponseStatus { NotReceived, OK, Timeout }

    /**
     * Gets the primary subscription key
     */
    public String getPrimaryKey() {
        return this.getString(R.string.primaryKey);
    }

    /**
     * Gets a value indicating whether or not to use the microphone.
     * @return true if [use microphone]; otherwise, false.
     */
    private Boolean getUseMicrophone() {
        return true;
    }

    /**
     * Gets the current speech recognition mode.
     * @return The speech recognition mode.
     */
    private SpeechRecognitionMode getMode() {
        return SpeechRecognitionMode.LongDictation;
    }

    /**
     * Gets the default locale.
     * @return The default locale.
     */
    private String getDefaultLocale() {
        return "en-us";
    }

    /**
     * Gets the Cognitive Service Authentication Uri.
     * @return The Cognitive Service Authentication Uri.  Empty if the global default is to be used.
     */
    private String getAuthenticationUri() {
        return this.getString(R.string.authenticationUri);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        coolMap = new HashMap<>();
        this._logText2 = findViewById(R.id.editText2);
        this._logText = findViewById(R.id.editText1);
        this._startButton = findViewById(R.id.button1);
        this._stopButton = findViewById(R.id.button2);
        this._stopButton.setEnabled(false);
        /*
        _logText2.setScroller(new Scroller(this));
        _logText2.setVerticalScrollBarEnabled(true);
        _logText2.setMovementMethod(new ScrollingMovementMethod());
        */
        if (getString(R.string.primaryKey).startsWith("Please")) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.add_subscription_key_tip_title))
                    .setMessage(getString(R.string.add_subscription_key_tip))
                    .setCancelable(false)
                    .show();
        }

        // setup the buttons
        final MainActivity This = this;
        this._startButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                This.StartButton_Click();
            }
        });
        this._stopButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                This.StopButtonClick();
            }
        });

        mTextInput = (TextInputEditText) findViewById(R.id.text_input);
        mClearButton = (ImageButton) findViewById(R.id.clear_all);

        mKeyPhrases = (TextView) findViewById(R.id.key_phrases);
        mDetectedLanguage = (TextView) findViewById(R.id.detected_language);

        mTextInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mTextInput.getText().toString().isEmpty()) {
                    mClearButton.setVisibility(View.GONE);
                } else {
                    mClearButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

                // Get input text string
                String textInputString = mTextInput.getText().toString().trim();

                // Request: text without language
                mDocument = new RequestDoc();
                mDocument.setId("1");
                mDocument.setText(textInputString);
                List<RequestDoc> documents = new ArrayList<>();
                documents.add(mDocument);
                mLanguageRequest = new LanguageRequest(documents);

                // Request: text with language hard-coded to "en" for demo purpose, not production quality
                mDocIncludeLanguage = new RequestDocIncludeLanguage();
                mDocIncludeLanguage.setId("1");
                mDocIncludeLanguage.setLanguage("en");
                mDocIncludeLanguage.setText(textInputString);
                List<RequestDocIncludeLanguage> textDocs = new ArrayList<>();
                textDocs.add(mDocIncludeLanguage);
                mTextIncludeLanguageRequest = new TextRequest(textDocs);
            }
        });

        mSubscriptionKey = Utils.getAPiKey(this); // get API key from either strings.xml or SharedPreferences

        // Set OnClick listeners
        mClearButton.setOnClickListener(this);
        ((TextView) findViewById(R.id.get_key_phrases)).setOnClickListener(this);

        // Request for network calls
        mRequest = new ServiceRequestClient(mSubscriptionKey);

    }
    /**
     * Clean up UI and network requests onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();
        // Cancel all network calls
        if (mLanguageServiceCall != null && !mLanguageServiceCall.isCancelled()) {
            mLanguageServiceCall.cancel();
        }
        if (mKeyPhrasesCall != null && !mKeyPhrasesCall.isCancelled()) {
            mKeyPhrasesCall.cancel();
        }
        if (mSentimentCall != null && !mSentimentCall.isCancelled()) {
            mSentimentCall.cancel();
        }
        // Dismiss dialog
        dismissProgressDialog();
    }

    //endregion

    /**
     * Handles UI onclick events
     *
     * @param v View
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.clear_all:
                clearText();
                break;
            case R.id.get_key_phrases:
                if (Utils.hasApiKey(this, mSubscriptionKey) && Utils.hasText(this, mTextInput)) {
                    getKeyPhrases();
                }
                break;
        }
    }

    //region UI
    private void loadSampleText(String sampleText) {
        mTextInput.requestFocus();
        mTextInput.setText(sampleText);
        mTextInput.clearFocus();        // clear focus once sample text is entered
        mTextInput.setError(null);      // clear error once sample text is entered
        mClearButton.setVisibility(View.GONE);
    }

    private void clearText() {
        mTextInput.setText("");
    }

    private void showProgressDialog() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setTitle(getString(R.string.progress_bar_title));
        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
    //endregion

    //region Network calls

    /**
     * Get languages from Text Analytics service
     */
    private void getLanguages() {
        showProgressDialog();

        mLanguageCallback = new ServiceCallback(mRequest.getRetrofit()) {
            @Override
            public void onResponse(Call call, Response response) {
                super.onResponse(call, response);
                LanguageResponse languageResponse = (LanguageResponse) response.body();
                if (response != null && response.isSuccessful()) {
                    String text = languageResponse.getDocuments().get(0).getDetectedLanguages().get(0).getName();
                    mDetectedLanguage.setText("  Detected Language: \n  "+text);
                }
                dismissProgressDialog();
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                super.onFailure(call, t);
                dismissProgressDialog();
            }
        };

        try {
            mLanguageServiceCall = mRequest.getLanguagesAsync(mLanguageRequest, 3, mLanguageCallback);
        } catch (IllegalArgumentException e) {
            dismissProgressDialog();
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * Get key phrases from Text Analytics service
     */
    private void getKeyPhrases() {
        showProgressDialog();

        mKeyPhrasesCallback = new ServiceCallback(mRequest.getRetrofit()) {
            @Override
            public void onResponse(Call call, Response response) {
                super.onResponse(call, response);
                KeyPhrasesResponse keyPhrasesResponse = (KeyPhrasesResponse) response.body();
                if (response != null && response.isSuccessful()) {
                    List<String> keyPhrasesStringList = keyPhrasesResponse.getDocuments().get(0).getKeyPhrases();
                    if (keyPhrasesStringList.size() > 0) {
                        String keyPhrasesString = keyPhrasesStringList.get(0);
                        for (int i = 1; i < keyPhrasesStringList.size(); i++) {
                            keyPhrasesString += ", " + keyPhrasesStringList.get(i);
                        }
                        mKeyPhrases.setText(keyPhrasesString);
                    }
                }
                dismissProgressDialog();
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                super.onFailure(call, t);
                dismissProgressDialog();
            }
        };

        try {
            mKeyPhrasesCall = mRequest.getKeyPhrasesAsync(mTextIncludeLanguageRequest, mKeyPhrasesCallback);
        } catch (IllegalArgumentException e) {
            dismissProgressDialog();
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Get sentiment score
     */
    private void getSentimentScore() {
        showProgressDialog();
        mSentimentCallback = new ServiceCallback(mRequest.getRetrofit()) {
            @Override
            public void onResponse(Call call, Response response) {
                super.onResponse(call, response);
                SentimentResponse sentimentResponse = (SentimentResponse) response.body();
                if (response != null && response.isSuccessful()) {
                    mSentimentScore.setText(sentimentResponse.getDocuments().get(0).getScore().toString());
                }
                dismissProgressDialog();
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                super.onFailure(call, t);
                dismissProgressDialog();
            }
        };
        try {
            mSentimentCall = mRequest.getSentimentAsync(mTextIncludeLanguageRequest, mSentimentCallback);
        } catch (IllegalArgumentException e) {
            dismissProgressDialog();
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //endregion

    //region Toolbar Menu
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
        if (id == R.id.action_add_key) {
            Intent intent = new Intent(this, KeyActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
    /**
     * Handles the Click event of the _startButton control.
     */
    private void StartButton_Click() {
        this._startButton.setEnabled(false);
        this.m_waitSeconds = 200;
        this.LogRecognitionStart();
        if (this.micClient == null) {
            this.micClient = SpeechRecognitionServiceFactory.createMicrophoneClient(
                    this,
                    this.getMode(),
                    this.getDefaultLocale(),
                    this,
                    this.getPrimaryKey());
            this.micClient.setAuthenticationUri(this.getAuthenticationUri());
        }
        this.micClient.startMicAndRecognition();
        this._stopButton.setEnabled(true);
    }

    private void StopButtonClick() {
        this._stopButton.setEnabled(false);
        if (this.micClient != null)
            this.micClient.endMicAndRecognition();
        this._startButton.setEnabled(true);
    }

    /**
     * Logs the recognition start.
     */
    private void LogRecognitionStart() {
        String recoSource;
        if (this.getUseMicrophone()) {
            recoSource = "microphone";
        } else if (this.getMode() == SpeechRecognitionMode.ShortPhrase) {
            recoSource = "short wav file";
        } else {
            recoSource = "long wav file";
        }

        this.WriteLine("\n--- Start speech recognition using " + recoSource + " with " + this.getMode() + " mode in " + this.getDefaultLocale() + " language ----\n\n");
    }

    public void onFinalResponseReceived(final RecognitionResult response) {
        boolean isFinalDicationMessage = this.getMode() == SpeechRecognitionMode.LongDictation &&
                (response.RecognitionStatus == RecognitionStatus.EndOfDictation ||
                        response.RecognitionStatus == RecognitionStatus.DictationEndSilenceTimeout);
        if (null != this.micClient && this.getUseMicrophone() && ((this.getMode() == SpeechRecognitionMode.ShortPhrase) || isFinalDicationMessage)) {
            // we got the final result, so it we can end the mic reco.  No need to do this
            // for dataReco, since we already called endAudio() on it as soon as we were done
            // sending all the data.
            this.micClient.endMicAndRecognition();
        }

        if (isFinalDicationMessage) {
            this._startButton.setEnabled(true);
            this.isReceivedResponse = FinalResponseStatus.OK;
        }

        if (!isFinalDicationMessage) {
            for (int i = 0; i < response.Results.length; i++) {
                this.WriteLine2(response.Results[i].DisplayText);
                loadSampleText(response.Results[i].DisplayText);
            }
            this.WriteLine2();
            getLanguages();
            //mKeyPhrases.setText("");
        }
    }

    /**
     * Called when a final response is received and its intent is parsed
     */
    public void onIntentReceived(final String payload) {
        this.WriteLine("--- Intent received by onIntentReceived() ---");
        this.WriteLine(payload);
        this.WriteLine();
    }

    public void onPartialResponseReceived(final String response) {
        //this.WriteLine("--- Partial result received by onPartialResponseReceived() ---");
        this.WriteLine(response);
        this.WriteLine();
    }

    public void onError(final int errorCode, final String response) {
        this._startButton.setEnabled(true);
        this.WriteLine("--- Error received by onError() ---");
        this.WriteLine("Error code: " + SpeechClientStatus.fromInt(errorCode) + " " + errorCode);
        this.WriteLine("Error text: " + response);
        this.WriteLine();
    }

    /**
     * Called when the microphone status has changed.
     * @param recording The current recording state
     */
    public void onAudioEvent(boolean recording) {
        this.WriteLine("--- Microphone status change received by onAudioEvent() ---");
        this.WriteLine("********* Microphone status: " + recording + " *********");
        if (recording) {
            this.WriteLine("Please start speaking.");
        }

        WriteLine();
        if (!recording) {
            this.micClient.endMicAndRecognition();
            this._startButton.setEnabled(true);
        }
    }

    /**
     * Writes the line.
     */
    private void WriteLine() {
        this.WriteLine("");
    }

    /**
     * Writes the line.
     * @param text The line to write.
     */
    private void WriteLine(String text) {
        Log.d("FuckAJ", text);
        this._logText.append(text + "\n");
        //this._logText.setText(text + "\n");
    }
    private void WriteLine2() {
        this.WriteLine("");
    }

    /**
     * Writes the line.
     * @param text The line to write.
     */
    private void WriteLine2(String text) {
        Log.d("FuckAJ", text);
        this._logText2.append(text + "\n");
        //this._logText.setText(text + "\n");
    }

//    /**
//     * Handles the Click event of the RadioButton control.
//     * @param rGroup The radio grouping.
//     * @param checkedId The checkedId.
//     */
//    private void RadioButton_Click(RadioGroup rGroup, int checkedId) {
//        // Reset everything
//        if (this.micClient != null) {
//            this.micClient.endMicAndRecognition();
//            try {
//                this.micClient.finalize();
//            } catch (Throwable throwable) {
//                throwable.printStackTrace();
//            }
//            this.micClient = null;
//        }
//
//        if (this.dataClient != null) {
//            try {
//                this.dataClient.finalize();
//            } catch (Throwable throwable) {
//                throwable.printStackTrace();
//            }
//            this.dataClient = null;
//        }
//        this._startButton.setEnabled(true);
//    }
//
//    /*
//     * Speech recognition with data (for example from a file or audio source).
//     * The data is broken up into buffers and each buffer is sent to the Speech Recognition Service.
//     * No modification is done to the buffers, so the user can apply their
//     * own VAD (Voice Activation Detection) or Silence Detection
//     *
//     * @param dataClient
//     * @param recoMode
//     * @param filename
//     */
//    private class RecognitionTask extends AsyncTask<Void, Void, Void> {
//        DataRecognitionClient dataClient;
//        SpeechRecognitionMode recoMode;
//        String filename;
//
//        RecognitionTask(DataRecognitionClient dataClient, SpeechRecognitionMode recoMode, String filename) {
//            this.dataClient = dataClient;
//            this.recoMode = recoMode;
//            this.filename = filename;
//        }
//
//        @Override
//        protected Void doInBackground(Void... params) {
//            try {
//                // Note for wave files, we can just send data from the file right to the server.
//                // In the case you are not an audio file in wave format, and instead you have just
//                // raw data (for example audio coming over bluetooth), then before sending up any
//                // audio data, you must first send up an SpeechAudioFormat descriptor to describe
//                // the layout and format of your raw audio data via DataRecognitionClient's sendAudioFormat() method.
//                // String filename = recoMode == SpeechRecognitionMode.ShortPhrase ? "whatstheweatherlike.wav" : "batman.wav";
//                InputStream fileStream = getAssets().open(filename);
//                int bytesRead = 0;
//                byte[] buffer = new byte[1024];
//
//                do {
//                    // Get  Audio data to send into byte buffer.
//                    bytesRead = fileStream.read(buffer);
//
//                    if (bytesRead > -1) {
//                        // Send of audio data to service.
//                        dataClient.sendAudio(buffer, bytesRead);
//                    }
//                } while (bytesRead > 0);
//
//            } catch (Throwable throwable) {
//                throwable.printStackTrace();
//            }
//            finally {
//                dataClient.endAudio();
//            }
//
//            return null;
//        }
//    }
}
