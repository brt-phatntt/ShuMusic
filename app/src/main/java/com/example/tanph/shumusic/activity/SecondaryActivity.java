package com.example.tanph.shumusic.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;
import com.example.tanph.shumusic.MyApplication;
import com.example.tanph.shumusic.R;
import com.example.tanph.shumusic.adapter.Secondary_Activity_Adapter;
import com.example.tanph.shumusic.model.Constants;
import com.example.tanph.shumusic.service.MediaPlayerService;
import com.example.tanph.shumusic.util.MusicLibrary;
import com.example.tanph.shumusic.util.UIElementHelper.FontFactory;

public class SecondaryActivity extends AppCompatActivity implements View.OnClickListener {

    private View rootView;
    private RecyclerView recyclerView;
    private Secondary_Activity_Adapter secondary_activity_adapter;
    private TextView songNameMiniPlayer,artistNameMiniPlayer;
    private ImageView buttonPlay,buttonNext,albumArt;

    private Drawable drawable;

    private BroadcastReceiver miniPlayerUpdateReceiver;

    private MediaPlayerService mediaPlayerService;

    int status;
    int key;
    String title;
    private long mLastClicktime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(MyApplication.getService()==null)
        {
            Process.killProcess(Process.myPid());
        }

        setContentView(R.layout.activity_secondary);

        mediaPlayerService = MyApplication.getService();

        drawable = ContextCompat.getDrawable(this,R.drawable.image1).mutate();

        Toolbar toolbar = findViewById(R.id.collapse_toolbar);

        //toolbar.setCollapsible(true); Need to underStand this thing.

        setSupportActionBar(toolbar);

        if(getSupportActionBar()!=null)
        {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if(getIntent()!=null)
        {
            status = getIntent().getIntExtra("status",-1);
            key = getIntent().getIntExtra("key",-1);
            title = getIntent().getStringExtra("title");
        }

        miniPlayerUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateMiniPlayerUI();
            }
        };

        setTitle(title);

        recyclerView = findViewById(R.id.recycler_view_secondary);

        switch (status)
        {
            case Constants.FRAGMENT_STATUS.ALBUM_FRAGMENT:
                secondary_activity_adapter = new Secondary_Activity_Adapter(this, MusicLibrary.getInstance().getSongListFromAlbumId(key,Constants.SORT_ORDER.ASC));
                break;
            case Constants.FRAGMENT_STATUS.ARTIST_FRAGMENT:
                secondary_activity_adapter = new Secondary_Activity_Adapter(this,MusicLibrary.getInstance().getSongListFromArtistId(key,Constants.SORT_ORDER.ASC));
                break;
        }

        if(secondary_activity_adapter!=null)
        {
            recyclerView.setAdapter(secondary_activity_adapter);
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        CardView miniPlayer = findViewById(R.id.mini_player_secondary);
        miniPlayer.setOnClickListener(this);

        ImageView collapse_image = findViewById(R.id.collapse_imageview);

        Glide.with(this)
                .asBitmap()
                .transition(BitmapTransitionOptions.withCrossFade(200))
                .load(MusicLibrary.getInstance().getAlbumUri(key))
                .apply(new RequestOptions()
                        .signature(new ObjectKey(String.valueOf(System.currentTimeMillis())))
                        .centerCrop()
                        .placeholder(drawable))
                .into(collapse_image);

        albumArt = findViewById(R.id.album_art_mini_player_secondary);

        FloatingActionButton floatingActionButton = findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(this);

        buttonPlay = findViewById(R.id.play_pause_mini_player);
        buttonPlay.setOnClickListener(this);

        buttonNext = findViewById(R.id.skip_next_mini_player);
        buttonNext.setOnClickListener(this);

        songNameMiniPlayer = findViewById(R.id.song_name_mini_player);
        artistNameMiniPlayer = findViewById(R.id.artist_name_mini_player);

        songNameMiniPlayer.setTypeface(FontFactory.getFont());
        artistNameMiniPlayer.setTypeface(FontFactory.getFont());

        albumArt = findViewById(R.id.album_art_mini_player_secondary);
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
                    buttonPlay.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_pause_black_24dp));
                } else if (mediaPlayerService.getStatus() == MediaPlayerService.PAUSED) {
                    buttonPlay.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_play_arrow_black_24dp));
                }

                songNameMiniPlayer.setText(mediaPlayerService.getCurrentTrack().getTitle());
                artistNameMiniPlayer.setText(mediaPlayerService.getCurrentTrack().getArtist_name());
            }
            else
            {
                Log.d("YOGI","exit");
                //Restart App this should not happen
                //why it should not happen..need to check the case
                //System.exit(0);

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

        MyApplication.setIsAppVisible(true);

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(miniPlayerUpdateReceiver,
                new IntentFilter(Constants.ACTION.UI_UPDATE));

        updateMiniPlayerUI();
    }

    @Override
    protected void onPause() {
        MyApplication.setIsAppVisible(false);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(miniPlayerUpdateReceiver);
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        //secondary_activity_adapter.clear();
        recyclerView.setAdapter(null);
        recyclerView = null;
        super.onDestroy();

    }


    @Override
    public void onClick(View v) {
        switch(v.getId()) {
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
                    buttonPlay.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_play_arrow_black_24dp));
                } else if (mediaPlayerService.getStatus() == MediaPlayerService.PAUSED) {
                    mediaPlayerService.resumeMedia();
                    buttonPlay.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_pause_black_24dp));
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

            case R.id.mini_player_secondary:
                final Intent intent = new Intent(this, com.example.tanph.shumusic.activity.NowPlayingActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_up_from_bottom,R.anim.fade_out);
        }
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
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_from_left,R.anim.slide_out_from_right);
    }
}
