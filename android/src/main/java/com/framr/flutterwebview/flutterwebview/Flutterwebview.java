package com.framr.flutterwebview.flutterwebview;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.Network;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.WebResourceResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.EventSink;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import static io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import static io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.platform.PlatformView;

public class Flutterwebview implements PlatformView, MethodCallHandler  {

    static final String LOG_TAG = "Flutterwebview";
    private final WebView browser;

    private final MethodChannel methodChannel;
    private EventChannel.EventSink onLoadingComplete;
    private EventChannel.EventSink onRequest;

    Flutterwebview(Context context, BinaryMessenger messenger, int id, Object o) {

        HashMap<String, Object> options = new HashMap<>();
        if(o instanceof HashMap) {
            options = (HashMap<String, Object>) o;
        }

        if(options.containsKey("debugging") && options.get("debugging").toString() == "true") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
        }

        browser = new WebView(context);
        browser.setBackgroundColor(0x000000);

        // Enable javascript
        browser.getSettings().setJavaScriptEnabled(true);
        browser.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        //browser.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        browser.getSettings().setJavaScriptEnabled(true);
        browser.getSettings().setSupportZoom(false);
        browser.getSettings().setLoadsImagesAutomatically(true);
        browser.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            browser.getSettings().setAllowFileAccess(true);
            browser.getSettings().setAllowContentAccess(true);
            browser.getSettings().setAppCacheEnabled(true);
            browser.getSettings().setAppCachePath(browser.getContext().getFilesDir().getPath() + "/cache/");
            browser.getSettings().setBuiltInZoomControls(false);
            browser.getSettings().setBlockNetworkLoads(false);
            browser.getSettings().setGeolocationEnabled(true);
            browser.getSettings().setDatabaseEnabled(true);
            browser.getSettings().setDatabasePath(browser.getContext().getFilesDir().getPath() + "/databases/");
            browser.getSettings().setDomStorageEnabled(true);
            browser.getSettings().setPluginState(WebSettings.PluginState.ON);
        }


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            browser.getSettings().setMediaPlaybackRequiresUserGesture(false);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // browser.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
            browser.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        } else {
            try {
                Method m = WebSettings.class.getMethod("setMixedContentMode", int.class);
                if ( m == null ) {
                    Log.e("WebSettings", "Error getting setMixedContentMode method");
                }
                else {
                    m.invoke(browser.getSettings(), 2); // 2 = MIXED_CONTENT_COMPATIBILITY_MODE
                    Log.i("WebSettings", "Successfully set MIXED_CONTENT_COMPATIBILITY_MODE");
                }
            }
            catch (Exception ex) {
                Log.e("WebSettings", "Error calling setMixedContentMode: " + ex.getMessage(), ex);
            }
        }

        browser.setAnimationCacheEnabled(true);

        // Log.d(LOG_TAG, o.getClass().toString());
        // Log.d(LOG_TAG, "Gooooo: "+o["javascriptInterface"]);
        if(options.containsKey("javascriptInterface") && options.get("javascriptInterface") instanceof String) {
            // Log.d(LOG_TAG, "Added JS interface: "+options.get("javascriptInterface").toString());
            browser.addJavascriptInterface(this, options.get("javascriptInterface").toString()); // "androidFramrTV"
        }

        // Set WebView client
        browser.setWebChromeClient(new WebChromeClient());
        browser.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                URI uri = URI.create(url);
                if(uri.getScheme() == "wss") {
                    return super.shouldInterceptRequest(view, url);
                }

                if(onRequest != null) {
                    onRequest.success(url);
                }

                final String[] _success = new String[1];
                _success[0] = null;
                final boolean[] _error = new boolean[1];
                _error[0] = false;

                // Log.d(LOG_TAG, new Date().toString());

                if(methodChannel != null) {
                    methodChannel.invokeMethod("onRequest", url, new MethodChannel.Result() {
                        @Override
                        public void success(Object o) {
                            _success[0] = o.toString();
                        }

                        @Override
                        public void error(String s, String s1, Object o) {
                            _error[0] = true;
                        }

                        @Override
                        public void notImplemented() {
                            _error[0] = true;
                        }
                    });

                    while(_success[0] == null && _error[0] == false) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ex) {}
                    }
                }

                // Log.d(LOG_TAG, new Date().toString());

                if(_error[0]) {
                    return super.shouldInterceptRequest(view, url);
                }

                if(_success[0] == "false") {
                    return null;
                }

                if(_success[0] != "") {
                    try {
                        JSONObject jsonObject = new JSONObject(_success[0]);

                        String mimeType = "text/plain";
                        if(jsonObject.has("mimeType")) {
                            mimeType = jsonObject.getString("mimeType");
                        }

                        String path = "";
                        if(jsonObject.has("path") && jsonObject.getString("path") != null) {
                            Log.w(LOG_TAG, "Path interception on "+url);
                            path = jsonObject.getString("path");

                            File f = new File(path);
                            if(!f.exists()) {
                                Log.e(LOG_TAG, "No resource loaded.");
                                return null;
                            }

                            FileInputStream fs;
                            try {
                                fs = new FileInputStream(f);
                            } catch(Exception e) {
                                Log.e(LOG_TAG, "Could not open resource.");
                                return null;
                            }

                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                return new WebResourceResponse(mimeType, "UTF-8", new BufferedInputStream(fs));
                            }
                        }

                        String content = "";
                        if(jsonObject.has("content")) {
                            content = jsonObject.getString("content");
                        }

                        Log.i(LOG_TAG, mimeType);
                        Log.i(LOG_TAG, content);
                        Log.i(LOG_TAG, jsonObject.toString());

                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            return new WebResourceResponse(mimeType,
                                    "UTF-8",
                                    new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                        }
                        else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            return new WebResourceResponse(mimeType,
                                    "UTF-8",
                                    new ByteArrayInputStream(content.getBytes()));
                        }

                    } catch (JSONException e) {
                        return null;
                    }
                }

                return super.shouldInterceptRequest(view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if(onLoadingComplete != null) {
                    onLoadingComplete.success(url);
                }

                super.onPageFinished(view, url);
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
            }
        });

        methodChannel = new MethodChannel(messenger, "com.framr.flutterwebview.flutterwebview/flutterwebview_" + id);
        methodChannel.setMethodCallHandler(this);

        new EventChannel(messenger, "com.framr.flutterwebview.flutterwebview/flutterwebview_loadingComplete_" + id)
                    .setStreamHandler(new EventChannel.StreamHandler() {
                @Override
                public void onListen(Object o, EventChannel.EventSink eventSink) {
                    onLoadingComplete = eventSink;
                }

                @Override
                public void onCancel(Object o) {
                    if(onLoadingComplete != null) {
                        onLoadingComplete.endOfStream();
                        onLoadingComplete = null;
                    }
                }
            });

        new EventChannel(messenger, "com.framr.flutterwebview.flutterwebview/flutterwebview_onRequest_" + id)
                .setStreamHandler(new EventChannel.StreamHandler() {
                    @Override
                    public void onListen(Object o, EventChannel.EventSink eventSink) {
                        onRequest = eventSink;
                    }

                    @Override
                    public void onCancel(Object o) {
                        if(onRequest != null) {
                            onRequest.endOfStream();
                            onRequest = null;
                        }
                    }
                });

    }

    @Override
    public View getView() {
        return browser;
    }

    @Override
    public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {
        switch (methodCall.method) {
            case "loadUrl":
                loadUrl(methodCall, result);
                break;
            default:
                result.notImplemented();
        }

    }

    private void loadUrl(MethodCall methodCall, Result result) {
        String url = (String) methodCall.arguments;
        browser.loadUrl(url);
        result.success(null);
    }

    @Override
    public void dispose() {
        browser.destroy();
    }

    private Activity getActivity() {
        Context context = browser.getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity)context;
            }
            context = ((ContextWrapper)context).getBaseContext();
        }
        return null;
    }

    @JavascriptInterface
    public void playJSVideo() {
        browser.post(new Runnable() {
            @Override
            public void run() {
                browser.loadUrl("javascript: $(document).find('video').each(function() { $(this).get(0).play(); });");
            }
        });

    }

    @JavascriptInterface
    public void playJSAudio() {
        browser.post(new Runnable() {
            @Override
            public void run() {
                browser.loadUrl("javascript: $(document).find('audio').each(function() { $(this).get(0).play(); });");
            }
        });

    }
}
