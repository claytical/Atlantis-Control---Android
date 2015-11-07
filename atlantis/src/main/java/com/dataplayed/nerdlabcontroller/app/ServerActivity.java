package com.dataplayed.nerdlabcontroller.app;



import android.app.Activity;
import android.content.Intent;
import android.net.nsd.NsdServiceInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import com.dataplayed.nerdlabcontroller.app.util.NsdHelper;
import com.dataplayed.nerdlabcontroller.app.util.MainActivity;



public class ServerActivity extends Activity {


     // UI references.
    private EditText mPlayerView;
    private EditText mServerView;
    public List<String> bonjourServicesFound;
    private ArrayAdapter<String> adapter;
    private Spinner sItems;
    NsdHelper mNsdHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

//        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mPlayerView = (EditText) findViewById(R.id.player_name);
        mServerView = (EditText) findViewById(R.id.server);

        bonjourServicesFound =  new ArrayList<String>();
        mNsdHelper = new NsdHelper(this);
        mNsdHelper.initializeNsd();
        mNsdHelper.discoverServices();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, bonjourServicesFound);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.add("Manually Enter IP Address");
        sItems = (Spinner) findViewById(R.id.spinner);

        sItems.setAdapter(adapter);
        sItems.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (adapterView.getSelectedItemId() == 0) {
                    mServerView.setVisibility(View.VISIBLE);
                } else {
                    mServerView.setVisibility(View.GONE);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        mNsdHelper.setServiceListener(new IASyncFetchListener() {
            @Override
            public void serviceFound(NsdServiceInfo service) {
                bonjourServicesFound.add(service.getHost().getHostAddress());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mServerView.setVisibility(View.GONE);
                        sItems.setVisibility(View.VISIBLE);
                        sItems.setSelection(1);

                    }
                });
            }

            @Override
            public void serviceRemoved(NsdServiceInfo service) {

            }
        });


    }
    public interface IASyncFetchListener extends EventListener {
        void serviceFound(NsdServiceInfo service);
        void serviceRemoved(NsdServiceInfo service);
    }


    public void playButtonOnClick(View v) {
// do something when the button is clicked
        Intent playIntent = new Intent(this, MainActivity.class);
        playIntent.putExtra("player", mPlayerView.getText().toString());
        if (sItems.getSelectedItemId() == 0) {
            playIntent.putExtra("server", mServerView.getText().toString());
        }
        else {
            playIntent.putExtra("server", sItems.getSelectedItem().toString());
        }
        this.startActivity(playIntent);

    }


    private class FindBonjourServers extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            return "Started";
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }


    }



}



