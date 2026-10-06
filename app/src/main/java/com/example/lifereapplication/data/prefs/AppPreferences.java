package com.example.lifereapplication.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 用户偏好设置（SharedPreferences 持久化）。
 *
 * <p>存放的是“少量、键值型、需要跨启动保留”的设置，例如提示样式偏好与
 * 各类提示的总开关（提示权限管理）。内容少、读写频率低，用
 * SharedPreferences 比引入数据库更轻。</p>
 */
public class AppPreferences {

    private static final String FILE_NAME = "lifere_prefs";

    private static final String KEY_TOAST_STYLE = "toast_style";
    private static final String KEY_NATIVE_ENABLED = "toast_native_enabled";
    private static final String KEY_CUSTOM_ENABLED = "toast_custom_enabled";
    private static final String KEY_MODERN_ENABLED = "toast_modern_enabled";
    private static final String KEY_DIALOG_ENABLED = "dialog_enabled";
    private static final String KEY_NOTIFY_ENABLED = "notify_enabled";
    private static final String KEY_LIFECYCLE_TRACE_ENABLED = "lifecycle_trace_enabled";
    private static final String KEY_LAST_PAGE = "last_page";

    private final SharedPreferences sp;

    public AppPreferences(Context context) {
        this.sp = context.getApplicationContext()
                .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    /** 提示样式偏好：默认“现代化”（Snackbar） */
    public String getToastStyle() {
        return sp.getString(KEY_TOAST_STYLE, "MODERN");
    }

    public void setToastStyle(String style) {
        sp.edit().putString(KEY_TOAST_STYLE, style).apply();
    }

    public boolean isNativeEnabled() {
        return sp.getBoolean(KEY_NATIVE_ENABLED, true);
    }

    public boolean isCustomEnabled() {
        return sp.getBoolean(KEY_CUSTOM_ENABLED, true);
    }

    public boolean isModernEnabled() {
        return sp.getBoolean(KEY_MODERN_ENABLED, true);
    }

    public boolean isDialogEnabled() {
        return sp.getBoolean(KEY_DIALOG_ENABLED, true);
    }

    public boolean isNotifyEnabled() {
        return sp.getBoolean(KEY_NOTIFY_ENABLED, true);
    }

    public boolean isLifecycleTraceEnabled() {
        return sp.getBoolean(KEY_LIFECYCLE_TRACE_ENABLED, true);
    }

    /** 右上角菜单跳转时记忆的目标页（Activity 类名），用于"返回时提示上一界面" */
    public String getLastPage() {
        return sp.getString(KEY_LAST_PAGE, null);
    }

    public void setLastPage(String pageClassName) {
        sp.edit().putString(KEY_LAST_PAGE, pageClassName).apply();
    }

    public void setEnabled(String key, boolean value) {
        sp.edit().putBoolean(key, value).apply();
    }

    public static String keyNative() {
        return KEY_NATIVE_ENABLED;
    }

    public static String keyCustom() {
        return KEY_CUSTOM_ENABLED;
    }

    public static String keyModern() {
        return KEY_MODERN_ENABLED;
    }

    public static String keyDialog() {
        return KEY_DIALOG_ENABLED;
    }

    public static String keyNotify() {
        return KEY_NOTIFY_ENABLED;
    }

    public static String keyLifecycleTrace() {
        return KEY_LIFECYCLE_TRACE_ENABLED;
    }
}
