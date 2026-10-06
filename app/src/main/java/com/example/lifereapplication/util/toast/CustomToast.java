package com.example.lifereapplication.util.toast;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;

import com.example.lifereapplication.R;

/**
 * 方案二：自定义样式 Toast。
 *
 * <p>通过 {@link Toast#setView(View)} 替换系统默认布局，实现图标、圆角、
 * 半透明背景、自定义位置。相比第三方框架（如 Onavia / Toaster），
 * 它不增加依赖、不需要 Application 注册，兼容性与可控性都更好。</p>
 *
 * <p>链路位置：ToastCenter 三种实现之一（自定义样式分支），业务代码不应直接调用本类。</p>
 */
public final class CustomToast {

    /**
     * 语义类型：CUSTOM 样式据此换图标，NATIVE/MODERN 样式据此加文字前缀，
     * 同时也让业务侧能表达「这条提示是什么性质」而不只是文案。
     */
    public enum Type {
        DEFAULT, SUCCESS, ERROR, WARNING, INFO
    }

    /** 纯静态工具类，禁止实例化 */
    private CustomToast() {
    }

    /** 默认样式短时长：最轻量的用法 */
    public static void show(Context context, String message) {
        show(context, message, Type.DEFAULT, false);
    }

    /** 字符串资源重载：文案统一走资源文件，便于本地化 */
    public static void show(Context context, @StringRes int messageRes) {
        show(context, context.getString(messageRes), Type.DEFAULT, false);
    }

    /** 带语义类型的短时长提示 */
    public static void show(Context context, String message, Type type) {
        show(context, message, type, false);
    }

    /** 长时长变体：给信息量较大的文案留足阅读时间 */
    public static void showLong(Context context, String message, Type type) {
        show(context, message, type, true);
    }

    /** 核心展示方法：所有重载收敛到这里，保证布局/定位逻辑只有一份 */
    public static void show(Context context, String message, Type type, boolean longDuration) {
        // 用应用级上下文 inflate：自定义 View 随 Toast 存活的时间可能比 Activity 长，
        // 若持有 Activity 引用会造成泄漏
        Context appContext = context.getApplicationContext();
        View view = LayoutInflater.from(appContext).inflate(R.layout.toast_custom, null);
        TextView toastText = view.findViewById(R.id.toastText);
        ImageView toastIcon = view.findViewById(R.id.toastIcon);

        toastText.setText(message);
        applyType(toastIcon, type);

        // 用构造器而非 Toast.makeText：makeText 会先创建一个马上要被 setView 覆盖的
        // 系统文本布局，纯属浪费
        Toast toast = new Toast(appContext);
        toast.setDuration(longDuration ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        toast.setView(view);
        // 自定义 Toast 需要显式指定边距与位置，否则在不同 ROM 上可能贴边；
        // y 偏移取 120px：从顶部下移一段距离，避免顶到状态栏/挖孔区域
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 120);
        toast.show();
    }

    /**
     * 按语义类型切换图标；DEFAULT 不显示图标——中性提示保持轻量，
     * 不用图标分散用户注意力。
     */
    private static void applyType(ImageView icon, Type type) {
        switch (type) {
            case SUCCESS:
                icon.setImageResource(R.drawable.ic_success);
                icon.setVisibility(View.VISIBLE);
                break;
            case ERROR:
                icon.setImageResource(R.drawable.ic_error);
                icon.setVisibility(View.VISIBLE);
                break;
            case WARNING:
                icon.setImageResource(R.drawable.ic_warning);
                icon.setVisibility(View.VISIBLE);
                break;
            case INFO:
                icon.setImageResource(R.drawable.ic_info);
                icon.setVisibility(View.VISIBLE);
                break;
            default:
                icon.setVisibility(View.GONE);
                break;
        }
    }
}
