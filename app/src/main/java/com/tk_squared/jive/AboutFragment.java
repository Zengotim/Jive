package com.tk_squared.jive;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by zengo on 4/15/2016.
 * Thank God for liberals!
 */
public class AboutFragment extends Fragment{
    public AboutFragment(){}
    //random change
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }
}
