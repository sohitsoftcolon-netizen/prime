package com.primeshows.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import java.io.ByteArrayInputStream;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

public class MainActivity extends Activity {

    // >>> Change this if your website address changes <<<
    private static final String HOME_URL = "https://primeshows.org/";
    private static final String HOME_HOST = "primeshows.org";

    // Ad / popup / tracker domains that are blocked inside the app.
    // Add more domains here if you still see ads from a particular network.
    private static final String[] BLOCKED_HOSTS = {
            "doubleclick.net", "googlesyndication.com", "googleadservices.com",
            "adservice.google.com", "google-analytics.com", "googletagmanager.com",
            "adsterra.com", "highperformanceformat.com", "highrevenuenetwork.com",
            "highrevenuegate.com", "effectivegatecpm.com", "profitablecpmrate.com",
            "profitablegatecpm.com", "popads.net", "popcash.net", "propellerads.com",
            "propellerclick.com", "onclickads.net", "onclkds.com", "clickadu.com",
            "exoclick.com", "exosrv.com", "juicyads.com", "trafficjunky.net",
            "hilltopads.net", "hilltopads.com", "adcash.com", "adnxs.com",
            "taboola.com", "outbrain.com", "revcontent.com", "mgid.com",
            "adskeeper.com", "adsco.re", "a-ads.com", "ad-maven.com", "admaven.com",
            "monetag.com", "richpartners.com", "pushwhy.com", "notifpush.com",
            "histats.com", "yllix.com", "bidvertiser.com", "ero-advertising.com",
            "mc.yandex.ru", "ads.pubmatic.com", "criteo.com", "amazon-adsystem.com",
            "serving-sys.com", "popunderjs.com", "tsyndicate.com", "realsrv.com",
            "syndication.exoclick.com", "o333o.com", "dolohen.com", "acscdn.com"
    };

    private static boolean isBlockedHost(String host) {
        if (host == null) return false;
        for (String b : BLOCKED_HOSTS) {
            if (host.equals(b) || host.endsWith("." + b)) return true;
        }
        return false;
    }

    // Hides common ad containers on the page
    private static final String HIDE_ADS_JS =
            "(function(){var css='[id*=\"ad-\" i],[id^=\"ads\" i],[class*=\"adsbygoogle\" i],"
                    + "[class*=\"ad-banner\" i],[class*=\"ad-container\" i],[class*=\"popup-ad\" i],"
                    + "ins.adsbygoogle,iframe[src*=\"ads\" i],iframe[src*=\"doubleclick\" i]"
                    + "{display:none!important;visibility:hidden!important}';"
                    + "var s=document.createElement('style');s.innerHTML=css;"
                    + "document.head&&document.head.appendChild(s);})();";

    private WebView webView;
    private ProgressBar progressBar;
    private FrameLayout fullscreenContainer;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progress);
        fullscreenContainer = findViewById(R.id.fullscreen_container);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);

        // Needed for D-pad / remote control navigation on TV
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.requestFocus();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                String host = uri.getHost();
                if (("http".equals(scheme) || "https".equals(scheme)) && host != null
                        && (host.equals(HOME_HOST) || host.endsWith("." + HOME_HOST))) {
                    return false; // stay inside the app
                }
                // Block automatic redirects/popups to other sites (usually ads).
                // Only open external links when the user actually tapped them.
                if (request.hasGesture() && !isBlockedHost(host)) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (Exception ignored) {
                    }
                }
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (isBlockedHost(request.getUrl().getHost())) {
                    return new WebResourceResponse("text/plain", "UTF-8",
                            new ByteArrayInputStream(new byte[0]));
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                view.evaluateJavascript(HIDE_ADS_JS, null);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    String html = "<html><body style='background:#0b0b10;color:#fff;font-family:sans-serif;"
                            + "display:flex;flex-direction:column;align-items:center;justify-content:center;"
                            + "height:100vh;margin:0;text-align:center'>"
                            + "<h2>No internet connection</h2>"
                            + "<p>Please check your connection and try again.</p>"
                            + "<a href='" + HOME_URL + "' style='background:#e50914;color:#fff;padding:14px 28px;"
                            + "border-radius:8px;text-decoration:none;font-size:20px'>Retry</a>"
                            + "</body></html>";
                    view.loadDataWithBaseURL(HOME_URL, html, "text/html", "UTF-8", null);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }

            // Fullscreen video support
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                fullscreenContainer.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                fullscreenContainer.setVisibility(View.VISIBLE);
                webView.setVisibility(View.GONE);
                enterImmersive();
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                fullscreenContainer.removeView(customView);
                fullscreenContainer.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                customView = null;
                if (customViewCallback != null) customViewCallback.onCustomViewHidden();
                customViewCallback = null;
                webView.requestFocus();
            }
        });

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(HOME_URL);
        }
        enterImmersive();
    }

    private void enterImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enterImmersive();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (customView != null) {
                webView.getWebChromeClient().onHideCustomView();
                return true;
            }
            if (webView.canGoBack()) {
                webView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
