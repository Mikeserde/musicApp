package com.example.music_liujinhong.player;

import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.music_liujinhong.R;

import java.util.ArrayList;
import java.util.List;

public class LyricsAdapter extends RecyclerView.Adapter<LyricsAdapter.LyricViewHolder> {
    private List<LyricItem> lyrics = new ArrayList<>();
    private int highlightedPosition = -1;

    // 补全必须实现的三个方法
    @NonNull
    @Override
    public LyricViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lyric_line, parent, false);
        return new LyricViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LyricViewHolder holder, int position) {
        boolean isHighlighted = position == highlightedPosition;
        holder.bind(lyrics.get(position).getText(), isHighlighted);
    }

    @Override
    public int getItemCount() {
        return lyrics.size();
    }

    // ViewHolder 内部类
    public static class LyricViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvLyric;

        public LyricViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLyric = itemView.findViewById(R.id.tv_lyric_line);
        }

        public void bind(String text, boolean isHighlighted) {
            tvLyric.setText(text);
            tvLyric.setTextColor(isHighlighted ? Color.WHITE : Color.GRAY);
            tvLyric.setTextSize(TypedValue.COMPLEX_UNIT_SP, isHighlighted ? 18 : 14);
        }
    }

    // 原有功能方法
    public void updateLyrics(List<LyricItem> newLyrics) {
        lyrics.clear();
        lyrics.addAll(newLyrics);
        notifyDataSetChanged();
    }

    public void highlightLine(int position) {
        int prev = highlightedPosition;
        highlightedPosition = position;
        if (prev != -1) notifyItemChanged(prev);
        if (position != -1) notifyItemChanged(position);
    }

    // 辅助方法
    public LyricItem getLyricAt(int position) {
        return lyrics.get(position);
    }
}