package com.example.lifereapplication.util.toast;

/**
 * Toast 防抖节流器。
 *
 * <p>规则（对应需求「最多三次，超过提示去设置」）：</p>
 * <ul>
 *     <li>时间窗 {@link #WINDOW_MS}（默认 2s）内的多次 show 合并为一次 ——
 *     防止快速连点刷屏；</li>
 *     <li>同一时间窗内点击计数 &gt; {@link #MAX_CLICKS}（3 次）时返回
 *     {@link Result#OVER_LIMIT}，由上层引导用户去【设置】关闭该类提示；</li>
 *     <li>计数按「窗口重置」语义：距上次点击超过窗口即清零，与常见
 *     RateLimiter 的滑动窗口实现一致。</li>
 * </ul>
 *
 * <p>纯逻辑无 Android 依赖，可用 JUnit 直接单测。</p>
 */
public final class ToastThrottle {

    public static final long WINDOW_MS = 2000L;
    public static final int MAX_CLICKS = 3;

    /** 节流裁决结果 */
    public enum Result {
        /** 正常放行，允许显示 */
        PASS,
        /** 窗口内重复点击，静默吞掉 */
        SUPPRESSED,
        /** 窗口内点击超过 3 次，应引导用户去设置 */
        OVER_LIMIT
    }

    private long lastClickAt = 0L;
    private int clicksInWindow = 0;
    private final long windowMs;
    private final int maxClicks;

    public ToastThrottle() {
        this(WINDOW_MS, MAX_CLICKS);
    }

    public ToastThrottle(long windowMs, int maxClicks) {
        this.windowMs = windowMs;
        this.maxClicks = maxClicks;
    }

    /** 每次 show 前调用一次，返回裁决结果 */
    public synchronized Result record(long now) {
        if (now - lastClickAt >= windowMs) {
            // 新窗口（窗口结束边界含在内）：重置计数
            clicksInWindow = 0;
        }
        lastClickAt = now;
        clicksInWindow++;

        if (clicksInWindow == 1) {
            return Result.PASS;              // 窗口内第一次：显示
        }
        if (clicksInWindow <= maxClicks) {
            return Result.SUPPRESSED;        // 2~3 次：吞掉但不惩罚
        }
        return Result.OVER_LIMIT;            // 第 4 次起：引导去设置
    }

    /** 供测试与重置场景使用 */
    public synchronized void reset() {
        lastClickAt = 0L;
        clicksInWindow = 0;
    }

    public synchronized int clicksInWindow() {
        return clicksInWindow;
    }
}
