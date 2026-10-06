package com.example.lifereapplication.ui.list;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.lifereapplication.R;
import com.example.lifereapplication.data.prefs.AppPreferences;
import com.example.lifereapplication.ui.chapter.ChapterActivity;
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
 * 列表演演三 · 自定义底部导航。
 *
 * <p>结构复用演示一（三 tab 列表 + 精确恢复 MODE_EXACT + tab 记忆），
 * 差异在<b>底部 tab 行全自定义</b>（不依赖 BottomNavigationView）：</p>
 * <ul>
 *     <li>悬浮白底圆角条（12dp 边距 + 20dp 圆角 + 描边 + elevation）；</li>
 *     <li>三个纯文字 tab（无图标干扰）：选中 = 品牌蓝胶囊 + 白字，未选 = 透明 + 灰字；</li>
 *     <li>状态由 {@code android:selected} 驱动，背景/文字全部走 selector。</li>
 * </ul>
 *
 * <p>状态隔离：listId 前缀 {@code third.*}、tab 记忆独立 key {@code list_tab.third}。</p>
 */
public class ListDemoThirdActivity extends BaseMenuActivity {

    private static final String PREF_KEY = "third";
    private static final String TAG_LINEAR = "third.linear";
    private static final String TAG_GRID2 = "third.grid2";
    private static final String TAG_GRID3 = "third.grid3";

    private AppPreferences prefs;
    private TextView tabLinear;
    private TextView tabGrid;
    private TextView tabGrid3;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_demo_third);
        setupToolbar();
        prefs = new AppPreferences(this);

        tabLinear = findViewById(R.id.tabLinear);
        tabGrid = findViewById(R.id.tabGrid);
        tabGrid3 = findViewById(R.id.tabGrid3);

        tabLinear.setOnClickListener(v -> selectTab(TAG_LINEAR));
        tabGrid.setOnClickListener(v -> selectTab(TAG_GRID2));
        tabGrid3.setOnClickListener(v -> selectTab(TAG_GRID3));

        // 恢复上次选中的 tab（独立 key，与演示一互不影响）
        String lastTag = prefs.getListTab(PREF_KEY);
        selectTab(TAG_GRID2.equals(lastTag) ? TAG_GRID2
                : TAG_GRID3.equals(lastTag) ? TAG_GRID3 : TAG_LINEAR);
    }

    /** 切换 tab：selected 状态驱动 selector（胶囊背景+文字色），Fragment 走 hide/show */
    private void selectTab(String tag) {
        prefs.setListTab(PREF_KEY, tag);
        tabLinear.setSelected(TAG_LINEAR.equals(tag));
        tabGrid.setSelected(TAG_GRID2.equals(tag));
        tabGrid3.setSelected(TAG_GRID3.equals(tag));
        showPrimary(tag);
    }

    /** 主栏切换：hide/show 保视图，listId 前缀 third.* 状态独立 */
    private void showPrimary(String tag) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment existing = fm.findFragmentByTag(tag);
        if (existing != null) {
            hideAll(fm);
            existing.getView().setVisibility(android.view.View.VISIBLE);
            return;
        }
        hideAll(fm);
        fm.beginTransaction()
                .add(R.id.containerList, ListFragment.newInstance(tag, gridSpan(tag)), tag)
                .commit();
    }

    private void hideAll(FragmentManager fm) {
        for (Fragment f : fm.getFragments()) {
            if (f.getView() != null) {
                f.getView().setVisibility(android.view.View.GONE);
            }
        }
    }

    /** span：线性=1；网格=0 自适应（最小列宽 170dp） */
    private int gridSpan(String tag) {
        return TAG_LINEAR.equals(tag) ? 1 : 0;
    }

    // ---------- 下拉菜单 ----------

    @Override
    protected Page page() {
        return Page.MAIN;
    }

    @Override
    protected String toolbarTitle() {
        return "列表演演三 · 自定义导航";
    }

    @Override
    protected List<NavDestination> destinations() {
        return Arrays.asList(
                new NavDestination(1, "返回本页", ListDemoThirdActivity.class,
                        "当前就在列表演演三，无需重复跳转", CustomToast.Type.WARNING),
                new NavDestination(2, "列表演演一", ListDemoActivity.class,
                        "从【列表演演三】跳到【列表演演一】—— 对比系统默认导航", CustomToast.Type.INFO),
                new NavDestination(3, "列表演演二·复述", ListDemoSecondActivity.class,
                        "从【列表演演三】跳到【列表演演二】—— 对比条目头对齐", CustomToast.Type.INFO),
                new NavDestination(4, "章节学习", ChapterActivity.class,
                        "从【列表演演三】跳到【章节学习】—— 页级进度记忆", CustomToast.Type.INFO),
                new NavDestination(5, "生命周期演示", LifecycleDemoActivity.class,
                        "从【列表演演三】跳到【生命周期演示】—— 看回调时序", CustomToast.Type.INFO),
                new NavDestination(6, "提示实验室", ToastLabActivity.class,
                        "从【列表演演三】跳到【提示实验室】—— 对比三种 Toast", CustomToast.Type.INFO),
                new NavDestination(7, "设置", SettingsActivity.class,
                        "从【列表演演三】跳到【设置】—— 管理提示权限", CustomToast.Type.INFO),
                new NavDestination(8, "回到首页", MainActivity.class,
                        "从【列表演演三】跳到【首页】—— 返回难题清单", CustomToast.Type.SUCCESS)
        );
    }
}
