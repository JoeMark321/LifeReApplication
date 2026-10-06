package com.example.lifereapplication.util.toast;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lifereapplication.data.prefs.AppPreferences;
import com.example.lifereapplication.ui.settings.SettingsActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Toast 管理系统的 View 层协调者（MVVM 的 V 与 VM 之间的桥）。
 *
 * <p>在 {@link ToastCenter} 原有能力（样式路由/权限管理）之上叠加：</p>
 * <ol>
 *     <li><b>防抖节流</b>：{@link ToastThrottle} 保证 2s 窗口内多次点击只弹一次；</li>
 *     <li><b>串行队列</b>：{@link ToastQueue} 保证多消息不互相遮罩；</li>
 *     <li><b>三次上限</b>：超过 3 次弹出模态对话框，引导用户到【设置】
 *     关闭对应提示类型 —— 把"用户嫌烦"的信号变成可操作的出口。</li>
 * </ol>
 */
public final class ToastDispatcher {

    /** 静态单例节流器：节流状态必须跨页面共享，否则换一页就重新计数，「最多三次」的规则形同虚设 */
    private static final ToastThrottle THROTTLE = new ToastThrottle();

    /** 纯静态入口，禁止实例化 */
    private ToastDispatcher() {
    }

    /**
     * 统一入口（代替直接调用 ToastCenter）：
     * 节流 → 入队 → 由 {@link ToastQueueHost} 观察渲染。
     *
     * @return 本次请求的实际处理结果（供上层测试/埋点）
     */
    public static ToastThrottle.Result dispatch(AppCompatActivity activity, String message,
                                                CustomToast.Type type, boolean longDuration) {
        // 第一步永远是节流裁决：把明显是连点的请求挡在队列之外，避免队列被无意义消息灌满
        ToastThrottle.Result result = THROTTLE.record(System.currentTimeMillis());

        switch (result) {
            case PASS:
                // 入队后由 ToastQueueHost（观察 LiveData 的 View 层宿主）串行渲染；
                // 权限与样式路由由 ToastCenter 在渲染时把关
                ToastQueue.get().enqueue(message, type, longDuration);
                return result;
            case SUPPRESSED:
                // 窗口内重复点击：完全静默——连第二条都嫌多，更不该给任何反馈打扰用户
                return result;
            case OVER_LIMIT:
            default:
                // 超过三次：不再入队 Toast（用户已经嫌烦，再弹 Toast 只会火上浇油），
                // 改用模态对话框给出「去设置关闭」的操作出口
                offerSettings(activity);
                return result;
        }
    }

    /**
     * 弹出「去设置」的引导对话框。
     * 为什么用模态对话框：用户反感的正是 Toast 这种轻提示，此时只有模态
     * 才能真正打断并把用户带到可操作的出口，把「嫌烦」信号变成关闭入口。
     */
    private static void offerSettings(AppCompatActivity activity) {
        AppPreferences prefs = new AppPreferences(activity);
        if (!prefs.isDialogEnabled()) {
            return; // 用户关闭了对话框提示：尊重设置，只静默
        }
        new MaterialAlertDialogBuilder(activity)
                .setTitle("提示太频繁？")
                .setMessage("短时间内点击次数较多。可以到【设置】里关闭不需要的提示类型，或更换提示样式。")
                .setPositiveButton("去设置", (d, w) ->
                        activity.startActivity(new Intent(activity, SettingsActivity.class)))
                .setNegativeButton("继续使用", null)
                .show();
    }

    /** 供测试重置节流窗口：避免用例间共享静态 THROTTLE 状态导致互相污染 */
    public static void resetThrottle() {
        THROTTLE.reset();
    }
}
