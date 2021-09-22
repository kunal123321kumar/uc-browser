package com.uc.browser.india;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.support.v7.app.AppCompatActivity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author true_ravi(studynotes.xyz)
 * <p>
 * Reference：
 * - https://www.zhihu.com/question/313166461
 * - https://github.com/Justson/AgentWeb1
 * - https://juejin.im/post/58a037df86b599006b3fade41
 * - Video playback problem https://www.jianshu.com/p/d6d379e3f41d1
 */
public class WebActivity extends AppCompatActivity implements View.OnClickListener {

    private WebView webView;
    private ProgressBar progressBar;
    private EditText textUrl;
    private ImageView webIcon, goBack, goForward, navSet, goHome, btnStart;

    private long exitTime = 0;

    private Context mContext;
    private InputMethodManager manager;

    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static final int PRESS_BACK_EXIT_GAP = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Prevent the bottom button from moving up
        getWindow().setSoftInputMode
                (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN |
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        setContentView(R.layout.activity_web);

        mContext = WebActivity.this;
        manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        // Bound control
        initView();

        // initialization WebView
        initWeb();
    }

    /**
     * Bound control
     */
    private void initView() {
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        textUrl = findViewById(R.id.textUrl);
        webIcon = findViewById(R.id.webIcon);
        btnStart = findViewById(R.id.btnStart);
        goBack = findViewById(R.id.goBack);
        goForward = findViewById(R.id.goForward);
        navSet = findViewById(R.id.navSet);
        goHome = findViewById(R.id.goHome);

        // Bind button click event
        btnStart.setOnClickListener(this);
        goBack.setOnClickListener(this);
        goForward.setOnClickListener(this);
        navSet.setOnClickListener(this);
        goHome.setOnClickListener(this);

        // Address input field acquisition and loss of focus processing
        textUrl.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    // Show current URL link TODO:Search page displays search terms
                    textUrl.setText(webView.getUrl());
                    // Cursor at the end
                    textUrl.setSelection(textUrl.getText().length());
                    // Show internet icon
                    webIcon.setImageResource(R.drawable.internet);
                    // Show jump button
                    btnStart.setImageResource(R.drawable.go);
                } else {
                    // Show website name
                    textUrl.setText(webView.getTitle());
                    // Show website icon
                    webIcon.setImageBitmap(webView.getFavicon());
                    // Show refresh button
                    btnStart.setImageResource(R.drawable.refresh);
                }
            }
        });

        // Monitor keyboard enter search
        textUrl.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    // Perform a search
                    btnStart.callOnClick();
                    textUrl.clearFocus();
                }
                return false;
            }
        });
    }


    /**
     * initialization web
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void initWeb() {
        // Rewrite WebViewClient
        webView.setWebViewClient(new MkWebViewClient());
        // Rewrite WebChromeClient
        webView.setWebChromeClient(new MkWebChromeClient());

        WebSettings settings = webView.getSettings();
        // Enable js function
        settings.setJavaScriptEnabled(true);
        // Set up the browser UserAgent
        settings.setUserAgentString(settings.getUserAgentString() + " Uc Browser/" + getVerName(mContext));

        // Adjust the picture to fit WebView the size of
        settings.setUseWideViewPort(true);
        // Zoom to the size of the screen
        settings.setLoadWithOverviewMode(true);

        // Support zoom, the default is true. Is the premise of the following。
        settings.setSupportZoom(true);
        // Set the built-in zoom control。If false，Then the WebView Non-scalable
        settings.setBuiltInZoomControls(true);
        // Hide native zoom controls
        settings.setDisplayZoomControls(false);

        // Cache
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        // Settings can access files
        settings.setAllowFileAccess(true);
        // Support opening new windows via JS
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        // Support automatic loading of pictures
        settings.setLoadsImagesAutomatically(true);
        // Set the default encoding format
        settings.setDefaultTextEncodingName("utf-8");
        // Local storage
        settings.setDomStorageEnabled(true);
        settings.setPluginState(WebSettings.PluginState.ON);

        // Resource hybrid mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // Load home page
        webView.loadUrl(getResources().getString(R.string.home_url));
    }


    /**
     * Rewrite WebViewClient
     */
    private class MkWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Set the new webpage opened by clicking on the webView to be displayed on the current interface without jumping to the new browser

            if (url == null) {
                // Return true to handle by yourself, return false to not handle
                return true;
            }

            // Normal content, open
            if (url.startsWith(HTTP) || url.startsWith(HTTPS)) {
                view.loadUrl(url);
                return true;
            }

            // Call a third-party application to prevent crashes (if there is no APP that handles the URL at the beginning of a scheme is installed on the phone, it will cause a crash)
            try {
                // TODO:A pop-up window prompts the user to call after permission
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            } catch (Exception e) {
                return true;
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            // The web page starts to load and a progress bar is displayed
            progressBar.setProgress(0);
            progressBar.setVisibility(View.VISIBLE);

            // Update status text
            textUrl.setText("Loading...");

            // Switch the default web icon
            webIcon.setImageResource(R.drawable.internet);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            // When the page is loaded, hide the progress bar
            progressBar.setVisibility(View.INVISIBLE);

            // Change title
            setTitle(webView.getTitle());
            // Show page title
            textUrl.setText(webView.getTitle());
        }
    }


    /**
     * Rewrite WebChromeClient
     */
    private class MkWebChromeClient extends WebChromeClient {
        private final static int WEB_PROGRESS_MAX = 100;

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);

            // Loading progress changes, refresh the progress bar
            progressBar.setProgress(newProgress);
            if (newProgress > 0) {
                if (newProgress == WEB_PROGRESS_MAX) {
                    progressBar.setVisibility(View.INVISIBLE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            super.onReceivedIcon(view, icon);

            // Change icon
            webIcon.setImageBitmap(icon);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);

            // Change title
            setTitle(title);
            // Show page title
            textUrl.setText(title);
        }
    }

    /**
     * Back button processing
     */
    @Override
    public void onBackPressed() {
        // Return to the previous page if able to go back
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            if ((System.currentTimeMillis() - exitTime) > PRESS_BACK_EXIT_GAP) {
                // Click twice to exit the program
                Toast.makeText(mContext, "Press again to exit the program",
                        Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                super.onBackPressed();
            }

        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            // Jump or refresh
            case R.id.btnStart:
                if (textUrl.hasFocus()) {
                    // Hide soft keyboard
                    if (manager.isActive()) {
                        manager.hideSoftInputFromWindow(textUrl.getApplicationWindowToken(), 0);
                    }

                    // The address bar has focus, it is a jump
                    String input = textUrl.getText().toString();
                    if (!isHttpUrl(input)) {
                        // Not a URL, load search engine processing
                        try {
                            // URL coding
                            input = URLEncoder.encode(input, "utf-8");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        input = "https://www.google.com/search?q=" + input + "&ie=UTF-8";
                    }
                    webView.loadUrl(input);

                    // Cancel the focus of the address bar
                    textUrl.clearFocus();
                } else {
                    // The address bar has no focus, it is refreshed
                    webView.reload();
                }
                break;

            // Back
            case R.id.goBack:
                webView.goBack();
                Toast.makeText(mContext, "GO BACK", Toast.LENGTH_SHORT).show();
                break;

            // go ahead
            case R.id.goForward:
                webView.goForward();
                Toast.makeText(mContext, "GO FORWARD", Toast.LENGTH_SHORT).show();
                break;

            // 设置
            case R.id.navSet:
                Toast.makeText(mContext, "MENU", Toast.LENGTH_SHORT).show();
                break;

            // Home page
            case R.id.goHome:
                webView.loadUrl(getResources().getString(R.string.home_url));
                Toast.makeText(mContext, "HOME", Toast.LENGTH_SHORT).show();
                break;

            default:
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            webView.getClass().getMethod("onPause").invoke(webView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            webView.getClass().getMethod("onResume").invoke(webView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Determine whether the string isURL（https://blog.csdn.net/bronna/article/details/77529145）
     *
     * @param urls String to be evaluated
     * @return true:YesURL、false:noURL
     */
    public static boolean isHttpUrl(String urls) {
        boolean isUrl;
        // Determine whether it is a regular expression for the URL
        String regex = "(((https|http)?://)?([a-z0-9]+[.])|(browser.))"
                + "\\w+[.|\\/]([a-z0-9]{0,})?[[.]([a-z0-9]{0,})]+((/[\\S&&[^,;\u4E00-\u9FA5]]+)+)?([.][a-z0-9]{0,}+|/?)";

        Pattern pat = Pattern.compile(regex.trim());
        Matcher mat = pat.matcher(urls.trim());
        isUrl = mat.matches();
        return isUrl;
    }

    /**
     * Get the version number name
     *
     * @param context Context
     * @return Current version name
     */
    private static String getVerName(Context context) {
        String verName = "unKnow";
        try {
            verName = context.getPackageManager().
                    getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return verName;
    }
}
