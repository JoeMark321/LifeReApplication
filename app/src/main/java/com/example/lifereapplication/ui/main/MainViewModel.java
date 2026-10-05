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
 */
public class MainViewModel extends AndroidViewModel {

    private final ProblemRepository repository;
    private final MutableLiveData<List<Problem>> problems = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    /** 仅内存的运行时状态：当前筛选分类，不落库 */
    private String currentCategory = "全部";

    public MainViewModel(@NonNull Application application) {
        super(application);
        this.repository = new ProblemRepository(application);
    }

    public LiveData<List<Problem>> getProblems() {
        if (problems.getValue() == null) {
            loadProblems();
        }
        return problems;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    /** 子线程预加载，主线程只收结果（懒加载 + 不卡启动） */
    public void loadProblems() {
        loading.setValue(true);
        repository.prefetchAsync(list -> {
            if ("全部".equals(currentCategory)) {
                problems.postValue(list);
            } else {
                problems.postValue(filter(list));
            }
            loading.postValue(false);
        });
    }

    /** 按分类筛选，状态只保存在内存 */
    public void filterByCategory(String category) {
        currentCategory = category == null ? "全部" : category;
        if (problems.getValue() != null) {
            problems.setValue(filter(problems.getValue()));
        }
    }

    public String getCurrentCategory() {
        return currentCategory;
    }

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
