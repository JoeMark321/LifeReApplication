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

    private ToastQueueHost(AppCompatActivity activity) {
        this.activity = activity;
    }

    /** 在 Activity.onCreate 中调用 */
    public static ToastQueueHost attach(AppCompatActivity activity) {
        ToastQueueHost host = new ToastQueueHost(activity);
        ToastQueue.get().current().observe(activity, host);
        return host;
    }

    /** 在 Activity.onDestroy 中调用 */
    public void detach() {
        ToastQueue.get().current().removeObserver(this);
    }

    @Override
    public void onChanged(ToastQueue.Event event) {
        if (event == null || activity.isFinishing()) {
            return;
        }
        // 渲染走统一出口（样式路由 + 权限管理），完成后启动队列计时
        ToastCenter.show(activity, event.message, event.type);
        ToastQueue.get().onShown();
    }
}
