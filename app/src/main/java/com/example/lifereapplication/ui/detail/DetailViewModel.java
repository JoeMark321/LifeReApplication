package com.example.lifereapplication.ui.detail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lifereapplication.data.model.Problem;
import com.example.lifereapplication.data.repository.ProblemRepository;

/**
 * 详情页 ViewModel（MVVM 中的 VM）。
 *
 * <p>职责：按 id 从 {@link ProblemRepository} 取单个题目，经 LiveData 推给页面。
 * 为什么选 AndroidViewModel：与 MainViewModel 同理——需要 Application 上下文
 * 构造 Repository；不持任何 View 引用，旋屏重建时 VM 原样存活，
 * Activity 重建后重新 observe 即可拿到数据。</p>
 *
 * <p>边界：本类不做 UI 决策（如"数据不存在要不要退出页面"），
 * 把 null 原样交给 View 自行处理，保持 VM 可独立于界面测试。</p>
 */
public class DetailViewModel extends AndroidViewModel {

    /** 数据源入口：仅本类持有，View 层不可见 */
    private final ProblemRepository repository;
    /** 对外以 LiveData 暴露只读视图：UI 无法从外部写值污染数据 */
    private final MutableLiveData<Problem> problem = new MutableLiveData<>();

    /** Repository 依赖 Application 而非 Activity，避免页面级 Context 泄漏 */
    public DetailViewModel(@NonNull Application application) {
        super(application);
        this.repository = new ProblemRepository(application);
    }

    /** 暴露只读数据流；订阅初期为 null，页面需处理空值（见 DetailActivity 渲染） */
    public LiveData<Problem> getProblem() {
        return problem;
    }

    /**
     * 异步读取：Room 禁止主线程查询，仓库内部切 IO 线程后回贴主线程。
     * 为什么用方法引用 problem::postValue：VM 不关心回调发生在哪个线程，
     * postValue 本身线程安全，线程切换职责完全留给 Repository。
     */
    public void loadProblem(int problemId) {
        repository.getProblemByIdAsync(problemId, problem::postValue);
    }
}
