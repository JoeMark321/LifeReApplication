package com.example.lifereapplication.util.toast;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Toast 消息队列（MVVM 中的 Model/State 层）。
 *
 * <p>解决的问题：同一时刻多个 Toast 相互顶掉/遮罩。做法是<b>串行队列</b>——
 * 每次只展示队首消息，展示时长结束后再弹出下一条；后续入队的消息永远
 * 不会打断正在展示的那条。</p>
 *
 * <p><b>MVVM 接入方式：</b>队列头以 {@link LiveData} 暴露（{@link #current()}），
 * View 层（ToastCenter/BaseMenuActivity）观察它并负责渲染——状态由本类持有，
 * 渲染与生命周期由 View 层处理，符合「ViewModel 持状态、View 只渲染」的分工。</p>
 *
 * <p>线程安全：enqueue 任意线程可调，展示回调固定主线程。</p>
 */
public final class ToastQueue {

    /** 一条待展示消息 */
    public static class Event {
        public final String message;
        public final CustomToast.Type type;
        public final long durationMs;

        Event(String message, CustomToast.Type type, long durationMs) {
            this.message = message;
            this.type = type;
            this.durationMs = durationMs;
        }
    }

    /** 各语义类型的默认展示时长（与系统 Toast 一致） */
    private static final long SHORT_MS = 2000L;
    private static final long LONG_MS = 3500L;

    private static final ToastQueue INSTANCE = new ToastQueue();

    public static ToastQueue get() {
        return INSTANCE;
    }

    private final MutableLiveData<Event> current = new MutableLiveData<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    /** 队列上限：防止恶意刷屏导致内存膨胀 */
    private static final int MAX_PENDING = 8;

    private final java.util.Deque<Event> pending = new java.util.ArrayDeque<>();
    private boolean showing = false;
    private Event showingEvent;

    private ToastQueue() {
    }

    /** 观察队列头：View 层收到非 null 即渲染，收到 null 表示当前已展示完 */
    public LiveData<Event> current() {
        return current;
    }

    /** 入队（任意线程）：串行展示，不打断当前消息 */
    public synchronized void enqueue(String message, CustomToast.Type type, boolean longDuration) {
        if (pending.size() >= MAX_PENDING) {
            pending.pollLast(); // 挤掉最旧，保住最新
        }
        long duration = longDuration ? LONG_MS : SHORT_MS;
        if (!showing) {
            startShow(new Event(message, type, duration));
        } else {
            pending.addLast(new Event(message, type, duration));
        }
    }

    /** 由 View 层在渲染完成（Snackbar 显示/Toast 弹出）后调用，启动计时 */
    public synchronized void onShown() {
        if (showingEvent == null) {
            return;
        }
        Event event = showingEvent;
        mainHandler.postDelayed(this::onFinished, event.durationMs);
    }

    /** 当前消息展示完毕：弹出下一条或复位 */
    public synchronized void onFinished() {
        showing = false;
        showingEvent = null;
        current.postValue(null);
        Event next = pending.pollFirst();
        if (next != null) {
            startShow(next);
        }
    }

    private void startShow(Event event) {
        showing = true;
        showingEvent = event;
        current.postValue(event);
    }

    /** 清空队列（页面销毁等场景） */
    public synchronized void clear() {
        pending.clear();
        mainHandler.removeCallbacksAndMessages(null);
        showing = false;
        showingEvent = null;
        current.postValue(null);
    }

    public synchronized int pendingCount() {
        return pending.size();
    }
}
