package com.example.tanph.shumusic.adapter;

import android.content.Context;
import android.os.SystemClock;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.tanph.shumusic.MyApplication;
import com.example.tanph.shumusic.R;
import com.example.tanph.shumusic.pojo.SingleDataItem;
import com.example.tanph.shumusic.service.MediaPlayerService;
import com.example.tanph.shumusic.util.MusicLibrary;
import com.example.tanph.shumusic.util.UIElementHelper.FontFactory;

import java.util.ArrayList;
import java.util.concurrent.Executors;

public class Secondary_Activity_Adapter extends RecyclerView.Adapter<Secondary_Activity_Adapter.Secondary_View_Holder> {

    private Context context;
    private LayoutInflater inflater;
    private ArrayList<SingleDataItem> dataItems = new ArrayList<>();
    private View viewParent;
    private Long mLastClickTime = 0L;

    //Instance of the Service
    private MediaPlayerService mediaPlayerService;

    private int position; //For maintaining the position of the currently played song;

    public Secondary_Activity_Adapter(Context context, ArrayList<String> data){
        this.context = context;
        inflater = LayoutInflater.from(context);
        setService();
        for(SingleDataItem dataItem : MusicLibrary.getInstance().getDataItemsForTracks())
        {
            if(data.contains(dataItem.title))
            {
                dataItems.add(dataItem);
            }
        }
    }
    @Override
    public Secondary_View_Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_layout_second,parent,false);
        viewParent = parent;
        return new Secondary_View_Holder(view);
    }

    @Override
    public void onBindViewHolder(Secondary_View_Holder holder, int position) {

        //Check for Null Title Song as well
        //If Title is null then show nothing just empty fields

        if(dataItems.get(position).title.equals(""))
        {
            holder.song_name.setText("");
            holder.artist_name.setText("");
            holder.duration.setText("");
            holder.popup.setVisibility(View.INVISIBLE);
        }
        else
        {
            holder.song_name.setText(dataItems.get(position).title);
            holder.artist_name.setText(dataItems.get(position).artist_name);
            holder.duration.setText(dataItems.get(position).duration);
        }
    }

    @Override
    public int getItemCount() {
        return dataItems.size();
    }

    public class Secondary_View_Holder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView song_name,artist_name,duration;
        public ImageButton popup;

        public Secondary_View_Holder(View itemView) {
            super(itemView);
            song_name = itemView.findViewById(R.id.song_name_item_layout_sec);
            song_name.setTypeface(FontFactory.getFont());

            artist_name = itemView.findViewById(R.id.artist_name_item_layout_sec);
            artist_name.setTypeface(FontFactory.getFont());

            duration = itemView.findViewById(R.id.song_duration);
            duration.setTypeface(FontFactory.getFont());

            popup = itemView.findViewById(R.id.popup);

            itemView.setOnClickListener(this);
            popup.setOnClickListener(this);
            itemView.findViewById(R.id.cardView_for_album_art).setVisibility(View.GONE);
        }

        @Override
        public void onClick(View v) {
            Secondary_Activity_Adapter.this
                    .onClick(v,getLayoutPosition());
        }
    }

    private void onClick(View v, int layoutPosition) {
        position = layoutPosition;
        if (dataItems.get(layoutPosition).title.equals("")) {
            return;
        }
        switch (v.getId())
        {
            case R.id.track_item:
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

    public void setService()
    {
        mediaPlayerService = MyApplication.getService();
    }
    private void play() {
        ArrayList<String> temp = new ArrayList<>();
        for (SingleDataItem dataItem : dataItems)
        {
            if(!dataItem.title.equals(""))
                temp.add(dataItem.title);
        }

        mediaPlayerService.setTrackList(temp);
        mediaPlayerService.playAtPosition(position);
    }
}
