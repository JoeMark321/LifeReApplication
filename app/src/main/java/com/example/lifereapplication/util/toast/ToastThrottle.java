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
 *
 * <p>在 Toast 六件套链路中处于<b>第一关</b>：ToastDispatcher.dispatch 先经本类裁决，
 * 通过的消息才进入 ToastQueue 串行队列——即先「限流」再「排队」，两层各司其职。</p>
 */
public final class ToastThrottle {

    /** 防抖窗口取 2s：足以覆盖「手抖连点/事件重放」的时间尺度，又短到不影响用户下一次有意操作 */
    public static final long WINDOW_MS = 2000L;
    /** 窗口内放行上限取 3：对应产品规则「最多三次」，超过即视为用户被骚扰，需要给出关闭出口 */
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

    /** 默认参数构造：业务侧不感知阈值细节，也保证全应用节流口径一致 */
    public ToastThrottle() {
        this(WINDOW_MS, MAX_CLICKS);
    }

    /** 参数化构造：仅供单测用非默认窗口/上限验证边界行为 */
    public ToastThrottle(long windowMs, int maxClicks) {
        this.windowMs = windowMs;
        this.maxClicks = maxClicks;
    }

    /**
     * 每次 show 前调用一次，返回裁决结果。
     * 为什么用 synchronized 而非原子类：时间戳与计数必须作为整体更新（有前后依赖），
     * 拆成 CAS 反而复杂；且本方法调用频率极低，锁开销可忽略。
     */
    public synchronized Result record(long now) {
        if (now - lastClickAt >= windowMs) {
            // 新窗口（窗口结束边界含在内）：重置计数
            clicksInWindow = 0;
        }
        // 以「本次点击」刷新窗口起点：用户只要还在连点，窗口就持续后移，
        // 避免把跨窗口的零星点击误累计成同一次骚扰
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

    /** 供测试与重置场景使用：清空窗口状态，保证用例之间互不污染 */
    public synchronized void reset() {
        lastClickAt = 0L;
        clicksInWindow = 0;
    }

    /** 当前窗口内已计入的点击数：主要供测试断言节流内部状态 */
    public synchronized int clicksInWindow() {
        return clicksInWindow;
    }
}
