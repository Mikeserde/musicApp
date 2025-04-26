package com.example.music_liujinhong.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.example.music_liujinhong.R;

import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder> {

    private Item item;

    public CardAdapter(Item item) {
        this.item = item;
    }

    private CardAdapter.OnCardClickListener listener;

    // 定义接口
    public interface OnCardClickListener {
        void onCardClick(Item item, int position);
    }

    // 设置监听器
    public void setOnItemClickListener(CardAdapter.OnCardClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_reycle_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Music music = item.getMusicList().get(position);
        Glide.with(holder.itemView.findViewById(R.id.card_item_image))
                        .load(music.getCoverUrl())
                        .transform(new CenterCrop())
                        .into((ImageView) holder.itemView.findViewById(R.id.card_item_image));
        holder.song.setText(music.getMusicName());
        holder.singer.setText(music.getAuthor());
        holder.itemView.findViewById(R.id.card_item_image).setOnClickListener(v->{
            listener.onCardClick(item, position);
        });

        ImageView add_btn = holder.itemView.findViewById(R.id.card_item_add_music);
        add_btn.setOnClickListener(v -> {
            Toast.makeText(holder.itemView.getContext(), String.format("将%s添加到音乐列表",music.getMusicName()), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return item.getMusicList().size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView song;
        TextView singer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.card_item_image);
            song = itemView.findViewById(R.id.card_item_song);
            singer = itemView.findViewById(R.id.card_item_singer);
        }
    }
}