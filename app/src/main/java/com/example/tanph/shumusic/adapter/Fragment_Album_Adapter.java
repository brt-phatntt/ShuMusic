package com.example.tanph.shumusic.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import com.example.tanph.shumusic.activity.MainActivity;
import com.example.tanph.shumusic.activity.SecondaryActivity;
import com.example.tanph.shumusic.model.Constants;
import com.example.tanph.shumusic.pojo.SingleDataItem;
import com.example.tanph.shumusic.service.MediaPlayerService;
import com.example.tanph.shumusic.util.MusicLibrary;
import com.example.tanph.shumusic.util.UIElementHelper.FontFactory;

import java.util.ArrayList;

public class Fragment_Album_Adapter extends RecyclerView.Adapter<Fragment_Album_Adapter.Album_View_Holder> {

    private Context context;
    private ArrayList<SingleDataItem> Album_List;
    private MediaPlayerService mediaPlayerService;

    private Drawable drawable;

    public Fragment_Album_Adapter(Context context,ArrayList<SingleDataItem> Album_List)
    {
        this.context = context;
        this.Album_List = Album_List;
        mediaPlayerService = MyApplication.getService();

        //Making Drawable Mutate
        // Go to the link "https://android-developers.googleblog.com/2009/05/drawable-mutations.html"
        drawable = ContextCompat.getDrawable(context,R.drawable.image1).mutate();
    }

    public void clear() {

    }

    public class Album_View_Holder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView thumbnail,overflow;
        TextView album_name,artist_name;


        public Album_View_Holder(View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            overflow = itemView.findViewById(R.id.overflow);

            album_name = itemView.findViewById(R.id.title);
            album_name.setTypeface(FontFactory.getFont());

            artist_name = itemView.findViewById(R.id.artist);
            artist_name.setTypeface(FontFactory.getFont());

            itemView.setOnClickListener(this);
            itemView.findViewById(R.id.overflow).setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Fragment_Album_Adapter.this.onClick(v,getLayoutPosition());
        }
    }

    private void onClick(View v, int layoutPosition)
    {
        String title;
        int key;
        switch (v.getId())
        {
            case R.id.cardView_root_layout:
                title = Album_List.get(layoutPosition).album_name;
                key = Album_List.get(layoutPosition).album_id;
                Intent intent = new Intent(context, SecondaryActivity.class);
                intent.putExtra("status", Constants.FRAGMENT_STATUS.ALBUM_FRAGMENT);
                intent.putExtra("title",title.trim()); //to trim whitespaces
                intent.putExtra("key",key);
                context.startActivity(intent);
                ((MainActivity)context).overridePendingTransition(R.anim.slide_in_from_right,R.anim.slide_out_from_left);
                Log.d("YOGI","cardView clicked" + layoutPosition);
                break;
        }

    }

    @Override
    public Album_View_Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_layout,parent,false);
        return new Album_View_Holder(view);
    }

    @Override
    public void onBindViewHolder(Album_View_Holder holder, int position) {
        holder.album_name.setText(Album_List.get(position).album_name);

        Uri uri = MusicLibrary.getInstance().getAlbumUri(Album_List.get(position).album_id);
        /*if(uri == null)
        {*/
        //Log.d("YOGI","Album adapter URI = "+uri);
        //}

        /*if(position==0)
        {
            holder.thumbnail.setImageBitmap(BitmapFactory.decodeFile(uri.toString()));
        }*/
        //load images using Glide
        Glide
                .with(context)
                .asBitmap()
                .load(uri)
                .transition(BitmapTransitionOptions.withCrossFade(200))
                .apply(new RequestOptions()
                        .signature(new ObjectKey(String.valueOf(System.currentTimeMillis())))
                        .centerCrop()
                        .placeholder(drawable))
                .into(holder.thumbnail);

        holder.artist_name.setText(Album_List.get(position).artist_name);

    }

    @Override
    public int getItemCount() {
        return Album_List.size();
    }


}
