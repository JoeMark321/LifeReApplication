package com.example.lifereapplication.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.LruCache;

/**
 * 列表滚动状态记忆器（内存 + 持久化双缓存）。
 *
 * <p><b>策略：</b></p>
 * <ul>
 *     <li>第一层：进程内 {@link LruCache}（上限 16 个列表），命中零 IO，
 *         恢复耗时 &lt; 1ms；</li>
 *     <li>第二层：SharedPreferences（异步 {@code apply()} 写入），覆盖
 *         「进程被杀重建」与「前后台切换后内存回收」场景，恢复约 5~15ms；</li>
 *     <li>内存占用：每条状态 = key 字符串 + int[2]，合计 &lt; 200B × 16 ≈ 3KB，
 *         远低于「应用总内存 5%」红线；写盘内容同样只有几个键值对。</li>
 * </ul>
 *
 * <p><b>写入时机（生命周期正确性）：</b>由 Fragment 在 {@code onPause()} 写入 ——
 * onPause 保证被调用（连对话框覆盖都触发），而 onStop/onDestroy 不保证；
 * 且 onPause 不允许耗时操作，而 {@code apply()} 是异步写，满足该约束。</p>
 *
 * <p><b>边界钳制：</b>恢复前用 {@link #clamp(int, int, int)} 校验：
 * 列表数据变少时位置被钳制到尾部偏移；空列表直接放弃恢复；
 * 负偏移归零。保证任何脏数据都不会让 RecyclerView 抛
 * {@code IndexOutOfBoundsException}。</p>
 */
public final class ScrollStateKeeper {

    private static final String SP_FILE = "scroll_states";
    private static final int MAX_MEMORY_ENTRIES = 16;

    private static final LruCache<String, int[]> MEMORY = new LruCache<>(MAX_MEMORY_ENTRIES);

    private ScrollStateKeeper() {
    }

    /**
     * 保存滚动状态（内存 + 异步落盘）。
     *
     * @param context  任意可用 Context（取 ApplicationContext）
     * @param listId   列表唯一标识（Fragment 参数注入，如 "demo.linear"）
     * @param position 第一个可见项的 adapter position
     * @param offset   该项相对 RecyclerView 顶部的像素偏移
     */
    public static void save(Context context, String listId, int position, int offset) {
        if (listId == null || position < 0) {
            return;
        }
        MEMORY.put(listId, new int[]{position, offset});
        sp(context).edit()
                .putInt(keyPosition(listId), position)
                .putInt(keyOffset(listId), offset)
                .apply();
    }

    /**
     * 恢复滚动状态：内存优先，落盘兜底。
     *
     * @return int[2] = {position, offset}；从未保存过返回 null
     */
    public static int[] restore(Context context, String listId) {
        if (listId == null) {
            return null;
        }
        int[] cached = MEMORY.get(listId);
        if (cached != null) {
            return cached;
        }
        SharedPreferences sp = sp(context);
        if (!sp.contains(keyPosition(listId))) {
            return null;
        }
        int[] state = {sp.getInt(keyPosition(listId), 0), sp.getInt(keyOffset(listId), 0)};
        MEMORY.put(listId, state);
        return state;
    }

    /**
     * 钳制为可安全恢复的状态（处理空列表 / 数据变少 / 尺寸变化）。
     *
     * @return 钳制后的 {position, offset}；列表为空时返回 null 表示放弃恢复
     */
    public static int[] clamp(int[] state, int itemCount, int viewportHeight) {
        if (state == null || itemCount <= 0) {
            return null;
        }
        int position = Math.min(Math.max(state[0], 0), itemCount - 1);
        int offset = Math.max(state[1], 0);
        // 偏移异常大（如列表项尺寸变化/横竖屏切换导致 viewport 变化）时归零，
        // 退化为“滚到该位置”而非“带偏移对齐”，视觉上仍然正确
        if (offset > viewportHeight) {
            offset = 0;
        }
        return new int[]{position, offset};
    }

    /** 清除单个列表的记忆（数据源重建后调用） */
    public static void clear(Context context, String listId) {
        if (listId == null) {
            return;
        }
        MEMORY.remove(listId);
        sp(context).edit()
                .remove(keyPosition(listId))
                .remove(keyOffset(listId))
                .apply();
    }

    private static SharedPreferences sp(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(SP_FILE, Context.MODE_PRIVATE);
    }

    private static String keyPosition(String listId) {
        return listId + ".pos";
    }

    private static String keyOffset(String listId) {
        return listId + ".offset";
    }
}
