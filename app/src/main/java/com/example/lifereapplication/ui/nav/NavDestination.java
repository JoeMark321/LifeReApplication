package com.example.lifereapplication.ui.nav;

import com.example.lifereapplication.util.toast.CustomToast;

/**
 * 一个下拉菜单项。
 *
 * @param label       菜单显示文字
 * @param target      目标 Activity，{@code null} 表示当前页（用于选中态提示）
 * @param toastMessage 从<b>来源页面</b>跳转时要提示的独特文案
 * @param toastType   提示语义类型
 */
public class NavDestination {

    private final int id;
    private final String label;
    private final Class<?> target;
    private final String toastMessage;
    private final CustomToast.Type toastType;

    public NavDestination(int id, String label, Class<?> target,
                          String toastMessage, CustomToast.Type toastType) {
        this.id = id;
        this.label = label;
        this.target = target;
        this.toastMessage = toastMessage;
        this.toastType = toastType;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public Class<?> getTarget() {
        return target;
    }

    public String getToastMessage() {
        return toastMessage;
    }

    public CustomToast.Type getToastType() {
        return toastType;
    }
}
