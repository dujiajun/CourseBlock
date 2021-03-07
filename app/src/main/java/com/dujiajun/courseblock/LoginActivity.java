package com.dujiajun.courseblock;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.dujiajun.courseblock.helper.CourseManager;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        WebView webView = findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                if (url.contains("index_initMenu.html") || url.contains("http://yjs.sjtu.edu.cn:81")) {
                    Toast.makeText(LoginActivity.this, R.string.already_logged_in, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
        CourseManager courseManager = CourseManager.getInstance(getApplicationContext());
        String loginUrl = courseManager.getLoginUrl();
        webView.loadUrl(loginUrl);
    }
}
