package com.example.tanph.shumusic.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;
import com.example.tanph.shumusic.MyApplication;
import com.example.tanph.shumusic.R;
import com.example.tanph.shumusic.pojo.SingleDataItem;
import com.example.tanph.shumusic.service.MediaPlayerService;
import com.example.tanph.shumusic.util.MusicLibrary;
import com.example.tanph.shumusic.util.UIElementHelper.FontFactory;

import java.util.ArrayList;
import java.util.concurrent.Executors;

public class Fragment_Track_Adapter extends RecyclerView.Adapter<Fragment_Track_Adapter.Track_View_Holder> {

    private Context context;
    private ArrayList<SingleDataItem> Track_List;
    private MediaPlayerService mediaPlayerService;

    private Drawable drawable;
    private Long mLastClickTime = 0L;
    private int position;

    public Fragment_Track_Adapter(Context context, ArrayList<SingleDataItem> Track_List){
        this.context = context;
        this.Track_List = Track_List;
        mediaPlayerService = MyApplication.getService();

        drawable = ContextCompat.getDrawable(context, R.drawable.image1).mutate();
    }

    @Override
    public Track_View_Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_layout,parent,false);
        return new Track_View_Holder(view);
    }

    @Override
    public void onBindViewHolder(Track_View_Holder holder, int position) {
        holder.song_name.setText(Track_List.get(position).title);

        //load images using Glide
        Glide
                .with(context)
                .asBitmap()
                .load(MusicLibrary.getInstance().getAlbumUri(Track_List.get(position).album_id))
                .transition(BitmapTransitionOptions.withCrossFade(200))
                .apply(new RequestOptions()
                        .signature(new ObjectKey(String.valueOf(System.currentTimeMillis())))
                        .centerCrop()
                        .placeholder(drawable))
                .into(holder.thumbnail);

        holder.artist_name.setText(Track_List.get(position).artist_name);
    }

    public void clear()
    {

    }

    @Override
    public int getItemCount() {
        return Track_List.size();
    }

    public class Track_View_Holder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView thumbnail,overflow;
        TextView song_name,artist_name;

        public Track_View_Holder(View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            overflow = itemView.findViewById(R.id.overflow);

            song_name = itemView.findViewById(R.id.title);
            song_name.setTypeface(FontFactory.getFont());

            artist_name = itemView.findViewById(R.id.artist);
            artist_name.setTypeface(FontFactory.getFont());

            itemView.setOnClickListener(this);
            itemView.findViewById(R.id.overflow).setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Fragment_Track_Adapter.this.onClick(v,getLayoutPosition());
        }
    }

    private void onClick(View v, int layoutPosition) {
        position = layoutPosition;
        switch (v.getId())
        {
            case R.id.cardView_root_layout:
                if(SystemClock.elapsedRealtime() - mLastClickTime <500)
                {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        play();
                    }
                });
                break;
        }
    }

    private void play() {
        ArrayList<String> temp = new ArrayList<>();
        for (SingleDataItem sd : Track_List)
        {
            if(!sd.title.equals(""))
            {
                temp.add(sd.title);
            }
        }
        mediaPlayerService.setTrackList(temp);
        mediaPlayerService.playAtPosition(position);
    }
}
