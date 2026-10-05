package com.example.lifereapplication.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.lifereapplication.data.local.dao.ProblemDao;
import com.example.lifereapplication.data.local.entity.ProblemEntity;

/**
 * 应用本地数据库。
 *
 * <p><b>性能与稳定性要点：</b></p>
 * <ul>
 *     <li>单例：数据库连接是重量级资源，全局只创建一个实例，避免句柄泄漏；</li>
 *     <li>{@code allowMainThreadQueries()} 未开启：所有查询强制走子线程；
 *         若误在主线程查询会直接抛异常，把问题暴露在开发期而不是线上；</li>
 *     <li>版本号与迁移：version 只能增不能改，结构变更必须提供 Migration，
 *         否则使用 {@code fallbackToDestructiveMigration()} 会导致用户数据丢失。</li>
 * </ul>
 */
@Database(entities = {ProblemEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "lifere.db";

    private static volatile AppDatabase instance;

    public abstract ProblemDao problemDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DB_NAME)
                            .build();
                }
            }
        }
        return instance;
    }
}
