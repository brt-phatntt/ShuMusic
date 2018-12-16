package com.example.tanph.shumusic.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.tanph.shumusic.MyApplication;
import com.example.tanph.shumusic.R;
import com.example.tanph.shumusic.pojo.SingleDataItem;
import com.example.tanph.shumusic.service.MediaPlayerService;
import com.example.tanph.shumusic.util.MusicLibrary;

import java.util.ArrayList;

public class SlidingQueueAdapter extends RecyclerView.Adapter<SlidingQueueAdapter.SlidingViewHolder> {

    private Context context;
    private ArrayList<SingleDataItem> dataItems = new ArrayList<>();
    private LayoutInflater inflater;
    private Drawable drawable;
    private MediaPlayerService mediaPlayerService;

    public SlidingQueueAdapter(Context context, ArrayList<String> list)
    {
        mediaPlayerService = MyApplication.getService();
        this.context = context;
        inflater = LayoutInflater.from(context);
        for (SingleDataItem dataItem : MusicLibrary.getInstance().getDataItemsForTracks())
        {
            if (list.contains(dataItem.title))
            {
                dataItems.add(dataItem);
            }
        }
        drawable = ContextCompat.getDrawable(context,R.drawable.image1).mutate();
    }

    @Override
    public SlidingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.item_song_sliding_queue,parent,false);
        return new SlidingViewHolder(v);
    }

    @Override
    public void onBindViewHolder(SlidingViewHolder holder, int position) {

        SingleDataItem dataItem = dataItems.get(position);

        Glide.with(context)
                .asBitmap()
                .load(MusicLibrary.getInstance().getAlbumUri(dataItem.album_id))
                .apply(new RequestOptions()
                        .centerCrop()
                        .placeholder(drawable)
                        .diskCacheStrategy(DiskCacheStrategy.ALL))
                .into(holder.album_art);

    }

    @Override
    public int getItemCount() {
        return (null!=dataItems ? dataItems.size() : 0);
    }

    public class SlidingViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {

        public ImageView album_art;
        public SlidingViewHolder(View itemView)
        {
            super(itemView);
            album_art = itemView.findViewById(R.id.album_art);
            itemView.setOnClickListener(this);
        }
        @Override
        public void onClick(View v) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mediaPlayerService.playAtPosition(getLayoutPosition());
                }
            },100);

        }
    }
}
