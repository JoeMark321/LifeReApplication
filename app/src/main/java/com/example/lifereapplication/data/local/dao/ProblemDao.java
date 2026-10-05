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

    @Query("SELECT * FROM problems ORDER BY stage ASC")
    List<ProblemEntity> getAll();

    @Query("SELECT * FROM problems WHERE id = :id LIMIT 1")
    ProblemEntity findById(int id);

    @Query("SELECT COUNT(*) FROM problems")
    int count();

    @Query("SELECT DISTINCT category FROM problems ORDER BY category ASC")
    List<String> getCategories();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ProblemEntity> problems);

    @Query("DELETE FROM problems")
    void clear();
}
