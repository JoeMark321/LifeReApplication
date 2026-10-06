package com.example.lifereapplication.ui.nav;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lifereapplication.R;
import com.example.lifereapplication.data.prefs.AppPreferences;
import com.example.lifereapplication.util.toast.CustomToast;
import com.example.lifereapplication.util.toast.ToastCenter;
import com.example.lifereapplication.util.toast.ToastDispatcher;
import com.example.lifereapplication.util.toast.ToastQueueHost;

import java.util.List;

/**
 * 带右上角下拉菜单的页面基类。
 *
 * <p>所有页面统一从这里出发生成 Toolbar 溢出菜单：</p>
 * <ul>
 *     <li>菜单项由子类通过 {@link #destinations()} 提供，可自由配置；</li>
 *     <li>点击后先弹出<b>该页面独有</b>的提示信息，再执行 Intent 跳转；</li>
 *     <li>跳转目标为当前页时只提示不跳转，避免重复创建实例；</li>
 *     <li>跳转统一使用淡入淡出动效，全项目观感一致。</li>
 * </ul>
 */
public abstract class BaseMenuActivity extends AppCompatActivity {

    /** 页面唯一标识，用于生成菜单项 id 与拼装提示文案 */
    public enum Page {
        MAIN("首页"),
        DETAIL("详情页"),
        LIFECYCLE("生命周期演示"),
        TOAST_LAB("提示实验室"),
        SETTINGS("设置");

        private final String title;

        Page(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Toast 串行队列宿主：观察队列头，页面不可见时自动暂停渲染（LiveData 语义）
        toastQueueHost = ToastQueueHost.attach(this);
    }

    private ToastQueueHost toastQueueHost;

    @Override
    protected void onDestroy() {
        if (toastQueueHost != null) {
            toastQueueHost.detach();
        }
        super.onDestroy();
    }

    /** 子类返回自身所属页面，用于标题与提示文案 */
    protected abstract Page page();

    /** 子类可覆盖以自定义标题（默认用页面名） */
    protected String toolbarTitle() {
        return null;
    }

    /** 子类返回本页面要展示的菜单项 */
    protected abstract List<NavDestination> destinations();

    /** 统一初始化 Toolbar，子类在 setContentView 之后调用 */
    protected void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar == null) {
            return;
        }
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            String title = toolbarTitle();
            getSupportActionBar().setTitle(title != null ? title : page().getTitle());
            getSupportActionBar().setSubtitle("MVVM 架构");
        }
        applyEdgeToEdgeInsets(toolbar);
    }

    /**
     * 全面屏（刘海屏/手势条）跨版本适配 —— 自动实现，一次接入全页面生效。
     *
     * <p>系统行为差异：</p>
     * <ul>
     *     <li><b>Android 15+（API 35+）</b>：强制 edge-to-edge，状态栏透明、
     *     内容直接顶到屏幕最上方 —— 必须手动用 insets 腾出空间，否则 Toolbar
     *     会被状态栏压住（这正是之前"顶上去"的根因）；</li>
     *     <li><b>Android 11~14</b>：系统默认 decorFits（内容从状态栏下开始），
     *     由主题 statusBarColor 着色，insets 通常为 0，本监听同样兼容；</li>
     *     <li><b>刘海屏横屏</b>：cutout 的 top 通常为 0（缺口在左右两侧），
     *     纵向内容不受影响。</li>
     * </ul>
     *
     * <p>统一策略：Toolbar 顶部内边距 = 状态栏 + 刘海高度（品牌蓝自然延伸
     * 进状态栏，标题不被遮挡）；内容底部内边距 = 手势条高度（列表不被
     * 手势条遮住）。监听挂在 android.R.id.content 上，所有页面自动继承。</p>
     */
    private void applyEdgeToEdgeInsets(Toolbar toolbar) {
        View decorContent = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(decorContent, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.statusBars()
                            | WindowInsetsCompat.Type.displayCutout()
                            | WindowInsetsCompat.Type.navigationBars());
            // 顶部：状态栏 + 刘海 → 让 Toolbar 自己垫高，蓝色延伸到状态栏
            toolbar.setPadding(toolbar.getPaddingLeft(), bars.top,
                    toolbar.getPaddingRight(), toolbar.getPaddingBottom());
            // 底部：手势条 → 内容区整体让位，滚动条与列表尾项不被遮挡
            v.setPadding(v.getPaddingLeft(), 0, v.getPaddingRight(), bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        for (NavDestination destination : destinations()) {
            menu.add(Menu.NONE, destination.getId(), Menu.NONE, destination.getLabel());
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        for (NavDestination destination : destinations()) {
            if (item.getItemId() != destination.getId()) {
                continue;
            }
            // 每个页面、每个菜单项都有独立提示文案（走节流+串行队列出口）
            ToastDispatcher.dispatch(this, destination.getToastMessage(),
                    destination.getToastType(), false);

            Class<?> target = destination.getTarget();
            if (target != null && target != getClass()) {
                // 记住上次访问的页面：返回本页时可提示"上次离开去哪了"
                new AppPreferences(this).setLastPage(target.getSimpleName());
                startActivity(new Intent(this, target));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** 统一的 Toast 出口，业务代码不再各自持有提示实现 */
    protected void tip(String message) {
        ToastCenter.show(this, message, CustomToast.Type.INFO);
    }

    protected void tip(String message, CustomToast.Type type) {
        ToastCenter.show(this, message, type);
    }
}
