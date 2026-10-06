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
        /** 提示文案 */
        public final String message;
        /** 语义类型：决定渲染时的图标（CUSTOM）或文字前缀（NATIVE/MODERN） */
        public final CustomToast.Type type;
        /** 展示时长（毫秒）：队列靠它决定何时弹出下一条 */
        public final long durationMs;

        Event(String message, CustomToast.Type type, long durationMs) {
            this.message = message;
            this.type = type;
            this.durationMs = durationMs;
        }
    }

    /**
     * 各语义类型的默认展示时长：取值对齐系统 Toast 的 LENGTH_SHORT/LONG 实际时长，
     * 沿用用户已习惯的系统节奏，避免「换了提示样式就换一种时长」的割裂感。
     */
    private static final long SHORT_MS = 2000L;
    private static final long LONG_MS = 3500L;

    /** 单例：队列必须进程级唯一——跨页面共用同一条串行通道，否则各页各队仍会互相顶掉 */
    private static final ToastQueue INSTANCE = new ToastQueue();

    /** 全局访问点 */
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

    /**
     * 入队（任意线程）：串行展示，不打断当前消息。
     * 为什么不直接 show：系统 Toast/Snackbar 同时弹出会互相顶掉，
     * 排队串行才能保证每条消息都被完整看到。
     */
    public synchronized void enqueue(String message, CustomToast.Type type, boolean longDuration) {
        if (pending.size() >= MAX_PENDING) {
            pending.pollLast(); // 挤掉最旧，保住最新：最新操作对应的反馈对用户最有价值，旧消息过期了也没意义
        }
        long duration = longDuration ? LONG_MS : SHORT_MS;
        if (!showing) {
            // 空闲则立即展示（省一次排队等待）；正在展示则只能排队，绝不抢占
            startShow(new Event(message, type, duration));
        } else {
            pending.addLast(new Event(message, type, duration));
        }
    }

    /**
     * 由 View 层在渲染完成（Snackbar 显示/Toast 弹出）后调用，启动计时。
     * 为什么不放在入队时计时：中间还隔着 LiveData 分发、样式路由、权限检查等步骤，
     * 只有 View 确认真正展示后开始计时，消息的实际可见时长才与 durationMs 一致。
     */
    public synchronized void onShown() {
        if (showingEvent == null) {
            return;
        }
        Event event = showingEvent;
        // 用主线程 Handler 计时而非 Timer：与渲染同在主线程、零额外线程开销，且 clear() 时能统一取消
        mainHandler.postDelayed(this::onFinished, event.durationMs);
    }

    /** 当前消息展示完毕：弹出下一条或复位 */
    public synchronized void onFinished() {
        showing = false;
        showingEvent = null;
        // 先发 null 让 View 层收尾本条渲染，再入队下一条，避免两条消息的渲染状态粘连
        current.postValue(null);
        // FIFO 取队首：先到先展示，符合用户对提示顺序的直觉
        Event next = pending.pollFirst();
        if (next != null) {
            startShow(next);
        }
    }

    /** 发布队首事件：用 postValue 而非 setValue，因为 enqueue 可能来自子线程，setValue 仅限主线程 */
    private void startShow(Event event) {
        showing = true;
        showingEvent = event;
        current.postValue(event);
    }

    /**
     * 清空队列（页面销毁等场景）。
     * 为什么 removeCallbacksAndMessages(null)：挂起的 onFinished 回调若不取消，
     * 会在清空后误触发，把已复位的展示状态又改一遍。
     */
    public synchronized void clear() {
        pending.clear();
        mainHandler.removeCallbacksAndMessages(null);
        showing = false;
        showingEvent = null;
        current.postValue(null);
    }

    /** 当前排队待展示的消息数：供测试与调试观察队列状态 */
    public synchronized int pendingCount() {
        return pending.size();
    }
}
