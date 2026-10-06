package com.example.lifereapplication.ui.toast;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.lifereapplication.R;
import com.example.lifereapplication.ui.lifecycle.LifecycleDemoActivity;
import com.example.lifereapplication.ui.main.MainActivity;
import com.example.lifereapplication.ui.nav.BaseMenuActivity;
import com.example.lifereapplication.ui.nav.NavDestination;
import com.example.lifereapplication.ui.settings.SettingsActivity;
import com.example.lifereapplication.util.DialogHelper;
import com.example.lifereapplication.util.NotificationHelper;
import com.example.lifereapplication.util.toast.CustomToast;
import com.example.lifereapplication.util.toast.ModernToast;
import com.example.lifereapplication.util.toast.NativeToast;
import com.example.lifereapplication.util.toast.ToastCenter;

import java.util.Arrays;
import java.util.List;

/**
 * 提示实验室：三种 Toast + 静默/对话框/通知的动手对比页。
 *
 * <p>每个按钮对应文档「提示系统」一节的一种调用姿势，全部走统一出口
 * {@link ToastCenter} 时会自动应用用户在设置页里的开关。</p>
 *
 * <p>职责边界：纯演示页，不持有业务状态；除通知按钮组有权限回调外，
 * 其余按钮都是一次性的独立演示，互不依赖。</p>
 */
public class ToastLabActivity extends BaseMenuActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toast_lab);
        setupToolbar();

        // 前三组（原生/自定义/现代化）故意直连各自实现——演示页要展示
        // "原始调用姿势"；对照组在最后的 btnCenter：同一条消息走统一出口，
        // 直观对比出样式路由与用户开关的效果差异。编号对应布局中的按钮分区。

        // 1. 原生
        Button btnNative = findViewById(R.id.btnNative);
        btnNative.setOnClickListener(v ->
                NativeToast.show(this, "原生 Toast：Thread 安全、样式由系统决定"));

        // 2. 自定义
        findViewById(R.id.btnCustomSuccess).setOnClickListener(v ->
                CustomToast.show(this, "操作成功：数据已保存", CustomToast.Type.SUCCESS));
        findViewById(R.id.btnCustomError).setOnClickListener(v ->
                CustomToast.show(this, "操作失败：网络不可用", CustomToast.Type.ERROR));
        findViewById(R.id.btnCustomWarning).setOnClickListener(v ->
                CustomToast.show(this, "存储空间不足，请及时清理", CustomToast.Type.WARNING));
        findViewById(R.id.btnCustomInfo).setOnClickListener(v ->
                CustomToast.show(this, "小提示：右上角菜单可切换页面", CustomToast.Type.INFO));

        // 3. 现代化
        findViewById(R.id.btnModern).setOnClickListener(v ->
                ModernToast.show(this, "Snackbar：贴在页面底部，可上滑关闭"));
        findViewById(R.id.btnModernAction).setOnClickListener(v ->
                ModernToast.showWithAction(this, "已删除 1 条记录", "撤销", item ->
                        ModernToast.show(this, "已恢复刚才删除的记录")));

        // 4. 静默 / 组合
        findViewById(R.id.btnSilent).setOnClickListener(v ->
                ToastCenter.showSilent("ToastLab", "这是一条静默提示，去 Logcat 过滤 ToastLab 查看"));

        findViewById(R.id.btnDialog).setOnClickListener(v ->
                DialogHelper.showConfirm(this, "确认操作", "这是一条模态提示：必须点确定或取消才能继续。确定要继续吗？",
                        new DialogHelper.OnDialogActionListener() {
                            @Override
                            public void onPositive() {
                                CustomToast.show(ToastLabActivity.this, "你点了确定", CustomToast.Type.SUCCESS);
                            }

                            @Override
                            public void onNegative() {
                                CustomToast.show(ToastLabActivity.this, "你点了取消", CustomToast.Type.INFO);
                            }

                            @Override
                            public void onCancel() {
                                CustomToast.show(ToastLabActivity.this, "你按返回键取消了", CustomToast.Type.WARNING);
                            }
                        }));

        findViewById(R.id.btnNotify).setOnClickListener(v -> {
            // 先探测再请求：已授权/已开启时直接发，避免每次点击都重复弹系统授权框
            if (NotificationHelper.canNotify(this)) {
                NotificationHelper.showNotification(this, "生命周期Re",
                        "这是一条后台提示：点我回到首页");
            } else {
                // Android 13+ 通知是运行时权限：首次点击先请求，授予后自动补发。
                // 1001 为本页自定义请求码（任意非 0 唯一值），回调里据此识别本次请求
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        });

        findViewById(R.id.btnCenter).setOnClickListener(v ->
                ToastCenter.show(this, "本条经 ToastCenter 出口，样式=设置页的偏好", CustomToast.Type.INFO));
    }

    /** 通知权限授予后自动补发一条，形成"请求→授权→收到"的闭环 */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 双重过滤：只认本页发起的 1001 请求；grantResults 判空防请求被
        // 取消/中断时返回空数组的边界情况
        if (requestCode == 1001 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // 为什么要手动补发：授权前那次请求已被系统拦截丢弃，系统不会
            // 自动重发，这里补一次才形成"请求→授权→收到"的完整闭环
            NotificationHelper.showNotification(this, "生命周期Re",
                    "通知权限已授予：这是一条后台提示，点我回到首页");
        } else {
            // 拒绝不是静默失败：明确告知后果，用户才知道去哪里重新开启
            ToastCenter.show(this, "通知权限被拒绝：后台提示将不可用", CustomToast.Type.WARNING);
        }
    }

    // ---------- 下拉菜单 ----------

    @Override
    protected Page page() {
        return Page.TOAST_LAB;
    }

    /** 菜单 id 取 1~4 的小整数：仅在本页菜单内匹配用，不会与 android.R.id 资源 id 冲突 */
    @Override
    protected List<NavDestination> destinations() {
        return Arrays.asList(
                new NavDestination(1, "返回本页", ToastLabActivity.class,
                        "当前就在提示实验室，无需重复跳转", CustomToast.Type.WARNING),
                new NavDestination(2, "生命周期演示", LifecycleDemoActivity.class,
                        "从【提示实验室】跳到【生命周期演示】—— 回去看回调时序", CustomToast.Type.INFO),
                new NavDestination(3, "设置", SettingsActivity.class,
                        "从【提示实验室】跳到【设置】—— 关掉不想要的提示类型", CustomToast.Type.INFO),
                new NavDestination(4, "回到首页", MainActivity.class,
                        "从【提示实验室】跳到【首页】—— 返回难题清单", CustomToast.Type.SUCCESS)
        );
    }
}
