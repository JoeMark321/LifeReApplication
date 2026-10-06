package com.example.lifereapplication.ui.list;

import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Arrays;
import java.util.List;

/**
 * 列表演示容器页：三种布局（线性 / 双列 / 三列）+ 横竖屏适配。
 *
 * <p><b>适配策略：</b></p>
 * <ul>
 *     <li>竖屏：单容器 + 底部导航切换；span = 1 / 2 / 3；</li>
 *     <li>横屏（layout-land）：双栏并排（线性 + 双列），宽屏利用率为 100%；
 *     切到三列时副栏同步换成三列网格，方便对比状态保持效果；</li>
 *     <li>大屏（平板 12.9"）：同样的 span 逻辑在更宽 viewport 下自然展示
 *     更多列内容，无需额外断点。</li>
 * </ul>
 *
 * <p><b>Fragment 生命周期要点：</b>show/hide 切换而非 replace ——
 * 列表视图不被销毁，状态保持只依赖 onPause 保存的精确位置；
 * 每个 Fragment 的 listId 独立，互不串扰。</p>
 */
public class ListDemoActivity extends BaseMenuActivity {

    private static final String TAG_LINEAR = "linear";
    private static final String TAG_GRID2 = "grid2";
    private static final String TAG_GRID3 = "grid3";

    private boolean isLandscape;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_demo);
        setupToolbar();
        isLandscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            String tag;
            if (id == R.id.nav_linear) {
                tag = TAG_LINEAR;
            } else if (id == R.id.nav_grid) {
                tag = TAG_GRID2;
            } else if (id == R.id.nav_grid3) {
                tag = TAG_GRID3;
            } else {
                return false;
            }
            // tab 记忆：每次选择即持久化，重进自动回到上次布局
            prefs.setListDemoTab(tag);
            showPrimary(tag);
            showSecondaryIfLand(tag.equals(TAG_LINEAR) ? TAG_GRID2 : TAG_LINEAR);
            return true;
        });

        prefs = new AppPreferences(this);
        // 恢复上次选择的 tab（与"章节学习"的页级进度形成对比：tab 级 vs 页+像素级）
        String lastTag = prefs.getListDemoTab();
        int restoreId;
        if (TAG_GRID2.equals(lastTag)) {
            restoreId = R.id.nav_grid;
        } else if (TAG_GRID3.equals(lastTag)) {
            restoreId = R.id.nav_grid3;
        } else {
            restoreId = R.id.nav_linear;
        }
        bottomNav.setSelectedItemId(restoreId); // 触发 listener 完成首次展示与恢复
    }

    private AppPreferences prefs;

    /** 主栏切换（竖屏即唯一栏）：span 随屏幕方向自适应 */
    private void showPrimary(String tag) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment existing = fm.findFragmentByTag(tag);
        if (existing != null) {
            // hide 其他，show 目标：视图不重建，滚动状态天然保留
            hideAll(fm);
            existing.getView().setVisibility(android.view.View.VISIBLE);
            return;
        }
        hideAll(fm);
        fm.beginTransaction()
                .add(R.id.containerList, ListFragment.newInstance(tag, spanFor(tag)), tag)
                .commit();
    }

    /** 横屏副栏：并排展示第二种布局 */
    private void showSecondaryIfLand(String tag) {
        if (!isLandscape) {
            return;
        }
        FragmentManager fm = getSupportFragmentManager();
        Fragment existing = fm.findFragmentByTag(tag + ".secondary");
        if (existing != null) {
            existing.getView().setVisibility(android.view.View.VISIBLE);
            return;
        }
        fm.beginTransaction()
                .add(R.id.containerSecondary,
                        ListFragment.newInstance(tag + ".secondary", secondarySpanFor(tag)),
                        tag + ".secondary")
                .commit();
    }

    private void hideAll(FragmentManager fm) {
        for (Fragment f : fm.getFragments()) {
            if (f.getView() != null) {
                f.getView().setVisibility(android.view.View.GONE);
            }
        }
    }

    /** span 自适应：竖屏 1/2/3，横屏 2/2/3（宽屏网格收益更大） */
    private int spanFor(String tag) {
        boolean land = isLandscape;
        switch (tag) {
            case TAG_LINEAR:
                return land ? 2 : 1;
            case TAG_GRID2:
                return 2;
            default:
                return 3;
        }
    }

    private int secondarySpanFor(String primaryTag) {
        return TAG_LINEAR.equals(primaryTag) ? 2 : 1;
    }

    // ---------- 下拉菜单 ----------

    @Override
    protected Page page() {
        return Page.LIFECYCLE; // 复用枚举标题语义：演示类页面
    }

    @Override
    protected String toolbarTitle() {
        return "列表演示 · 状态保持";
    }

    @Override
    protected List<NavDestination> destinations() {
        return Arrays.asList(
                new NavDestination(1, "返回本页", ListDemoActivity.class,
                        "当前就在列表演示页，无需重复跳转", CustomToast.Type.WARNING),
                new NavDestination(2, "列表演演二·复述", ListDemoSecondActivity.class,
                        "从【列表演演】跳到【列表演演二】—— 体验内容复述对比", CustomToast.Type.INFO),
                new NavDestination(3, "章节学习", ChapterActivity.class,
                        "从【列表演演】跳到【章节学习】—— 页级进度记忆对比", CustomToast.Type.INFO),
                new NavDestination(4, "生命周期演示", LifecycleDemoActivity.class,
                        "从【列表演演】跳到【生命周期演示】—— 看回调时序", CustomToast.Type.INFO),
                new NavDestination(5, "提示实验室", ToastLabActivity.class,
                        "从【列表演演】跳到【提示实验室】—— 对比三种 Toast", CustomToast.Type.INFO),
                new NavDestination(6, "设置", SettingsActivity.class,
                        "从【列表演演】跳到【设置】—— 管理提示权限", CustomToast.Type.INFO),
                new NavDestination(7, "回到首页", MainActivity.class,
                        "从【列表演演】跳到【首页】—— 返回难题清单", CustomToast.Type.SUCCESS)
        );
    }
}
