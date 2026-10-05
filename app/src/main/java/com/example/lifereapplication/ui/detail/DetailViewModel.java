package com.example.lifereapplication.ui.detail;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.lifereapplication.data.model.Problem;
import com.example.lifereapplication.data.repository.ProblemRepository;

public class DetailViewModel extends ViewModel {
    private final ProblemRepository repository = new ProblemRepository();
    private final MutableLiveData<Problem> problem = new MutableLiveData<>();

    public LiveData<Problem> getProblem() {
        return problem;
    }

    public void loadProblem(int problemId) {
        Problem loadedProblem = repository.getProblemById(problemId);
        if (loadedProblem != null) {
            problem.setValue(loadedProblem);
        }
    }
}
