package com.example.lifereapplication.ui.nav;

import com.example.lifereapplication.util.toast.CustomToast;

/**
 * 一个下拉菜单项 —— 把"跳转去哪 + 弹什么提示"声明成一条不可变配置数据。
 *
 * <p>为什么存在：菜单内容因页面而异，若每页各写一套菜单点击逻辑会大量重复；
 * 有了本类，子类只声明数据，由 {@link BaseMenuActivity} 用同一段循环完成
 * 渲染、提示与跳转。为什么设计成不可变（final 字段、无 setter）：
 * 菜单项在页面生命周期内是纯配置，被意外篡改会破坏 id 与点击行为的对应关系。</p>
 *
 * @param label       菜单显示文字
 * @param target      目标 Activity，{@code null} 表示当前页（用于选中态提示）
 * @param toastMessage 从<b>来源页面</b>跳转时要提示的独特文案
 * @param toastType   提示语义类型
 */
public class NavDestination {

    /** 菜单项 id：供基类用 MenuItem.getItemId() 反查本条数据，小整数即可（如 1~4），仅在本页菜单内需要唯一 */
    private final int id;
    private final String label;
    private final Class<?> target;
    private final String toastMessage;
    private final CustomToast.Type toastType;

    /**
     * 为什么 id 由调用方显式传入而不是内部自动生成：点击回调里要用 id
     * 反查这条数据，显式传入让每个页面"菜单顺序 ↔ id"的对应关系
     * 一眼可读、可控可调。
     */
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
