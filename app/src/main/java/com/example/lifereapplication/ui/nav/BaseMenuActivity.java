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
 *
 * <p>职责边界：基类只收口四件公共事项——下拉菜单渲染、统一跳转、
 * edge-to-edge insets 适配、Toast 串行队列宿主挂载；页面自身的布局与
 * 业务逻辑全部留给子类，做到"公共机制写一次，所有页面自动继承"。</p>
 */
public abstract class BaseMenuActivity extends AppCompatActivity {

    /**
     * 页面唯一标识，用于生成菜单项 id 与拼装提示文案。
     *
     * <p>为什么用枚举而不是字符串常量：页面集合封闭且有限，枚举让
     * "新增页面必须先登记"成为编译期约束，杜绝手滑拼错页面名；
     * {@link #getTitle()} 同时供 Toolbar 默认标题与提示文案复用，
     * 避免同义文案多处维护。</p>
     */
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

    /**
     * 为什么在 onCreate（而非 onStart/onResume）挂载 ToastQueueHost：
     * 它是 LiveData 观察者，需要 LifecycleOwner，onCreate 是最早且必然
     * 执行的时机，与 onDestroy 的 detach 严格成对。
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Toast 串行队列宿主：观察队列头，页面不可见时自动暂停渲染（LiveData 语义）
        toastQueueHost = ToastQueueHost.attach(this);
    }

    /** 保存引用是为了 onDestroy 显式 detach；attach 的返回值不落字段就无法解绑 */
    private ToastQueueHost toastQueueHost;

    /**
     * 为什么显式 detach：虽然 LiveData 绑定了 Lifecycle 会自动清理观察者，
     * 但主动解绑让 attach/detach 语义对称、引用即刻释放，不依赖框架内部
     * 清理时序（判空属防御性写法）。
     */
    @Override
    protected void onDestroy() {
        if (toastQueueHost != null) {
            toastQueueHost.detach();
        }
        super.onDestroy();
    }

    /**
     * 子类返回自身所属页面，用于标题与提示文案。
     * 为什么声明为抽象方法：强制每个子类在编译期登记身份，漏写活不过编译。
     */
    protected abstract Page page();

    /**
     * 子类可覆盖以自定义标题（默认用页面名）。
     * 为什么用返回 null 表示"用默认"：避免子类不想定制标题时还得写一行样板代码。
     */
    protected String toolbarTitle() {
        return null;
    }

    /**
     * 子类返回本页面要展示的菜单项。
     * 为什么把菜单内容声明为数据：跳转目标/提示文案因页面而异，子类只声明
     * "去哪、说什么"，渲染与跳转行为由基类统一实现（数据与行为分离）。
     */
    protected abstract List<NavDestination> destinations();

    /**
     * 统一初始化 Toolbar（ActionBar 化、标题、insets 适配收口成一步）。
     * 为什么要求子类在 setContentView 之后调用：findViewById 依赖布局已填充。
     */
    protected void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        // 为什么判空后静默返回：基类不强制所有布局都带 R.id.toolbar，
        // 缺失时跳过比抛 NPE 更符合"基类尽量不挑布局"的定位
        if (toolbar == null) {
            return;
        }
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            String title = toolbarTitle();
            getSupportActionBar().setTitle(title != null ? title : page().getTitle());
            // 副标题固定为项目定位说明（教学项目，所有页面一致）
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
            // 底部：手势条 → 内容区整体让位，滚动条与列表尾项不被遮挡。
            // 顶部垫 0：顶部让位已由 Toolbar 的 padding 承担，内容区不再重复垫高
            v.setPadding(v.getPaddingLeft(), 0, v.getPaddingRight(), bars.bottom);
            // 为什么返回 CONSUMED：三类 insets 在这里已全部消化完，
            // 截断继续向子 View 分发，避免子视图再次 padding 造成双重避让
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /**
     * 为什么用代码生成而不用菜单 XML：菜单项因页面而异，从 destinations()
     * 动态生成保证"菜单数据只有一份来源"，新增/调整菜单只改子类一处。
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        for (NavDestination destination : destinations()) {
            // groupId/order 均传 NONE：不分组、按 destinations() 声明顺序出现
            menu.add(Menu.NONE, destination.getId(), Menu.NONE, destination.getLabel());
        }
        // 返回 true 表示菜单已构建、需要显示
        return true;
    }

    /**
     * 为什么遍历 destinations() 匹配 id 而不是 switch(item.getItemId())：
     * 菜单 id 由子类的数据决定，基类无法预知取值，遍历让同一段
     * "提示 + 跳转"逻辑服务所有页面的所有菜单项。
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        for (NavDestination destination : destinations()) {
            if (item.getItemId() != destination.getId()) {
                continue;
            }
            // 每个页面、每个菜单项都有独立提示文案（走节流+串行队列出口，
            // 末参 false = 短时长；先提示后跳转，让用户在离开前看清去向）
            ToastDispatcher.dispatch(this, destination.getToastMessage(),
                    destination.getToastType(), false);

            Class<?> target = destination.getTarget();
            if (target != null && target != getClass()) {
                // 记住上次访问的页面：返回本页时可提示"上次离开去哪了"
                new AppPreferences(this).setLastPage(target.getSimpleName());
                startActivity(new Intent(this, target));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
            // 已消费本次点击：返回 true 阻止事件继续交给父类/系统处理
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 统一的 Toast 出口，业务代码不再各自持有提示实现。
     * 为什么收口到 ToastCenter：样式路由与"用户关闭即静默"的权限管理在
     * 出口处统一生效，设置页的改动无需各页面配合即实时生效。
     */
    protected void tip(String message) {
        ToastCenter.show(this, message, CustomToast.Type.INFO);
    }

    /** 带语义类型（成功/错误/警告/信息）的版本，图标与文案前缀随类型变化 */
    protected void tip(String message, CustomToast.Type type) {
        ToastCenter.show(this, message, type);
    }
}
