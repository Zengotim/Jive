package com.tk_squared.jive;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by Kevin on 12/29/2015.
 * science bitches
 */


public class tkkDataMod {

    public interface Callbacks {
        void onDataLoaded(ArrayList<tkkStation> _stations);
    }


    private static tkkDataMod instance = null;
    private ArrayList<tkkStation> stations;
    private tkkStationsDataSource dataSource;
    private Activity _activity;
    private int tasks = 0;
    private int completes = 0;

    //Saves the favicon
    public void saveIcon(int idx, Bitmap icon){
        //save the icon to station at index
        stations.get(idx).setIcon(new BitmapDrawable(_activity.getResources(), icon));
        dataSource.updateStation(stations.get(idx), _activity);
    }

    private class GetServerDataTask extends  AsyncTask<Void, Integer, Integer> {

        String body;
        Boolean update = false;
        ArrayList<JSONObject> jsons;

        public GetServerDataTask(){
            this.jsons = new ArrayList<>();
        }

        public GetServerDataTask(Boolean u) {
            this.jsons = new ArrayList<>();
            this.update = u;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            instance.stations = dataSource.getAllStations(_activity);
            System.out.println(stations.size());

            try {
                URL url = new URL(_activity.getString(R.string.stations_list_url));
                URLConnection con = url.openConnection();
                File vFile = new File(_activity.getApplicationContext().getFilesDir(),_activity.getString(R.string.server_list_version));
                long remoteModified = con.getLastModified();
                long localModified = vFile.lastModified();
                update = (remoteModified > localModified);

                if(!update) {
                    if (!vFile.exists()) {
                        if (vFile.createNewFile()){
                            //updateListVersion(vFile, serverListVersion);
                            update = true;
                        }
                    }
                }

                if(update) {
                    Log.i("tkkDataMod", "Newer server file, downloading to local!");
                    InputStream in = con.getInputStream();
                    this.body = fileReader(in);
                    String[] lines = this.body.split("~#%#~");
                    this.body = lines[1];

                    lines = this.body.split("~~@~~");
                    tasks = lines.length;
                    for (String s : lines) {
                        jsons.add(new JSONObject(s));
                    }
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                Log.i("IOException", "ITS AN IOEXCEPTION!!");
                e.printStackTrace();
            } catch (JSONException e) {
                Log.i("JSONException", "ITS A JSONEXCEPTION!!");
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(Integer result) {
            if(update){
                instance.deleteAllStations();
                for(int i = 0; i < jsons.size(); ++i) {
                    JSONObject json = jsons.get(i);
                    String name;
                    String url;
                    try {
                        name = json.getString("name");
                        url = json.getString("url");
                        CreateStationTask worker = new CreateStationTask(name, url, i);
                        worker.executeOnExecutor(THREAD_POOL_EXECUTOR);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            } else {
                Callbacks cb = (Callbacks)_activity;
                cb.onDataLoaded(instance.stations);
            }

        }

        private String fileReader(InputStream inputStream) throws IOException {

            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line);
            }
            return total.toString();
        }
    }

    private class CreateStationTask extends AsyncTask<Void, Integer, Integer>{

        private Bitmap bitmap;
        private String name;
        private Uri uri;
        private int idx;

        public CreateStationTask(String name, String uri, int idx) {
            this.name = name;
            this.uri = Uri.parse(uri);
            this.idx = idx;
        }


        @Override
        protected Integer doInBackground(Void... unused){
            try {
                //  String
                if(bitmap == null) {
                    bitmap = BitmapFactory.decodeResource(_activity.getApplicationContext().getResources(), R.drawable.ic_launcher);
                }

            }
            catch (Exception e){
                Log.i("Exception", e.toString());

            }
            return 0;
        }

        protected void onPostExecute(Integer result){
            if(this.bitmap == null) {
                this.bitmap = BitmapFactory.decodeResource(_activity.getApplicationContext().getResources(), R.drawable.ic_launcher);
            }
            tkkStation newStation = dataSource.createStation(this.name, this.uri, this.bitmap, this.idx, _activity);
            instance.stations.add(newStation);
            if(++completes >= tasks) {
                Callbacks cb = (Callbacks)_activity;
                cb.onDataLoaded(instance.stations);
            }
        }
    }

    private tkkDataMod(){
    }


    //Used to create tkkDataMod singleton
    public static tkkDataMod getInstance(Activity activity){

        if(instance == null) {
            instance = new tkkDataMod();
            instance.stations = new ArrayList<>();
            // uncomment to delete the database
            //TkkActivity.getTkkContext().deleteDatabase("stations.db");


            instance._activity = activity;
            instance.dataSource = new tkkStationsDataSource(instance._activity.getApplicationContext());


            try {

                instance.dataSource.open();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            instance.populateStations();
        }
        return instance;
    }


    //Called to populate the stations list
    private void populateStations(){
        GetServerDataTask reader = new GetServerDataTask();
        reader.execute();
    }

    //Deletes current stations list and table entries and pulls fresh list from the server
    public void repopulateStations(){
        instance.deleteAllStations();
        GetServerDataTask reader = new GetServerDataTask(true);
        reader.execute();
    }

    public void moveStation(int idx, int newIdx){
        moveStation(getStationAt(idx), newIdx);
    }

    public void moveStation(tkkStation s, int newIdx){
        stations.remove(s);
        stations.add(newIdx,s);
        int iter = s.getIndex() <= newIdx ? s.getIndex() : newIdx;

        for (int i = iter; i < stations.size(); ++i){
            tkkStation temp = stations.get(i);
            temp.setIndex(i);
            dataSource.updateStation(s, _activity);
        }
    }

    public void removeStationAt(int i){
        tkkStation s = stations.get(i);
        dataSource.deleteStation(s);
        stations.remove(i);
    }

    public void deleteAllStations() {
        stations = null;
        stations = new ArrayList<>();
        dataSource.deleteAll();
    }

    public tkkStation getStationAt(int idx) {
        return stations.get(idx);
    }

    public ArrayList<tkkStation> getStations(){
        return stations;
    }

    //region Description:Unused methods according to AS
    public void addStationAt(int idx, tkkStation s){
        stations.set(idx, s);
    }

    public void removeStation(tkkStation s){
        dataSource.deleteStation(s);
        stations.remove(s);
    }

    public void setStations(ArrayList<tkkStation> s) {
        if(stations != null){
            stations.clear();
        }
        stations = s;
    }

    public void addStation(tkkStation s){
        //dataSource.createStation()
        stations.add(s);
    }

    public void destroyInstance(){
        instance = null;
    }
    //endregion
}