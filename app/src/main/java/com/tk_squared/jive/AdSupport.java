package com.tk_squared.jive;

import android.util.Log;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.smaato.soma.AdDimension;
import com.smaato.soma.BannerView;
import com.smaato.soma.interstitial.Interstitial;
import com.smaato.soma.interstitial.InterstitialAdListener;


/**
 * Created by zengo on 4/19/2016.
 * This is the Smaato version of
 * this file!
 * Make sure you are using the
 * correct version!!
 */

class AdSupport implements InterstitialAdListener {

    private Interstitial interstitial;
    private final TkkActivity activity;

    public AdSupport(TkkActivity activity){
        this.activity = activity;
    }

    public void showInterstitial(){
        if(interstitial.isInterstitialReady()){
            interstitial.show();
        }
    }


    public void setupAdSupport(){
        //Set up interstitial ads
        setInterstitialAd();
        //Set up banner ads
        setupSmaato();
    }

    private void setInterstitialAd(){
        interstitial = new Interstitial(activity);
        interstitial.setInterstitialAdListener(this);
        interstitial.getAdSettings().setPublisherId(activity.getResources().getInteger(R.integer.smaato_pub_id));
        interstitial.getAdSettings().setAdspaceId(activity.getResources().getInteger(R.integer.smaato_inter_id));
    }

    private void interBannerLoad(){
        Runnable r = new Runnable(){
            @Override
            public void run(){
                interstitial.asyncLoadNewBanner();
                Log.i("SOME AdSupport", "Loading new interstitual");
            }
        };
        activity.getHandler().postDelayed(r,
                /*activity.getResources().getInteger(R.integer.smaato_interstitial_reload_delay)*/ 20);
    }

    //region Description:Callback methods for InterstitialListener
    @Override
    public void onReadyToShow(){
        Log.i("SOMA AdSupport", "Interstitual ready to show");
    }

    @Override
    public void onWillShow(){
        //Rejoice!
    }

    @Override
    public void onFailedToLoadAd(){
        Log.i("SOMA AdSupport", "Interstitual failed to load");
        interBannerLoad();
    }

    @Override
    public void onWillClose(){
        interBannerLoad();
    }

    @Override
    public void onWillOpenLandingPage(){
        //$$!
    }
    //endregion


    private void setupSmaato(){

        BannerView bv = activity.findViewById(R.id.ad_container);
        bv.setAutoReloadEnabled(true);
        bv.setAutoReloadFrequency(activity.getResources().getInteger(R.integer.smaato_reload_delay));
        bv.getAdSettings().setPublisherId(activity.getResources().getInteger(R.integer.smaato_pub_id));
        bv.getAdSettings().setAdspaceId(activity.getResources().getInteger(R.integer.smaato_ad_id));
        bv.getAdSettings().setAdDimension(AdDimension.DEFAULT);
        bv.setAutoReloadEnabled(true);
        bv.setAutoReloadFrequency(10);
        bv.setLocationUpdateEnabled(true);
        bv.asyncLoadNewBanner();
    }

    public void adCleanup(){
        interstitial.destroy();
    }

    public void loadInterstitial(){
        interstitial.asyncLoadNewBanner();
    }
    //endregion
}
