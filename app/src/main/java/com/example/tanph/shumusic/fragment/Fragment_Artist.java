package com.example.tanph.shumusic.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.tanph.shumusic.R;
import com.example.tanph.shumusic.adapter.Fragment_Artist_Adapter;
import com.example.tanph.shumusic.util.MusicLibrary;

public class Fragment_Artist extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View artist_view;
    private Fragment_Artist_Adapter fragment_artist_adapter;

    public Fragment_Artist(){}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("YOGI","onCreate Fragment_Artist");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        artist_view = inflater.inflate(R.layout.fragment_artist,container,false);
        swipeRefreshLayout = artist_view.findViewById(R.id.fragment_album_swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(this);

        recyclerView = artist_view.findViewById(R.id.fragment_album_recycler_view);
        new LoadSongs().execute("");
        Log.d("YOGI","onCreateView Fragment_Artist");
        return artist_view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView.setAdapter(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        recyclerView = null;
        if(fragment_artist_adapter!=null)
        {
            fragment_artist_adapter.clear();
        }
    }

    public void onRefresh() {
        new AsyncTask<String,Void,String>() {
            @Override
            protected String doInBackground(String... strings) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                swipeRefreshLayout.setRefreshing(false);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"");
    }

    private class LoadSongs extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... strings) {
            fragment_artist_adapter = new Fragment_Artist_Adapter(getContext(), MusicLibrary.getInstance().getDataItemsForArtists());
            Log.d("YOGI","doInBackground Fragment_Artist");
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                recyclerView.setAdapter(fragment_artist_adapter);
                recyclerView.setLayoutManager(new GridLayoutManager(getContext(),3));
                recyclerView.setItemAnimator(new DefaultItemAnimator());
                recyclerView.setHasFixedSize(true);
                Log.d("YOGI","doPostExecute Fragment_Artist");
            }
        }
    }
}

