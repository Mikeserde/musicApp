package com.example.music_liujinhong.player;

import android.content.Context;
import android.media.MediaCodec;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class LyricFetcher {
    private static final String TAG = "LyricFetcher";
    private static final int TIMEOUT_MS = 5000;

    public interface LyricFetchListener {
        void onLyricsFetched(List<LyricItem> lyrics);
        void onError(String error);
    }

    public static void fetchFromUrl(Context context, String url, LyricFetchListener listener) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                // 直接进行网络请求
                URL lyricUrl = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) lyricUrl.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);

                // 处理响应
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder content = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                    reader.close();

//                    Log.d(TAG,"content.toString():"+content.toString());
                    // 解析内容
                    List<LyricItem> parsed = parseContent(content.toString());

//                    // 打印日志，输出歌词内容
//                    if (parsed != null && !parsed.isEmpty()) {
//                        for (LyricItem lyric : parsed) {
//                            Log.d(TAG, "Fetched lyric: " + lyric.getTime() + " - " + lyric.getText());
//                        }
//                    } else {
//                        Log.d(TAG, "No lyrics found in the fetched content.");
//                    }


                    handler.post(() -> listener.onLyricsFetched(parsed));
                } else {
                    throw new IOException("HTTP error: " + conn.getResponseCode());
                }
            } catch (Exception e) {
                Log.e(TAG, "Fetch failed", e);
                handler.post(() -> listener.onError(getReadableError(e)));
            }
        });
    }

    private static List<LyricItem> parseContent(String content) {
        List<LyricItem> lyrics = new ArrayList<>();
        // 修改正则表达式以匹配中括号形式的时间标签，允许小数部分为两位或三位
        Pattern pattern = Pattern.compile("((?:\\[\\d{2}:\\d{2}\\.\\d{2,3}\\])+)(.*)");

        try (Scanner scanner = new Scanner(content)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String timeTags = matcher.group(1);
                    String text = matcher.group(2).trim();

                    // 根据每个时间标签拆分，使用正向肯定查找匹配 '['
                    String[] timeParts = timeTags.split("(?=\\[)");
                    for (String timePart : timeParts) {
                        // 移除中括号
                        timePart = timePart.replaceAll("[\\[\\]]", "");
                        lyrics.add(new LyricItem(timePart, text));
                    }
                } else {
                    Log.d(TAG, "Line didn't match: " + line);
                }
            }
        }

        // 根据时间排序歌词，并过滤掉时间无效的项
        Collections.sort(lyrics, (a, b) -> Long.compare(a.getTime(), b.getTime()));
        return lyrics.stream()
                .filter(item -> item.getTime() >= 0)
                .collect(Collectors.toList());
    }

    // 错误处理方法保持不变
    private static String getReadableError(Exception e) {
        if (e instanceof UnknownHostException) {
            return "网络不可用";
        } else if (e instanceof SocketTimeoutException) {
            return "请求超时";
        }
        return "加载失败";
    }
}
