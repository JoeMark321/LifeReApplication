package com.example.lifereapplication.util.toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

/**
 * Toast 队列的 View 层宿主：观察 {@link ToastQueue} 的 LiveData，
 * 把队首消息交给 {@link ToastCenter} 按用户偏好渲染，渲染完通知队列计时。
 *
 * <p>接入方式：Activity 在 onCreate 调 {@link #attach}，onDestroy 调 {@link #detach}。
 * LiveData 的生命周期感知保证：页面 STOPPED 时不渲染（消息留队列），回到前台
 * 自动补播——与 LiveData「只在 STARTED~RESUMED 回调」语义一致。</p>
 */
public final class ToastQueueHost implements Observer<ToastQueue.Event> {

    private final AppCompatActivity activity;

    /** 只能通过 attach 创建：保证宿主与观察者成对出现，外部无需自行实例化 */
    private ToastQueueHost(AppCompatActivity activity) {
        this.activity = activity;
    }

    /**
     * 在 Activity.onCreate 中调用。
     * 返回宿主实例是为了让调用方在 onDestroy 时能对同一个对象调 detach 成对解绑。
     */
    public static ToastQueueHost attach(AppCompatActivity activity) {
        ToastQueueHost host = new ToastQueueHost(activity);
        ToastQueue.get().current().observe(activity, host);
        return host;
    }

    /**
     * 在 Activity.onDestroy 中调用。
     * 为什么 observe 已随生命周期自动清理还要显式 detach：保留对称 API，
     * 便于测试或需要提前断开观察的场景主动解绑。
     */
    public void detach() {
        ToastQueue.get().current().removeObserver(this);
    }

    @Override
    public void onChanged(ToastQueue.Event event) {
        // event 为 null 是队列的「本条展示完毕」复位信号，无需渲染；
        // isFinishing 的 Activity 没有可用窗口，强行弹 Snackbar 会泄漏甚至崩溃
        if (event == null || activity.isFinishing()) {
            return;
        }
        // 渲染走统一出口（样式路由 + 权限管理），完成后启动队列计时
        ToastCenter.show(activity, event.message, event.type);
        // 必须在真实渲染之后再回调 onShown：计时不从入队算起，
        // 否则 LiveData 分发/权限检查的耗时会被算进展示时长，消息被提前顶掉
        ToastQueue.get().onShown();
    }
}
