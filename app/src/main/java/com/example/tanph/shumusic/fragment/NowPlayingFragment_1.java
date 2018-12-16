package com.example.tanph.shumusic.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.example.tanph.shumusic.MyApplication;
import com.example.tanph.shumusic.R;
import com.example.tanph.shumusic.adapter.SlidingQueueAdapter;
import com.example.tanph.shumusic.model.Constants;
import com.example.tanph.shumusic.service.MediaPlayerService;
import com.example.tanph.shumusic.util.MusicLibrary;
import com.example.tanph.shumusic.util.ShumusicUtils;
import com.example.tanph.shumusic.widgets.PlayPauseButton;

import net.steamcrafted.materialiconlib.MaterialIconView;

public class NowPlayingFragment_1 extends Fragment {

    private MaterialIconView next,previous;
    private PlayPauseButton playPause;
    private View playPauseWrapper;
    private SeekBar mProgressBar;
    private ImageView albumArt,blurredAlbumArt;
    private TextView mSongTitle,mSongArtist,mSongAlbum,mSongElapsedTime,mSongDuration;
    private Toolbar mToolbar;
    private RecyclerView mRecyclerView;
    private MediaPlayerService mediaPlayerService;
    private Drawable drawable;
    private SlidingQueueAdapter adapter;
    private BroadcastReceiver broadcastReceiver;

    private int overflowcounter = 0;
    private boolean fragmentPaused = false;

