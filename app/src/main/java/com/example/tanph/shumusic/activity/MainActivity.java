package com.example.tanph.shumusic.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;
import com.example.tanph.shumusic.MyApplication;
import com.example.tanph.shumusic.R;
import com.example.tanph.shumusic.fragment.Fragment_Album;
import com.example.tanph.shumusic.fragment.Fragment_Artist;
import com.example.tanph.shumusic.fragment.Fragment_Track;
import com.example.tanph.shumusic.model.Constants;
import com.example.tanph.shumusic.service.MediaPlayerService;
import com.example.tanph.shumusic.util.MusicLibrary;
import com.example.tanph.shumusic.util.UIElementHelper.FontFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    //Broadcast receiver for miniplayer update
    private BroadcastReceiver mReceiverForMiniPlayerUpdate;

    private ViewPager viewPager;
    private TabLayout tabLayout;
    private ViewPagerAdapter viewPagerAdapter;
    private ImageView buttonPlayPause,buttonNext;
    private ImageView albumArt;
    private TextView songNameMiniPlayer, artistNameMiniPlayer;
    private NavigationView navigationView;
    private View rootView;
    private Drawable drawable;

    //Service Related
    private MediaPlayerService mediaPlayerService;
    private boolean mBound = false;
    private ServiceConnection mServiceConnection =  new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalBinder localBinder = (MediaPlayerService.LocalBinder) service;
            mediaPlayerService = localBinder.getService();
            MyApplication.setService(mediaPlayerService);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };


    //Tab Sequence
    int [] savedTabSeqInt = {0,1,2};
    private long mLastClicktime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(MyApplication.getService()== null)
        {
            Process.killProcess(Process.myPid());
        }
        mediaPlayerService = MyApplication.getService();

        mediaPlayerService.onTaskRemoved = false;
        Log.d("YOGI","onCreate of MainActivity and onTaskRemoved = "+mediaPlayerService.onTaskRemoved);
        /*if(MyApplication.getContext() == this)
        {
            Log.d("YOGI","YES");
        }
        else
        {
            Log.d("YOGI","NO");
        }*/
        setContentView(R.layout.activity_main);

        rootView = findViewById(R.id.root_view_drawer_act_main);

        navigationView =  findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);

        setTitle("Library");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        mReceiverForMiniPlayerUpdate =  new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateMiniPlayerUI();
            }
        };

        LinearLayout miniPlayer =  findViewById(R.id.miniPlayer);
        miniPlayer.setOnClickListener(this);

        buttonPlayPause = findViewById(R.id.play_pause_mini_player);
        buttonPlayPause.setOnClickListener(this);

        buttonNext = findViewById(R.id.skip_next_mini_player);
        buttonNext.setOnClickListener(this);

        songNameMiniPlayer = findViewById(R.id.song_name_mini_player);
        songNameMiniPlayer.setTypeface(FontFactory.getFont());

        artistNameMiniPlayer = findViewById(R.id.artist_name_mini_player);
        artistNameMiniPlayer.setTypeface(FontFactory.getFont());

        albumArt = findViewById(R.id.album_art_mini_player);
        drawable = ContextCompat.getDrawable(this,R.drawable.image1).mutate();

        //Drawer Layout initialized
        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);

        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();


        //get the tab sequence
        String savedTabSeq = MyApplication.getPref().getString(MyApplication.getContext()
                .getString(R.string.pref_tab_sequence), Constants.TABS.DEFAULT_SEQ);
        StringTokenizer stringTokenizer = new StringTokenizer(savedTabSeq,",");
        savedTabSeqInt = new int[Constants.TABS.NUMBER_OF_TABS];
        for(int i=0;i<Constants.TABS.NUMBER_OF_TABS;i++)
        {
            savedTabSeqInt[i] = Integer.parseInt(stringTokenizer.nextToken());
        }

        viewPager = findViewById(R.id.viewPager);
        setUpViewPager(viewPager);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        //Automatically for TabLayout will be automatically populated from
        //PagerAdapter's Page Titles.
        tabLayout.setupWithViewPager(viewPager);
        updateMiniPlayerUI();
    }

    private void updateMiniPlayerUI() {
        //check for the service first
        if(mediaPlayerService!=null)
        {
            if(mediaPlayerService.getCurrentTrack()!=null)
            {
                //load images using Glide
                Glide
                        .with(this)
                        .asBitmap()
                        .load(MusicLibrary.getInstance().getAlbumUri(mediaPlayerService.getCurrentTrack().getAlbum_id()))
                        .apply(new RequestOptions()
                                .signature(new ObjectKey(String.valueOf(System.currentTimeMillis())))
                                /*.circleCrop()*/
                                .placeholder(drawable))
                        .into(albumArt);

                if (mediaPlayerService.getStatus() == MediaPlayerService.PLAYING) {
                    buttonPlayPause.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_pause_black_24dp));
                } else if (mediaPlayerService.getStatus() == MediaPlayerService.PAUSED) {
                    buttonPlayPause.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_play_arrow_black_24dp));
                }

                songNameMiniPlayer.setText(mediaPlayerService.getCurrentTrack().getTitle());
                artistNameMiniPlayer.setText(mediaPlayerService.getCurrentTrack().getArtist_name());
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(getIntent()!=null)
        {
            Log.d("YOGI",""+getIntent().getAction());
        }
        MyApplication.setIsAppVisible(true);

        //Register Receiver for UI_UPDATE
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiverForMiniPlayerUpdate,
                new IntentFilter(Constants.ACTION.UI_UPDATE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyApplication.setIsAppVisible(false);
        LocalBroadcastManager.getInstance(getApplicationContext())
                .unregisterReceiver(mReceiverForMiniPlayerUpdate);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        updateMiniPlayerUI();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.play_pause_mini_player:
                if (mediaPlayerService.getCurrentTrack() == null) {
                    Toast.makeText(this, "Nothing to Play!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (SystemClock.elapsedRealtime() - mLastClicktime < 300) {
                    return;
                }

                mLastClicktime = SystemClock.elapsedRealtime();

                if (mediaPlayerService.getStatus() == MediaPlayerService.PLAYING) {
                    mediaPlayerService.pauseMedia();
                    buttonPlayPause.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_play_arrow_black_24dp));
                } else if (mediaPlayerService.getStatus() == MediaPlayerService.PAUSED) {
                    mediaPlayerService.resumeMedia();
                    buttonPlayPause.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_pause_black_24dp));
                }
                break;
            case R.id.skip_next_mini_player:
                if (mediaPlayerService.getCurrentTrack() == null) {
                    Toast.makeText(this, "Nothing to Play!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(SystemClock.elapsedRealtime()-mLastClicktime < 1000)
                {
                    return;
                }

                mLastClicktime = SystemClock.elapsedRealtime();
                mediaPlayerService.skipToNext();
                break;
            case R.id.miniPlayer:
                final Intent intent = new Intent(this, com.example.tanph.shumusic.activity.NowPlayingActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_up_from_bottom,R.anim.fade_out);
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        viewPager.clearOnPageChangeListeners();
        viewPager = null;
        viewPagerAdapter = null;
        navigationView.setNavigationItemSelectedListener(null);
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

    }

    private void setUpViewPager(ViewPager viewPager)
    {
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());

        for (int tab : savedTabSeqInt)
        {
            switch (tab)
            {
                case Constants.TABS.ALBUMS:
                    Fragment_Album fragment_album = new Fragment_Album();
                    viewPagerAdapter.addFragment(fragment_album,Constants.TABS.ALBUMS_NAME);
                    break;
                case Constants.TABS.TRACKS:
                    Fragment_Track fragment_track = new Fragment_Track();
                    viewPagerAdapter.addFragment(fragment_track,Constants.TABS.TRACKS_NAME);
                    break;
                case Constants.TABS.ARTISTS:
                    Fragment_Artist fragment_artist = new Fragment_Artist();
                    viewPagerAdapter.addFragment(fragment_artist,Constants.TABS.ARTISTS_NAME);
                    break;
            }
        }
        viewPager.setAdapter(viewPagerAdapter);
    }

    private class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        void addFragment(Fragment fragment,String title)
        {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();

        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
        {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        else
        {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
        }
    }
}

