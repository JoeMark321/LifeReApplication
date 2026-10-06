package com.example.lifereapplication.ui.chapter;

/**
 * 章节数据模型（演示 Arrays.asList(new ExampleItem...) 适配器写法）。
 */
public class ExampleItem {

    private final String title;
    private final String subtitle;

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
