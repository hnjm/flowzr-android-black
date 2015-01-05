package com.flowzr.activity;

import com.flowzr.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebViewFragment extends Fragment {
	private String curURL;

    public void init(String url) {
        curURL = url;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container,

            Bundle savedInstanceState) {

        View view = inflater
                .inflate(R.layout.webview, container, false);

        if (curURL != null) {

            WebView webview = (WebView) view.findViewById(R.id.webPage);

            webview.getSettings().setJavaScriptEnabled(true);

            webview.setWebViewClient(new webClient());

            webview.loadUrl(curURL);

        }

        return view;

    }

    public void updateUrl(String url) {
        curURL = url;
        WebView webview = (WebView) getView().findViewById(R.id.webPage);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.loadUrl(url);
    }

    private class webClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            return false;

        }

    }

}
