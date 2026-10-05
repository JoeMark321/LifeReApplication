package com.example.lifereapplication.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.example.lifereapplication.R;

public class CustomToast {

    public enum Type {
        DEFAULT, SUCCESS, ERROR, WARNING, INFO
    }

    public static void show(Context context, String message) {
        show(context, message, Type.DEFAULT);
    }

    public static void show(Context context, @StringRes int messageResId) {
        show(context, context.getString(messageResId), Type.DEFAULT);
    }

    public static void show(Context context, String message, Type type) {
        View view = LayoutInflater.from(context).inflate(R.layout.toast_custom, null);
        TextView toastText = view.findViewById(R.id.toastText);
        ImageView toastIcon = view.findViewById(R.id.toastIcon);

        toastText.setText(message);

        switch (type) {
            case SUCCESS:
                toastIcon.setImageResource(R.drawable.ic_success);
                toastIcon.setVisibility(View.VISIBLE);
                break;
            case ERROR:
                toastIcon.setImageResource(R.drawable.ic_error);
                toastIcon.setVisibility(View.VISIBLE);
                break;
            case WARNING:
                toastIcon.setImageResource(R.drawable.ic_warning);
                toastIcon.setVisibility(View.VISIBLE);
                break;
            case INFO:
                toastIcon.setImageResource(R.drawable.ic_info);
                toastIcon.setVisibility(View.VISIBLE);
                break;
            default:
                toastIcon.setVisibility(View.GONE);
                break;
        }

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(view);
        toast.show();
    }

    public static void showLong(Context context, String message) {
        show(context, message, Type.DEFAULT);
    }

    public static void showLong(Context context, String message, Type type) {
        View view = LayoutInflater.from(context).inflate(R.layout.toast_custom, null);
        TextView toastText = view.findViewById(R.id.toastText);
        ImageView toastIcon = view.findViewById(R.id.toastIcon);

        toastText.setText(message);

        switch (type) {
            case SUCCESS:
                toastIcon.setImageResource(R.drawable.ic_success);
                toastIcon.setVisibility(View.VISIBLE);
                break;
            case ERROR:
                toastIcon.setImageResource(R.drawable.ic_error);
                toastIcon.setVisibility(View.VISIBLE);
                break;
            case WARNING:
                toastIcon.setImageResource(R.drawable.ic_warning);
                toastIcon.setVisibility(View.VISIBLE);
                break;
            case INFO:
                toastIcon.setImageResource(R.drawable.ic_info);
                toastIcon.setVisibility(View.VISIBLE);
                break;
            default:
                toastIcon.setVisibility(View.GONE);
                break;
        }

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(view);
        toast.show();
    }
}
