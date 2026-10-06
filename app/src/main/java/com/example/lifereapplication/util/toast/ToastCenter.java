package com.example.lifereapplication.util.toast;

import android.app.Activity;

import com.example.lifereapplication.data.prefs.AppPreferences;

/**
 * 提示中心 —— 全应用统一的提示出口。
 *
 * <p>职责：</p>
 * <ol>
 *     <li>按用户偏好（{@link AppPreferences}）选择提示样式；</li>
 *     <li>执行<b>提示权限管理</b>：某类提示被用户关闭时直接静默跳过，
 *         不弹任何东西，且不影响业务流程；</li>
 *     <li>提供“静默提示”能力：只记日志/只更新 UI，不打扰用户。</li>
 * </ol>
 *
 * <p>约定：业务代码只调用本类，不直接调用三种 Toast 实现。</p>
 */
public final class ToastCenter {

    /** 统一出口只暴露静态方法，禁止实例化 */
    private ToastCenter() {
    }

    /** 普通提示：按用户偏好样式弹出 */
    public static void show(Activity activity, String message) {
        show(activity, message, CustomToast.Type.DEFAULT);
    }

    /**
     * 带语义类型的提示：成功 / 错误 / 警告 / 信息。
     * 流程：先解析用户偏好得到可用样式（样式路由），再按分支做权限校验——
     * 每个分支都复核一次开关，防止偏好在路由与渲染之间被并发修改的边缘情况。
     */
    public static void show(Activity activity, String message, CustomToast.Type type) {
        AppPreferences prefs = new AppPreferences(activity);
        ToastStyle style = resolveStyle(prefs);

        if (style == ToastStyle.NATIVE) {
            if (!prefs.isNativeEnabled()) {
                return; // 权限管理：用户已关闭原生提示，静默跳过且不影响业务流程
            }
            // 原生 Toast 无图标能力，用 decorate 把语义类型转成文字前缀
            NativeToast.show(activity, decorate(message, type));
            return;
        }

        if (style == ToastStyle.CUSTOM) {
            if (!prefs.isCustomEnabled()) {
                return;
            }
            CustomToast.show(activity, message, type);
            return;
        }

        // MODERN 分支：同样先做权限复核，再交给 Snackbar 实现
        if (!prefs.isModernEnabled()) {
            return;
        }
        ModernToast.show(activity, decorate(message, type));
    }

    /**
     * 静默提示：不弹出任何 UI，仅在 Logcat 留痕。
     * 适合高频、低价值的反馈（如滚动位置变化、焦点切换）。
     */
    public static void showSilent(String tag, String message) {
        android.util.Log.i(tag, "[silent] " + message);
    }

    /** 静默提示 + 轻量 UI 反馈（如更新一个 TextView），不打断用户操作 */
    public static void showSilent(String tag, String message, Runnable uiUpdater) {
        android.util.Log.i(tag, "[silent] " + message);
        if (uiUpdater != null) {
            uiUpdater.run();
        }
    }

    /**
     * 确保至少有一种提示可用：全部被关闭时退回某一种仍开启的样式。
     * 为什么必须有兜底：否则用户把三个开关全关掉后，所有业务反馈都会无声丢失，
     * 业务方还以为提示成功送达了。
     */
    private static ToastStyle resolveStyle(AppPreferences prefs) {
        ToastStyle style;
        try {
            // 偏好里存的是字符串，版本升级或手改数据可能存进非法值，必须 try-catch 防崩
            style = ToastStyle.valueOf(prefs.getToastStyle());
        } catch (IllegalArgumentException e) {
            // 解析失败默认 MODERN：它是体验与稳定性的折中首选
            style = ToastStyle.MODERN;
        }
        boolean enabled = style == ToastStyle.NATIVE ? prefs.isNativeEnabled()
                : style == ToastStyle.CUSTOM ? prefs.isCustomEnabled()
                : prefs.isModernEnabled();
        if (!enabled) {
            // 当前样式被关闭，按 MODERN → CUSTOM → NATIVE 顺序找可用样式：
            // 优先保留体验较好的样式，原生 Toast 作为最后保底，保证一定有出口
            if (prefs.isModernEnabled()) {
                return ToastStyle.MODERN;
            }
            if (prefs.isCustomEnabled()) {
                return ToastStyle.CUSTOM;
            }
            return ToastStyle.NATIVE;
        }
        return style;
    }

    /**
     * 把语义类型转成文字前缀（✓ ✕ ⚠ ⓘ）。
     * 为什么这么做：NATIVE/MODERN 两种样式无法展示图标，只能靠文本符号
     * 传达成败等语义；CUSTOM 自带图标，不走此方法。
     */
    private static String decorate(String message, CustomToast.Type type) {
        switch (type) {
            case SUCCESS:
                return "✓ " + message;
            case ERROR:
                return "✕ " + message;
            case WARNING:
                return "⚠ " + message;
            case INFO:
                return "ⓘ " + message;
            default:
                return message;
        }
    }
}
