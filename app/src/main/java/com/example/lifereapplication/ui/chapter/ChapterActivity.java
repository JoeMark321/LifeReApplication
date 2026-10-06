package com.example.lifereapplication.ui.chapter;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.example.lifereapplication.R;
import com.example.lifereapplication.data.prefs.AppPreferences;
import com.example.lifereapplication.ui.lifecycle.LifecycleDemoActivity;
import com.example.lifereapplication.ui.list.ListDemoActivity;
import com.example.lifereapplication.ui.main.MainActivity;
import com.example.lifereapplication.ui.nav.BaseMenuActivity;
import com.example.lifereapplication.ui.nav.NavDestination;
import com.example.lifereapplication.ui.settings.SettingsActivity;
import com.example.lifereapplication.ui.toast.ToastLabActivity;
import com.example.lifereapplication.util.toast.CustomToast;
import com.example.lifereapplication.util.toast.ToastDispatcher;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Arrays;
import java.util.List;

/**
 * ViewPager2 + Fragment 章节联动演示页。
 *
 * <p>联动关系：{@code ViewPager2 ↔ ChapterPagerAdapter(FragmentStateAdapter) ↔ ListFragment}，
 * TabLayout 通过 {@link TabLayoutMediator} 与 ViewPager2 双向同步（点 tab 翻页 /
 * 翻页选中 tab），全部由一行 Mediator 代码完成。</p>
 */
public class ChapterActivity extends BaseMenuActivity {

    private ViewPager2 viewPager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter);
        setupToolbar();

        viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        ChapterPagerAdapter pagerAdapter = new ChapterPagerAdapter(this);
        // 章节进度记忆：翻页即记（onPageSelected 持久化），重进时无动画直接定位。
        // 页内滚动位置由 ListFragment + ScrollStateKeeper 自行恢复（listId=chapter.N）。
        prefs = new AppPreferences(this);

        viewPager.setAdapter(pagerAdapter);

        // 恢复上次章节：在 TabLayoutMediator.attach 之前 setCurrentItem，
        // 保证 tab 与页面一次性同步到位（避免先显示第 0 章再跳的闪动）。
        int lastChapter = prefs.getLastChapter();
        if (lastChapter > 0 && lastChapter < ChapterPagerAdapter.CHAPTERS.size()) {
            viewPager.setCurrentItem(lastChapter, false);
            ToastDispatcher.dispatch(this,
                    "已回到上次学习的位置：第 " + (lastChapter + 1) + " 章",
                    CustomToast.Type.SUCCESS, false);
        }

        // Mediator 完成 Tab↔Page 双向联动（附自动清除旧 tab 的策略）
        new TabLayoutMediator(tabLayout, viewPager, true,
                (tab, position) -> tab.setText(pagerAdapter.pageTitle(position))
        ).attach();

        // 翻页时给一句独特提示（走节流队列，连翻不会刷屏），并持久化进度
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                prefs.setLastChapter(position);
                ToastDispatcher.dispatch(ChapterActivity.this,
                        "第 " + (position + 1) + " 章：" + ChapterPagerAdapter.CHAPTERS.get(position).getSubtitle(),
                        CustomToast.Type.INFO, false);
            }
        });
    }

    private AppPreferences prefs;

    /** 禁掉用户横向滑动时与列表纵向手势打架： ViewPager2 内部已处理，无需干预 */

    // ---------- 下拉菜单 ----------

    @Override
    protected Page page() {
        return Page.MAIN;
    }

    @Override
    protected String toolbarTitle() {
        return "章节学习 · ViewPager2";
    }

    @Override
    protected List<NavDestination> destinations() {
        return Arrays.asList(
                new NavDestination(1, "返回本页", ChapterActivity.class,
                        "当前就在章节学习页，无需重复跳转", CustomToast.Type.WARNING),
                new NavDestination(2, "列表演示", ListDemoActivity.class,
                        "从【章节学习】跳到【列表演示】—— 单页对比三种布局", CustomToast.Type.INFO),
                new NavDestination(3, "生命周期演示", LifecycleDemoActivity.class,
                        "从【章节学习】跳到【生命周期演示】—— 看回调时序", CustomToast.Type.INFO),
                new NavDestination(4, "提示实验室", ToastLabActivity.class,
                        "从【章节学习】跳到【提示实验室】—— 对比三种 Toast", CustomToast.Type.INFO),
                new NavDestination(5, "设置", SettingsActivity.class,
                        "从【章节学习】跳到【设置】—— 管理提示权限", CustomToast.Type.INFO),
                new NavDestination(6, "回到首页", MainActivity.class,
                        "从【章节学习】跳到【首页】—— 返回难题清单", CustomToast.Type.SUCCESS)
        );
    }
}
