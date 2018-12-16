package com.example.tanph.shumusic.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.tanph.shumusic.R;
import com.example.tanph.shumusic.adapter.Fragment_Album_Adapter;
import com.example.tanph.shumusic.util.MusicLibrary;

public class Fragment_Album extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View album_view;
    private Fragment_Album_Adapter fragment_album_adapter;

    public Fragment_Album(){}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        album_view = inflater.inflate(R.layout.fragment_album,container,false);
        swipeRefreshLayout = album_view.findViewById(R.id.fragment_album_swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(this);

        recyclerView = album_view.findViewById(R.id.fragment_album_recycler_view);
        new LoadSongs().execute("");
        return album_view;
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
        if(fragment_album_adapter!=null)
        {
            fragment_album_adapter.clear();
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

    private class LoadSongs extends AsyncTask<String,Void,String>
    {

        @Override
        protected String doInBackground(String... strings) {
            fragment_album_adapter = new Fragment_Album_Adapter(getContext(), MusicLibrary.getInstance().getDataItemsForAlbums());
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if(result!=null)
            {
                recyclerView.setAdapter(fragment_album_adapter);
                recyclerView.setLayoutManager(new GridLayoutManager(getContext(),3));
                recyclerView.setItemAnimator(new DefaultItemAnimator());
                recyclerView.setHasFixedSize(true);
            }

        }
    }

}
