package com.tk_squared.jive;

import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;


/**
 * Created by Kevin for tk^2 on 12/29/2015.
 * science bitches
 */
public class tkkStation {
    private final long id;
    private final Uri uri;
    private final String name;
    private BitmapDrawable icon;
    private int index;

    public tkkStation(long _id, String n, BitmapDrawable b, Uri u) {
        id = _id;
        uri = u;
        icon = b;
        name = n;
    }

    public BitmapDrawable getIcon() { return icon; }

    public void setIcon(BitmapDrawable i) { icon = i; }

    public long getId() { return id; }

    public int getIndex(){
        return index;
    }

    public void setIndex(int idx) {
        index = idx;
    }

    public String getName(){
        return name;
    }

    public Uri getUri(){
        return uri;
    }

}