package com.tk_squared.jive;

import android.app.Activity;
import android.app.Fragment;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.Date;

/**
 * Created by zengo on 4/15/2016.
 * Thank God for liberals!
 */
public class AboutFragment extends Fragment{

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);
        Activity activity = getActivity();
        String v = "";
        try {
            v = getString(R.string.version_name)
                    .concat(activity.getPackageManager()
                            .getPackageInfo(activity.getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.i("About Exception", "This application has no name!");
        }
        ((TextView)view.findViewById(R.id.about_app_version)).setText(v);
        String s = activity.getString(R.string.compile_date) + new Date(BuildConfig.TIMESTAMP).toString();
        ((TextView)view.findViewById(R.id.about_app_date))
                .setText(s);
        return view;
    }
}
