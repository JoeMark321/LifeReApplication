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

    private ToastCenter() {
    }

    /** 普通提示：按用户偏好样式弹出 */
    public static void show(Activity activity, String message) {
        show(activity, message, CustomToast.Type.DEFAULT);
    }

    /** 带语义类型的提示：成功 / 错误 / 警告 / 信息 */
    public static void show(Activity activity, String message, CustomToast.Type type) {
        AppPreferences prefs = new AppPreferences(activity);
        ToastStyle style = resolveStyle(prefs);

        if (style == ToastStyle.NATIVE) {
            if (!prefs.isNativeEnabled()) {
                return; // 权限管理：用户已关闭原生提示
            }
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

        // MODERN
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

    /** 确保至少有一种提示可用：全部被关闭时退回原生 Toast 提示唯一一次 */
    private static ToastStyle resolveStyle(AppPreferences prefs) {
        ToastStyle style;
        try {
            style = ToastStyle.valueOf(prefs.getToastStyle());
        } catch (IllegalArgumentException e) {
            style = ToastStyle.MODERN;
        }
        boolean enabled = style == ToastStyle.NATIVE ? prefs.isNativeEnabled()
                : style == ToastStyle.CUSTOM ? prefs.isCustomEnabled()
                : prefs.isModernEnabled();
        if (!enabled) {
            // 当前样式被关闭，按 MODERN → CUSTOM → NATIVE 顺序找可用样式
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
