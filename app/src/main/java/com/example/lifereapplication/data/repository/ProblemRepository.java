package com.example.lifereapplication.data.repository;

import com.example.lifereapplication.data.model.Problem;

import java.util.ArrayList;
import java.util.List;

public class ProblemRepository {
    public List<Problem> getProblems() {
        List<Problem> problems = new ArrayList<>();
        problems.add(new Problem(1, "Activity生命周期", "理解Activity的七个核心回调方法", "中等", "通过Logcat观察生命周期回调顺序..."));
        problems.add(new Problem(2, "配置变更处理", "屏幕旋转时如何保存和恢复状态", "简单", "使用onSaveInstanceState保存关键状态..."));
        problems.add(new Problem(3, "内存管理", "系统回收进程时如何保证数据不丢失", "困难", "结合ViewModel和SavedStateHandle..."));
        problems.add(new Problem(4, "MVVM架构实践", "如何在Android中实现MVVM模式", "中等", "使用ViewModel+LiveData+Repository..."));
        problems.add(new Problem(5, "RecyclerView优化", "长列表性能优化与内存管理", "困难", "使用DiffUtil和视图复用机制..."));
        problems.add(new Problem(6, "数据持久化", "本地数据存储方案选择与实现", "简单", "Room数据库+SharedPreferences..."));
        return problems;
    }

    public Problem getProblemById(int id) {
        for (Problem problem : getProblems()) {
            if (problem.getId() == id) {
                return problem;
            }
        }
        return null;
    }
}
