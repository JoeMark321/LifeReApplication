package com.example.lifereapplication.ui.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lifereapplication.data.model.Problem;
import com.example.lifereapplication.data.repository.ProblemRepository;

import java.util.List;

/**
 * 首页 ViewModel。
 *
 * <p>选 {@link AndroidViewModel} 而非 {@code ViewModel}，是为了拿到
 * Application 上下文去构造 Repository；<b>绝不持有 Activity 引用</b>，
 * 否则旋屏重建时旧 Activity 无法回收。<b>跨配置变更存活</b>：旋屏后
 * problems 值仍在，不会重复查库。</p>
 *
 * <p>MVVM 边界：Activity 只 observe 暴露的 LiveData 并转发点击事件；
 * Repository 的构造、线程切换、筛选规则全部封在本类——View 层不 import
 * Repository，将来把内存数据换成数据库/网络，页面代码一行不改。</p>
 */
public class MainViewModel extends AndroidViewModel {

    /** 数据源入口：只在本类持有，View 层不可见 */
    private final ProblemRepository repository;
    /** 对外以 LiveData 暴露只读视图：UI 无法从外部 postValue 污染数据 */
    private final MutableLiveData<List<Problem>> problems = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    /** 仅内存的运行时状态：当前筛选分类，不落库 */
    private String currentCategory = "全部";

    /** Application 由系统注入：Repository 需要全局 Context，
     *  用 Application 而非 Activity 才不会随页面销毁而泄漏 */
    public MainViewModel(@NonNull Application application) {
        super(application);
        this.repository = new ProblemRepository(application);
    }

    /**
     * 懒加载入口：首个观察者订阅时才触发查库。
     * 为什么判空放在 getter 而非构造函数：构造发生在 onCreate，
     * 无人订阅时预加载纯属浪费；VM 跨旋屏存活 + 判空，天然防重复加载。
     */
    public LiveData<List<Problem>> getProblems() {
        if (problems.getValue() == null) {
            loadProblems();
        }
        return problems;
    }

    /** 暴露加载态：UI 据此显隐进度，无需轮询或额外回调 */
    public LiveData<Boolean> getLoading() {
        return loading;
    }

    /**
     * 子线程预加载，主线程只收结果（懒加载 + 不卡启动）。
     * 为什么查询不在本方法内同步做：onCreate 主线程同步查库会拖慢首帧。
     */
    public void loadProblems() {
        loading.setValue(true); // 本方法只在主线程被调（UI 触发），可直接 setValue
        repository.prefetchAsync(list -> {
            // 回调运行在子线程：必须 postValue（setValue 会抛 "Wrong thread" 异常）
            if ("全部".equals(currentCategory)) {
                problems.postValue(list);
            } else {
                // 筛选也放到子线程完成，主线程只负责渲染
                problems.postValue(filter(list));
            }
            loading.postValue(false);
        });
    }

    /**
     * 按分类筛选，状态只保存在内存。
     * 为什么状态放 VM 而非 Activity 字段：旋屏后 Activity 重建、VM 存活，
     * 用户选的分类不丢；为什么用 setValue：由 UI 点击触发必在主线程，
     * 即时分发、省去 post 的线程切换开销。
     */
    public void filterByCategory(String category) {
        currentCategory = category == null ? "全部" : category;
        if (problems.getValue() != null) {
            problems.setValue(filter(problems.getValue()));
        }
    }

    /** 供 UI 旋屏重建后回显当前选中的分类 */
    public String getCurrentCategory() {
        return currentCategory;
    }

    /** 内存线性过滤：数据量为十几条级，遍历即可；"全部"直接返回原列表避免无谓拷贝 */
    private List<Problem> filter(List<Problem> source) {
        if ("全部".equals(currentCategory) || source == null) {
            return source;
        }
        List<Problem> result = new java.util.ArrayList<>();
        for (Problem problem : source) {
            if (currentCategory.equals(problem.getCategory())) {
                result.add(problem);
            }
        }
        return result;
    }
}
