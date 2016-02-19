package com.tk_squared.jive;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.millennialmedia.internal.utils.IOUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
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
                InputStream in = con.getInputStream();
                this.body = IOUtils.convertStreamToString(in);
                String[] lines = this.body.split("~#%#~");
                String serverListVersion = lines[0];
                File vFile = new File(_activity.getApplicationContext().getFilesDir(),_activity.getString(R.string.server_list_version));

                BufferedReader reader;
                if(!update) {
                    if (!vFile.exists()) {
                        vFile.createNewFile();
                        updateListVersion(vFile, serverListVersion);
                        update = true;
                    } else {
                        try {
                            reader = new BufferedReader(new FileReader(vFile));
                            String date;

                            while ((date = reader.readLine()) != null) {
                                if (!date.equals(serverListVersion)) {
                                    update = true;
                                    updateListVersion(vFile, serverListVersion);
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            Log.i("FileException", e.toString());
                        }
                    }
                }

                if(update) {
                    this.body = lines[1];

                    lines = this.body.split("~~@~~");

                    for (int i = 0; i < lines.length; ++i) {
                        ++tasks;

                        jsons.add(new JSONObject(lines[i]));
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
                    String iconUrl;
                    try {
                        name = json.getString("name");
                        url = json.getString("url");
                        iconUrl = json.getString("icon");
                        CreateStationTask worker = new CreateStationTask(name, url, iconUrl);
                        worker.execute();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            } else {
                Callbacks cb = (Callbacks)_activity;
                cb.onDataLoaded(instance.stations);
            }

        }

        private void updateListVersion(File vFile, String sVersion) {
            FileOutputStream writer;
            try {
                writer = new FileOutputStream(vFile, false);
                writer.write(sVersion.getBytes());
                writer.flush();
                writer.close();
                update = true;
            } catch (IOException e){
                Log.i("FOS", "File Writing failed to update server list version");
            }
        }
    }

    private class CreateStationTask extends AsyncTask<Void, Integer, Integer>{

        private Bitmap bitmap;
        private String name;
        private String iconURL;
        private Uri uri;

        public CreateStationTask(String name, String uri, String iconURL) {
            this.name = name;
            this.iconURL = iconURL;
            this.uri = Uri.parse(uri);
        }


        @Override
        protected Integer doInBackground(Void... unused){
            try {
                //  String
                if(bitmap == null) {
                    if(iconURL == null)  iconURL = "http://www.google.com/favicon.ico";
                    bitmap = BitmapFactory.decodeStream((InputStream) new URL(iconURL).getContent());
                }

            }
            catch(MalformedURLException e){
                Log.i("MalformedURLException", e.toString());
                this.bitmap = BitmapFactory.decodeResource(_activity.getApplicationContext().getResources(), R.drawable.ic_launcher);
                //do nothing
            }
            catch (IOException e){
                Log.i("IOException", "IOException: " + e.toString());
                Log.i(_activity.getString(R.string.app_name), "Using "+_activity.getString(R.string.app_name)+" icon for station: " + this.name);
                bitmap = BitmapFactory.decodeResource(_activity.getApplicationContext().getResources(), R.drawable.ic_launcher);
            }
            return 0;
        }

        protected void onProgressUpdate(Integer... progress){

        }

        protected void onPostExecute(Integer result){
            if(this.bitmap == null) {

                Log.i(_activity.getString(R.string.app_name), "Icon is null. Using "+_activity.getString(R.string.app_name)+" icon for station: " + this.name);
                this.bitmap = BitmapFactory.decodeResource(_activity.getApplicationContext().getResources(), R.drawable.ic_launcher);
            }
            instance.stations.add(dataSource.createStation(this.name, this.uri, this.bitmap, _activity));

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

            return instance;
        }/* TIM KILL THIS ELSE STATEMENT IF IT CAUSES PROBLEMS */ else {
            instance = null;
            return tkkDataMod.getInstance(activity);
        }

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

    public void destroyInstance(){
        instance = null;
    }

    public ArrayList<tkkStation> getStations(){
        return stations;
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

    public void addStationAt(int idx, tkkStation s){
        stations.set(idx, s);
    }

    public void removeStation(tkkStation s){
        dataSource.deleteStation(s);
        stations.remove(s);
    }

    public void removeStationAt(int i){

        tkkStation s = stations.get(i);
        //removeStation(s);
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





}