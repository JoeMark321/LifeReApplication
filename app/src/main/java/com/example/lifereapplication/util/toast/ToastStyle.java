package com.example.lifereapplication.util.toast;

/** 三种提示样式 */
public enum ToastStyle {
    /** 原生风格：系统 Toast，最简单、样式不可定制 */
    NATIVE,
    /** 自定义风格：自定义布局 + 图标 + 圆角半透明背景 */
    CUSTOM,
    /** 现代化风格：Material Snackbar，可带操作按钮，可滑动消失 */
    MODERN
}
