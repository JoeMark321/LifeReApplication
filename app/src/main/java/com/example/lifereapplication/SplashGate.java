package com.example.lifereapplication;

/**
 * 开屏门禁：标记本次进程是否还需要展示开屏。
 *
 * <p>静态变量随进程存活——冷启动（进程新建）时为 true，消费后置 false；
 * 热启动（进程未死，从桌面/最近任务回来）不再展示，满足"只有重启才有一次动画"。
 * 进程被系统回收后再次启动，flag 归位，开屏自然重现。</p>
 *
 * <p>为什么用静态变量而不是 SharedPreferences：开屏"本进程是否播过"天然是
 * 进程级状态——进程死了状态就该归位，静态变量的生命周期与需求语义完全一致，
 * 还省去持久化读写与过期清理。为什么私有构造：纯静态工具类，禁止实例化。</p>
 */
public final class SplashGate {

    /** 初值 true：进程刚建立即视为冷启动；volatile 保证写入对其他线程立即可见 */
    private static volatile boolean pending = true;

    private SplashGate() {
    }

    /**
     * 是否展示开屏；展示过即消费掉，本进程内不再展示。
     * 调用点唯一（MainActivity 冷启动判断），"先读后写"非原子在此
     * 不构成并发风险，无需加锁或 CAS。
     */
    public static boolean consumeColdLaunch() {
        if (pending) {
            // 置 false 与返回 true 必须同一次调用内完成：保证"展示一次"语义不被打破
            pending = false;
            return true;
        }
        return false;
    }
}
