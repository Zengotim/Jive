package com.tk_squared.jive;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.os.IBinder;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.ArrayList;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.support.v7.widget.ShareActionProvider;
import android.widget.TextView;

import com.smaato.soma.AdDownloaderInterface;
import com.smaato.soma.AdListenerInterface;
import com.smaato.soma.BannerView;
import com.smaato.soma.ReceivedBannerInterface;
import com.smaato.soma.bannerutilities.constant.BannerStatus;


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
    public ArrayList<tkkStation> getTkkData() {
        return tuxData.getStations();
    }
    private FragmentManager fm;
    private ProgressBar progBar;
    private boolean listEditEnabled = false;
    public boolean getListEditEnabled() {
        return listEditEnabled;
    }
    private final Handler handler = new Handler(); public Handler getHandler() {return handler;}
    private MusicIntentReceiver musicIntentReceiver;
    private Runnable r;
    private ServiceConnection mConnection;
    private final AdSupport adSupport = new AdSupport(this);
    //endregion

    public TkkActivity() {}

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

        adSupport.setupAdSupport();
        /*BannerView bannerView = findViewById(R.id.ad_container);
        bannerView.addAdListener(new AdListenerInterface() {
            @Override
            public void onReceiveAd(AdDownloaderInterface adDownloaderInterface, ReceivedBannerInterface receivedBannerInterface) {
                if (receivedBannerInterface.getStatus() == BannerStatus.ERROR) {
                    Log.i("BannerAd", "" + receivedBannerInterface.getErrorCode() + " : " + receivedBannerInterface.getErrorMessage());
                }
            }
        });
        */
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
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(getString(R.string.app_icon_url)));
            shareIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.app_name));
            shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message) + " "
                    + getString(R.string.subtitle) + '\n' + fragment.getArguments().getString("name") +
                    '\n' + getString(R.string.get_message) + '\n' + getString(R.string.download_url));
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
                                //do nothing
                            }
                        });
                AlertDialog a = cDialog.show();
                TextView mView = (TextView)a.findViewById(android.R.id.message);
                if (mView != null) {mView.setGravity(Gravity.CENTER);}
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
        mConnection = null;
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(musicIntentReceiver, filter);
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancelAll();
        super.onResume();
    }

    @Override
    public void onDestroy(){
        handler.removeCallbacks(r);
        tuxData.destroyInstance();
        //This doesn't really work
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancelAll();
        adSupport.adCleanup();
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
        r = new Runnable() {
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
        adSupport.showInterstitial();
        displayWebView(station);
    }

    //Callback method for tkkDataMod.Callbacks
    @Override
    public void onDataLoaded() {
        Log.i("ListData", Integer.toString(tuxData.getStations().size()));
        progBar.setVisibility(View.GONE);
        displayListView();
        adSupport.loadInterstitial();
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


    private void sendNotification() {
        final Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (fragment instanceof TkkWebViewFragment) {
            mConnection = new ServiceConnection() {
                public void onServiceConnected(ComponentName className, IBinder binder) {
                    ((NotificationKillerService.KillBinder) binder).service.startService(new Intent(
                            TkkActivity.this, NotificationKillerService.class));
                    Notification.Builder builder = new Notification.Builder(fragment.getActivity())
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setContentText(getString(R.string.app_name))
                            .setContentText(((TkkWebViewFragment) fragment).getCurrentName());
                    Intent intent = new Intent(fragment.getActivity(), TkkActivity.class);
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(fragment.getActivity())
                            .addParentStack(TkkActivity.class)
                            .addNextIntent(intent);
                    PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                    builder.setContentIntent(pendingIntent);
                    NotificationManager nm =
                            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    nm.notify(NotificationKillerService.NOTIFICATION_ID, builder.build());
                }

                public void onServiceDisconnected(ComponentName className) {
                }
            };
            bindService(new Intent(TkkActivity.this, NotificationKillerService.class), mConnection,
                    Context.BIND_AUTO_CREATE);
        }
    }
    //endregion
}