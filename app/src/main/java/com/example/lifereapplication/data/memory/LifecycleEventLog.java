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

    /** 上限 40 条：约一屏列表的展示量，够回溯最近一轮页面操作，同时保证内存严格有界 */
    private static final int MAX_SIZE = 40;

    /** ArrayDeque：头尾增删都是 O(1)，正好匹配"新的插队头、满了删队尾"的滚动日志需求 */
    private static final Deque<String> EVENTS = new ArrayDeque<>();

    /** 纯静态工具类（状态在静态队列里），禁止实例化 */
    private LifecycleEventLog() {
    }

    /**
     * 记录一条生命周期事件（时间戳 + 页面 + 回调名）。
     *
     * <p>为什么 addFirst（新事件插队头）：展示时最新事件在最上面，符合日志阅读
     * 习惯，读取端不用再倒序；为什么裁剪用 while 不用 if：语义是"无论超出多少
     * 都裁回上限"，即便未来出现批量写入也安全。</p>
     */
    public static void record(String page, String callback) {
        // 时间戳写入时就拼进字符串：展示端拿来即用，免去每次渲染重新格式化
        EVENTS.addFirst(System.currentTimeMillis() + "  " + page + " → " + callback);
        while (EVENTS.size() > MAX_SIZE) {
            EVENTS.removeLast(); // 淘汰最旧事件，维持有界缓冲
        }
    }

    /**
     * 取当前全部日志。
     * 为什么返回副本而不是原队列引用：调用方遍历时新的生命周期回调随时可能写入
     * （record 与遍历可能在不同线程/时机发生），直接遍历会抛
     * ConcurrentModificationException；复制一份把读写隔离开。
     */
    @NonNull
    public static List<String> snapshot() {
        return new ArrayList<>(EVENTS);
    }

    /** 清空日志（重新开始一轮演示前调用，避免新旧记录混在一起干扰阅读） */
    public static void clear() {
        EVENTS.clear();
    }

    /** 当前日志条数（调试 / 界面展示用） */
    public static int size() {
        return EVENTS.size();
    }
}
