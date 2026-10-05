package com.example.lifereapplication.util.toast;

import android.content.Context;
import android.widget.Toast;

/**
 * 方案一：原生 Toast。
 *
 * <p>优点：零成本、线程安全（内部自动切主线程）、不会有 BadTokenException。</p>
 * <p>缺点：样式受系统控制（部分 ROM 会改样式）、不能自定义布局、无法交互。</p>
 */
public final class NativeToast {

    private NativeToast() {
    }

    public static void show(Context context, String message) {
        Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    public static void showLong(Context context, String message) {
        Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}
