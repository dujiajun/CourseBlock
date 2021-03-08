package com.dujiajun.courseblock;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebSettings;
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
        WebSettings settings = webView.getSettings();
        settings.setUserAgentString("Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1 Edg/89.0.4389.72");
        settings.setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri uri = Uri.parse(url);
                try {
                    if ("jaccount".equals(uri.getScheme())) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                        return true;
                    }
                } catch (Exception e) {
                    return true;
                }
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
