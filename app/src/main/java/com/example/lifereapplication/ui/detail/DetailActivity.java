package com.example.lifereapplication.ui.detail;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ScrollView;
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
 *
 * <p>为什么页面不 import Repository：数据获取统一经 ViewModel——来源是内存
 * 缓存还是 Room 对 V 层透明，本类只关心"拿到 Problem 后怎么渲染"。
 * 另含 ScrollView 滚动位置记忆（离开时存、进入时恢复，见 onCreate/onPause）。</p>
 */
public class DetailActivity extends BaseMenuActivity {

    /** 跳转契约：调用方必须携带难题 id，缺失视为参数错误直接退出 */
    public static final String EXTRA_PROBLEM_ID = "extra_problem_id";
    /** 来源页标记：仅用于生成"从【xx】跳到【详情页】"的教学提示 */
    public static final String EXTRA_SOURCE_PAGE = "extra_source_page";

    private DetailViewModel viewModel;

    /**
     * 入口装配：校验参数 → 来源提示 → 数据观察 → 滚动恢复。
     * 为什么用 Intent extras 传 id 而非静态变量：extras 存在任务栈里，
     * 旋屏重建后 getIntent() 仍能取到；静态字段则可能被进程回收清掉。
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        setupToolbar();

        int problemId = getIntent().getIntExtra(EXTRA_PROBLEM_ID, -1);
        // 参数缺失快速失败：与其渲染一张空详情，不如立即退出避免半可用页面
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

        // observe + 方法引用：数据一到就绑定；页面销毁时 LiveData 自动解绑观察者
        viewModel = new ViewModelProvider(this).get(DetailViewModel.class);
        viewModel.getProblem().observe(this, this::bindProblemData);
        viewModel.loadProblem(problemId);

        // 详情页滚动记忆：解决"滑到一半返回，再进又从开头看"的问题。
        // key 按 problemId 区分，不同难题互不串扰。
        detailScrollKey = "detail." + problemId;
        scrollView = findViewById(R.id.scrollDetail);
        int[] state = com.example.lifereapplication.util.ScrollStateKeeper
                .restore(this, detailScrollKey);
        if (state != null && state[1] > 0) { // state[1] 是纵向偏移，为 0 说明上次就停在顶部
            // 为什么 post 而非立即 scrollTo：此刻内容尚未完成布局，直接滚会被
            // 未完成的测量钳回 0；post 排到首帧布局之后再执行才生效
            scrollView.post(() -> scrollView.scrollTo(0, state[1]));
        }
    }

    /** 滚动记忆键："detail.<problemId>"，不同难题的记忆互不串扰 */
    private String detailScrollKey;
    private ScrollView scrollView;

    /**
     * 滚动位置在 onPause 保存（保证被调用的收尾回调），scrollY 存入 offset 位。
     * 为什么选 onPause：它是离开页面前必然执行的回调（连对话框遮挡都触发），
     * 而 onStop/onDestroy 不保证；且这里只做一次内存写 + 异步落盘，
     * 满足 onPause 不做耗时操作的约束。
     */
    @Override
    public void onPause() {
        if (scrollView != null && detailScrollKey != null) {
            com.example.lifereapplication.util.ScrollStateKeeper
                    .save(this, detailScrollKey, 0, scrollView.getScrollY());
        }
        super.onPause();
    }

    /**
     * 渲染题目数据：页面元素固定且无复用需求，直接 findViewById + setText，
     * 引入 ViewHolder 方案没有收益。
     */
    private void bindProblemData(Problem problem) {
        // VM 回传 null 说明 id 无对应数据：留在空白页只会让用户困惑，直接退出
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

        // 顶栏同步显示主题名：下滑到正文深处也能看到当前在看什么
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(problem.getTitle());
        }
    }

    /** 返回同样走淡入淡出：与进入动画对称，往返观感一致 */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    // ---------- 下拉菜单 ----------

    /** 向基类报备页面身份（DETAIL）：菜单高亮与访问记录依赖它 */
    @Override
    protected Page page() {
        return Page.DETAIL;
    }

    /** 详情页的菜单去向：刻意只保留教学相关入口，保持菜单简短聚焦 */
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
