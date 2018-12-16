package com.example.tanph.shumusic.pojo;

public class SingleDataItem {

    //General Info
    public int id;
    public String title;

    //Artist Related Info
    public int artist_id;
    public String artist_name;

    //Album Related Info
    public int album_id;
    public String album_name;

    public String duration;
    public String year;

    public String file_path;

    public int number_of_albums;
    public int number_of_tracks;

    //Constructor For Single Track Related Info
    public SingleDataItem(int id,String title,int artist_id,String artist_name,int album_id,
                          String album_name,String duration,String year,String file_path )
    {
        this.id = id;
        this.title = title;
        this.artist_id = artist_id;
        this.artist_name = artist_name;
        this.album_id = album_id;
        this.album_name = album_name;
        this.duration = getDurInTimeFormat(duration);
        this.year = year;
        this.file_path = file_path;
    }

    //Constructor For Single Artist Related Info
    public SingleDataItem(int id,String artist_name,
                          int number_of_albums,int number_of_tracks)
    {
        this.artist_id = id;
        this.title = artist_name;
        this.artist_name = artist_name;
        this.number_of_albums = number_of_albums;
        this.number_of_tracks = number_of_tracks;
    }

    //Constructor For Single Album Related Info
    public SingleDataItem(int id,String album_name,String artist_name,
                          int number_of_tracks,String year)
    {
        this.album_id = id;
        this.album_name = album_name;
        this.artist_name = artist_name;
        this.number_of_tracks = number_of_tracks;
        this.year = year;
    }

    private String getDurInTimeFormat(String duration) {
        int minutes = (((Integer.parseInt(duration))/1000)/60);
        int seconds = (((Integer.parseInt(duration))/1000)%60);
        String secondStr = String.format("%02d",seconds);
        String durFormat = String.valueOf(minutes)+":"+secondStr;
        return durFormat;
    }
}
