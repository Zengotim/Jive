package com.tk_squared.jive;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
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
        void onDataLoaded();
    }


    private static tkkDataMod instance = null;
    private ArrayList<tkkStation> stations;
    private tkkStationsDataSource dataSource;
    private Activity _activity;

    //Saves the favicon
    public void saveIcon(int idx, Bitmap icon){
        //save the icon to station at index
        stations.get(idx).setIcon(new BitmapDrawable(_activity.getResources(), icon));
        dataSource.updateStation(stations.get(idx));
    }

    private class GetServerDataTask extends  AsyncTask<Void, Integer, Integer> {

        String body;
        Boolean update = false;

        Bitmap defaultIcon;
        JSONArray jsons;

        public GetServerDataTask(Boolean u) {
            this.update = u;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            instance.stations = dataSource.getAllStations(_activity);
            System.out.println(stations.size());

            try {
                defaultIcon = BitmapFactory.decodeResource(_activity.getApplicationContext()
                        .getResources(), R.drawable.ic_launcher);
                File vFile = new File(_activity.getApplicationContext().getFilesDir(), "stations.json");
                if (!update) {
                    // URL url = new URL(_activity.getString(R.string.stations_list_url));
                    URL url = new URL("http://tk-squared.com/Jive/stations_.json");
                    URLConnection con = url.openConnection();


                    InputStream in = con.getInputStream();
                    this.body = streamReader(in);

                    this.jsons = new JSONArray(this.body);


                    if (!vFile.exists()) {
                        if (vFile.createNewFile()) {
                            Log.i("#STATIONSJSON#", "Need to create local stations.json");
                            createStationsJSON(vFile);
                            update = true;
                            // con.

                        }
                    } else if (con.getLastModified() > vFile.lastModified()) {
                        Log.i("#STATIONS.JSON#", "Need to update local stations.json");
                        createStationsJSON(vFile);
                        update = true;

                    }
                } else {

                    this.jsons = jsonFileReader(vFile);

                }
                if (update) {
                    instance.deleteAllStations();

                    for (int i = 0; i < this.jsons.length(); ++i) {
                        JSONObject json = this.jsons.getJSONObject(i);
                        String name = json.getString("name");
                        String url = json.getString("url");
                        instance.stations.add(dataSource.createStation(name, Uri.parse(url), defaultIcon, i, _activity));


                    }
                }
            }catch (MalformedURLException e) {
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
            Callbacks cb = (Callbacks)_activity;
            cb.onDataLoaded();

        }

        private void createStationsJSON(File vFile){
            FileOutputStream writer;
            try {
                writer = new FileOutputStream(vFile, false);
                writer.write(this.body.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.i("FILE WRITER", "Finished writing to stations.json");
        }

        private String streamReader(InputStream inputStream) throws IOException {

            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line);
            }
            return total.toString();
        }
        private JSONArray jsonFileReader(File file){
            JSONArray temp = new JSONArray();
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                String lines = "";
                while ((line = br.readLine()) != null) {
                    lines += line;
                }
                temp = new JSONArray(lines);
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return temp;
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
        GetServerDataTask reader = new GetServerDataTask(false);
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

    private void moveStation(tkkStation s, int newIdx){
        stations.remove(s);
        stations.add(newIdx,s);
        int start = s.getIndex() <= newIdx ? s.getIndex() : newIdx;

        for (int i = start; i < stations.size(); ++i){

            stations.get(i).setIndex(i);
        }

        dataSource.updateStation(s);
    }

    public void removeStationAt(int i){
        // tkkStation s = stations.get(i);
        dataSource.deleteStation(stations.get(i));
        stations.remove(i);
    }

    private void deleteAllStations() {
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


    public void destroyInstance(){
        closeDataSource();
        instance = null;
    }

    private void closeDataSource(){
        this.dataSource.close();
    }

    //region Description:Unused methods according to AS
  /*  public void addStationAt(int idx, tkkStation s){
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
                completes = 0;
                cb.onDataLoaded(instance.stations);
            }
        }
    }
*/
    //endregion
}