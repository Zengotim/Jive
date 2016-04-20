package com.tk_squared.jive;

/**
 * Created by zengo on 4/13/2016.
 * With lovingkindness
 */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

@SuppressLint("unused")
@SuppressWarnings("unused")
public class MusicIntentReceiver extends BroadcastReceiver {
    private Activity activity;

    //The manifest freaks out without a default constructor.
    public MusicIntentReceiver(){}

    public MusicIntentReceiver(Activity activity){
        this.activity = activity;
    }

    @Override public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
            int state = intent.getIntExtra("state", -1);
            FragmentManager fm = activity.getFragmentManager();
            switch (state) {
                case 0:
                    //Headset was unplugged, back off the music
                    if (fm.getBackStackEntryCount() > 1) {
                        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
                        if (fragment instanceof TkkWebViewFragment) {
                            ((TkkWebViewFragment) fragment).getWebview().destroy();
                        }
                        fm.popBackStack();
                    }
                    break;
                case 1:
                    //Headset was plugged in, who cares?
                    break;
                default:
                    //If ya don't know what to do, then don't do it.
            }
        }
    }
}