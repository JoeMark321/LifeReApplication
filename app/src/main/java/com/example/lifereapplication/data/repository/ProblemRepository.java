package com.example.lifereapplication.data.repository;

import android.content.Context;
import android.util.LruCache;

import com.example.lifereapplication.data.local.AppDatabase;
import com.example.lifereapplication.data.local.entity.ProblemEntity;
import com.example.lifereapplication.data.model.Problem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 学习主题数据仓库 —— 唯一数据入口（Single Source of Truth）。
 *
 * <p><b>存储分级策略：</b></p>
 * <table border="1">
 *     <caption>数据分级</caption>
 *     <tr><th>数据</th><th>存储位置</th><th>原因</th></tr>
 *     <tr><td>学习主题内容（标题/描述/方案/分类）</td><td>Room 数据库</td>
 *         <td>需要跨进程启动保留，且可能被结构化查询（按分类、按序号）</td></tr>
 *     <tr><td>单个主题详情</td><td>LruCache 内存缓存 + DB 兜底</td>
 *         <td>详情被反复打开，命中内存即可省去一次 IO</td></tr>
 *     <tr><td>分类列表</td><td>内存缓存</td><td>内容少、变化少，重复查库收益低</td></tr>
 *     <tr><td>生命周期事件日志、筛选状态</td><td>仅内存</td>
 *         <td>属于易失的运行时状态，落库反而造成无效写入与表膨胀</td></tr>
 * </table>
 *
 * <p><b>性能设计：</b>读多写少。首页列表读一次后进缓存；详情用 LruCache
 * （上限 4MB）缓存反序列化后的对象；写操作只在首次建库时执行一次。</p>
 */
public class ProblemRepository {

    /** 内存缓存上限：4MB，约可容纳数百个详情对象，远大于实际用量 */
    private static final int DETAIL_CACHE_SIZE = 4 * 1024 * 1024;

    private final AppDatabase database;
    private final LruCache<Integer, Problem> detailCache;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private volatile List<Problem> allProblemsCache;
    private volatile List<String> categoryCache;

    public ProblemRepository(Context context) {
        this.database = AppDatabase.getInstance(context);
        this.detailCache = new LruCache<Integer, Problem>(DETAIL_CACHE_SIZE) {
            @Override
            protected int sizeOf(Integer key, Problem value) {
                // 粗略按字符串长度估算内存占用
                return (value.getTitle().length() + value.getSolution().length()) * 2 + 256;
            }
        };
    }

    /** 全量主题：内存缓存 → 数据库，都不命中则写入初始数据 */
    public List<Problem> getProblems() {
        if (allProblemsCache != null) {
            return allProblemsCache;
        }
        List<Problem> fromDb = queryAll();
        if (fromDb == null || fromDb.isEmpty()) {
            seedDatabaseIfNeeded();
            fromDb = queryAll();
        }
        allProblemsCache = fromDb;
        return fromDb;
    }

    /** 单个主题：优先命中 LruCache，未命中走数据库并回填缓存 */
    public Problem getProblemById(int id) {
        Problem cached = detailCache.get(id);
        if (cached != null) {
            return cached;
        }
        ProblemEntity entity = database.problemDao().findById(id);
        if (entity == null) {
            return null;
        }
        Problem problem = toDomain(entity);
        detailCache.put(problem.getId(), problem);
        return problem;
    }

    /** 分类列表：仅内存缓存，因为内容少且极少变化 */
    public List<String> getCategories() {
        if (categoryCache == null) {
            categoryCache = database.problemDao().getCategories();
        }
        return categoryCache;
    }

    public void prefetchAsync(final Callback callback) {
        ioExecutor.execute(() -> {
            List<Problem> list = getProblems();
            if (callback != null) {
                callback.onLoaded(list);
            }
        });
    }

    /** 详情异步读取：详情页必须用这个，避免主线程查库崩溃 */
    public void getProblemByIdAsync(int id, final DetailCallback callback) {
        ioExecutor.execute(() -> {
            Problem problem = getProblemById(id);
            if (callback != null) {
                callback.onLoaded(problem);
            }
        });
    }

    public interface DetailCallback {
        void onLoaded(Problem problem);
    }

    /** 主动淘汰内存缓存（用户下拉刷新 / 内容更新后调用） */
    public void invalidateCache() {
        allProblemsCache = null;
        categoryCache = null;
        detailCache.evictAll();
    }

