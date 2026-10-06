package com.example.lifereapplication.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 学习主题的数据库实体。
 *
 * <p><b>为什么需要实体：</b>Room 的字段、类型、注解必须与表结构一一对应，
 * 如果把领域模型 {@code Problem} 直接当实体用，后续领域模型演化（例如增加
 * 派生字段、拆分子对象）就会被迫带动数据库迁移。因此这里单独定义实体，
 * 由 Repository 负责实体与领域模型互转。</p>
 */
@Entity(tableName = "problems")
public class ProblemEntity {

    // 字段用 public 是 Room 惯例：Room 直接按字段读写，省去 getter/setter 样板；
    // 实体只在 data 层内部流转、经 Repository 转成 Problem 后才对外，public 不会破坏封装

    /** 主键：与内置种子数据的固定 id 一一对应，是详情跳转与缓存 key 的依据 */
    @PrimaryKey
    public int id;

    // @NonNull 标记 NOT NULL 列；配合默认值 ""，无参构造后字段立即可用，
    // 也防止写入侧漏字段时插入 NULL 引发后续 NPE
    @NonNull
    public String title = "";

    @NonNull
    public String description = "";

    @NonNull
    public String difficulty = "";

    @NonNull
    public String solution = "";

    @NonNull
    public String category = "";

    /** 建议学习顺序（1 开始），列表查询按它升序展示 */
    public int stage;

    /** 是否重点主题（首页置顶卡片展示用），与领域模型 Problem.featured 对应 */
    public boolean featured;

    public ProblemEntity() {
        // Room 需要无参构造：Room 通过反射实例化实体时走这里，字段靠上面的默认值兜底
    }
}
