package com.example.tanph.shumusic.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.example.tanph.shumusic.MyApplication;
import com.example.tanph.shumusic.R;
import com.example.tanph.shumusic.model.Constants;
import com.example.tanph.shumusic.pojo.SingleDataItem;
import com.example.tanph.shumusic.pojo.SingleTrackItem;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MusicLibrary {

    private static MusicLibrary musicLibrary;
    private Context context;
    private ContentResolver contentResolver;
    private AtomicInteger atomicInteger = new AtomicInteger();
    private int libraryLoadCounter;

    //Removing Short Clip Time
    private int SHORT_CLIP_TIME_IN_MS;

    //Removeing Tracks Containing Some Character
    private String REMOVE_TRACK_CONTAINING_1,REMOVE_TRACK_CONTAINING_2,REMOVE_TRACK_CONTAINING_3;

    //Folder's List
    private ArrayList<String> foldersList = new ArrayList<>();

    //Data For Tracks Fragment
    private ArrayList<SingleDataItem> dataItemsForTracks = new ArrayList<>();

    //Data For Artist Fragment
    private ArrayList<SingleDataItem> dataItemsForArtists = new ArrayList<>();

    //Data For Album Fragment
    private ArrayList<SingleDataItem> dataItemsForAlbums = new ArrayList<>();


    private MusicLibrary()
    {
        this.context = MyApplication.getContext();
        this.contentResolver = context.getContentResolver();
        RefreshLibrary();
    }

    public static MusicLibrary getInstance()
    {
        if(musicLibrary==null)
        {
            musicLibrary = new MusicLibrary();
        }
        return musicLibrary;
    }

    private void RefreshLibrary() {

        //Filter Audio Based on Track Duration
        SHORT_CLIP_TIME_IN_MS = MyApplication.getPref()
                .getInt(context.getString(R.string.pref_hide_short_clips),10)*1000;

        //Filter Audio Based on Name
        REMOVE_TRACK_CONTAINING_1 = MyApplication.getPref()
                .getString(context.getString(R.string.pref_hide_tracks_starting_with_1),"");
        REMOVE_TRACK_CONTAINING_2 = MyApplication.getPref()
                .getString(context.getString(R.string.pref_hide_tracks_starting_with_2),"");
        REMOVE_TRACK_CONTAINING_3 = MyApplication.getPref()
                .getString(context.getString(R.string.pref_hide_tracks_starting_with_3),"");

        atomicInteger.set(0);
        dataItemsForTracks.clear();
        dataItemsForArtists.clear();
        dataItemsForAlbums.clear();

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();

                fillDataForTracks();
                fillDataForArtists();
                fillDataForAlbums();

                while (libraryLoadCounter!=3)
                {

                }

                atomicInteger.set(0);
                libraryLoadCounter = 0;
            }
        });
    }

    private void fillDataForTracks() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                String selection = MediaStore.Audio.Media.IS_MUSIC+"!= 0";
                String[] projection = {
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST_ID,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ALBUM_ID,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.YEAR,
                        MediaStore.Audio.Media.DATA};
                String sortOrder = MediaStore.Audio.Media.TITLE+" ASC";
                Cursor cursor = null;
                cursor = contentResolver.query(uri,projection,selection,null,sortOrder);

                if(cursor!=null && cursor.getCount()>0)
                {
                    while (cursor.moveToNext())
                    {
                        //Remove Tracks Starting with the specified prefix
                        if(!REMOVE_TRACK_CONTAINING_1.equals("") &&
                                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
                                        .startsWith(REMOVE_TRACK_CONTAINING_1))
                            continue;

                        if(!REMOVE_TRACK_CONTAINING_2.equals("") &&
                                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
                                        .startsWith(REMOVE_TRACK_CONTAINING_2))
                            continue;

                        if(!REMOVE_TRACK_CONTAINING_3.equals("") &&
                                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
                                        .startsWith(REMOVE_TRACK_CONTAINING_3))
                            continue;

                        //Remove Songs with short clip
                        if(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))>SHORT_CLIP_TIME_IN_MS)
                        {
                            dataItemsForTracks.add(new SingleDataItem(
                                    cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)),
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                                    cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)),
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)),
                                    cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)),
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)),
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)),
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)),
                                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                            ));
                        }
                    }
                }
                if(cursor!=null)
                {
                    cursor.close();
                }
                libraryLoadCounter = atomicInteger.incrementAndGet();
                fillFoldersList();
            }
        });
    }

    private void fillFoldersList() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                for(SingleDataItem singleDataItem : dataItemsForTracks)
                {
                    String data_path = singleDataItem.file_path;
                    data_path = data_path.substring(0,data_path.lastIndexOf("/"));
                    if(!foldersList.contains(data_path))
                    {
                        foldersList.add(data_path);
                    }
                }
            }
        });
    }

    private void fillDataForArtists() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                Uri uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
                String[] projection = {
                        MediaStore.Audio.Artists._ID,
                        MediaStore.Audio.Artists.ARTIST,
                        MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
                        MediaStore.Audio.Artists.NUMBER_OF_TRACKS
                };

                String sortOrder = MediaStore.Audio.Artists.ARTIST+" ASC";
                Cursor cursor = null;
                cursor = contentResolver.query(uri,
                        projection,
                        null,
                        null,
                        sortOrder);

                if(cursor!=null && cursor.getCount()>0)
                {
                    while (cursor.moveToNext())
                    {
                        dataItemsForArtists.add(new SingleDataItem(
                                cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Artists._ID)),
                                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST)),
                                cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)),
                                cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS))
                        ));
                    }
                }

                if(cursor!=null)
                {
                    cursor.close();
                }

                libraryLoadCounter = atomicInteger.incrementAndGet();
            }
        });
    }

    private void fillDataForAlbums() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
                String[] projection = {
                        MediaStore.Audio.Albums._ID,
                        MediaStore.Audio.Albums.ALBUM,
                        MediaStore.Audio.Albums.ARTIST,
                        MediaStore.Audio.Albums.NUMBER_OF_SONGS,
                        MediaStore.Audio.Albums.FIRST_YEAR
                };
                String sortOrder = MediaStore.Audio.Albums.ALBUM+" ASC";
                Cursor cursor = null;
                cursor = contentResolver.query(uri,
                        projection,
                        null,
                        null,
                        sortOrder);

                if(cursor!=null & cursor.getCount()>0)
                {
                    while (cursor.moveToNext())
                    {
                        dataItemsForAlbums.add(
                                new SingleDataItem(
                                        cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Albums._ID)),
                                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM)),
                                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST)),
                                        cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS)),
                                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.FIRST_YEAR))
                                )
                        );
                    }
                }
                if(cursor!=null)
                {
                    cursor.close();
                }
                libraryLoadCounter = atomicInteger.incrementAndGet();
            }
        });
    }

    public Uri getAlbumUri(int album_id)
    {
        Uri mArtWorkUri = Uri.parse("content://media/external/audio/albumart");
        Uri mImageUri = ContentUris.withAppendedId(mArtWorkUri, album_id);
        //Log.d("YOGI", "getAlbumArtUri: path " + new File(mImageUri.getPath()).length());
        if(mImageUri==null){
            //String packageName = context.getPackageName();
            //Uri uri = Uri.parse("android.resource://"+packageName+"/drawable/ic_batman_1");
            return null;
            //return getUriToDrawable(context,R.drawable.ic_batman_1);
        }
        return mImageUri;
        /*Cursor albumCursor = contentResolver.query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Albums.ALBUM_ART,
                        MediaStore.Audio.Albums._ID},
                MediaStore.Audio.Albums._ID+" = ?",
                new String[]{String.valueOf(album_id)},
                null);

        boolean queryResult = albumCursor.moveToFirst();
        String result = null;

        if(queryResult)
        {
            result = albumCursor.getString(albumCursor
                    .getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
        }
        try {
            return Uri.parse(result);
        }
        catch (NullPointerException npe)
        {
            //Removed below Toast as it was getting executed for every single null album art URI
            *//*Toast.makeText(MyApplication.getContext(),"album art Uri is null",Toast.LENGTH_LONG).show();*//*
            return null;
        }*/

    }

    public ArrayList<SingleDataItem> getDataItemsForTracks(){
        return dataItemsForTracks;
    }

    public ArrayList<SingleDataItem> getDataItemsForArtists(){
        return dataItemsForArtists;
    }

    public ArrayList<SingleDataItem> getDataItemsForAlbums(){
        return dataItemsForAlbums;
    }

    public ArrayList<String> getSongListFromAlbumId(int album_id,int sort_order)
    {
        ArrayList<String> arrayList = new ArrayList<>();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC+"!=0"+" AND "+MediaStore.Audio.Media.ALBUM_ID+"="+album_id;

        String sortorder="";
        if(sort_order == Constants.SORT_ORDER.ASC)
        {
            sortorder = MediaStore.Audio.Media.TITLE+" ASC";
        }
        else
        {
            sortorder = MediaStore.Audio.Media.TITLE+" DESC";
        }

        Cursor cursor = null;

        cursor = contentResolver.query(
                uri,
                projection,
                selection,
                null,
                sortorder
        );

        if(cursor!=null)
        {
            while (cursor.moveToNext())
            {
                if(cursor.getInt(1)>SHORT_CLIP_TIME_IN_MS)
                {
                    arrayList.add(cursor.getString(0));
                }
            }

            cursor.close();
            return arrayList;
        }
        return null;
    }

    public ArrayList<String> getSongListFromArtistId(int artist_id,int sort_order)
    {
        ArrayList<String> arrayList = new ArrayList<>();


        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        /*uri = MediaStore.Audio.Artists.Albums.getContentUri();*/
        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC+"!=0"+" AND "+MediaStore.Audio.Media.ARTIST_ID+"="+artist_id;

        String sortorder="";
        if(sort_order == Constants.SORT_ORDER.ASC)
        {
            sortorder = MediaStore.Audio.Media.TITLE+" ASC";
        }
        else
        {
            sortorder = MediaStore.Audio.Media.TITLE+" DESC";
        }

        Cursor cursor = null;

        cursor = contentResolver.query(
                uri,
                projection,
                selection,
                null,
                sortorder
        );

        if(cursor!=null)
        {
            while (cursor.moveToNext())
            {
                if(cursor.getInt(1)>SHORT_CLIP_TIME_IN_MS)
                {
                    arrayList.add(cursor.getString(0));
                }
            }

            cursor.close();
            return arrayList;
        }
        return null;
    }

    public SingleTrackItem getTrackItemFromTitle(String title) {
        // title could contain "'", It will affect in SQL search
        //So replace all "'" with "''"
        if(title.contains("'"))
        {
            title = title.replaceAll("'","''");
        }

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC+"!= 0"+" AND "+
                MediaStore.Audio.Media.TITLE+" = '"+title+"'";

        Cursor cursor = null;

        cursor = contentResolver.query(
                uri,
                projection,
                selection,
                null,
                null
        );

        if(cursor!=null && cursor.getCount()>0)
        {
            cursor.moveToFirst();

            SingleTrackItem singleTrackItem = new SingleTrackItem(
                    cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)),
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                    cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)),
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)),
                    cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)),
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)),
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)),
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
            );
            cursor.close();
            return singleTrackItem;
        }
        return null;
    }

    public int getFirstAlbumId(int artist_id) {

        Uri uri = MediaStore.Audio.Artists.Albums.getContentUri("external",artist_id);
        String[] projection = {
                MediaStore.Audio.Albums._ID
        };


        Cursor cursor = null;

        cursor = contentResolver.query(
                uri,
                projection,
                null,
                null,
                null);

        Log.d("SHU","getFirstAlbumID Cursor = "+cursor);
        if(cursor!=null && cursor.getCount()>0)
        {
            cursor.moveToFirst();
            int i = cursor.getInt(0);
            cursor.close();
            return i;
        }
        return -1;
    }
}
