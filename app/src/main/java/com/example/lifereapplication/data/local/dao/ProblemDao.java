package com.example.lifereapplication.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.lifereapplication.data.local.entity.ProblemEntity;

import java.util.List;

/**
 * 学习主题数据访问对象。
 *
 * <p>注意：DAO 中的方法是<b>同步</b>的。业务上由 Repository 切到 IO 线程执行，
 * 避免阻塞主线程；同步实现比返回 {@code LiveData} 更利于在 Repository 层做
 * 缓存兜底与单元测试。</p>
 */
@Dao
public interface ProblemDao {

    /** 全量读取。ORDER BY stage：列表展示顺序 = 建议学习顺序，排序下推给 SQLite 而非内存排序 */
    @Query("SELECT * FROM problems ORDER BY stage ASC")
    List<ProblemEntity> getAll();

    /** 按主键查详情。LIMIT 1：主键本就唯一，显式写出让查询意图明确，也允许引擎扫到即停 */
    @Query("SELECT * FROM problems WHERE id = :id LIMIT 1")
    ProblemEntity findById(int id);

    /** 只取行数。为什么不用 getAll().size() 判空：COUNT 在库内即可完成，不必把全部行读进内存 */
    @Query("SELECT COUNT(*) FROM problems")
    int count();

    /** 分类去重列表。用 SELECT DISTINCT：去重在库内顺手完成，省掉全量数据进内存再 Java 去重 */
    @Query("SELECT DISTINCT category FROM problems ORDER BY category ASC")
    List<String> getCategories();

    /**
     * 批量插入。为什么用 REPLACE：种子数据 id 固定，重复播种时覆盖旧行而不是
     * 因主键冲突抛异常，让"播种"天然幂等。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ProblemEntity> problems);

    /** 清空全表：提供重置兜底能力，配合重新播种可恢复初始内容 */
    @Query("DELETE FROM problems")
    void clear();
}
