package com.example.tanph.shumusic.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.example.tanph.shumusic.MyApplication;
import com.example.tanph.shumusic.R;
import com.example.tanph.shumusic.activity.MainActivity;
import com.example.tanph.shumusic.model.Constants;
import com.example.tanph.shumusic.pojo.SingleTrackItem;
import com.example.tanph.shumusic.util.MusicLibrary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener,MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener,MediaPlayer.OnInfoListener,
        MediaPlayer.OnBufferingUpdateListener,AudioManager.OnAudioFocusChangeListener{

    public static final int STOPPED = -1, PAUSED = 0, PLAYING =1;


    public boolean onTaskRemoved = false;
    //Notification Manager and Notification Id
    private static final int NOTIFICATION_ID = 101;
    private NotificationManager notificationManager;

    //MediaPlayer
    private MediaPlayer mediaPlayer;

    //For MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSessionCompat;
    private MediaControllerCompat mediaControllerCompat;
    private PlaybackStateCompat.Builder playbackStateBuilder;
    private MediaControllerCompat.TransportControls transportControls;

    //Audio Player
    /**
     * AudioFocus Start here
     */

    private AudioManager audioManager;

    //For API level 26(O) and above
    private AudioFocusRequest mFocusRequest;
    private AudioAttributes mAudioAttributes;
    //

    /**
     * AudioFocus End here
     */

    //Binder to given to clients
    private final IBinder binder = new LocalBinder();

    //Now Playing Item,PlayList and status
    private ArrayList<String> trackList = new ArrayList<>();
    private SingleTrackItem currentTrack;
    private int currentTrackPosition;
    private int status;
    private int resumePosition;
    private boolean mAudioPauseBecauseOfFocusLoss = false;

    //Volume Adjustments
    private int currentVolume = 0;
    private Handler volumehandler;
    private boolean mVolumeBeingRaised=false;
    private boolean mVolumeBeingDecreased = false;

    //Receiver for HeadSet Plug and UnPlug.
    //If Minimum API level is LolliPop then use AudioManager.ACTION_HEADSET_PLUG instead.
    private IntentFilter headsetFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
    private HeadSetReceiver headSetReceiver =  new HeadSetReceiver();
    private BroadcastReceiver mBroadcastReceiver;

    //Handle Incoming Phone Calls
    private boolean ongoingCall = false;
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;

    //Pending Intents for Notification

    private PendingIntent open_ShuMusic_Intent;
    private PendingIntent swipe_To_Dismiss_Intent;
    private PendingIntent previous_Intent;
    private PendingIntent play_pause_Intent;
    private PendingIntent next_Intent;
    private PendingIntent dismiss_Intent;


    /**
     *
     * Service LifeCycle Methods
     *
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        Log.d("SHU","onBind Service Thread : "+Thread.currentThread());
        return binder;
    }


    //First call received when an activity starts this service

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("SHU","onCreate Service Thread : "+Thread.currentThread());
        volumehandler = new Handler(Looper.getMainLooper());

        //Initialize all the pending Intents for Notifications
        initializeIntents();

        //Initialize all the Receivers
        initializeReceivers();

        //Need to Initialize the Media Session First;
        initializeMediaSession();

        //Initialize the notification manager
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //Initialize AudioManager
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        //Initialize the AudioAttributes and AudioFocusRequest for API>26(O)
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
        {
            mAudioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();

            mFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(mAudioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this)
                    .setWillPauseWhenDucked(true)
                    .build();
        }

        //Initialize the Media Player
        initMediaPlayer();


        //Initialize Current Track Position and Stress
        currentTrackPosition = -1;
        setStatus(STOPPED);

        //Register the Receiver for HeadSet Unplugged.
        //As this Broadcast is global so don't register with LocalBroadcastmanager
        this.registerReceiver(headSetReceiver,headsetFilter);

        //Manage Incoming Phone Calls
        //Pause the player on incoming Call
        //Resume on Hangup;

        //callStateListener();

        //ACTION_AUDIO_BECOME_NOISY : Change in audio outputs

        registerNoisyReceiver();
    }

    private void initializeMediaSession() {

        //Initialize MediaSessionCompat Object
        mediaSessionCompat = new MediaSessionCompat(getApplicationContext(),getPackageName()+".MediaSessionCompat");

        //Set the flags
        mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS|
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);

        //Set the Callback to receives Transport Controls, Media Button events
        mediaSessionCompat.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                resumeMedia();
                super.onPlay();
            }

            @Override
            public void onPause() {
                pauseMedia();
                super.onPause();
            }

            @Override
            public void onSkipToNext() {
                skipToNext();
                super.onSkipToNext();
            }

            @Override
            public void onSkipToPrevious()
            {
                skipToPrevious();
                super.onSkipToPrevious();
            }

            @Override
            public void onStop()
            {
                stopMedia();
                super.onStop();
            }

            @Override
            public void onSeekTo(long pos) {
                super.onSeekTo(pos);
            }

            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
                return super.onMediaButtonEvent(mediaButtonEvent);
            }
        });

        // Create an instance of PlaybackState
        //Set Initial state as Stopped , position in ms is 0 and normal playback speed is 1.0
        //Set the current capabilities available on this session using setActions()

        playbackStateBuilder = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_STOPPED,0,1)
                .setActions(PlaybackStateCompat.ACTION_PLAY|PlaybackStateCompat.ACTION_PAUSE
                        |PlaybackStateCompat.ACTION_PLAY_PAUSE|PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        |PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS|PlaybackStateCompat.ACTION_STOP
                        |PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID);

        mediaSessionCompat.setPlaybackState(playbackStateBuilder.build());
        mediaSessionCompat.setActive(true);

        //Initialize the MediaControllerCompat.TransportControls
        transportControls = mediaSessionCompat.getController().getTransportControls();

    }

    private void initializeReceivers() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction())
                {
                    case Constants.ACTION.PLAY_PAUSE_ACTION:
                        if(status==PLAYING)
                        {
                            transportControls.pause();
                        }
                        else
                        {
                            transportControls.play();
                        }

                        break;
                    case Constants.ACTION.PREVIOUS_ACTION:
                        transportControls.skipToPrevious();
                        break;
                    case Constants.ACTION.NEXT_ACTION:
                        transportControls.skipToNext();
                        break;
                    case Constants.ACTION.DISMISS_ACTION:
                        if(getStatus()==PLAYING)
                        {
                            //Note that pause transaction is an async Transaction.
                            mediaPlayer.pause();
                            setStatus(PAUSED);
                            setMediaSessionState();
                            resumePosition = mediaPlayer.getCurrentPosition();
                            //buildNotification();
                            notifyUI();

                            //pauseMedia();
                        }

                        stopForeground(true);
                        if(onTaskRemoved == true)
                        {
                            stopSelf();
                        }
                        /*notificationManager.cancelAll();*/
                        break;
                    case Constants.ACTION.SWIPE_TO_DISMISS_ACTION:
                        stopForeground(true);
                        if(onTaskRemoved == true)
                        {
                            stopSelf();
                        }
                        break;
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION.PLAY_PAUSE_ACTION);
        intentFilter.addAction(Constants.ACTION.NEXT_ACTION);
        intentFilter.addAction(Constants.ACTION.PREVIOUS_ACTION);
        intentFilter.addAction(Constants.ACTION.DISMISS_ACTION);
        intentFilter.addAction(Constants.ACTION.SWIPE_TO_DISMISS_ACTION);

        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(mBroadcastReceiver,intentFilter);
    }

    private void initializeIntents() {
        //Notification Pending Intent

        Intent notificationIntent;
        notificationIntent = new Intent(this,MainActivity.class);
        notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        open_ShuMusic_Intent = PendingIntent.getActivity(this,0
                ,notificationIntent,0);


        //previous Pending Intent

        Intent previousIntent = new Intent(this,MediaPlayerService.class);
        previousIntent.setAction(Constants.ACTION.PREVIOUS_ACTION);
        previous_Intent = PendingIntent.getService(this,0
                ,previousIntent,0);

        //play_pause Pending Intent

        Intent playpauseIntent = new Intent(this,MediaPlayerService.class);
        playpauseIntent.setAction(Constants.ACTION.PLAY_PAUSE_ACTION);
        play_pause_Intent = PendingIntent.getService(this,0
                ,playpauseIntent,0);

        //next Pending Intent

        Intent nextIntent = new Intent(this,MediaPlayerService.class);
        nextIntent.setAction(Constants.ACTION.NEXT_ACTION);
        next_Intent = PendingIntent.getService(this,0
                ,nextIntent,0);

        //dismiss Pending Intent

        Intent dismissIntent = new Intent(this,MediaPlayerService.class);
        dismissIntent.setAction(Constants.ACTION.DISMISS_ACTION);
        dismiss_Intent = PendingIntent.getService(this, 0
                , dismissIntent, 0);

        //swipe to Dismiss Intent

        Intent swipetodismissIntent = new Intent(this,MediaPlayerService.class);
        swipetodismissIntent.setAction(Constants.ACTION.SWIPE_TO_DISMISS_ACTION);
        swipe_To_Dismiss_Intent = PendingIntent.getService(this,0
                ,swipetodismissIntent,0);
    }


    //Going to be ca    lled for every Intent Request.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //onTaskRemoved = false;
        Log.d("SHU","onStartCommand action = "+intent.getAction());

        //Set the Service in the MyApplication If it is not
        if (MyApplication.getService() == null)
        {
            MyApplication.setService(this);
        }

        MediaButtonReceiver.handleIntent(mediaSessionCompat,intent);

        //just send the incoming intent as a broadcast

        if(intent!=null && intent.getAction()!=null)
        {
            /*Log.d("SHU","action = "+intent.getAction());*/
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
        return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {

        Log.d("SHU","onUnbind Service");
        return super.onUnbind(intent);
    }

    @Override    public void onDestroy() {

        Log.d("SHU","onDestroy Service");
        super.onDestroy();

        //Release the MediaPlayer
        if(mediaPlayer!=null)
        {
            mediaPlayer.pause();
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
        }

        //Remove the Audio Focus
        removeAudioFocus();

        //Unregsiter All Kind of Receivers

        unregisterReceiver(noisyReceiver);
        unregisterReceiver(headSetReceiver);
        LocalBroadcastManager.getInstance(getApplicationContext())
                .unregisterReceiver(mBroadcastReceiver);

        //Disable the call state lisntner
        if(telephonyManager!=null)
        {
            telephonyManager.listen(phoneStateListener,PhoneStateListener.LISTEN_NONE);
        }

        //Remove the Notification
        removeNotification();

        //Remove the MediaSession as well
        mediaSessionCompat.setActive(false);
        mediaSessionCompat.release();
    }

    private void removeNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus()
    {
        return status;
    }

    public SingleTrackItem getCurrentTrack()
    {
        if(currentTrackPosition < 0 )
            return null;
        return currentTrack;
    }

    public void seekTo(int progress) {
        if(mediaPlayer!=null)
        {
            mediaPlayer.seekTo(progress);
        }
    }

    /**
     *
     * Binder Class
     *
     */

    public class LocalBinder extends Binder
    {
        public MediaPlayerService getService()
        {
            return MediaPlayerService.this;
        }
    }

    /**
     *
     *MediaPlayer Callback Methods
     *
     */

    @Override
    public void onCompletion(MediaPlayer mp) {

        if(currentTrackPosition==trackList.size()-1)
        {
            playTrack(0);
            currentTrackPosition = 0;
            buildNotification();
            notifyUI();
        }
        else
        {
            skipToNext();
        }

        /*stopMedia();

        removeNotification();

        //stop the service
        stopSelf();*/
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //MediaPlayer is now in Prepared State
        //and ready to playback.
        playMedia();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        switch (what)
        {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {

    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    /**
     *
     *
     * CallBack for AudioFocusChange
     *
     * */


    @Override
    public void onAudioFocusChange(int focusState)
    {
        //Invoked when the Audio Focus of the system has been update

        switch (focusState)
        {
            case AudioManager.AUDIOFOCUS_GAIN:
                //resume player
                if(getStatus()!=PLAYING)
                {
                    if(mAudioPauseBecauseOfFocusLoss==true)
                    {
                        Log.d("SHU","call gone permanenet");
                        resumeMedia();
                        mAudioPauseBecauseOfFocusLoss = false;
                    }

                }


                mediaPlayer.setVolume(1.0f,1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                //Lost focus for indefinite amount of time
                //stop playback
                if(getStatus() == PLAYING)
                {
                    mAudioPauseBecauseOfFocusLoss = true;
                    Log.d("SHU","call incoming");
                    pauseMedia();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                //AudioFocus has been lost for a short time
                //and is likely to resume. So just pause the media playback
                if(getStatus() == PLAYING)
                {
                    mAudioPauseBecauseOfFocusLoss = true;
                    Log.d("SHU","call incoming transient");
                    pauseMedia();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                //Lost focus for short time. but we can continue our playback
                //but with volume ducked down.
                if(getStatus() == PLAYING)
                {
                    mediaPlayer.setVolume(0.2f,0.2f);
                }
                break;
        }
    }

    /**
     *
     * Request Audio Focus
     *
     */

    private boolean requestAudioFocus()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            int result = audioManager.requestAudioFocus(mFocusRequest);
            if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            int result = audioManager.requestAudioFocus(this,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
            if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            {
                return true;
            }
            else
            {
                return false;
            }
        }
    }

    private void removeAudioFocus()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            audioManager.abandonAudioFocusRequest(mFocusRequest);
        }
        else
        {
            audioManager.abandonAudioFocus(this);
        }
    }

    /**
     *
     * MediaPlayer Actions
     *
     */

    private void initMediaPlayer(){
        if(mediaPlayer==null)
        {
            mediaPlayer = new MediaPlayer();//New MediaPlayer instance
        }

        //Set up the MediaPlayer Event Listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);

        //Reset the MediaPlayer so that it may not be pointing to another data source
        mediaPlayer.reset();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            mediaPlayer.setAudioAttributes(mAudioAttributes);
        }
        else
        {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }
    }

    private void playMedia()
    {
        if(!mediaPlayer.isPlaying())
        {
            mediaPlayer.start();
            setStatus(PLAYING);
        }
        Log.d("SHU","Media Started");
    }

    public void pauseMedia()
    {
        if(mediaPlayer.isPlaying())
        {
            //Note that pause transaction is an async Transaction.
            mediaPlayer.pause();
            setStatus(PAUSED);
            setMediaSessionState();
            resumePosition = mediaPlayer.getCurrentPosition();
            buildNotification();
            notifyUI();
        }
        Log.d("SHU","Media Paused");
    }

    private void stopMedia()
    {
        if(mediaPlayer==null)
            return;
        if(mediaPlayer.isPlaying())
        {
            //Note : You can call stop() from prepared,started,paused
            // and playbackcompleted stated
            mediaPlayer.pause();
        }
        mediaPlayer.stop();
        mediaPlayer.reset();
        setStatus(STOPPED);
        setMediaSessionState();
    }

    public void resumeMedia()
    {
        if(!mediaPlayer.isPlaying())
        {
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
            increaseVolumeGradually();
            setStatus(PLAYING);
            setMediaSessionState();
            buildNotification();
            notifyUI();
        }
        Log.d("SHU","Media Resumed");
    }

    public void skipToNext() {
        if(currentTrackPosition==trackList.size()-1)
        {
            playTrack(0);
            currentTrackPosition = 0;
        }
        else
        {
            playTrack(currentTrackPosition+1);
        }
        buildNotification();
        notifyUI();
    }

    public void skipToPrevious()
    {
        if(currentTrackPosition==0)
        {
            playTrack(trackList.size()-1);
            currentTrackPosition = trackList.size()-1;
        }
        else
        {
            playTrack(currentTrackPosition-1);
        }
        buildNotification();
        notifyUI();
    }

    private void playTrack(int position) {

        SingleTrackItem trackItem = MusicLibrary.getInstance().getTrackItemFromTitle(trackList.get(position));

        if(trackItem == null)
        {
            //Something is wrong with Storage or TrackItem may be deleted!!!

            //Post the task in the main thread using Handler

            Handler handler = new Handler(getApplicationContext().getMainLooper());

            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext()
                            ,"Something is wrong!! Track may be deleted or Database is not available"
                            ,Toast.LENGTH_LONG).show();
                }
            });
            return;
        }

        //Set the Current Track and position
        currentTrack = trackItem;
        currentTrackPosition = position;

        //Request The Audio Focus Before Playing
        if(!requestAudioFocus())
        {
            //If not granted then just return
            return;
        }

        //If the mediaPlayer is either paused or playing then first stop that
        if(status!=STOPPED)
        {
            /*stopMedia();*/

            if(mediaPlayer.isPlaying())
            {
                mediaPlayer.pause();
            }
            mediaPlayer.stop();
            mediaPlayer.reset();
            setStatus(STOPPED);
        }

        //set The data source for the media
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(currentTrack.getFile_path());
            mediaPlayer.prepareAsync();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        //Increase the Volume Gradually
        increaseVolumeGradually();

        //Set the Status of MediaPlayer
        setStatus(PLAYING);

        //Set the Session State and SessionMetaData as well
        setMediaSessionState();

        setSessionMetaData(true);


    }

    private void setSessionMetaData(boolean enable) {
        //spawn a new thread to set the metadata

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                MediaMetadataCompat.Builder mediaMetadataBuilder = new MediaMetadataCompat.Builder();
                mediaMetadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE,currentTrack.getTitle());
                mediaMetadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM,currentTrack.getAlbum_name());
                mediaMetadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST,currentTrack.getArtist_name());

                Uri uri = MusicLibrary.getInstance().getAlbumUri(getCurrentTrack().getAlbum_id());
                Bitmap albumArt = null;
                if (uri != null)
                {
                    mediaMetadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                            uri.toString());
                    try {
                        albumArt = Glide.
                                with(MediaPlayerService.this)
                                .asBitmap()
                                .load(uri.toString())
                                /*.into(new SimpleTarget<Bitmap>()
                                {

                                    @Override
                                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                                    }
                                });*/
                                .submit(Target.SIZE_ORIGINAL,Target.SIZE_ORIGINAL)
                                .get();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }

                }
                else
                {
                    mediaMetadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                            null);
                }
                mediaMetadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION,currentTrack.getDurInt());

                mediaMetadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART,albumArt);

                mediaSessionCompat.setMetadata(mediaMetadataBuilder.build());
            }
        });
    }

    private void setMediaSessionState() {

        if(status == PLAYING)
        {
            playbackStateBuilder.setState(PlaybackStateCompat.STATE_PLAYING,0,1);
        }
        else if(status == PAUSED)
        {
            playbackStateBuilder.setState(PlaybackStateCompat.STATE_PAUSED,0,1);
        }
        else
        {
            playbackStateBuilder.setState(PlaybackStateCompat.STATE_STOPPED,0,1);
        }
        mediaSessionCompat.setPlaybackState(playbackStateBuilder.build());
    }

    public void setTrackList(ArrayList<String> temp)
    {
        trackList.clear();
        trackList.addAll(temp);
    }

    public ArrayList<String> getTrackList()
    {
        return trackList;
    }

    public void playAtPosition(int position)
    {
        playTrack(position);
        buildNotification();
        notifyUI();
    }

    private void notifyUI()
    {
        Intent UI_Notification_Intent = new Intent();
        UI_Notification_Intent.setAction(Constants.ACTION.UI_UPDATE);
        LocalBroadcastManager.getInstance(getApplicationContext())
                .sendBroadcast(UI_Notification_Intent);
    }


    /**
     *
     * Handling Incoming Call's
     *
     */

    private void callStateListener()
    {
        //Get the Telephony Manager
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        //start listening for PhoneState Changes
        phoneStateListener = new PhoneStateListener(){
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);

                switch (state)
                {
                    //if atleast one call exists or Phone is rining
                    //pause the media player
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if(getStatus() == PLAYING)
                        {
                            Log.d("SHU","Call is Ringing, Media Player Paused");
                            pauseMedia();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if(mediaPlayer!=null)
                        {
                            if(ongoingCall)
                            {
                                Log.d("SHU","Call Halted, Media Player Resumed");
                                ongoingCall = false;
                                resumeMedia();
                            }
                        }
                        break;
                }
            }
        };


        //Register the listener with the telephony manager
        //Listen for the changes to the device call state

        telephonyManager.listen(phoneStateListener,PhoneStateListener.LISTEN_CALL_STATE);
    }

    /**
     *
     *Receiver for ACTION_AUDIO_BECOMES_NOISY
     *
     */

    private BroadcastReceiver noisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //pause audio for this event
            if(intent.getAction()==AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            {
                pauseMedia();
            }
        }
    };

    private void registerNoisyReceiver() {
        //Register before playing and unregister after playing
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

        registerReceiver(noisyReceiver,intentFilter);
    }

    private void buildNotification()
    {
        Executors.newSingleThreadExecutor().execute(new Runnable() {

            @Override
            public void run() {
                if(currentTrack==null)
                    return;


                //For android O+, use notification_channel
                NotificationCompat.Builder builder = new NotificationCompat.Builder(MediaPlayerService.this,getString(R.string.notification_channel_id));

                Bitmap largeIcon = null;
                try
                {


                    largeIcon = Glide.
                            with(MediaPlayerService.this)
                            .asBitmap()
                            .load(MusicLibrary.getInstance().getAlbumUri(getCurrentTrack().getAlbum_id()).toString())
                            /*.into(new SimpleTarget<Bitmap>()
                            {

                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                                }
                            });*/
                            .submit(Target.SIZE_ORIGINAL,Target.SIZE_ORIGINAL)
                            .get();



                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }


                //show the time stamp
                builder.setShowWhen(true);
                //Attach the Notification Style
                android.support.v4.media.app.NotificationCompat.MediaStyle mediaStyle = new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        //show the playback Controls in Compat View
                        .setShowActionsInCompactView(0,1,2);

                if(mediaSessionCompat!=null)
                {
                    //Attach the MediaSession Token so that System UI can Identify this
                    //as an active MediaSession  and respond accordingly
                    mediaStyle.setMediaSession(mediaSessionCompat.getSessionToken());
                }
                builder.setStyle(mediaStyle);
                //set the notification color
                builder.setColor(getResources().getColor(R.color.colorAccent));

                //set the large and small icons
                builder.setSmallIcon(android.R.drawable.stat_sys_headset);

                if(largeIcon!=null)
                {
                    builder.setLargeIcon(largeIcon);
                }
                else
                {
                    builder.setLargeIcon(BitmapFactory.decodeResource(MediaPlayerService.this.getResources(),R.drawable.image1));
                }


                String next = "Next: ";
                if(currentTrackPosition == trackList.size()-1)
                {
                    next += "Empty Queue";
                }
                else
                {
                    next += trackList.get(currentTrackPosition+1);
                }

                //set the title and text of the
                builder.setContentTitle(getCurrentTrack().getTitle());
                builder.setContentText(next);

                //add the actions
                builder.addAction(new NotificationCompat.Action(R.drawable.ic_skip_previous_black_24dp,"previous",previous_Intent));
                if(getStatus()==PLAYING)
                {
                    Log.d("SHU","NOTIFICATION PLAYINGF and PAUSE BUTTON");
                    builder.addAction(new NotificationCompat.Action(R.drawable.ic_pause_black_24dp,"play_pause",play_pause_Intent));
                }
                else
                {
                    builder.addAction(new NotificationCompat.Action(R.drawable.ic_play_arrow_black_24dp,"play_pause",play_pause_Intent));
                    Log.d("SHU","NOTIFICATION PAUSED or STOPPED and PLAY BUTTON");
                }

                builder.addAction(new NotificationCompat.Action(R.drawable.ic_skip_next_black_24dp,"next",next_Intent));
                builder.addAction(new NotificationCompat.Action(R.drawable.ic_close_black_24dp,"dismiss",dismiss_Intent));

                //Set Ticker Message
                builder.setTicker("ShuMusic");
                //Dismiss Notification upon click ?
                builder.setAutoCancel(false);
                //Set PendingIntent for Notification Click
                builder.setContentIntent(open_ShuMusic_Intent);
                //Set Pending Intent for swipe to dissmiss
                builder.setDeleteIntent(swipe_To_Dismiss_Intent);

                //Set the Priority
                builder.setPriority(NotificationCompat.PRIORITY_MAX);
                //Set the Visibility
                builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

                //Create Notification Channel for API 26+
                createNotificationChannel();


                notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

                if(getStatus() == PLAYING)
                {
                    startForeground(NOTIFICATION_ID,builder.build());
                    Log.d("SHU","NOTIFICATION PLAYINGF");
                }
                else
                {
                    stopForeground(false);
                    //Post a notification to be shown in the status bar
                    notificationManager.notify(NOTIFICATION_ID,builder.build());
                    Log.d("SHU","NOTIFICATION PAUSED or STOPPED");
                }
            }
        });
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(getString(R.string.notification_channel_id), name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }




    //Receiver for HeadSet Plug and UnPlug
    private class HeadSetReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Intent.ACTION_HEADSET_PLUG))
            {
                int state = intent.getIntExtra("state",-1);
                switch (state)
                {
                    case 0:
                        Log.d("SHU","headset is unplugged");
                        break;
                    case 1:
                        Log.d("SHU","headset is plugged");
                        break;
                    default:
                        Log.d("SHU","No Idea about Headset Status");
                }
            }
        }
    }


    private void increaseVolumeGradually() {
        //have to increase the volume from zero to current volume level gradually
        if(!mVolumeBeingRaised)
        {
            currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
        else
        {
            volumehandler.removeCallbacksAndMessages(gradualIncreaseVolumeRunnable);
        }
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,0,0);
        volumehandler.post(gradualIncreaseVolumeRunnable);


    }

    private Runnable gradualIncreaseVolumeRunnable = new Runnable() {
        @Override
        public void run() {
            if(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)<currentVolume)
            {
                mVolumeBeingRaised = true;
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)+1,0);
                volumehandler.postDelayed(this,100);
            }
            else
            {
                mVolumeBeingRaised = false;
            }
        }
    };

    public int getCurrentPosition()
    {
        return mediaPlayer.getCurrentPosition();
    }

    public int getTotalDuration()
    {
        return mediaPlayer.getDuration();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        onTaskRemoved = true;
        Log.d("SHU","onTaskRemoved = "+onTaskRemoved);
        if(getStatus()!=PLAYING)
        {
            stopForeground(true);
            stopSelf();
        }
    }
}
