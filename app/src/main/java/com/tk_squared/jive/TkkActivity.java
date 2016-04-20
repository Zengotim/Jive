package com.tk_squared.jive;



/**************************************************************
 * *********       Ad Support Section        ******** *******
 */
import com.millennialmedia.InlineAd;
import com.millennialmedia.MMException;
import com.millennialmedia.MMSDK;
import com.millennialmedia.InterstitialAd;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
/*************************************************************
 *    *********     **********             ******    ********
 */




import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.ArrayList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.support.v7.widget.ShareActionProvider;
import android.widget.TextView;


/**
 * Created by zengo on 1/30/2016.
 * You know it Babe!
 */
public class TkkActivity extends AppCompatActivity
        implements TkkListViewFragment.Callbacks, tkkDataMod.Callbacks,
        TkkWebViewFragment.Callbacks {

    //region Description: Variables and Accessors
    private tkkDataMod tuxData;
    public tkkDataMod getData() {
        return tuxData;
    }
    public void setData(tkkDataMod data) {
        tuxData = data;
    }
    public ArrayList<tkkStation> getTkkData() {
        return tuxData.getStations();
    }
    private FragmentManager fm;
    private ProgressBar progBar;
    private boolean listEditEnabled = false;
    public boolean getListEditEnabled() {
        return listEditEnabled;
    }
    private Handler handler = new Handler();
    private MusicIntentReceiver musicIntentReceiver;
    private boolean interstitialShowing = false;

    //endregion

    public TkkActivity() {
    }

    //region Description: Lifecycle and Super Overrides
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tkk);

        //show Splashscreen and progress indicator
        fm = getFragmentManager();
        displaySplashFragment();
        progBar = (ProgressBar) findViewById(R.id.progress_bar);
        if (progBar != null){
            progBar.setVisibility(View.VISIBLE);
        }

        setupAdSupport();

        //Set up the headphone jack listener
        musicIntentReceiver = new MusicIntentReceiver(this);

        //Get data model
        tuxData = tkkDataMod.getInstance(this);

    }

    @Override
    public void onBackPressed() {
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (fragment instanceof TkkWebViewFragment){
                if (((TkkWebViewFragment) fragment).getWebview().canGoBack()) {
                    ((TkkWebViewFragment) fragment).getWebview().goBack();
                } else  {
                    ((TkkWebViewFragment) fragment).getWebview().clearCache(true);
                    ((TkkWebViewFragment) fragment).getWebview().destroy();
                    if (fm.getBackStackEntryCount() > 1) {
                        fm.popBackStack();
                    }
                }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        MenuInflater menuInflater = getMenuInflater();
        //ListView Menu - edit list, get new list, about
        if (fragment instanceof TkkListViewFragment) {
            menuInflater.inflate(R.menu.menu_tkk, menu);
            listEditEnabled = false;
            ((TkkListViewFragment) fragment)
                    .getListView()
                    .setRearrangeEnabled(false);
        } else if (fragment instanceof TkkWebViewFragment) {
            //WebView Menu - share button
            menuInflater.inflate(R.menu.menu_webview, menu);
            MenuItem item = menu.findItem(R.id.menu_item_share);
            ShareActionProvider mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(getString(R.string.app_icon_url)));
            shareIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.app_name));
            shareIntent.putExtra(Intent.EXTRA_TEXT, ((TkkWebViewFragment) fragment).getCurrentName());
            shareIntent.putExtra(Intent.EXTRA_TEXT, ((TkkWebViewFragment) fragment).getCurrentUrl());
            mShareActionProvider.setShareIntent(shareIntent);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //Get new list
            case R.id.action_fetch:
                TkkListViewFragment f = ((TkkListViewFragment) fm.findFragmentById(R.id.fragment_container));
                AlertDialog.Builder cDialog = new AlertDialog.Builder((f.getListView().getContext()));
                cDialog
                        .setMessage("Do you want to download a new stations list?\n(This will add deleted stations back)")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int id){
                                progBar.setVisibility(View.VISIBLE);
                                tuxData.repopulateStations();
                                ((ArrayAdapter)((TkkListViewFragment)fm.findFragmentById(R.id.fragment_container))
                                        .getListView().getAdapter()).notifyDataSetChanged();

                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int id){
                                Log.i("#PPCITY#", "It's about to be piss pants city over here!");
                            }
                        });
                AlertDialog a = cDialog.show();
                TextView mView = (TextView)a.findViewById(android.R.id.message);
                mView.setGravity(Gravity.CENTER);
                return true;
            //Edit list mode
            case R.id.action_edit:
                listEditEnabled = !listEditEnabled;
                item.setChecked(listEditEnabled);
                TkkListViewFragment fragment =
                        ((TkkListViewFragment) fm.findFragmentById(R.id.fragment_container));
                fragment.getListView()
                        .setRearrangeEnabled(listEditEnabled);
                setDeleteButtons(fragment);
                return true;
            //About screen
            case R.id.action_about:
                displayAbout();
                return true;
            //The fuck that happen?
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPause(){
        unregisterReceiver(musicIntentReceiver);
        sendNotification();
        super.onPause();
    }

    @Override
    public void onResume(){
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(musicIntentReceiver, filter);
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancelAll();
        super.onResume();
    }

    @Override
    public void onDestroy(){
        tuxData.destroyInstance();
        //This doesn't really work
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancelAll();
        adCleanup();
        super.onDestroy();
    }

    @Override
    public void onStop(){
        super.onStop();
    }
    //endregion

    //region Description: Fragment handling
    private void displaySplashFragment(){
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (fragment == null) {
            fragment = new SplashFragment();
            fm.beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit();
        }
    }


    //Displays the About screen
    private void displayAbout() {
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (!(fragment instanceof AboutFragment)) {
            fragment = new AboutFragment();
            fm.beginTransaction().replace(R.id.fragment_container, fragment)
                    .addToBackStack("About")
                    .commit();
        }
        //Auto-return from about screen.
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                }
            }
        };
        handler.postDelayed(r, getResources().getInteger(R.integer.about_screen_delay));
    }

    private void displayListView(){
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (!(fragment instanceof TkkListViewFragment)) {
            fragment = new TkkListViewFragment();
            fm.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack("ListView")
                    .commit();
        }
    }

    private void displayWebView(tkkStation station){
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (!(fragment instanceof TkkWebViewFragment)) {
            fragment = new TkkWebViewFragment();
            Bundle args = new Bundle();
            args.putString("uri", station.getUri().toString());
            args.putString("name", station.getName());
            args.putInt("index", station.getIndex());
            fragment.setArguments(args);
            fm.beginTransaction().replace(R.id.fragment_container, fragment)
                    .addToBackStack("webView")
                    .commit();
        }
    }
    //endregion

    //region Description: Interface methods



    //Callback method for TuxedoActivityFragment.Callbacks
    @Override
    public void onStationSelected(tkkStation station) {
        showInterstitial();
        displayWebView(station);
    }

    //Callback method for tkkDataMod.Callbacks
    @Override
    public void onDataLoaded(ArrayList<tkkStation> stations) {
        progBar.setVisibility(View.GONE);
        displayListView();
        //interstitial.asyncLoadNewBanner();
    }

    //callback method for TkkWebViewFragment.Callbacks
    @Override
    public void onIconReceived(Integer idx, Bitmap icon){
        tuxData.saveIcon(idx, icon);
    }
    //endregion

    //region Description: private methods for utility
    //Method for setting visibility for delete buttons
    //Seeing as how I can't seem to make them work in the edit mode
    private void setDeleteButtons(TkkListViewFragment fragment){

        ListView listView = fragment.getListView();
        ((TkkListViewFragment.StationAdapter)(listView.getAdapter())).setShowDelete(!listEditEnabled);

        for( int i = 0; i < listView.getCount(); i++) {
            View row = listView.getChildAt(i);
            if (row != null) {
                if (listEditEnabled) {
                    row.findViewById(R.id.delete_button).setVisibility(View.GONE);
                } else {
                    row.findViewById(R.id.delete_button).setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void sendNotification(){
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (fragment instanceof TkkWebViewFragment){
            Notification.Builder builder = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentText(getString(R.string.app_name))
                    .setContentText(((TkkWebViewFragment) fragment).getCurrentName());
            Intent intent = new Intent(this, TkkActivity.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this)
                    .addParentStack(TkkActivity.class)
                    .addNextIntent(intent);
            PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);NotificationManager nm =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.notify(1, builder.build());
        }
    }


    //region Description: Ad Support

    /*****************************************************
     * ****      Ad Support Section  ***  *******  ******
     */

    /***************************************************
     * ******     Ad Support Section      *****  ******
     */
    private InterstitialAd interstitial;

    /***************************************************
     *            ****************         ***** ******
     */



    private void loadInter(){
        if (interstitial != null) {
            interstitial.load(this, null);
        }
        Runnable r = new Runnable(){
            @Override
            public void run(){
                loadInter();
            }
        };
        handler.postDelayed(r, 60000);
    }

    private void showInterstitial(){
        // Check that the ad is ready.
        if (interstitial.isReady()) {
            // Show the Ad using the display options you configured.
            try {
                interstitialShowing = true;
                interstitial.show(this);
            } catch (MMException e) {
                e.printStackTrace();
            }

        }
    }

    private void setupAdSupport(){
        //Set up ad support
        setMMedia();
        setAdSpace();
        setInterstitialAd();
        loadInter();
    }

    //region Description: Ad Support settings
    private void setMMedia() {
        MMSDK.initialize(this);
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
            InlineAd inlineAd = InlineAd.createInstance("220358",
                    (LinearLayout) findViewById(R.id.ad_container));
            final InlineAd.InlineAdMetadata inlineAdMetadata = new InlineAd.InlineAdMetadata().
                    setAdSize(InlineAd.AdSize.BANNER);

            inlineAd.request(inlineAdMetadata);

            inlineAd.setListener(new InlineAd.InlineListener() {
                @Override
                public void onRequestSucceeded(InlineAd inlineAd) {

                    if (inlineAd != null) {
                        // set a refresh rate of 30 seconds that will be applied after the first request
                        inlineAd.setRefreshInterval(30000);

                        // The InlineAdMetadata instance is used to pass additional metadata to the server to
                        // improve ad selection
                        final InlineAd.InlineAdMetadata inlineAdMetadata = new InlineAd.InlineAdMetadata().
                                setAdSize(InlineAd.AdSize.BANNER);

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

        } catch (MMException e) {
            // abort loading ad
        }
    }

    private void setInterstitialAd(){
        try {
            interstitial = InterstitialAd.createInstance(getString(R.string.mmedia_inter_apid));

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
                    interstitialShowing = false;

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

    private void adCleanup(){
        //nothing to do
    }
    //endregion

}