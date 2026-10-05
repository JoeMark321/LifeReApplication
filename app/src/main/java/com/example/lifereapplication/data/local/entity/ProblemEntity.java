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

    @PrimaryKey
    public int id;

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

    public int stage;

    public boolean featured;

    public ProblemEntity() {
        // Room 需要无参构造
    }
}
