package com.example.lifereapplication.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

/**
 * 弹窗统一入口（AlertDialog 的薄封装）。
 *
 * <p><b>为什么存在：</b>本应用的提示系统支持原生 Toast / 自定义 Toast /
 * Snackbar / Dialog / 通知等多种样式，其中 Dialog 样式统一收口到这里：
 * ① 调用处一行代码，不必每次 new Builder 堆样板；② 全应用按钮文案
 * （"确定"/"取消"）与交互风格一致；③ 作为生命周期教学应用，"对话框覆盖
 * 宿主时只走 onPause 不走 onStop"是重点演示场景，统一封装便于各页面复用
 * 与对比。</p>
 *
 * <p><b>为什么参数是 Activity 而不是 Context：</b>AlertDialog 需要 Activity 的
 * window token 才能弹出，传 ApplicationContext 会直接崩溃；且弹窗生命周期
 * 本就该跟随 Activity（宿主销毁时自动消失）。</p>
 */
public class DialogHelper {

    /**
     * 弹窗结果回调。
     *
     * <p>为什么 onNegative 和 onCancel 要拆成两个：点击"取消"按钮走
     * {@link #onNegative}；而按返回键、点击弹窗外部区域只会触发
     * {@link #onCancel}（不触发任何按钮回调）。拆开后调用方才能精确区分
     * 用户是"明确选择"还是"随手关闭"。</p>
     */
    public interface OnDialogActionListener {
        /** 用户点击"确定"按钮 */
        void onPositive();

        /** 用户点击"取消"按钮 */
        void onNegative();

        /** 用户按返回键或点击弹窗外部导致弹窗关闭 */
        void onCancel();
    }

    /**
     * 纯告知弹窗：只有一个"确定"按钮，点击即关。
     * 为什么不接收回调：信息告知类弹窗不关心用户何时关闭，收回调只会逼调用方传 null。
     */
    public static void showAlert(Activity activity, String title, String message) {
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                // listener 传 null：系统默认行为就是点击后关闭弹窗，无需额外逻辑
                .setPositiveButton("确定", null)
                .show();
    }

    /**
     * 确认弹窗："确定 / 取消"双按钮，另监听返回键 / 点外部关闭。
     *
     * <p>为什么统一判空 listener：调用方有时只想要弹窗的打断效果、不关心结果，
     * 这里判空后调用方可以直接传 null，省去每个调用点写空实现。</p>
     */
    public static void showConfirm(Activity activity, String title, String message, OnDialogActionListener listener) {
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定", (dialog, which) -> {
                    if (listener != null) listener.onPositive();
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    if (listener != null) listener.onNegative();
                })
                // 必须单独监听：返回键/点外部关闭不会走任何按钮回调，不挂 onCancel 就无法感知这种关闭方式
                .setOnCancelListener(dialog -> {
                    if (listener != null) listener.onCancel();
                })
                .show();
    }

    /**
     * 列表选择弹窗：展示 options 供用户单选一项。
     *
     * <p>为什么直接用系统 {@link DialogInterface.OnClickListener} 而不再包自定义
     * 接口：系统回调已携带 which（选中下标），信息够用且贴近平台习惯，再包一层
     * 只会增加样板。</p>
     */
    public static void showOptions(Activity activity, String title, String[] options, DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setItems(options, listener)
                .show();
    }
}
