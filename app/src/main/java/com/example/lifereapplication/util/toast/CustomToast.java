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
 */
public final class CustomToast {

    public enum Type {
        DEFAULT, SUCCESS, ERROR, WARNING, INFO
    }

    private CustomToast() {
    }

    public static void show(Context context, String message) {
        show(context, message, Type.DEFAULT, false);
    }

    public static void show(Context context, @StringRes int messageRes) {
        show(context, context.getString(messageRes), Type.DEFAULT, false);
    }

    public static void show(Context context, String message, Type type) {
        show(context, message, type, false);
    }

    public static void showLong(Context context, String message, Type type) {
        show(context, message, type, true);
    }

    public static void show(Context context, String message, Type type, boolean longDuration) {
        Context appContext = context.getApplicationContext();
        View view = LayoutInflater.from(appContext).inflate(R.layout.toast_custom, null);
        TextView toastText = view.findViewById(R.id.toastText);
        ImageView toastIcon = view.findViewById(R.id.toastIcon);

        toastText.setText(message);
        applyType(toastIcon, type);

        Toast toast = new Toast(appContext);
        toast.setDuration(longDuration ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        toast.setView(view);
        // 自定义 Toast 需要显式指定边距与位置，否则在不同 ROM 上可能贴边
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 120);
        toast.show();
    }

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
