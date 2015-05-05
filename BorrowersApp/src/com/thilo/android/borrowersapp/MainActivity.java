package com.thilo.android.borrowersapp;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.thilo.anroid.borrowersapp.R;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class MainActivity extends ActionBarActivity {

    public static final String LOG_TAG = "WhoHasMyStuff";
    public static final String FIRST_START = "FirstStart";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        AdView mAdView = (AdView) findViewById(R.id.adView1);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        if (findViewById(R.id.mainActivity) != null) {
            if (savedInstanceState != null) {
                return;
            }

            ListLentObjects firstFragment = new ListLentObjects();

            firstFragment.setArguments(getIntent().getExtras());

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.mainActivity, firstFragment).commit();
        }
    }

}
