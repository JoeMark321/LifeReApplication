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

    /** 固定文件名：每次启动都打开同一个库文件，数据才能跨启动保留 */
    private static final String DB_NAME = "lifere.db";

    // volatile：配合下方双重检查锁，防止"new 实例"未完全初始化就被其他线程读到
    private static volatile AppDatabase instance;

    /** DAO 入口：Room 在编译期生成实现，业务侧通过该方法取 DAO，而不是自己构造 */
    public abstract ProblemDao problemDao();

    /**
     * 双重检查锁单例。为什么要两层判空：第一层让热路径（实例已存在）无锁直接
     * 返回，避免每次调用都付同步开销；第二层防止两个线程同时通过第一层后重复
     * 建库——只有抢到锁的第一个线程真正创建，后来的进锁后发现已有实例直接复用。
     */
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    // 用 ApplicationContext：数据库与进程同生命周期，
                                    // 绑 Activity 上下文会让库实例持有页面引用造成泄漏
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
