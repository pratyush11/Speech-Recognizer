package com.example.pratyush.speechrecognizer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Build;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by pratyush on 16/4/18.
 */

public class MainActivity extends AppCompatActivity {

    private TextView input;
    private Button shareButton;
    private DatabaseHelper mDBHelper;
    private SQLiteDatabase mDb;
    private SpeechRecognizer sr;
    private static final String TAG = "MainActivity";
    private Uri altDynamicLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        shareButton = findViewById(R.id.shareButton);
        final String shareText = input.getText().toString();

        Uri.Builder builder = new Uri.Builder()
                .scheme("https")
                .authority("mn798.app.goo.gl")
                .path("/")
                .appendQueryParameter("link", "https://barsys.io")
                .appendQueryParameter("apn", getApplicationContext().getPackageName())
                .appendQueryParameter("shareText", shareText);

        altDynamicLink = builder.build();

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

//                DynamicLink dynamicLink = FirebaseDynamicLinks.getInstance().createDynamicLink()
//                        .setLink(Uri.parse("https://barsys.io"))
//                        .setDynamicLinkDomain("mn798.app.goo.gl")
//                        //.setAndroidParameters(new DynamicLink.AndroidParameters().Builder().build())
//                        //.setIosParameters(new DynamicLink.IosParameters().Builder("com.llc.barsys").build())
//                        .buildDynamicLink();

                shareLink();

            }
        });

        // ATTENTION: This was auto-generated to handle app links.
        Intent appLinkIntent = getIntent();
        if (appLinkIntent.getType().contains("image/")) {
            //TODO: Handle image data
        } else if(appLinkIntent.getType().equals("text/plain")) {
            //TODO: Update view
        }
        String appLinkAction = appLinkIntent.getAction();
        Uri appLinkData = appLinkIntent.getData();

        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnSuccessListener(this, new OnSuccessListener<PendingDynamicLinkData>() {
                    @Override
                    public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
                        Uri deepLink = null;
                        if (pendingDynamicLinkData != null) {
                            deepLink = pendingDynamicLinkData.getLink();

                            input.setText(deepLink.getQueryParameter("shareText"));

                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "dynamicLink OnFailure: ", e);
                    }
                });

        requestRecordAudioPermission();

        Button detect = findViewById(R.id.detectButton);
        input = findViewById(R.id.input);
        mDBHelper = new DatabaseHelper(this);

        //Get writable database
        try {
            mDb = mDBHelper.getWritableDatabase();
        } catch (SQLException mSQLException) {
            Log.d(TAG, mSQLException.getLocalizedMessage());
        }

        //Cursor to get cocktail names
        Cursor resultSet = mDb.rawQuery("SELECT name FROM COCKTAIL;", null);
        resultSet.moveToFirst();
        final String[] names = new String[resultSet.getCount()];
        for (int i = 0; i < resultSet.getCount(); i++) {
            names[i] = resultSet.getString(0);
            Log.i(TAG, names[i]);
            resultSet.moveToNext();
        }

        detect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sr = SpeechRecognizer.createSpeechRecognizer(MainActivity.this);
                sr.setRecognitionListener(new RecognitionListener() {
                    @Override
                    public void onReadyForSpeech(Bundle bundle) {

                    }

                    @Override
                    public void onBeginningOfSpeech() {

                    }

                    @Override
                    public void onRmsChanged(float v) {

                    }

                    @Override
                    public void onBufferReceived(byte[] bytes) {

                    }

                    @Override
                    public void onEndOfSpeech() {

                    }

                    @Override
                    public void onError(int i) {
                        Log.d(TAG, "error " + i);
                    }

                    @Override
                    public void onResults(Bundle bundle) {
                        String str = new String();
                        Log.d(TAG, "onResults " + bundle);
                        ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        for (int i = 0; i < data.size(); i++) {
                            Log.d(TAG, "result " + data.get(i));
                            str += data.get(i);

                        }
                        input.setText("Top result: " + data.get(0));
                        List<Integer> dist = new ArrayList<Integer>();
//                        String text = "Capiroska Daiquiri Barbarella Bellini Kamikaze Caipiroska";
//                        String[] data1 = text.split(" ");
                        String keyword = data.get(0);
                        for (int i = 0; i < names.length; i++) {
                            dist.add(distance(names[i], keyword));
                        }
                        Collections.sort(dist);
                        Log.d(TAG, "Did you mean: ");
                        for (int i = 0; i < names.length; i++) {
                            if (distance(names[i], keyword) == dist.get(0)) {
                                Log.d(TAG, names[i] + " ");
                            }
                        }

                        //Regex to detect command phrase
                        for (String string : data) {
                            if (string.matches("make\\s*7\\s*and\\s*7\\s*")) {
                                Toast.makeText(MainActivity.this, "Make drink detected", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onPartialResults(Bundle bundle) {
                        String str = new String();
                        Log.d(TAG, "onPartialResults " + bundle);
                        ArrayList data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        for (int i = 0; i < data.size(); i++) {
                            Log.d(TAG, "result " + data.get(i));
                            str += data.get(i);
                        }
                        input.setText("Top partial result: " + data.get(0));
                    }

                    @Override
                    public void onEvent(int i, Bundle bundle) {

                    }
                });
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);      //Use LANGUAGE_MODEL_FREE_FORM for non-localized voice input
                intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.example.pratyush.speechrecognizer");

                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
                sr.startListening(intent);
            }
        });

    }

    public void shareLink() {
        try {
            URL url = new URL(URLDecoder.decode(altDynamicLink.toString(),
                    "UTF-8"));
            Log.i(TAG, "URL = " + url.toString());
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Firebase Deep Link");
            intent.putExtra(Intent.EXTRA_TEXT, url.toString());
            startActivity(intent);
        } catch (Exception e) {
            Log.i(TAG, "Could not decode Uri: " + e.getLocalizedMessage());
        }
    }

    private void requestRecordAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String requiredPermission = Manifest.permission.RECORD_AUDIO;
            if (checkCallingOrSelfPermission(requiredPermission) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(new String[]{requiredPermission}, 101);
            }
        }
    }

    public static int distance(String a, String b)
    {
        a = a.toLowerCase();
        b = b.toLowerCase();
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++)
            costs[j] = j;
        for (int i = 1; i <= a.length(); i++)
        {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++)
            {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }
}
