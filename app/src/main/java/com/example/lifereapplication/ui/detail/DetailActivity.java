package com.example.lifereapplication.ui.detail;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.lifereapplication.R;
import com.example.lifereapplication.data.model.Problem;
import com.example.lifereapplication.ui.lifecycle.LifecycleDemoActivity;
import com.example.lifereapplication.ui.main.MainActivity;
import com.example.lifereapplication.ui.nav.BaseMenuActivity;
import com.example.lifereapplication.ui.nav.NavDestination;
import com.example.lifereapplication.ui.settings.SettingsActivity;
import com.example.lifereapplication.ui.toast.ToastLabActivity;
import com.example.lifereapplication.util.toast.CustomToast;

import java.util.Arrays;
import java.util.List;

/**
 * 详情页（MVVM 中的 V）。
 *
 * <p>职责：从 Intent 接收 problemId（含来源页标记），渲染 ViewModel 提供的数据。
 * 数据由 {@link DetailViewModel} 经仓库的内存缓存/数据库获得，页面自身不做 IO。</p>
 */
public class DetailActivity extends BaseMenuActivity {

    public static final String EXTRA_PROBLEM_ID = "extra_problem_id";
    public static final String EXTRA_SOURCE_PAGE = "extra_source_page";

    private DetailViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        setupToolbar();

        int problemId = getIntent().getIntExtra(EXTRA_PROBLEM_ID, -1);
        if (problemId == -1) {
            CustomToast.show(this, "参数错误：未找到题目ID", CustomToast.Type.ERROR);
            finish();
            return;
        }

        // 每个来源页跳到详情页都有独特提示
        String source = getIntent().getStringExtra(EXTRA_SOURCE_PAGE);
        if (source == null || source.isEmpty()) {
            source = "未知页面";
        }
        tip("从【" + source + "】跳到【详情页】— 正在加载序号 " + problemId, CustomToast.Type.INFO);

        viewModel = new ViewModelProvider(this).get(DetailViewModel.class);
        viewModel.getProblem().observe(this, this::bindProblemData);
        viewModel.loadProblem(problemId);
    }

    private void bindProblemData(Problem problem) {
        if (problem == null) {
            tip("数据加载失败：该主题不存在", CustomToast.Type.ERROR);
            finish();
            return;
        }

        TextView textTitle = findViewById(R.id.textTitle);
        TextView textCategory = findViewById(R.id.textCategory);
        TextView textDifficulty = findViewById(R.id.textDifficulty);
        TextView textDescription = findViewById(R.id.textDescription);
        TextView textSolutionContent = findViewById(R.id.textSolutionContent);

        textTitle.setText(problem.getTitle());
        textCategory.setText(problem.getCategory());
        textDifficulty.setText("难度：" + problem.getDifficulty());
        textDescription.setText(problem.getDescription());
        textSolutionContent.setText(problem.getSolution());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(problem.getTitle());
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    // ---------- 下拉菜单 ----------

    @Override
    protected Page page() {
        return Page.DETAIL;
    }

    @Override
    protected List<NavDestination> destinations() {
        return Arrays.asList(
                new NavDestination(1, "返回本页", DetailActivity.class,
                        "当前就在详情页，无需重复跳转", CustomToast.Type.WARNING),
                new NavDestination(2, "生命周期演示", LifecycleDemoActivity.class,
                        "从【详情页】跳到【生命周期演示】—— 观察 A→B 的回调顺序", CustomToast.Type.INFO),
                new NavDestination(3, "提示实验室", ToastLabActivity.class,
                        "从【详情页】跳到【提示实验室】—— 动手试三种 Toast", CustomToast.Type.INFO),
                new NavDestination(4, "设置", SettingsActivity.class,
                        "从【详情页】跳到【设置】—— 调整提示样式偏好", CustomToast.Type.INFO),
                new NavDestination(5, "回到首页", MainActivity.class,
                        "从【详情页】跳到【首页】—— 返回难题清单", CustomToast.Type.SUCCESS)
        );
    }
}
