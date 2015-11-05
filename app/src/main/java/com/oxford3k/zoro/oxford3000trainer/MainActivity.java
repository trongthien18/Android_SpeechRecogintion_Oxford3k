package com.oxford3k.zoro.oxford3000trainer;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

import static android.widget.Toast.makeText;
import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, RecognitionListener {

    private SpeechRecognizer recognizer;

    private WebView mWebView;
    private ProgressDialog mProgressDialog;
    private TextView mTxtWord;
    private TextView mTxtStatus;
    private ImageButton mButton;

    private String mCurrentWord;

    private ArrayList<String> dictionary;
    private ArrayList<String> favorite;

    private static final int REQUEST_CODE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        favorite = FavoriteListManager.GetInstance().Load(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (favorite.indexOf(mCurrentWord) == -1) {
                    favorite.add(mCurrentWord);
                    FavoriteListManager.GetInstance().Save(getApplicationContext(), favorite);
                }
                
                Snackbar.make(view, "Added in your favorite list", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mTxtWord = (TextView) findViewById(R.id.txtWord);
        mTxtStatus = (TextView) findViewById(R.id.txtStatus);

        mButton = (ImageButton)findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recognizer.startListening("ok", 5000);
                mTxtStatus.setText("Listening...");
                mTxtStatus.setTextColor(Color.BLACK);
            }
        });
        mButton.setVisibility(View.INVISIBLE);

        Button nextWord = (Button) findViewById(R.id.button2);
        nextWord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.loadUrl("about:blank");
                RandomNewWord();
                Reset();
            }
        });

        mProgressDialog = new ProgressDialog(this);

        mWebView = (WebView)findViewById(R.id.webView);
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        mWebView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);

        //mProgressDialog = ProgressDialog.show(this, "Oxford", "Loading...");

        mWebView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            public void onPageFinished(WebView view, String url) {
                if (url.equals("about:blank"))
                    return;

                if (mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
                view.scrollTo(0, (int)(view.getHeight() * 0.7));
            }

            public void onLoadResource(WebView view, String url) {
                if (url.equals("about:blank"))
                    return;
                view.scrollTo(0, (int)(view.getHeight() * 0.7));
            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(view.getContext(), "Oh no! " + description, Toast.LENGTH_SHORT).show();
                view.setVisibility(View.INVISIBLE);
            }
        });

        //ReloadNewWord("girl");
        //new LoadData("http://www.oxfordlearnersdictionaries.com/definition/english/written").execute();

        dictionary = new ArrayList<>();

        try {
            InputStream in = this.getAssets().open("Oxford.txt");

            if (in != null) {
                // prepare the file for reading
                InputStreamReader input = new InputStreamReader(in);
                BufferedReader br = new BufferedReader(input);
                String line = br.readLine();
                while ((line = br.readLine()) != null)
                {
                    dictionary.add(line);
                }
                in.close();
            }else{
                System.out.println("It's the assests");
            }

        } catch (IOException e) {
            System.out.println("Couldn't Read File Correctly");
        }

        RandomNewWord();

        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task

        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(MainActivity.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {

                } else {
                    Reset();
                    mButton.setVisibility(View.VISIBLE);
                    mTxtStatus.setText("Tap the button and speak that word!");
                }
            }
        }.execute();
    }

    private void ReloadNewWord(String word) {
        mWebView.loadUrl("http://www.oxfordlearnersdictionaries.com/definition/english/" + word);

        word = word.toUpperCase();
        word = word.replace('-', ' ');
        mCurrentWord = word;
        mTxtWord.setText(word);
    }

    private void RandomNewWord()
    {
        Random rand = new Random();
        int n = rand.nextInt(dictionary.size());

        String word = dictionary.get(n);

        ReloadNewWord(word);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nv_fav) {
            Intent i = new Intent(this, FavoriteList.class);
            startActivityForResult(i, REQUEST_CODE);
        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {
            Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
            emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            emailIntent.setType("vnd.android.cursor.item/email");
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] {"trongthien18@gmail.com"});
            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "I love you :D");
            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "I love you so much :v");
            startActivity(Intent.createChooser(emailIntent, "Send mail using..."));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==REQUEST_CODE)
        {
            if (resultCode == 100) {
                String word = data.getStringExtra("word");
                word = word.toLowerCase();
                word.trim();
                ReloadNewWord(word);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        recognizer.cancel();
        recognizer.shutdown();
    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onEndOfSpeech() {
        Reset();
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        String[] tmp = text.split(" ");
        for (int i = 0; i < tmp.length; i++) {
            if (tmp[i].equals(mCurrentWord)) {
                Reset();
                mTxtStatus.setText("Correct");
                mTxtStatus.setTextColor(Color.GREEN);
                RandomNewWord();
            }
        }

        ((TextView) findViewById(R.id.result_text)).setText(text);
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        ((TextView) findViewById(R.id.result_text)).setText("");
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();

            String[] tmp = text.split(" ");
            for (int i = 0; i < tmp.length; i++) {
                if (tmp[i].equals(mCurrentWord)) {
                    Reset();
                    mTxtStatus.setText("Correct");
                    mTxtStatus.setTextColor(Color.GREEN);

                    RandomNewWord();
                }
            }

            makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onError(Exception e) {

    }

    @Override
    public void onTimeout() {
        Reset();
    }

    private void Reset() {
        recognizer.stop();
        mTxtStatus.setText("Tap the button and speak that word!");
        mTxtStatus.setTextColor(Color.BLACK);
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "oxford.dic"))

                        // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                .setRawLogDir(assetsDir)

                        // Threshold to tune for keyphrase to balance between false alarms and misses
                .setKeywordThreshold(1e-45f)

                        // Use context-independent phonetic search, context-dependent is too slow for mobile
                .setBoolean("-allphone_ci", true)

                .getRecognizer();
        recognizer.addListener(this);

        /** In your application you might not need to add all those searches.
         * They are added here for demonstration. You can leave just one.
         */

        // Create keyword-activation search.
        //recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);


        // Create language model search
        File languageModel = new File(assetsDir, "oxford.lm");
        recognizer.addNgramSearch("ok", languageModel);

    }
}