    private final View.OnClickListener mButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mediaPlayerService.getCurrentTrack() == null) {
                Toast.makeText(getContext(), "Nothing to Play!", Toast.LENGTH_SHORT).show();
                return;
            }
            if(!playPause.isPlayed())
            {
                playPause.setPlayed(true);
                playPause.startAnimation();
            }
            else
            {
                playPause.setPlayed(false);
                playPause.startAnimation();
            }

            if(mediaPlayerService.getStatus()==MediaPlayerService.PLAYING)
            {
                mediaPlayerService.pauseMedia();
            }
            else if(mediaPlayerService.getStatus() == MediaPlayerService.PAUSED)
            {
                mediaPlayerService.resumeMedia();
            }
        }
    };


    //Runnable for Seek Bar
    public Runnable mUpdateProgress = new Runnable() {
        @Override
        public void run() {
            long currentPosition = mediaPlayerService.getCurrentPosition();
            if(mProgressBar!=null)
            {
                mProgressBar.setProgress((int) currentPosition);
                if (mSongElapsedTime!=null)
                {
                    mSongElapsedTime.setText(ShumusicUtils.makeShortTimeString(getContext(),currentPosition/1000));
                }
            }

            overflowcounter--;
            int delay = 250;
            if(overflowcounter<0 && !fragmentPaused)
            {
                overflowcounter++;
                mProgressBar.postDelayed(mUpdateProgress,delay);
                Log.d("YOGI","mUpdateProgress");
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(MyApplication.getService()==null)
        {
            Process.killProcess(Process.myPid());
        }
        mediaPlayerService = MyApplication.getService();
        drawable = ContextCompat.getDrawable(getContext(),R.drawable.image1).mutate();

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateSongDetails();
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("YOGI","Now_Playing_Fragment_1_SetSongDetails");
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_now_playing_fragment_1, container, false);

        setSongDetails(rootView);
        return rootView;

    }

    public void setSongDetails(View view) {

        next = view.findViewById(R.id.next);
        previous = view.findViewById(R.id.previous);
        playPause = view.findViewById(R.id.playpause);
        playPauseWrapper = view.findViewById(R.id.playpausewrapper);
        mProgressBar = view.findViewById(R.id.song_progress);
        albumArt = view.findViewById(R.id.album_art);
        blurredAlbumArt = view.findViewById(R.id.album_art_blurred);
        mSongTitle = view.findViewById(R.id.song_title);
        mSongArtist = view.findViewById(R.id.song_artist);
        mSongAlbum = view.findViewById(R.id.song_album);
        mSongElapsedTime = view.findViewById(R.id.song_elapsed_time);
        mSongDuration = view.findViewById(R.id.song_duration);
        mToolbar = view.findViewById(R.id.toolbar);
        mRecyclerView = view.findViewById(R.id.queue_recyclerview_horizontal);

        if(mToolbar!=null)
        {
            ((AppCompatActivity)getActivity()).setSupportActionBar(mToolbar);
            final ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("");
        }

        if(playPause!=null && getActivity()!=null)
        {
            playPause.setColor(ContextCompat.getColor(getContext(),android.R.color.white));
        }

        setSongDetails();


    }

    void setSongDetails()
    {
        updateSongDetails();

        if(mRecyclerView!=null && mediaPlayerService.getCurrentTrack()!=null)
        {
            setQueueSongs();
        }

        setSeekBarListener();

        if(next!=null)
        {
            next.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mediaPlayerService.getCurrentTrack() == null) {
                        Toast.makeText(getContext(), "Nothing to Play!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mediaPlayerService.skipToNext();
                            updateSongDetails();
                        }
                    },200);
                }
            });
        }

        if(previous!=null)
        {
            previous.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mediaPlayerService.getCurrentTrack() == null) {
                        Toast.makeText(getContext(), "Nothing to Play!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mediaPlayerService.skipToPrevious();
                            updateSongDetails();
                        }
                    }, 200);
                }
            });
        }

        if(playPauseWrapper!=null)
        {
            playPauseWrapper.setOnClickListener(mButtonClickListener);
        }

    }

    private void setSeekBarListener() {
        if(mProgressBar!=null)
        {
            mProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if(fromUser && mediaPlayerService.getCurrentTrack()!=null)
                    {
                        mediaPlayerService.seekTo(progress);
                    }
                    else if(fromUser) {
                        mProgressBar.setProgress(0);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

        }
    }

    private void setQueueSongs() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(),LinearLayoutManager.HORIZONTAL,false));
        adapter = new SlidingQueueAdapter(getContext(),mediaPlayerService.getTrackList());
        mRecyclerView.setAdapter(adapter);
        //mRecyclerView.scrollToPosition();
    }

    private void updateSongDetails() {
        if(albumArt!=null)
        {
            Uri uri = null;
            if(mediaPlayerService.getCurrentTrack()!=null)
            {
                uri = MusicLibrary.getInstance().getAlbumUri(mediaPlayerService.getCurrentTrack().getAlbum_id());
            }
            Glide.with(getContext())
                    .asBitmap()
                    .transition(BitmapTransitionOptions.withCrossFade(200))
                    .load(uri)
                    .apply(new RequestOptions()
                            .centerCrop()
                            .placeholder(drawable)
                            .diskCacheStrategy(DiskCacheStrategy.ALL))
                    .listener(new RequestListener<Bitmap>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                            Log.d("YOGI","glide_load_failed_now_playing_fragment");
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d("YOGI","glide_on_Resource_Ready_now_playing_fragment");
                            doAlbumArtStuff(resource);
                            return false;
                        }
                    })
                    .into(albumArt);

        }

        if(mSongTitle!=null && mediaPlayerService.getCurrentTrack()!=null)
        {
            if(mediaPlayerService.getCurrentTrack().getTitle()!=null)
            {
                String songName = mediaPlayerService.getCurrentTrack().getTitle();
                mSongTitle.setText(mediaPlayerService.getCurrentTrack().getTitle());
                if(songName.length()<=23)
                {
                    mSongTitle.setTextSize(25);
                }
                else if(songName.length()>=30)
                {
                    mSongTitle.setTextSize(18);
                }
                else
                {
                    mSongTitle.setTextSize(18+(songName.length()-24));
                }
            }
        }
        else if (mediaPlayerService.getCurrentTrack()==null)
        {
            mSongTitle.setText("<No_Song_Exists>");
        }

        if(mSongArtist!=null && mediaPlayerService.getCurrentTrack()!=null)
        {
            mSongArtist.setText(mediaPlayerService.getCurrentTrack().getArtist_name());
        }
        else if(mediaPlayerService.getCurrentTrack()==null) {
            mSongArtist.setText("<No_Artist_Exists>");
        }

        if(mSongAlbum!=null && mediaPlayerService.getCurrentTrack()!=null)
        {
            mSongAlbum.setText(mediaPlayerService.getCurrentTrack().getAlbum_name());
        }
        else if(mediaPlayerService.getCurrentTrack()==null)
        {
            mSongAlbum.setText("<No_Album_Exists>");
        }

        if(playPause!=null)
        {
            updatePlayPauseButton();
        }

        if(mSongDuration!=null && mediaPlayerService.getCurrentTrack()!=null)
        {
            mSongDuration.setText(ShumusicUtils.makeShortTimeString(getContext(),mediaPlayerService.getCurrentTrack().getDurInt()/1000));
        }
        else if (mediaPlayerService.getCurrentTrack()==null)
        {
            mSongDuration.setText(ShumusicUtils.makeShortTimeString(getContext(),0));
        }

        if(mProgressBar!=null && mediaPlayerService.getCurrentTrack()!=null)
        {
            mProgressBar.setMax(mediaPlayerService.getCurrentTrack().getDurInt());

            if(mUpdateProgress!=null)
            {
                mProgressBar.removeCallbacks(mUpdateProgress);
            }
            mProgressBar.postDelayed(mUpdateProgress,10);
        }
    }

    private void updatePlayPauseButton() {
        if(mediaPlayerService.getCurrentTrack()==null)
        {
            playPause.setPlayed(false);
            playPause.startAnimation();
            return;
        }
        if(mediaPlayerService.getStatus() == MediaPlayerService.PLAYING)
        {
            if(!playPause.isPlayed())
            {
                playPause.setPlayed(true);
                playPause.startAnimation();
            }
        }
        else if(mediaPlayerService.getStatus() == MediaPlayerService.PAUSED)
        {
            if(playPause.isPlayed())
            {
                playPause.setPlayed(false);
                playPause.startAnimation();
            }
        }
    }

    private void doAlbumArtStuff(Bitmap resource) {
        SetBlurredAlbumArt blurredAlbumArt = new SetBlurredAlbumArt();
        blurredAlbumArt.execute(resource);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("YOGI","NowPlayingFragment Paused");
        fragmentPaused = true;
        LocalBroadcastManager.getInstance(getContext().getApplicationContext()).unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        fragmentPaused = false;

        LocalBroadcastManager.getInstance(getContext().getApplicationContext()).registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.ACTION.UI_UPDATE));
        if(mediaPlayerService!=null)
        {
            updateSongDetails();
        }
        /*if(mProgressBar!=null && mediaPlayerService.getCurrentTrack()!=null)
        {
            mProgressBar.postDelayed(mUpdateProgress,10);
        }*/
        Log.d("YOGI","NowPlayingFragment Resumed");
    }

    private class SetBlurredAlbumArt extends AsyncTask<Bitmap, Void, Drawable>
    {

        @Override
        protected Drawable doInBackground(Bitmap... bitmaps) {
            Drawable drawable = null;
            try
            {
                drawable = ShumusicUtils.createBlurredImageFromBitmap(bitmaps[0],getContext(),12);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return drawable;
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            super.onPostExecute(drawable);
            if(drawable!=null)
            {
                if(blurredAlbumArt.getDrawable()!=null)
                {
                    final TransitionDrawable td = new TransitionDrawable(new Drawable[]{
                            blurredAlbumArt.getDrawable(),
                            drawable
                    });

                    blurredAlbumArt.setImageDrawable(td);
                    td.startTransition(150);
                }
                else {
                    blurredAlbumArt.setImageDrawable(drawable);
                }
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
    }
}
