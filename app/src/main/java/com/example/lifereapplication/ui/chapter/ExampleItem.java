package com.example.lifereapplication.ui.chapter;

/**
 * 章节数据模型（演示 Arrays.asList(new ExampleItem...) 适配器写法）。
 *
 * <p><b>为什么字段全 final、只读不写：</b>章节数据在适配器里是静态数据源，
 * 做成不可变对象后不会被中途误改导致页面与数据脱节，也天然线程安全；
 * 需要"改数据"时整体替换 ExampleItem 实例即可。</p>
 */
public class ExampleItem {

    private final String title;
    private final String subtitle;

    /** 全参构造 + final 字段：实例一经创建即不可变 */
    public ExampleItem(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }
}
