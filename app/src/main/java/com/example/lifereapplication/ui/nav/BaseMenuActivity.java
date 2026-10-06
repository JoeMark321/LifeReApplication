package com.example.lifereapplication.ui.nav;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.lifereapplication.R;
import com.example.lifereapplication.util.toast.CustomToast;
import com.example.lifereapplication.util.toast.ToastCenter;

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
            // 每个页面、每个菜单项都有独立提示文案
            ToastCenter.show(this, destination.getToastMessage(), destination.getToastType());

            Class<?> target = destination.getTarget();
            if (target != null && target != getClass()) {
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
