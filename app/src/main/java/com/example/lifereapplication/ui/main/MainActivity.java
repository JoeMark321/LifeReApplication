package com.example.lifereapplication.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lifereapplication.R;
import com.example.lifereapplication.SplashGate;
import com.example.lifereapplication.data.memory.LifecycleEventLog;
import com.example.lifereapplication.data.model.Problem;
import com.example.lifereapplication.data.prefs.AppPreferences;
import com.example.lifereapplication.ui.detail.DetailActivity;
import com.example.lifereapplication.ui.chapter.ChapterActivity;
import com.example.lifereapplication.ui.lifecycle.LifecycleDemoActivity;
import com.example.lifereapplication.ui.list.ListDemoActivity;
import com.example.lifereapplication.ui.list.ListDemoSecondActivity;
import com.example.lifereapplication.ui.list.ListDemoThirdActivity;
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
 *
 * <p><b>MVVM 边界：为什么 Activity 不直接碰 Repository？</b>
 * 数据的获取、筛选、缓存全部下沉到 {@link MainViewModel}，本类只 observe
 * LiveData 并把用户点击原样转发。这样旋屏重建时数据不丢（VM 存活），
 * 页面只关心"展示什么"而非"数据从哪来"，将来换数据源 View 层零改动。</p>
 *
 * <p>结构三段：冷启动开屏（系统 SplashScreen + 应用内 SplashGate 遮罩）、
 * 难题列表（{@link ProblemAdapter}：头部引导区 + 卡片 + 内嵌横向横幅）、
 * 右上角数据驱动下拉菜单（{@link #destinations()}）。</p>
 */
public class MainActivity extends BaseMenuActivity
        implements ProblemAdapter.OnItemClickListener, ProblemAdapter.OnHeaderActionListener,
        ProblemAdapter.OnBannerClickListener {

    /** 数据唯一来源：Activity 只 observe，不 import Repository——换数据源 UI 零改动 */
    private MainViewModel viewModel;
    /** 列表适配器：头部引导区 + 难题卡片 + 内嵌横幅，点击经三个回调接口上抛 */
    private ProblemAdapter adapter;
    /** 用户设置入口：生命周期埋点开关、"上次访问"标签都从这里读 */
    private AppPreferences prefs;

    /**
     * 冷启动入口，按依赖顺序做三件事：系统开屏 → 应用内遮罩 → 列表装配。
     *
     * <p>为什么 installSplashScreen 要放在 super.onCreate 之前：
     * SplashScreen 库要求在窗口内容建立前接管启动主题，否则会先闪一帧旧主题。</p>
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // 系统级 Splash（品牌蓝 + 图标）与主题无缝衔接
        androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 应用内开屏遮罩：跨 ROM 可控的 2s 停留 + 300ms 淡出。
        // 仅冷启动（进程新建）展示一次；热启动/旋屏重建不展示（SplashGate + savedInstanceState 双保险）。
        // 系统级 SplashScreen 在 MIUI/HyperOS 上"一闪而过"不可控，故停留与淡出全部自绘。
        View splashOverlay = findViewById(R.id.splashOverlay);
        // 双保险缺一不可：SplashGate 是进程级冷启动标记——热启动（进程未死）回前台
        // 返回 false，旋屏重建（进程同样未死）也返回 false；savedInstanceState == null
        // 再挡一层"进程活着但 Activity 被重建"的边角情况，两者齐备才认定冷启动
        boolean showSplash = SplashGate.consumeColdLaunch() && savedInstanceState == null;
        if (showSplash) {
            // 遮罩参数的设计动机：常驻 2s 是"冷启动停留"的演示时长；300ms 淡出让揭开
            // 可感知又不拖沓（直接 setVisibility(GONE) 会生硬闪切）。
            // 为什么延迟到之后才揭开：postDelayed 期间遮罩之下的真实内容已完成首次绘制，
            // 淡出露出的是成图而非白屏。
            splashOverlay.setVisibility(View.VISIBLE);
            splashOverlay.postDelayed(() -> splashOverlay.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction(() -> splashOverlay.setVisibility(View.GONE))
                            .start(),
                    2000);
        }
        prefs = new AppPreferences(this);
        setupToolbar(); // 顶栏与右上角菜单由基类统一装配，本类只提供 page/destinations

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this)); // 默认纵向：符合"一次一题"的阅读流
        // 三个 this：Activity 同时实现卡片/快捷卡/横幅三个回调接口，
        // adapter 只上抛"点了什么"，"点了去哪"由页面决定
        adapter = new ProblemAdapter(this, this, this);
        recyclerView.setAdapter(adapter);

        // ViewModelProvider 以 Activity 为 LifecycleOwner：旋屏重建拿到同一个 VM，
        // problems 值仍在——这就是列表不重复查库的原因
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        // observe 传 this：页面销毁时 LiveData 自动移除观察者，不会回调已死的 Activity
        viewModel.getProblems().observe(this, problems -> {
            // LiveData 首次订阅时 value 尚未异步回填，空值直接忽略、等待推送
            if (problems == null) {
                return;
            }
            adapter.setProblems(problems);
            // 横幅数据：adapter 内部缓存，HeaderHolder 创建时自动填充。
            // 过滤 featured 只是展示选材（isFeatured 标记），不构成独立数据状态，留在 View 层即可
            List<Problem> featured = new java.util.ArrayList<>();
            for (Problem p : problems) {
                if (p.isFeatured()) {
                    featured.add(p);
                }
            }
            adapter.submitFeatured(featured);
            // 数据就绪后依次演示三种提示体系（选这个时机：页面已有内容，提示不会盖在白屏上）
            NativeToast.show(this, "原生 Toast：数据已就绪（" + problems.size() + " 条）");
            CustomToast.show(this, "自定义 Toast：图标 + 圆角样式", CustomToast.Type.SUCCESS);
            ToastCenter.show(this, "现代 Toast：样式跟随用户偏好设置", CustomToast.Type.INFO);
        });

        trace("onCreate");
    }

    /** 首页横幅点击：从"本周精选"直达对应主题详情 */
    @Override
    public void onBannerClick(Problem problem) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(DetailActivity.EXTRA_PROBLEM_ID, problem.getId());
        intent.putExtra(DetailActivity.EXTRA_SOURCE_PAGE, "首页横幅");
        startActivity(intent);
        // 系统自带淡入淡出：与卡片入口的转场统一观感，无需自定义动画资源
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /** 即将可见：同步流程图高亮，教学文案与真实回调一一对应 */
    @Override
    protected void onStart() {
        super.onStart();
        trace("onStart");
        adapter.updateLifecycleStage("onStart");
    }

    /** 获得焦点：除刷新流程图外，用静默提示演示"onResume 该做什么"这一知识点 */
    @Override
    protected void onResume() {
        super.onResume();
        trace("onResume");
        adapter.updateLifecycleStage("onResume");
        // showSilent 静默通道：把知识点递给用户，又不打断刚获得的交互焦点
        ToastCenter.showSilent("MainActivity", "onResume：可交互，适合启动动画/恢复播放");
    }

    /** 失去焦点：流程图同步演示"此时只做轻量保存，别拖慢跳转" */
    @Override
    protected void onPause() {
        super.onPause();
        trace("onPause");
        adapter.updateLifecycleStage("onPause");
    }

    /** 完全不可见：只记日志、不再刷新流程图——界面即将离开，改了也看不见 */
    @Override
    protected void onStop() {
        super.onStop();
        trace("onStop");
    }

    /** 最终清理：记录回调即完成本页教学使命，页面未持有需手动释放的原生资源 */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        trace("onDestroy");
    }

    /**
     * 生命周期事件仅记录到内存日志（按设置开关），不落库、不打扰用户。
     * 为什么不写数据库：每个回调都会触发，落库的 IO 开销会违背
     * "onPause/onStop 里不做重活"这条本应用正在教的原则。
     */
    private void trace(String callback) {
        if (prefs != null && prefs.isLifecycleTraceEnabled()) {
            LifecycleEventLog.record("首页", callback);
            ToastCenter.showSilent("Lifecycle", "首页." + callback);
        }
    }

    /** 卡片点击 → 详情页：extra 携带 id 与来源标记，详情页据此生成教学提示 */
    @Override
    public void onItemClick(Problem problem) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(DetailActivity.EXTRA_PROBLEM_ID, problem.getId());
        intent.putExtra(DetailActivity.EXTRA_SOURCE_PAGE, "首页 · 序号" + problem.getStage());
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * 头部三张快捷卡的点击上抛：tag → 固定难题的映射收在 Activity 侧
     * （新手→第 1 关 / 踩坑→第 5 关 / 面试→第 9 关），卡片文案或布局
     * 后续调整不会波及跳转逻辑。
     */
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

    /** 快捷入口统一走这里：Intent 组装与来源页标记只写一遍，多个入口不漂移 */
    private void openDetail(int problemId, String toastMessage) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(DetailActivity.EXTRA_PROBLEM_ID, problemId);
        intent.putExtra(DetailActivity.EXTRA_SOURCE_PAGE, "首页快捷入口");
        startActivity(intent);
        ToastCenter.show(this, toastMessage, CustomToast.Type.INFO);
    }

    // ---------- 右上角下拉菜单 ----------

    /** 上次访问页面标签（右上角菜单保存逻辑的可视化） */
    private String lastPageLabel() {
        String last = prefs.getLastPage();
        return last == null ? "无记录" : last.replace("Activity", "");
    }

    /** 向基类报备页面身份：菜单高亮与"上次访问"记录都依赖它区分页面 */
    @Override
    protected Page page() {
        return Page.MAIN;
    }

    /**
     * 数据驱动的下拉菜单：每项自带"从哪到哪 + 考什么"的教学文案，
     * 新增跳转只需加一行 NavDestination，无需改基类。
     * target 为 null 的是纯展示项（"关于本项目"），只弹信息不发生真实跳转。
     */
    @Override
    protected List<NavDestination> destinations() {
        return Arrays.asList(
                new NavDestination(1, "返回本页", MainActivity.class,
                        "当前就在首页，无需重复跳转", CustomToast.Type.WARNING),
                new NavDestination(2, "章节学习", ChapterActivity.class,
                        "从【首页】跳到【章节学习】—— ViewPager2 联动 Fragment", CustomToast.Type.INFO),
                new NavDestination(3, "列表演演", ListDemoActivity.class,
                        "从【首页】跳到【列表演演】—— 体验滚动位置记忆", CustomToast.Type.INFO),
                new NavDestination(4, "列表演演二·复述", ListDemoSecondActivity.class,
                        "从【首页】跳到【列表演演二】—— 内容复述对比", CustomToast.Type.INFO),
                new NavDestination(5, "列表演演三·自定义导航", ListDemoThirdActivity.class,
                        "从【首页】跳到【列表演演三】—— 自定义底部导航样式", CustomToast.Type.INFO),
                new NavDestination(6, "生命周期演示", LifecycleDemoActivity.class,
                        "从【首页】跳到【生命周期演示】—— 实时观察回调顺序", CustomToast.Type.INFO),
                new NavDestination(7, "提示实验室", ToastLabActivity.class,
                        "从【首页】跳到【提示实验室】—— 对比三种 Toast", CustomToast.Type.INFO),
                new NavDestination(8, "设置", SettingsActivity.class,
                        "从【首页】跳到【设置】—— 管理提示权限与样式", CustomToast.Type.INFO),
                new NavDestination(9, "关于本项目", null,
                        "上次访问：" + lastPageLabel() + " · LifeReApplication v2.0", CustomToast.Type.SUCCESS)
        );
    }
}