    private List<Problem> queryAll() {
        List<ProblemEntity> entities = database.problemDao().getAll();
        List<Problem> result = new ArrayList<>(entities.size());
        for (ProblemEntity entity : entities) {
            result.add(toDomain(entity));
        }
        return result;
    }

    private void seedDatabaseIfNeeded() {
        if (database.problemDao().count() > 0) {
            return;
        }
        database.problemDao().insertAll(buildSeedData());
        invalidateCache();
    }

    private Problem toDomain(ProblemEntity entity) {
        return new Problem(
                entity.id,
                entity.title,
                entity.description,
                entity.difficulty,
                entity.solution,
                entity.category,
                entity.stage,
                entity.featured
        );
    }

    private static List<ProblemEntity> buildSeedData() {
        List<ProblemEntity> list = new ArrayList<>();
        list.add(entity(1, "Activity 完整生命周期", "七个核心回调方法的调用顺序与各自职责", "入门",
                "onCreate → onStart → onResume → onPause → onStop → onDestroy。\n\n"
                        + "1. onCreate：一次性初始化，findViewById、恢复 savedInstanceState、创建 ViewModel。\n"
                        + "2. onStart：界面即将可见，适合注册广播、刷新轻量数据。\n"
                        + "3. onResume：获得焦点，可交互，适合启动动画、相机预览。\n"
                        + "4. onPause：失去焦点但部分可见，只做轻量操作（提交未保存表单、暂停动画）。\n"
                        + "5. onStop：完全不可见，释放大对象、反注册监听、写数据库。\n"
                        + "6. onRestart：从停止态回到前台。\n"
                        + "7. onDestroy：最终清理，置空引用避免泄漏。",
                "生命周期", 1, true));
        list.add(entity(2, "三种生命周期的划分关系", "完整、可见、前台三种范围的差异", "入门",
                "完整生命周期 onCreate ~ onDestroy：决定“什么时候创建/销毁对象”。\n"
                        + "可见生命周期 onStart ~ onStop：决定“什么时候注册/注销 UI 相关监听”。\n"
                        + "前台生命周期 onResume ~ onPause：决定“什么时候占用独占资源（相机、传感器）”。\n\n"
                        + "口诀：重的放 onStop，独占的放 onResume/onPause，一次性的放 onCreate。",
                "生命周期", 2, true));
        list.add(entity(3, "A 跳 B 的回调顺序", "覆盖、半透明、返回三种场景时序分析", "中等",
                "标准覆盖（B 完全不透明）：\n"
                        + "A.onPause → B.onCreate → B.onStart → B.onResume → A.onStop。\n\n"
                        + "半透明/对话框风格 B：\n"
                        + "A.onPause → B.onCreate → B.onStart → B.onResume，A 只走到 onPause 不会 onStop。\n\n"
                        + "按返回键：\n"
                        + "B.onPause → A.onRestart → A.onStart → A.onResume → B.onStop → B.onDestroy。\n\n"
                        + "注意：onStop 的时机由系统决定，不能假设“B 一启动 A 就一定 onStop”，"
                        + "也不要把关键保存逻辑只写在 onPause。",
                "跳转与返回", 3, false));
        list.add(entity(4, "系统配置变更与状态恢复", "旋屏、分屏、深浅色切换时的状态保存", "中等",
                "默认情况下配置变更会重建 Activity：onSaveInstanceState → onStop → onDestroy → onCreate → onStart → onResume。\n\n"
                        + "保存入口有两个：\n"
                        + "1. onSaveInstanceState(Bundle)：仅适合少量可序列化 UI 状态；\n"
                        + "2. ViewModel：跨配置变更保留大数据，但进程被杀后失效。\n\n"
                        + "组合拳：ViewModel + SavedStateHandle 同时覆盖两种情况。\n"
                        + "若确需自行处理（如视频播放器），用 android:configChanges 声明，但只做为优化手段。",
                "状态保存", 4, true));
        list.add(entity(5, "进程回收与数据可靠性", "后台被系统杀掉后如何不丢数据", "困难",
                "onDestroy 不保证被调用！内存不足时系统直接结束进程，Activity 只是“被记住”了任务栈。\n\n"
                        + "可靠方案分层：\n"
                        + "1. 瞬时 UI 状态 → onSaveInstanceState / SavedStateHandle；\n"
                        + "2. 跨页面共享状态 → ViewModel（Application 级 ViewModel 或单例仓库）；\n"
                        + "3. 必须长期保留 → Room / DataStore / 文件，且写入时机要早（onPause/onStop 就落盘）。\n\n"
                        + "本项目即采用第三层（Room）+ 第二层（内存缓存仓库）的双保险。",
                "状态保存", 5, false));
        list.add(entity(6, "Activity 与 Fragment 生命周期联动", "两者回调的对应关系与常见误区", "中等",
                "宿主 onStart 期间 add 的 Fragment，其 onStart 与宿主同批次；\n"
                        + "宿主 onResume 完成后 Fragment 才会 onResume；\n"
                        + "宿主 onPause 后 Fragment 立即 onPause；宿主 onStop 后 Fragment 立即 onStop。\n\n"
                        + "常见误区：\n"
                        + "1. 在 Fragment 构造方法里传参（应用被杀恢复时会丢）—— 用 newInstance + arguments；\n"
                        + "2. 在 onCreateView 之外持有很多 View 引用 —— onDestroyView 后必须置空；\n"
                        + "3. 用 FragmentPagerAdapter + BEHAVIOR_SET_USER_VISIBLE_HINT 处理可见性，建议改用 ViewPager2 + FragmentStateAdapter。",
                "跳转与返回", 6, false));
        list.add(entity(7, "RecyclerView 与生命周期协同", "列表滚动、图片加载与资源释放", "困难",
                "1. Adapter 数据在 onStart 之后绑定，避免 onCreate 阶段做耗时任务；\n"
                        + "2. 列表位置状态用 LayoutManager.onSaveInstanceState() 存入 Bundle；\n"
                        + "3. 图片加载绑定 Item 生命周期：Glide.with(fragment) 而非 with(context)；\n"
                        + "4. 列表复用靠 onViewRecycled 释放位图与监听，防止滚动时内存抖动；\n"
                        + "5. 长列表用 DiffUtil 局部刷新，notifyDataSetChanged 会丢动画且全量重绑。",
                "性能优化", 7, false));
        list.add(entity(8, "多窗口与画中画模式", "分屏、PIP 下的生命周期表现", "困难",
                "多窗口（Android 7.0+）：\n"
                        + "两个窗口同时可见，未获得焦点的 Activity 处于 PAUSED 而不是 STOPPED；\n"
                        + "拖动分隔条改变尺寸会触发 onConfigurationChanged，默认同样重建 Activity。\n\n"
                        + "画中画：\n"
                        + "进入 PIP 回调 onPictureInPictureModeChanged，Activity 变为 PAUSED 但可见；\n"
                        + "应在 onPause 暂停播放、onResume 恢复播放，而不是 onStop。\n\n"
                        + "建议：resizeableActivity 显式声明，并用 lowestAvailableMemory 测试。",
                "多窗口", 8, false));
        list.add(entity(9, "MVVM 架构下的生命周期解耦", "ViewModel + LiveData 消除样板代码", "中等",
                "View（Activity）只负责渲染与转发事件；\n"
                        + "ViewModel 持有业务状态，不知道 View 的存在；\n"
                        + "Repository 管理数据来源，View 不关心来自网络还是数据库。\n\n"
                        + "LiveData.observe(this, ...) 天然具备生命周期感知：\n"
                        + "只有 STARTED ~ STOPPED 之间才会回调，Activity 销毁后自动移除观察者，"
                        + "从根本上避免了“页面已销毁还回调刷新 UI”的崩溃。\n\n"
                        + "本项目所有页面均遵循该分层，右上角菜单跳转即可对比验证。",
                "架构设计", 9, true));
        return list;
    }

    private static ProblemEntity entity(int id, String title, String description,
                                        String difficulty, String solution,
                                        String category, int stage, boolean featured) {
        ProblemEntity e = new ProblemEntity();
        e.id = id;
        e.title = title;
        e.description = description;
        e.difficulty = difficulty;
        e.solution = solution;
        e.category = category;
        e.stage = stage;
        e.featured = featured;
        return e;
    }

    public interface Callback {
        void onLoaded(List<Problem> problems);
    }
}
