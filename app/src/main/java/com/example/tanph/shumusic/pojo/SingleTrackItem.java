package com.example.tanph.shumusic.pojo;

import android.media.MediaMetadataRetriever;

import com.example.tanph.shumusic.MyApplication;
import com.example.tanph.shumusic.R;

import java.io.Serializable;

public class SingleTrackItem implements Serializable {
    private int id;
    private String title;

    private int artist_id;
    private String artist_name;

    private int album_id;
    private String album_name;

    private String file_path;
    private String duration;

    //Default Constructor
    public SingleTrackItem(){

    }

    public SingleTrackItem(int id,String title,int artist_id,String artist_name,int album_id,
                           String album_name,String file_path,String duration)
    {
        this.id = id;
        this.title = title;
        this.artist_id = artist_id;
        this.artist_name = artist_name;
        this.album_id = album_id;
        this.album_name = album_name;
        this.file_path = file_path;
        this.duration = duration;
    }

    //constructor for creating from Data Source using MediaMetadataRetriever

    public SingleTrackItem(String file_path)
    {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(file_path);
        this.title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        this.artist_name = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        this.album_name = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
        this.duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        this.file_path = file_path;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getArtist_id() {
        return artist_id;
    }

    public void setArtist_id(int artist_id) {
        this.artist_id = artist_id;
    }

    public String getArtist_name() {
        return artist_name;
    }

    public void setArtist_name(String artist_name) {
        this.artist_name = artist_name;
    }

    public int getAlbum_id() {
        return album_id;
    }

    public void setAlbum_id(int album_id) {
        this.album_id = album_id;
    }

    public String getAlbum_name() {
        return album_name;
    }

    public void setAlbum_name(String album_name) {
        this.album_name = album_name;
    }

    public String getFile_path() {
        return file_path;
    }

    public void setFile_path(String file_path) {
        this.file_path = file_path;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public int getDurInt()
    {
        int durationInms = 0;

        durationInms = Integer.parseInt(duration);
        return durationInms;
    }

    public String makeShortTimeString()
    {
        long durationInSec = 0L,hours,mins;

        durationInSec = (Long.parseLong(duration))/1000;
        hours = durationInSec/3600;
        durationInSec%=3600;
        mins = durationInSec/60;
        durationInSec%=60;

        final String durationFormat = MyApplication.getContext().getResources().getString(
                hours == 0 ? R.string.durationformatshort : R.string.durationformatlong);

        return String.format(durationFormat,hours,mins,durationInSec);


    }
}
