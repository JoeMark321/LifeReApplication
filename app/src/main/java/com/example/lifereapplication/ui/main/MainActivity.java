package com.example.lifereapplication.ui.main;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lifereapplication.R;
import com.example.lifereapplication.data.memory.LifecycleEventLog;
import com.example.lifereapplication.data.model.Problem;
import com.example.lifereapplication.data.prefs.AppPreferences;
import com.example.lifereapplication.ui.detail.DetailActivity;
import com.example.lifereapplication.ui.lifecycle.LifecycleDemoActivity;
import com.example.lifereapplication.ui.list.ListDemoActivity;
import com.example.lifereapplication.ui.nav.BaseMenuActivity;
import com.example.lifereapplication.ui.nav.NavDestination;
import com.example.lifereapplication.ui.settings.SettingsActivity;
import com.example.lifereapplication.ui.toast.ToastLabActivity;
import com.example.lifereapplication.util.toast.CustomToast;
import com.example.lifereapplication.util.toast.NativeToast;
import com.example.lifereapplication.util.toast.ToastCenter;

import java.util.Arrays;
import java.util.List;

/**
 * 首页（MVVM 中的 V）。
 *
 * <p>职责：渲染列表、转发点击、把自身生命周期阶段实时写到流程图与日志。
 * 每次回调都演示“该阶段该做什么/不该做什么”，与详情页内容互相印证。</p>
 */
public class MainActivity extends BaseMenuActivity
        implements ProblemAdapter.OnItemClickListener, ProblemAdapter.OnHeaderActionListener {

    private MainViewModel viewModel;
    private ProblemAdapter adapter;
    private AppPreferences prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = new AppPreferences(this);
        setupToolbar();

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProblemAdapter(this, this);
        recyclerView.setAdapter(adapter);

        setupBanner();

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        viewModel.getProblems().observe(this, problems -> {
            if (problems == null) {
                return;
            }
            adapter.setProblems(problems);
            // 数据就绪后依次演示三种提示体系
            NativeToast.show(this, "原生 Toast：数据已就绪（" + problems.size() + " 条）");
            CustomToast.show(this, "自定义 Toast：图标 + 圆角样式", CustomToast.Type.SUCCESS);
            ToastCenter.show(this, "现代 Toast：样式跟随用户偏好设置", CustomToast.Type.INFO);
        });

        trace("onCreate");
    }

    /** 首页顶部横向精选区：仅在此小段区域内响应水平手势 */
    private void setupBanner() {
        RecyclerView rvBanner = findViewById(R.id.rvBanner);
        if (rvBanner == null) {
            return;
        }
        rvBanner.setLayoutManager(BannerAdapter.horizontalLayout(this));
        BannerAdapter bannerAdapter = new BannerAdapter();
        // tag 携带回调，避免 Adapter 持有 Activity 强引用
        rvBanner.setTag((BannerAdapter.OnBannerClickListener) problem -> {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(DetailActivity.EXTRA_PROBLEM_ID, problem.getId());
            intent.putExtra(DetailActivity.EXTRA_SOURCE_PAGE, "首页横幅");
            startActivity(intent);
        });
        rvBanner.setAdapter(bannerAdapter);
        // 精选内容 = 标记为 featured 的主题，懒加载自列表数据
        viewModel.getProblems().observe(this, problems -> {
            if (problems == null) {
                return;
            }
            List<Problem> featured = new java.util.ArrayList<>();
            for (Problem p : problems) {
                if (p.isFeatured()) {
                    featured.add(p);
                }
            }
            bannerAdapter.submit(featured);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        trace("onStart");
        adapter.updateLifecycleStage("onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        trace("onResume");
        adapter.updateLifecycleStage("onResume");
        ToastCenter.showSilent("MainActivity", "onResume：可交互，适合启动动画/恢复播放");
    }

    @Override
    protected void onPause() {
        super.onPause();
        trace("onPause");
        adapter.updateLifecycleStage("onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        trace("onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        trace("onDestroy");
    }

    /** 生命周期事件仅记录到内存日志（按设置开关），不落库、不打扰用户 */
    private void trace(String callback) {
        if (prefs != null && prefs.isLifecycleTraceEnabled()) {
            LifecycleEventLog.record("首页", callback);
            ToastCenter.showSilent("Lifecycle", "首页." + callback);
        }
    }

    @Override
    public void onItemClick(Problem problem) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(DetailActivity.EXTRA_PROBLEM_ID, problem.getId());
        intent.putExtra(DetailActivity.EXTRA_SOURCE_PAGE, "首页 · 序号" + problem.getStage());
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onQuickActionClick(String tag) {
        switch (tag) {
            case "beginner":
                openDetail(1, "新手必读 → 打开第 1 关：Activity 完整生命周期");
                break;
            case "pitfall":
                openDetail(5, "踩坑清单 → 打开第 5 关：进程回收与数据可靠性");
                break;
            case "interview":
                openDetail(9, "面试速记 → 打开第 9 关：MVVM 与生命周期解耦");
                break;
            default:
                break;
        }
    }

    private void openDetail(int problemId, String toastMessage) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(DetailActivity.EXTRA_PROBLEM_ID, problemId);
        intent.putExtra(DetailActivity.EXTRA_SOURCE_PAGE, "首页快捷入口");
        startActivity(intent);
        ToastCenter.show(this, toastMessage, CustomToast.Type.INFO);
    }

    // ---------- 右上角下拉菜单 ----------

    @Override
    protected Page page() {
        return Page.MAIN;
    }

    @Override
    protected List<NavDestination> destinations() {
        return Arrays.asList(
                new NavDestination(1, "返回本页", MainActivity.class,
                        "当前就在首页，无需重复跳转", CustomToast.Type.WARNING),
                new NavDestination(2, "列表演示", ListDemoActivity.class,
                        "从【首页】跳到【列表演示】—— 体验滚动位置记忆", CustomToast.Type.INFO),
                new NavDestination(3, "生命周期演示", LifecycleDemoActivity.class,
                        "从【首页】跳到【生命周期演示】—— 实时观察回调顺序", CustomToast.Type.INFO),
                new NavDestination(4, "提示实验室", ToastLabActivity.class,
                        "从【首页】跳到【提示实验室】—— 对比三种 Toast", CustomToast.Type.INFO),
                new NavDestination(5, "设置", SettingsActivity.class,
                        "从【首页】跳到【设置】—— 管理提示权限与样式", CustomToast.Type.INFO),
                new NavDestination(6, "关于本项目", null,
                        "LifeReApplication v2.0 · MVVM + Room + LiveData", CustomToast.Type.SUCCESS)
        );
    }
}
