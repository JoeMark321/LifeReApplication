package com.example.lifereapplication.ui.lifecycle;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.lifereapplication.R;
import com.example.lifereapplication.data.memory.LifecycleEventLog;
import com.example.lifereapplication.data.prefs.AppPreferences;
import com.example.lifereapplication.util.ScrollStateKeeper;
import com.example.lifereapplication.ui.detail.DetailActivity;
import com.example.lifereapplication.ui.dialog.DialogStyleActivity;
import com.example.lifereapplication.ui.main.MainActivity;
import com.example.lifereapplication.ui.nav.BaseMenuActivity;
import com.example.lifereapplication.ui.nav.NavDestination;
import com.example.lifereapplication.ui.settings.SettingsActivity;
import com.example.lifereapplication.ui.toast.ToastLabActivity;
import com.example.lifereapplication.util.toast.CustomToast;

import java.util.Arrays;
import java.util.List;

/**
 * 生命周期演示页：把抽象回调变成可操作的实验。
 *
 * <p>三个实验覆盖文档第 3 节的核心结论：</p>
 * <ol>
 *     <li>A→B 完全覆盖：A.onPause → B.onCreate…B.onResume → A.onStop；</li>
 *     <li>对话框风格覆盖：A 只 onPause，不会 onStop；</li>
 *     <li>回桌面：onStop 触发但 onDestroy 不触发，进程仍存活。</li>
 * </ol>
 */
public class LifecycleDemoActivity extends BaseMenuActivity {

    /**
     * 滚动状态 key：ScrollStateKeeper 按 key 全局存取（内存 LruCache + SP），
     * 用"页面名.用途"的命名空间避免与其他页面的滚动状态互相覆盖。
     */
    private static final String SCROLL_KEY = "lifecycle.demo";

    private AppPreferences prefs;
    private TextView textLog;
    private TextView textFlowHint;
    private ScrollView scrollView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lifecycle_demo);
        prefs = new AppPreferences(this);
        setupToolbar();

        textLog = findViewById(R.id.textLog);
        textFlowHint = findViewById(R.id.textFlowHint);

        // 滚动状态记忆：滑动一半退出再进，精确回到当前 scrollY（与详情页同方案）
        scrollView = findViewById(R.id.scrollDemo);
        int[] saved = ScrollStateKeeper.restore(this, SCROLL_KEY);
        // saved[1] 是纵向偏移（scrollY），> 0 才有恢复的意义，零位无需折腾
        if (saved != null && saved[1] > 0) {
            // 为什么 post 延迟滚动：此刻 ScrollView 尚未完成布局，
            // 直接 scrollTo 会被随后的布局过程重置，投递到下一帧才生效
            scrollView.post(() -> scrollView.scrollTo(0, saved[1]));
        }

        Button btnOpenDetail = findViewById(R.id.btnOpenDetail);
        Button btnOpenDialog = findViewById(R.id.btnOpenDialog);
        Button btnGoHome = findViewById(R.id.btnGoHome);
        Button btnClearLog = findViewById(R.id.btnClearLog);

        btnOpenDetail.setOnClickListener(v -> {
            Intent intent = new Intent(this, DetailActivity.class);
            // 携带题目 id 与来源页：详情页据此渲染对应难题内容与"来自哪里"
            intent.putExtra(DetailActivity.EXTRA_PROBLEM_ID, 3);
            intent.putExtra(DetailActivity.EXTRA_SOURCE_PAGE, "生命周期演示");
            startActivity(intent);
        });

        btnOpenDialog.setOnClickListener(v ->
                startActivity(new Intent(this, DialogStyleActivity.class)));

        btnGoHome.setOnClickListener(v -> {
            // 为什么发 HOME Intent 而不是 finish()：模拟用户真实"回桌面"，
            // 触发本页 onStop（onDestroy 不触发、进程存活）——正是实验 3 要观察的现象
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            startActivity(home);
        });

        btnClearLog.setOnClickListener(v -> {
            LifecycleEventLog.clear();
            // 清完立即刷新 UI：不等下一次 onResume，操作反馈即时可见
            refreshLog();
        });

        // 放在 onCreate 末尾：此时 prefs 已初始化，开关判断才可用
        trace("onCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        trace("onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        trace("onResume");
        refreshLog();
    }

    @Override
    public void onPause() {
        trace("onPause");
        // 记录滚动位置（保证被调用的收尾回调；apply 异步写不卡 onPause）
        // position 传 0：本页是整页 ScrollView 而非条目列表，"第一可见项"恒为 0，偏移即 scrollY
        if (scrollView != null) {
            ScrollStateKeeper.save(this, SCROLL_KEY, 0, scrollView.getScrollY());
        }
        super.onPause();
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

    /**
     * 记录一条生命周期回调到全局日志。
     * 为什么先过 prefs 开关：日志由设置页"生命周期日志"开关控制，关闭即静默
     * （与提示系统"用户关闭即静默"同一思路）；为什么写入静态的
     * LifecycleEventLog：日志需跨页面汇聚展示，不随本页销毁而丢失。
     */
    private void trace(String callback) {
        // 判空属防御性写法：正常时序下 trace 均在 onCreate 初始化完成之后调用
        if (prefs != null && prefs.isLifecycleTraceEnabled()) {
            LifecycleEventLog.record("演示页", callback);
        }
    }

    /** onResume 时刷新：返回本页立即能看到刚才离开期间发生的回调 */
    private void refreshLog() {
        List<String> events = LifecycleEventLog.snapshot();
        if (events.isEmpty()) {
            textLog.setText("（暂无日志，操作后实时刷新）");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String event : events) {
            sb.append(event).append('\n');
        }
        textLog.setText(sb.toString());
        if (textFlowHint != null) {
            textFlowHint.setText("蓝色 = 当前阶段（onResume）。对照日志，验证上方实验结论。");
        }
    }

    // ---------- 下拉菜单 ----------

    @Override
    protected Page page() {
        return Page.LIFECYCLE;
    }

    /** 菜单 id 取 1~4 的小整数：仅在本页菜单内匹配用，不会与 android.R.id 资源 id 冲突 */
    @Override
    protected List<NavDestination> destinations() {
        return Arrays.asList(
                new NavDestination(1, "返回本页", LifecycleDemoActivity.class,
                        "当前就在生命周期演示页，无需重复跳转", CustomToast.Type.WARNING),
                new NavDestination(2, "提示实验室", ToastLabActivity.class,
                        "从【生命周期演示】跳到【提示实验室】—— 试试刚学的提示技巧", CustomToast.Type.INFO),
                new NavDestination(3, "设置", SettingsActivity.class,
                        "从【生命周期演示】跳到【设置】—— 可关闭回调日志", CustomToast.Type.INFO),
                new NavDestination(4, "回到首页", MainActivity.class,
                        "从【生命周期演示】跳到【首页】—— 返回难题清单", CustomToast.Type.SUCCESS)
        );
    }
}
