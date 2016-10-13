package com.gmail.jorgegilcavazos.ballislife.View.Activities;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.gmail.jorgegilcavazos.ballislife.R;
import com.gmail.jorgegilcavazos.ballislife.Utils.MyDebug;
import com.gmail.jorgegilcavazos.ballislife.Utils.RedditAuthentication;

import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.http.oauth.OAuthHelper;

import java.net.URL;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Toolbar toolbar = (Toolbar) findViewById(R.id.login_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setTitle("Log in to Reddit");

        final OAuthHelper oAuthHelper = RedditAuthentication.redditClient.getOAuthHelper();
        final Credentials credentials = Credentials.installedApp(RedditAuthentication.CLIENT_ID,
                RedditAuthentication.REDIRECT_URL);
        String[] scopes = {"identity", "edit", "flair", "mysubreddits", "read", "vote",
                "submit", "subscribe"};
        URL authURL = oAuthHelper.getAuthorizationUrl(credentials, true, true, scopes);

        final WebView webView = (WebView) findViewById(R.id.login_webview);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (url.contains("code=")) {
                    webView.stopLoading();
                    new AuthenticateTask(oAuthHelper, credentials).execute(url);
                    finish();
                }
            }
        });
        webView.loadUrl(authURL.toExternalForm());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class AuthenticateTask extends AsyncTask<String, Void, Void> {
        private OAuthHelper mOAuthHelper;
        private Credentials mCredentials;

        public AuthenticateTask(OAuthHelper oAuthHelper, Credentials credentials) {
            mOAuthHelper = oAuthHelper;
            mCredentials = credentials;
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                OAuthData oAuthData = mOAuthHelper.onUserChallenge(params[0], mCredentials);
                if (oAuthData != null) {
                    RedditAuthentication.redditClient.authenticate(oAuthData);
                    RedditAuthentication.isLoggedIn = true;
                    RedditAuthentication.isAuthenticated = true;

                    String refreshToken = RedditAuthentication.redditClient.getOAuthData()
                            .getRefreshToken();
                    String username = RedditAuthentication.redditClient.me().getFullName();

                    if (RedditAuthentication.redditClient.isAuthenticated()) {
                        SharedPreferences preferences = getSharedPreferences(
                                MainActivity.MY_PREFERENCES, MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString(RedditAuthentication.REDDIT_USERNAME_KEY, username);
                        editor.putString(RedditAuthentication.REDDIT_TOKEN_KEY, refreshToken);
                        editor.apply();
                    }
                    return null;
                } else {
                    if (MyDebug.LOG) {
                        Log.i(TAG, "OAuthData returned null.");
                    }
                }
            } catch (OAuthException e) {
                if (MyDebug.LOG) {
                    Log.e(TAG, "Could not get OAuthData. ", e);
                }
            }
            return null;
        }
    }

}