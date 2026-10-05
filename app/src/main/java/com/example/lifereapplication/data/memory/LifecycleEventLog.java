package com.example.lifereapplication.data.memory;

import androidx.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 生命周期事件日志 —— <b>仅存内存，绝不落库</b>。
 *
 * <p>为什么只放内存：日志是易失的调试信息，写入数据库会带来三个问题——
 * ① 每次回调都触发磁盘 IO，与“onPause/onStop 要快”的原则冲突；
 * ② 表数据持续膨胀需要清理策略；③ 进程被杀后旧日志没有阅读价值。
 * 这里用固定长度双端队列做<b>有界</b>缓冲，天然防止内存无限增长。</p>
 */
public final class LifecycleEventLog {

    private static final int MAX_SIZE = 40;

    private static final Deque<String> EVENTS = new ArrayDeque<>();

    private LifecycleEventLog() {
    }

    public static void record(String page, String callback) {
        EVENTS.addFirst(System.currentTimeMillis() + "  " + page + " → " + callback);
        while (EVENTS.size() > MAX_SIZE) {
            EVENTS.removeLast();
        }
    }

    @NonNull
    public static List<String> snapshot() {
        return new ArrayList<>(EVENTS);
    }

    public static void clear() {
        EVENTS.clear();
    }

    public static int size() {
        return EVENTS.size();
    }
}
