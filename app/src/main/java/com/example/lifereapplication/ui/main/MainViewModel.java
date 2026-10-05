package com.example.lifereapplication.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.lifereapplication.data.model.Problem;
import com.example.lifereapplication.data.repository.ProblemRepository;

import java.util.List;

public class MainViewModel extends ViewModel {
    private final ProblemRepository repository = new ProblemRepository();
    private final MutableLiveData<List<Problem>> problems = new MutableLiveData<>();

    public LiveData<List<Problem>> getProblems() {
        if (problems.getValue() == null) {
            problems.setValue(repository.getProblems());
        }
        return problems;
    }
}
