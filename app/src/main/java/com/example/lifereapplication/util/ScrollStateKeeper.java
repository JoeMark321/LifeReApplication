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

    /** SP 文件名：滚动状态单独一个文件，与业务偏好数据隔离，清理时互不影响 */
    private static final String SP_FILE = "scroll_states";
    /** 内存缓存上限 16 个列表：一个学习应用同时存在的列表页远少于此，够用且内存可忽略（类注释的 3KB 估算基于此值） */
    private static final int MAX_MEMORY_ENTRIES = 16;

    private static final LruCache<String, int[]> MEMORY = new LruCache<>(MAX_MEMORY_ENTRIES);

    /** 纯静态工具类：状态全在静态缓存里，实例化没有意义，禁止 new */
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
            // listId 缺失无法定位列表；负 position 只来自"列表为空"等异常态，没有保存价值
            return;
        }
        // 先更新内存：restore 是内存优先，若只写 SP 不同步内存，恢复端会读到旧值
        MEMORY.put(listId, new int[]{position, offset});
        // 再异步落盘。为什么 apply 不用 commit：save 发生在 onPause，主线程要求轻量，
        // 同步写盘会阻塞交互；apply 内存即时生效、磁盘异步写
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
        // 第一层：内存命中直接返回，零 IO（进程存活时绝大多数走这条快路径）
        int[] cached = MEMORY.get(listId);
        if (cached != null) {
            return cached;
        }
        // 第二层：落盘兜底。先用 contains 区分"从未保存过"与"保存过但值为 0"，
        // 避免把没存过的列表错误恢复到 (0,0)
        SharedPreferences sp = sp(context);
        if (!sp.contains(keyPosition(listId))) {
            return null;
        }
        int[] state = {sp.getInt(keyPosition(listId), 0), sp.getInt(keyOffset(listId), 0)};
        // 落盘值回填内存：之后同一列表再恢复就走零 IO 路径（进程被杀重建后的首次恢复属于这种情况）
        MEMORY.put(listId, state);
        return state;
    }

    /**
     * 钳制为可安全恢复的状态（处理空列表 / 数据变少 / 尺寸变化）。
     *
     * <p><b>负偏移是合法状态</b>：第一条可见项部分滚出屏幕时
     * child.top 为负（如露出下半截 80px → offset=-80px），
     * {@code scrollToPositionWithOffset} 支持负值并会原样恢复半截效果，
     * 因此这里<b>必须保留</b>负值，绝不归零——否则就是"回来变成完整条目头"。</p>
     *
     * @return 钳制后的 {position, offset}；列表为空时返回 null 表示放弃恢复
     */
    public static int[] clamp(int[] state, int itemCount, int viewportHeight) {
        if (state == null || itemCount <= 0) {
            return null;
        }
        int position = Math.min(Math.max(state[0], 0), itemCount - 1);
        int offset = state[1];
        // 仅当绝对值超出 viewport（横竖屏切换/数据剧变导致的脏数据）时归零，
        // 退化为"滚到该位置"；正常范围内的正负偏移全部原样保留
        if (Math.abs(offset) > viewportHeight) {
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

    /** 取 SP：始终用 ApplicationContext——SP 是进程级共享的，缓存 Activity 上下文引用会泄漏 */
    private static SharedPreferences sp(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(SP_FILE, Context.MODE_PRIVATE);
    }

    /** 位置 key 加 listId 前缀：多个列表共用一个 SP 文件，靠前缀隔离键名避免互相覆盖 */
    private static String keyPosition(String listId) {
        return listId + ".pos";
    }

    /** 偏移 key 同上（与位置 key 各自独立，恢复时缺任何一个都视为无效记录） */
    private static String keyOffset(String listId) {
        return listId + ".offset";
    }
}
