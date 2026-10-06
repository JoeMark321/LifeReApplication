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
 * <p>链路位置：ToastCenter 三种实现之一（默认首选样式），业务代码不应直接调用本类。</p>
 *
 * <p>坑点：Snackbar 需要一个宿主 View。这里统一取
 * {@code android.R.id.content}（Activity 的内容根视图），
 * 因此调用方必须传入 Activity，不能只传 Application Context。</p>
 */
public final class ModernToast {

    /** 纯静态工具类，禁止实例化 */
    private ModernToast() {
    }

    /** 短时长 Snackbar：与系统 Toast 的默认节奏保持一致 */
    public static void show(Activity activity, String message) {
        show(activity, message, Snackbar.LENGTH_SHORT);
    }

    /** 长时长变体：给信息量较大的文案留足阅读时间 */
    public static void showLong(Activity activity, String message) {
        show(activity, message, Snackbar.LENGTH_LONG);
    }

    /** 字符串资源重载：文案统一走资源文件，便于本地化与复用 */
    public static void show(Activity activity, @StringRes int messageRes) {
        show(activity, activity.getString(messageRes), Snackbar.LENGTH_SHORT);
    }

    /**
     * 核心展示方法：其余重载都收敛到这里，保证前置校验与配色逻辑只有一份。
     */
    public static void show(Activity activity, String message, int duration) {
        // 结束中的 Activity 没有可用的窗口/View 树，强行挂 Snackbar 会泄漏甚至崩溃
        if (activity == null || activity.isFinishing()) {
            return;
        }
        // 宿主取内容根视图：极端情况下（Activity 尚未完成布局）可能拿不到，需防御
        View anchor = activity.findViewById(android.R.id.content);
        if (anchor == null) {
            return;
        }
        Snackbar snackbar = Snackbar.make(anchor, message, duration);
        // 显式指定品牌底色与文字色：默认 Snackbar 配色随主题漂移，与本应用视觉不一致，
        // 且默认配色无法保证深浅色模式下的对比度
        snackbar.setBackgroundTint(ContextCompat.getColor(activity, R.color.snackbar_bg));
        snackbar.setTextColor(ContextCompat.getColor(activity, R.color.snackbar_text));
        snackbar.show();
    }

    /**
     * 带操作按钮的现代化提示，适合“撤销/查看”场景。
     * 固定 LENGTH_LONG：带操作的消息需要留出「读完 + 找到并点击 Action」的时间，
     * 短时长会让用户来不及反应。
     */
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
