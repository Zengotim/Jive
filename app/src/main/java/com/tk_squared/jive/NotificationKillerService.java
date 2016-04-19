package com.tk_squared.jive;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

/**
 * Created by zengo on 4/19/2016.
 * With a little help from the internet
 * This service kills the notification
 * that Android leaves orphaned if the user
 * swipe-closes the app.
 * Yeah, it's a hack, but holy shit Android
 * Give us a real way to make it work already!
 */
public class NotificationKillerService extends Service {

    public class KillBinder extends Binder {
        public final Service service;

        public KillBinder(Service service) {
            this.service = service;
        }

    }

    public static int NOTIFICATION_ID = 666;
    private final IBinder mBinder = new KillBinder(this);

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }
    @Override
    public void onCreate() {
        NotificationManager mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNM.cancel(NOTIFICATION_ID);
        this.onDestroy();
    }
}
