package com.example.lifereapplication.ui.list;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

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
 * 列表演演三 · 自定义底部导航 + 横滑切页。
 *
 * <p>内容区为 {@link ViewPager2} 承载三个列表 Fragment：</p>
 * <ul>
 *     <li><b>左右手势滑动</b>即可在线性 ⇄ 双列 ⇄ 三列间切换；</li>
 *     <li>底部自定义 tab 行与页面<b>双向联动</b>：滑动时 tab 跟随点亮，
 *     点 tab 时页面平滑翻过去；</li>
 *     <li>tab 行全自定义（悬浮白底圆角条 + 纯文字 tab，
 *     选中 = 品牌蓝胶囊 + 白字，未选 = 透明 + 灰字）。</li>
 * </ul>
 *
 * <p>状态隔离：listId 前缀 {@code third.*}、tab 记忆独立 key {@code list_tab.third}；
 * 页内滚动位置由 ListFragment 自行保存/恢复（MODE_EXACT 精确恢复）。</p>
 */
public class ListDemoThirdActivity extends BaseMenuActivity {

    private static final String PREF_KEY = "third";
    private static final String[] LIST_IDS = {"third.linear", "third.grid2", "third.grid3"};
    private static final String[] TAGS = {"third.linear", "third.grid2", "third.grid3"};
    private static final int[] SPANS = {1, 0, 0}; // 0 = 网格列数按屏宽自适应

    private AppPreferences prefs;
    private ViewPager2 viewPager;
    private TextView tabLinear;
    private TextView tabGrid;
    private TextView tabGrid3;

    /** 三页适配器：复用 ListFragment，listId 各自独立（状态互不串扰） */
    private class ThirdPagerAdapter extends FragmentStateAdapter {

        ThirdPagerAdapter(@NonNull FragmentActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return ListFragment.newInstance(LIST_IDS[position], SPANS[position]);
        }

        @Override
        public int getItemCount() {
            return LIST_IDS.length;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean containsItem(long itemId) {
            return itemId >= 0 && itemId < LIST_IDS.length;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_demo_third);
        setupToolbar();
        prefs = new AppPreferences(this);

        tabLinear = findViewById(R.id.tabLinear);
        tabGrid = findViewById(R.id.tabGrid);
        tabGrid3 = findViewById(R.id.tabGrid3);

        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new ThirdPagerAdapter(this));

        // 页面变化 → tab 点亮 + 持久化进度（手势滑动与点 tab 都会走这里）
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                prefs.setListTab(PREF_KEY, TAGS[position]);
                updateTabStates(position);
            }
        });

        // tab 点击 → 页面平滑翻过去（双向联动的另一半）
        tabLinear.setOnClickListener(v -> viewPager.setCurrentItem(0, true));
        tabGrid.setOnClickListener(v -> viewPager.setCurrentItem(1, true));
        tabGrid3.setOnClickListener(v -> viewPager.setCurrentItem(2, true));

        // 恢复上次停留的页（独立 key，与演示一互不影响）
        String lastTag = prefs.getListTab(PREF_KEY);
        int lastIndex = TAGS[1].equals(lastTag) ? 1 : TAGS[2].equals(lastTag) ? 2 : 0;
        viewPager.setCurrentItem(lastIndex, false);
    }

    /** tab 点亮状态：selected 驱动 selector（胶囊背景 + 文字色） */
    private void updateTabStates(int position) {
        tabLinear.setSelected(position == 0);
        tabGrid.setSelected(position == 1);
        tabGrid3.setSelected(position == 2);
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
