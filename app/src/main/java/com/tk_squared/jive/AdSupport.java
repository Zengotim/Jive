package com.tk_squared.jive;

import com.millennialmedia.InlineAd;
import com.millennialmedia.MMException;
import com.millennialmedia.MMSDK;
import com.millennialmedia.InterstitialAd;

import android.widget.LinearLayout;


/**
 * Created by zengo on 4/19/2016.
 * This is the One by AOL version of this class!!
 * make sure you are using the correct version!!
 */
public class AdSupport {

    private TkkActivity activity;
    private InterstitialAd interstitial;

    public AdSupport(TkkActivity activity){
        this.activity = activity;
    }

    private void loadInter(){
        if (interstitial != null) {
            interstitial.load(activity, null);
        }
        Runnable r = new Runnable(){
            @Override
            public void run(){
                loadInter();
            }
        };
        activity.getHandler().postDelayed(r,
                activity.getResources().getInteger(R.integer.interstitial_reload_delay));
    }

    public void showInterstitial(){
        // Check that the ad is ready.
        if (interstitial.isReady()) {
            // Show the Ad using the display options you configured.
            try {
                interstitial.show(activity);
            } catch (MMException e) {
                e.printStackTrace();
            }

        }
    }

    public void setupAdSupport(){
        //Set up ad support
        setMMedia();
        setAdSpace();
        setInterstitialAd();
        loadInter();
    }

    //region Description: Ad Support settings
    private void setMMedia() {
        MMSDK.initialize(activity);
        //really nosey shit i don't like here!
        /*UserData userData = new UserData()
                .setAge(<age>)
                .setChildren(<children>)
                .setCountry(<country>)
                .setDma(<dma>)
                .setDob(<dob>)
                .setEducation(<education>)
                .setEthnicity(<ethnicity>)
                .setGender(<gender>)
                .setIncome(<income>)
                .setKeywords(<keywords>)
                .setMarital(<marital>)
                .setPolitics(<politics>)
                .setPostalCode(<postal-code>)
                .setState(<state>);
        MMSDK.setUserData(userData);*/
    }

    private void setAdSpace() {

        try {
            // NOTE: The ad container argument passed to the createInstance call should be the
            // view container that the ad content will be injected into.
            LinearLayout l = (LinearLayout) activity.findViewById(R.id.ad_container);
            InlineAd inlineAd;
            if (l != null) {
                inlineAd = InlineAd.createInstance(activity.getString(R.string.mmedia_apid),
                        l);
                final InlineAd.InlineAdMetadata inlineAdMetadata = new InlineAd.InlineAdMetadata().
                        setAdSize(InlineAd.AdSize.BANNER);

                inlineAd.request(inlineAdMetadata);


                inlineAd.setListener(new InlineAd.InlineListener() {
                    @Override
                    public void onRequestSucceeded(InlineAd inlineAd) {

                        if (inlineAd != null) {
                            // set a refresh rate of 30 seconds that will be applied after the first request
                            inlineAd.setRefreshInterval
                                    (activity.getResources().getInteger(R.integer.inline_ad_refresh_rate));
                            // The InlineAdMetadata instance is used to pass additional metadata to the server to
                            // improve ad selection
                            //final InlineAd.InlineAdMetadata inlineAdMetadata = new InlineAd.InlineAdMetadata().
                            //        setAdSize(InlineAd.AdSize.BANNER);

                        }
                    }


                    @Override
                    public void onRequestFailed(InlineAd inlineAd, InlineAd.InlineErrorStatus errorStatus) {

                    }


                    @Override
                    public void onClicked(InlineAd inlineAd) {

                    }


                    @Override
                    public void onResize(InlineAd inlineAd, int width, int height) {

                    }


                    @Override
                    public void onResized(InlineAd inlineAd, int width, int height, boolean toOriginalSize) {

                    }


                    @Override
                    public void onExpanded(InlineAd inlineAd) {

                    }


                    @Override
                    public void onCollapsed(InlineAd inlineAd) {

                    }


                    @Override
                    public void onAdLeftApplication(InlineAd inlineAd) {

                    }
                });
            }

        } catch (MMException e) {
            // abort loading ad
        }
    }

    private void setInterstitialAd(){
        try {
            interstitial = InterstitialAd.createInstance(activity.getString(R.string.mmedia_inter_apid));

            interstitial.setListener(new InterstitialAd.InterstitialListener() {
                @Override
                public void onLoaded(InterstitialAd interstitialAd) {

                }


                @Override
                public void onLoadFailed(InterstitialAd interstitialAd,
                                         InterstitialAd.InterstitialErrorStatus errorStatus) {
                    loadInter();
                }


                @Override
                public void onShown(InterstitialAd interstitialAd) {

                }


                @Override
                public void onShowFailed(InterstitialAd interstitialAd,
                                         InterstitialAd.InterstitialErrorStatus errorStatus) {

                }


                @Override
                public void onClosed(InterstitialAd interstitialAd) {


                }


                @Override
                public void onClicked(InterstitialAd interstitialAd) {

                }


                @Override
                public void onAdLeftApplication(InterstitialAd interstitialAd) {

                }


                @Override
                public void onExpired(InterstitialAd interstitialAd) {
                }
            });

        } catch (MMException e) {
            // abort loading ad
        }
    }

    public void adCleanup(){
        //nothing to do
        //used in Smaato version
    }

    public void loadInterstitial(){
        //nothing to do
        //used in Smaato version
    }
    //endregion


}
