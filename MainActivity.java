package com.fungame.counter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "FunGameDevicePrefs";
    private static final String KEY_MODE = "device_mode"; // "tv" or "counter"
    private static final String KEY_PORTAL_URL = "portal_url";

    // Replace with your hosted Admin/Portal URL or load local assets/app.html
    private static final String DEFAULT_URL = "file:///android_asset/app.html";

    private WebView webView;
    private TextView modeBadge;
    private SharedPreferences prefs;

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUI();

        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        webView = findViewById(R.id.gameWebView);
        modeBadge = findViewById(R.id.modeBadge);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                applyDeviceModeToPage();
            }
        });

        webView.addJavascriptInterface(new NativeBridge(this), "AndroidNative");

        // Long-press on bottom badge lets Admin switch between TV Screen and Counter PC mode
        modeBadge.setOnLongClickListener(v -> {
            showDeviceModeDialog();
            return true;
        });

        String startUrl = prefs.getString(KEY_PORTAL_URL, DEFAULT_URL);
        webView.loadUrl(startUrl);
    }

    private String getCurrentMode() {
        return prefs.getString(KEY_MODE, "tv");
    }

    private void applyDeviceModeToPage() {
        String mode = getCurrentMode();
        runOnUiThread(() -> {
            modeBadge.setText("counter".equals(mode) ? "COUNTER PC" : "TV SCREEN");
            String js = "if (window.setDeviceScreenMode) { window.setDeviceScreenMode('" + mode + "'); }";
            webView.evaluateJavascript(js, null);
        });
    }

    private void showDeviceModeDialog() {
        String[] options = {
            "Switch to TV Display Screen (TV Mode)",
            "Switch to Customer Counter PC Screen (Counter Mode)",
            "Reload Live Portal"
        };
        new AlertDialog.Builder(this)
            .setTitle("APK Screen Mode")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    prefs.edit().putString(KEY_MODE, "tv").apply();
                    applyDeviceModeToPage();
                } else if (which == 1) {
                    prefs.edit().putString(KEY_MODE, "counter").apply();
                    applyDeviceModeToPage();
                } else if (which == 2) {
                    webView.reload();
                }
            })
            .show();
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    public void triggerNativePrint(String jobName) {
        runOnUiThread(() -> {
            PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
            if (printManager != null) {
                PrintDocumentAdapter adapter = webView.createPrintDocumentAdapter(jobName);
                printManager.print(jobName, adapter, new PrintAttributes.Builder().build());
            }
        });
    }

    public class NativeBridge {
        private final Context ctx;
        NativeBridge(Context ctx) { this.ctx = ctx; }

        @JavascriptInterface
        public String getDeviceMode() {
            return getCurrentMode();
        }

        @JavascriptInterface
        public void setDeviceMode(String mode) {
            if ("tv".equals(mode) || "counter".equals(mode)) {
                prefs.edit().putString(KEY_MODE, mode).apply();
                applyDeviceModeToPage();
            }
        }

        @JavascriptInterface
        public void printSlip(String slipId) {
            triggerNativePrint("Slip_" + (slipId != null ? slipId : "Receipt"));
        }
    }
}
