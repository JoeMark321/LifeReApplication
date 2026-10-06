package com.example.lifereapplication;

/**
 * 开屏门禁：标记本次进程是否还需要展示开屏。
 *
 * <p>静态变量随进程存活——冷启动（进程新建）时为 true，消费后置 false；
 * 热启动（进程未死，从桌面/最近任务回来）不再展示，满足"只有重启才有一次动画"。
 * 进程被系统回收后再次启动，flag 归位，开屏自然重现。</p>
 */
public final class SplashGate {

    private static volatile boolean pending = true;

    private SplashGate() {
    }

    /** 是否展示开屏；展示过即消费掉，本进程内不再展示 */
    public static boolean consumeColdLaunch() {
        if (pending) {
            pending = false;
            return true;
        }
        return false;
    }
}
