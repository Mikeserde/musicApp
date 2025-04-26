package com.example.music_liujinhong.home;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.module.LoadMoreModule;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.example.music_liujinhong.R;
import com.youth.banner.Banner;
import com.youth.banner.adapter.BannerImageAdapter;
import com.youth.banner.holder.BannerImageHolder;
import com.youth.banner.indicator.CircleIndicator;
import java.util.ArrayList;


public class MultiTypeAdapter extends BaseMultiItemQuickAdapter<Item, BaseViewHolder>implements LoadMoreModule,CardAdapter.OnCardClickListener {
    public interface ItemType {
        int BANNER = 1;
        int CARD = 2;
        int SINGLE = 3;
        int DOUBLE = 4;
    }

    private OnItemClickListener listener;

    // 定义接口
    public interface OnItemClickListener {
        void onItemClick(Item item, int position);
    }

    // 设置监听器
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCardClick(Item item, int position) {
        listener.onItemClick(item, position);
    }

    public MultiTypeAdapter(@Nullable ArrayList<Item> data) {
        super(data);
        addItemType(ItemType.BANNER, R.layout.banner_item);
        addItemType(ItemType.CARD, R.layout.card_item);
        addItemType(ItemType.SINGLE, R.layout.single_item);
        addItemType(ItemType.DOUBLE, R.layout.double_item);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder holder, Item item) {
        switch (holder.getItemViewType()) {
            case ItemType.BANNER:
                bindBanner(holder,item);
                break;
            case ItemType.CARD:
                bindCard(holder,item);
                break;
            case ItemType.SINGLE:
                bindSingle(holder,item);
                break;
            case ItemType.DOUBLE:
                bindDouble(holder,item);
                break;
        }
    }

    private void bindDouble(BaseViewHolder holder, Item item) {
        View view = holder.getView(R.id.double_item);
        //左侧一列
        ImageView imageView1 = view.findViewById(R.id.double_item_image1);
        Glide.with(view.getContext())
                .load(item.getMusicList().get(0).getCoverUrl())
                .transform(new CenterCrop())
                .into(imageView1);

        imageView1.setOnClickListener(v -> {
                listener.onItemClick(item, 0);
        });

        TextView songText1 = view.findViewById(R.id.double_item_song1);
        songText1.setText(item.getMusicList().get(0).getMusicName());

        TextView singerText1 = view.findViewById(R.id.double_item_singer1);
        singerText1.setText(item.getMusicList().get(0).getAuthor());
        ImageView add_btn1 = view.findViewById(R.id.double_item_add1);
        add_btn1.setOnClickListener(v -> {
            Toast.makeText(getContext(), String.format("将%s添加到音乐列表",item.getMusicList().get(0).getMusicName()), Toast.LENGTH_SHORT).show();
        });


        //右侧一列
        ImageView imageView2 = view.findViewById(R.id.double_item_image2);
        Glide.with(getContext())
                .load(item.getMusicList().get(1).getCoverUrl())
                .transform(new CenterCrop())
                .into(imageView2);

        imageView2.setOnClickListener(v -> {
            listener.onItemClick(item, 1);
        });

        TextView songText2 = view.findViewById(R.id.double_item_song2);
        songText2.setText(item.getMusicList().get(1).getMusicName());

        TextView singerText2 = view.findViewById(R.id.double_item_singer2);
        singerText2.setText(item.getMusicList().get(1).getAuthor());

        TextView tileText = view.findViewById(R.id.double_item_title);
        tileText.setText(item.getTitle());

        ImageView add_btn2 = view.findViewById(R.id.double_item_add2);
        add_btn2.setOnClickListener(v -> {
            Toast.makeText(getContext(), String.format("将%s添加到音乐列表",item.getMusicList().get(1).getMusicName()), Toast.LENGTH_SHORT).show();
        });
    }

    private void bindCard(BaseViewHolder holder, Item item) {
        View view = holder.getView(R.id.card_item);
        TextView titleText = view.findViewById(R.id.card_item_title);
        titleText.setText(item.getTitle());

        RecyclerView recyclerView = view.findViewById(R.id.card_item_recycler);
        // 配置RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                view.getContext(),
                LinearLayoutManager.HORIZONTAL, // 水平方向
                false
        );
        recyclerView.setLayoutManager(layoutManager);

        // 设置适配器
        CardAdapter adapter = new CardAdapter(item);
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(this);
    }

    private void bindBanner(BaseViewHolder holder, Item item) {
        View view = holder.getView(R.id.banner_item);
        Banner banner = view.findViewById(R.id.banner);
        Context context = view.getContext();
        ImageView add_btn = view.findViewById(R.id.banner_add_music);
        // 设置 Banner 适配器
        banner.setAdapter(new BannerImageAdapter<Music>(item.getMusicList()) {
                    @Override
                    public void onBindView(BannerImageHolder holder, Music music, int position, int size) {
                        add_btn.setOnClickListener(v -> {
                            Toast.makeText(getContext(), String.format("将%s添加到音乐列表",music.getMusicName()), Toast.LENGTH_SHORT).show();
                        });
                        // 数据绑定
                        holder.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        Glide.with(holder.imageView.getContext())
                                .load(music.getCoverUrl())
                                .transform(new CenterCrop())
                                .into(holder.imageView);
                        holder.imageView.setOnClickListener(v -> {
                            listener.onItemClick(item, position);
                        });
                    }
                }).addBannerLifecycleObserver((LifecycleOwner) context)
                .setIndicator(new CircleIndicator(view.getContext()));
        // 开始轮播
        banner.start();
    }

    private void bindSingle(BaseViewHolder holder, Item item) {
        View view = holder.getView(R.id.single_item);
        ImageView imageView = view.findViewById(R.id.single_item_image);
        Glide.with(view.getContext())
                .load(item.getMusicList().get(0).getCoverUrl())
                .transform(new CenterCrop())
                .into(imageView);

        imageView.setOnClickListener(v->{
            listener.onItemClick(item, 0);
        });

        TextView songText = view.findViewById(R.id.single_item_song);
        songText.setText(item.getMusicList().get(0).getMusicName());

        TextView singerText = view.findViewById(R.id.single_item_singer);
        singerText.setText(item.getMusicList().get(0).getAuthor());

        TextView tileText = view.findViewById(R.id.single_item_title);
        tileText.setText(item.getTitle());

        ImageView add_btn = view.findViewById(R.id.single_item_add);
        add_btn.setOnClickListener(v -> {
            Toast.makeText(getContext(), String.format("将%s添加到音乐列表",item.getMusicList().get(0).getMusicName()), Toast.LENGTH_SHORT).show();
        });
    }
}

