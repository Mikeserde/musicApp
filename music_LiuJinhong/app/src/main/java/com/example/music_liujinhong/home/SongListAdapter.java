package com.example.music_liujinhong.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.music_liujinhong.R;

import java.util.List;

public class SongListAdapter extends RecyclerView.Adapter<SongListAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(int position);
        void onRemoveClick(int position);
    }

    private List<Music> songList;
    private OnItemClickListener listener;

    public SongListAdapter(List<Music> songList, OnItemClickListener listener) {
        this.songList = songList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song_list, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Music music = songList.get(position);
        holder.tvSongTitle.setText(music.getMusicName());
        holder.tvSongArtist.setText("- " + music.getAuthor());

        // 点击整行，回调给外部
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position);
            }
        });

        // 点击右侧按钮，回调删除或更多操作
        holder.ivRemove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songList == null ? 0 : songList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSongTitle, tvSongArtist;
        ImageView ivRemove;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSongTitle = itemView.findViewById(R.id.tv_song_title);
            tvSongArtist = itemView.findViewById(R.id.tv_song_artist);
            ivRemove = itemView.findViewById(R.id.iv_remove);
        }
    }
}
