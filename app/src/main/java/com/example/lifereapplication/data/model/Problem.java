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
 *
 * <p><b>为什么设计成不可变对象：</b>字段全部 final，构造后状态不再改变——
 * 被放进 Repository 的 LruCache 后可被多线程随意读，天然线程安全，也不存在
 * "缓存里的对象被外部改坏"的问题；需要变化就 new 新对象，小对象代价可忽略。</p>
 */
public class Problem {

    /** 唯一标识：与数据库主键一致，详情页跳转和 LruCache 的 key 都用它 */
    private final int id;
    // 以下四个为持久化教学内容字段，值由 Repository 从 ProblemEntity 转换而来
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

    /** 全字段构造是唯一创建入口：对象一出生就是完整、可用的（配合不可变设计） */
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

    // ---- 只读访问器：不提供任何 setter——字段全 final，这是不可变性的对外保证 ----
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
