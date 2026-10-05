package com.example.lifereapplication.data.model;

/**
 * 学习主题模型（领域层对象，不依赖任何框架）。
 *
 * <p>字段分为两类：</p>
 * <ul>
 *     <li><b>持久化字段</b>：title / description / difficulty / solution / category / stage，
 *         属于教学内容，需要跨进程启动保留，因此写入 Room 数据库；</li>
 *     <li><b>派生字段</b>：由持久化字段计算而来，不落库，仅在内存中使用。</li>
 * </ul>
 */
public class Problem {

    private final int id;
    private final String title;
    private final String description;
    private final String difficulty;
    private final String solution;
    /** 分类，用于首页分组与筛选 */
    private final String category;
    /** 建议学习顺序（1 开始），首页序号徽标即取自该字段 */
    private final int stage;
    /** 是否为重点主题，首页置顶卡片使用 */
    private final boolean featured;

    public Problem(int id, String title, String description, String difficulty,
                   String solution, String category, int stage, boolean featured) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.solution = solution;
        this.category = category;
        this.stage = stage;
        this.featured = featured;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getSolution() {
        return solution;
    }

    public String getCategory() {
        return category;
    }

    public int getStage() {
        return stage;
    }

    public boolean isFeatured() {
        return featured;
    }
}
