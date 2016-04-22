package com.tk_squared.jive;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by Tim on 1/4/2016.
 * 'Cuz Tim rocks.
 */

@SuppressLint("SetJavaScriptEnabled")
public class TkkWebViewFragment extends Fragment{

    //region Description: Variables and Constructor and Callbacks
    private WebView webview; public WebView getWebview(){ return webview;}
    private String currentUrl;
    private String currentName; public String getCurrentName() {return currentName;}
    private Integer currentIndex;

    public interface Callbacks {
        void onIconReceived(Integer index, Bitmap icon);
    }
    public TkkWebViewFragment(){}

    //endregion

    //region Description: Lifecycle and Super Overrides
    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        //Make a toolbar
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.webview_toolbar);
        toolbar.setSubtitle(R.string.subtitle);
        AppCompatActivity activity = (AppCompatActivity)getActivity();
        activity.setSupportActionBar(toolbar);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_webview, container, false);
    }

    @Override
    public void onResume(){
        super.onResume();
        setupWebView();
    }

    @Override
    public void onDestroy(){
        webview.clearCache(true);
        super.onDestroy();
    }
    //endregion

    //region Description: private methods for setups
    private void setupWebView(){
        //Setup the WebView
        if (webview == null) {
            webview = (WebView) getActivity().findViewById(R.id.webview_view);
            webview.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onReceivedIcon(WebView webView, Bitmap icon) {
                    ((TkkWebViewFragment.Callbacks) getActivity()).onIconReceived(currentIndex, icon);
                }
            });
            webview.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.loadUrl(url);
                    return false;
                }
            });
            webview.getSettings().setJavaScriptEnabled(true);

            currentName = getArguments().getString("name");
            currentUrl = getArguments().getString("uri");
            currentIndex = getArguments().getInt("index");

            webview.loadUrl(currentUrl);
        }
    }
    //endregion
}