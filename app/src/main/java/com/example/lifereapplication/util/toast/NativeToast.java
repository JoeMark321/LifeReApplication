package com.example.lifereapplication.util.toast;

import android.content.Context;
import android.widget.Toast;

/**
 * 方案一：原生 Toast。
 *
 * <p>优点：零成本、线程安全（内部自动切主线程）、不会有 BadTokenException。</p>
 * <p>缺点：样式受系统控制（部分 ROM 会改样式）、不能自定义布局、无法交互。</p>
 *
 * <p>链路位置：ToastCenter 三种实现中最底层的兜底方案（样式兜底链的最后一环），
 * 业务代码不应直接调用本类。</p>
 */
public final class NativeToast {

    /** 纯静态工具类，禁止实例化 */
    private NativeToast() {
    }

    /**
     * 短时长原生提示。
     * 为什么用 ApplicationContext 而不是传入的 Context：Toast 的存活时间
     * 可能超过展示它的 Activity，持有 Activity 引用会造成泄漏；
     * 应用级上下文与 Toast 同生命周期，最安全。
     */
    public static void show(Context context, String message) {
        Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    /** 长时长变体：给信息量较大的文案留足阅读时间 */
    public static void showLong(Context context, String message) {
        Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}
