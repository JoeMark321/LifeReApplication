package com.example.lifereapplication.ui.list;

import android.os.Bundle;
import android.os.SystemClock;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.lifereapplication.R;
import com.example.lifereapplication.ui.chapter.ChapterActivity;
import com.example.lifereapplication.ui.lifecycle.LifecycleDemoActivity;
import com.example.lifereapplication.ui.main.MainActivity;
import com.example.lifereapplication.ui.nav.BaseMenuActivity;
import com.example.lifereapplication.ui.nav.NavDestination;
import com.example.lifereapplication.ui.settings.SettingsActivity;
import com.example.lifereapplication.ui.toast.ToastLabActivity;
import com.example.lifereapplication.util.ScrollStateKeeper;
import com.example.lifereapplication.util.toast.CustomToast;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 列表演示二 · 内容复述页。
 *
 * <p>与"列表演示"一期的差异：一期只恢复滚动位置；二期进入时还会
 * <b>复述</b>记忆内容——上次看到第几条、那条的文本是什么、恢复耗时多少，
 * 把状态记忆从"体感"变成"可验证的数据"。</p>
 *
 * <p>与"章节学习"的对比（文档 4.3 节详述）：</p>
 * <ul>
 *     <li>章节学习 = <b>页级</b>进度（ViewPager2 position）+ 页内像素级位置；</li>
 *     <li>列表演示一 = <b>tab 级</b>布局选择记忆；</li>
 *     <li>列表演演二 = 同一列表的<b>条目级</b>记忆 + 内容复述展示。</li>
 * </ul>
 */
public class ListDemoSecondActivity extends BaseMenuActivity {

    private static final String LIST_ID = "demo.second";
    private static final int ROW_COUNT = 100;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_demo_second);
        setupToolbar();

        TextView textRecall = findViewById(R.id.textRecall);
        TextView textTiming = findViewById(R.id.textTiming);

        // 计时：restore 全程（含可能的磁盘回读）必须远低于 200ms 阈值
        long t0 = SystemClock.elapsedRealtime();
        int[] state = ScrollStateKeeper.restore(this, LIST_ID);
        int[] safe = ScrollStateKeeper.clamp(state, ROW_COUNT, getResources()
                .getDisplayMetrics().heightPixels);
        long cost = SystemClock.elapsedRealtime() - t0;

        if (safe == null) {
            textRecall.setText("首次进入：暂无历史进度。\n先在列表里滑动到某个位置，"
                    + "退出（返回键）后再进来，这里会复述你上次看到的内容。");
            textTiming.setText("恢复耗时：—");
        } else {
            String itemText = String.format(Locale.CHINA,
                    "条目 #%03d —— 滑动后退出再进入，会精确回到当前位置", safe[0] + 1);
            textRecall.setText("上次看到：第 " + (safe[0] + 1) + " 条（偏移 " + safe[1]
                    + "px，条目头对齐策略）\n"
                    + "内容复述：" + itemText);
            textTiming.setText(String.format(Locale.CHINA,
                    "恢复耗时：%d ms（阈值 200ms）", cost));
        }

        // 复用同一长列表 Fragment：listId 独立 + ITEM_HEAD 模式（偏移清零，
        // 恢复时条目完整显示）——与演示一的 MODE_EXACT 像素级恢复形成对照
        Fragment listFragment = ListFragment.newInstance(LIST_ID, 1, ListFragment.MODE_ITEM_HEAD);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerList, listFragment)
                .commit();
    }

    // ---------- 下拉菜单 ----------

    @Override
    protected Page page() {
        return Page.MAIN;
    }

    @Override
    protected String toolbarTitle() {
        return "列表演示二 · 内容复述";
    }

    @Override
    protected List<NavDestination> destinations() {
        return Arrays.asList(
                new NavDestination(1, "返回本页", ListDemoSecondActivity.class,
                        "当前就在列表演示二，无需重复跳转", CustomToast.Type.WARNING),
                new NavDestination(2, "章节学习", ChapterActivity.class,
                        "从【列表演示二】跳到【章节学习】—— 页级进度记忆对比", CustomToast.Type.INFO),
                new NavDestination(3, "生命周期演示", LifecycleDemoActivity.class,
                        "从【列表演示二】跳到【生命周期演示】—— 看回调时序", CustomToast.Type.INFO),
                new NavDestination(4, "提示实验室", ToastLabActivity.class,
                        "从【列表演示二】跳到【提示实验室】—— 对比三种 Toast", CustomToast.Type.INFO),
                new NavDestination(5, "设置", SettingsActivity.class,
                        "从【列表演示二】跳到【设置】—— 管理提示权限", CustomToast.Type.INFO),
                new NavDestination(6, "回到首页", MainActivity.class,
                        "从【列表演示二】跳到【首页】—— 返回难题清单", CustomToast.Type.SUCCESS)
        );
    }
}
