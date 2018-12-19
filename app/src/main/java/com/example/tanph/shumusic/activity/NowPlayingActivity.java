package com.example.tanph.shumusic.activity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.example.tanph.shumusic.R;
import com.example.tanph.shumusic.fragment.NowPlayingFragment_1;

public class NowPlayingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);

        Fragment fragment = new NowPlayingFragment_1();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.container,fragment).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        int count = getSupportFragmentManager().getBackStackEntryCount();

        if(count==0)
        {
            super.onBackPressed();
            overridePendingTransition(R.anim.fade_in,R.anim.slide_down_from_bottom);
            Log.d("SHU","no fragment");

        }
        else {
            getSupportFragmentManager().popBackStack();
            Log.d("SHU","Fragment Count was "+count);
        }
    }
}

