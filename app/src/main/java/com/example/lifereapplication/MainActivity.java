package com.example.lifereapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lifereapplication.data.model.Problem;
import com.example.lifereapplication.ui.detail.DetailActivity;
import com.example.lifereapplication.ui.main.MainViewModel;
import com.example.lifereapplication.ui.main.ProblemAdapter;

public class MainActivity extends AppCompatActivity implements ProblemAdapter.OnItemClickListener {

    private MainViewModel viewModel;
    private ProblemAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView textArchitecture = findViewById(R.id.textArchitecture);
        textArchitecture.setText("当前架构: MVVM");

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProblemAdapter(this);
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        viewModel.getProblems().observe(this, problems -> {
            if (problems != null) {
                adapter.setProblems(problems);
            }
        });
    }

    @Override
    public void onItemClick(Problem problem) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(DetailActivity.EXTRA_PROBLEM_ID, problem.getId());
        startActivity(intent);
    }
}
