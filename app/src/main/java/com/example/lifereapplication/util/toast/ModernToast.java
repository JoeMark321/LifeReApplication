package com.example.lifereapplication.util.toast;

import android.app.Activity;
import android.view.View;

import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.example.lifereapplication.R;
import com.google.android.material.snackbar.Snackbar;

/**
 * 方案三：现代化 Toast（Material Snackbar）。
 *
 * <p>特点：</p>
 * <ul>
 *     <li>贴在所属容器底部，跟随页面容器而非屏幕，语义上属于当前页面；</li>
 *     <li>支持 Action 按钮，可承载“撤销”这类补救操作；</li>
 *     <li>可上滑关闭，不会阻断用户操作；</li>
 *     <li>自动适配深色模式与无障碍朗读。</li>
 * </ul>
 *
 * <p>坑点：Snackbar 需要一个宿主 View。这里统一取
 * {@code android.R.id.content}（Activity 的内容根视图），
 * 因此调用方必须传入 Activity，不能只传 Application Context。</p>
 */
public final class ModernToast {

    private ModernToast() {
    }

    public static void show(Activity activity, String message) {
        show(activity, message, Snackbar.LENGTH_SHORT);
    }

    public static void showLong(Activity activity, String message) {
        show(activity, message, Snackbar.LENGTH_LONG);
    }

    public static void show(Activity activity, @StringRes int messageRes) {
        show(activity, activity.getString(messageRes), Snackbar.LENGTH_SHORT);
    }

    public static void show(Activity activity, String message, int duration) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        View anchor = activity.findViewById(android.R.id.content);
        if (anchor == null) {
            return;
        }
        Snackbar snackbar = Snackbar.make(anchor, message, duration);
        snackbar.setBackgroundTint(ContextCompat.getColor(activity, R.color.snackbar_bg));
        snackbar.setTextColor(ContextCompat.getColor(activity, R.color.snackbar_text));
        snackbar.show();
    }

    /** 带操作按钮的现代化提示，适合“撤销/查看”场景 */
    public static void showWithAction(Activity activity, String message,
                                      String actionText, View.OnClickListener listener) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        View anchor = activity.findViewById(android.R.id.content);
        if (anchor == null) {
            return;
        }
        Snackbar.make(anchor, message, Snackbar.LENGTH_LONG)
                .setAction(actionText, listener)
                .show();
    }
}
