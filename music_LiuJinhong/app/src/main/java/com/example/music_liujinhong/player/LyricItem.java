package com.example.music_liujinhong.player;

public class LyricItem {
    private final long time;
    private final String text;

    // 增强构造函数
    public LyricItem(String timeStr, String text) {
        this.time = parseTime(timeStr);
        this.text = text.trim();
    }

    private long parseTime(String timeStr) {
        try {
            String[] parts = timeStr.split("[:.]");
            int min = Integer.parseInt(parts[0]);
            int sec = Integer.parseInt(parts[1]);
            int millis = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return min * 60_000L + sec * 1_000L + millis * 10L;
        } catch (Exception e) {
            return 0; // 无效时间返回0
        }
    }

    // Getters
    public long getTime() { return time; }
    public String getText() { return text; }
}
