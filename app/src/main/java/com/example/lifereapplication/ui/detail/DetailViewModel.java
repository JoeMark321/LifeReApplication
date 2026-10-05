package com.example.lifereapplication.ui.detail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lifereapplication.data.model.Problem;
import com.example.lifereapplication.data.repository.ProblemRepository;

public class DetailViewModel extends AndroidViewModel {

    private final ProblemRepository repository;
    private final MutableLiveData<Problem> problem = new MutableLiveData<>();

    public DetailViewModel(@NonNull Application application) {
        super(application);
        this.repository = new ProblemRepository(application);
    }

    public LiveData<Problem> getProblem() {
        return problem;
    }

    /** 异步读取：Room 禁止主线程查询，仓库内部切 IO 线程后回贴主线程 */
    public void loadProblem(int problemId) {
        repository.getProblemByIdAsync(problemId, problem::postValue);
    }
}
