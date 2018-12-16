package com.example.tanph.shumusic.model;

public class Constants {

    public interface ACTION{
        String UI_UPDATE = "com.example.tanph.shumusic.update.ui";
        String MAIN_ACTION = "com.example.tanph.shumusic.action.main";
        String PREVIOUS_ACTION = "com.example.tanph.shumusic.action.previous";
        String NEXT_ACTION = "com.example.yogi.com.example.tanph.shumusic.action.next";
        String PLAY_PAUSE_ACTION = "com.example.tanph.shumusic.action.play_pause";
        String DISMISS_ACTION = "com.example.tanph.shumusic.action.dismiss";
        String SWIPE_TO_DISMISS_ACTION = "com.example.tanph.shumusic.action.swipe_to_dismiss";
    }

    public interface FONTS{
        int MONOSPACE = 0;
        int NORMAL = 1;
        int SANS_SERIF = 2;
        int SERIF = 3;
    }

    public interface TABS{
        int ALBUMS = 0;
        int TRACKS = 1;
        int ARTISTS = 2;

        String ALBUMS_NAME = "Album";
        String TRACKS_NAME = "Track";
        String ARTISTS_NAME = "Artist";

        int NUMBER_OF_TABS = 3;
        String DEFAULT_SEQ = "0,1,2";
    }

    public interface FRAGMENT_STATUS{
        int TRACK_FRAGMENT = 0,ALBUM_FRAGMENT = 1,ARTIST_FRAGMENT = 2;
    }

    public interface SORT_ORDER{
        int ASC = 0;
        int DESC = 1;
    }
}

