package com.example.lifereapplication.util.toast;

/**
 * 三种提示样式。
 *
 * <p>为什么用枚举：取值封闭可穷举，序列化成字符串存入偏好后仍能安全解析
 * （解析失败由 {@link ToastCenter#resolveStyle} 兜底）。</p>
 *
 * <p>链路位置：它是样式路由的「决策依据」——ToastCenter.show 读取用户保存的
 * 本枚举值决定把消息交给哪个实现渲染；本枚举本身不含任何展示逻辑。</p>
 */
public enum ToastStyle {
    /** 原生风格：系统 Toast，最简单、样式不可定制 */
    NATIVE,
    /** 自定义风格：自定义布局 + 图标 + 圆角半透明背景 */
    CUSTOM,
    /** 现代化风格：Material Snackbar，可带操作按钮，可滑动消失 */
    MODERN
}
