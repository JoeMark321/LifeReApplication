package com.example.lifereapplication.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class DialogHelper {

    public interface OnDialogActionListener {
        void onPositive();

        void onNegative();

        void onCancel();
    }

    public static void showAlert(Activity activity, String title, String message) {
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }

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
                .setOnCancelListener(dialog -> {
                    if (listener != null) listener.onCancel();
                })
                .show();
    }

    public static void showOptions(Activity activity, String title, String[] options, DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setItems(options, listener)
                .show();
    }
}
