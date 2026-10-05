package com.example.lifereapplication.ui.detail;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.lifereapplication.R;
import com.example.lifereapplication.data.model.Problem;
import com.example.lifereapplication.util.CustomToast;

public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_PROBLEM_ID = "extra_problem_id";

    private DetailViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        TextView textArchitecture = findViewById(R.id.textArchitecture);
        textArchitecture.setText("当前架构: MVVM");
        textArchitecture.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));

        int problemId = getIntent().getIntExtra(EXTRA_PROBLEM_ID, -1);
        if (problemId == -1) {
            CustomToast.show(this, "参数错误：未找到题目ID", CustomToast.Type.ERROR);
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(DetailViewModel.class);
        viewModel.loadProblem(problemId);

        viewModel.getProblem().observe(this, this::bindProblemData);
    }

    private void bindProblemData(Problem problem) {
        if (problem == null) {
            CustomToast.show(this, "数据加载失败", CustomToast.Type.ERROR);
            return;
        }

        TextView textTitle = findViewById(R.id.textTitle);
        TextView textDifficulty = findViewById(R.id.textDifficulty);
        TextView textDescription = findViewById(R.id.textDescription);
        TextView textSolutionContent = findViewById(R.id.textSolutionContent);

        textTitle.setText(problem.getTitle());
        textDifficulty.setText("难度: " + problem.getDifficulty());
        textDescription.setText(problem.getDescription());
        textSolutionContent.setText(problem.getSolution());

        CustomToast.show(this, "成功加载： " + problem.getTitle(), CustomToast.Type.SUCCESS);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
