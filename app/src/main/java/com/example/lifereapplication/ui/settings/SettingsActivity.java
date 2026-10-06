package com.example.lifereapplication.ui.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.lifereapplication.R;
import com.example.lifereapplication.data.prefs.AppPreferences;
import com.example.lifereapplication.ui.lifecycle.LifecycleDemoActivity;
import com.example.lifereapplication.ui.main.MainActivity;
import com.example.lifereapplication.ui.nav.BaseMenuActivity;
import com.example.lifereapplication.ui.nav.NavDestination;
import com.example.lifereapplication.ui.toast.ToastLabActivity;
import com.example.lifereapplication.util.toast.CustomToast;
import com.example.lifereapplication.util.toast.ToastCenter;
import com.example.lifereapplication.util.toast.ToastStyle;

import java.util.Arrays;
import java.util.List;

/**
 * 设置页：提示样式偏好 + 提示权限管理。
 *
 * <p>所有开关写入 SharedPreferences（键值型少量数据，无需数据库），
 * ToastCenter 在每次弹出前读取，实现真正的“用户关闭即静默”。</p>
 */
public class SettingsActivity extends BaseMenuActivity {

    private AppPreferences prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupToolbar();
        prefs = new AppPreferences(this);

        // 样式偏好
        RadioGroup radioGroup = findViewById(R.id.radioGroupStyle);
        Switch switchNative = findViewById(R.id.switchNative);
        Switch switchCustom = findViewById(R.id.switchCustom);
        Switch switchModern = findViewById(R.id.switchModern);
        Switch switchDialog = findViewById(R.id.switchDialog);
        Switch switchNotify = findViewById(R.id.switchNotify);
        Switch switchTrace = findViewById(R.id.switchTrace);

        // 为什么先回填 UI 再挂监听（监听在下方才注册）：程序化 setChecked
        // 若发生在监听挂载之后会触发回调，把"初始化默认值"误当成用户操作
        // 写回 prefs；先回填后挂监听从执行顺序上杜绝这一污染
        switchNative.setChecked(prefs.isNativeEnabled());
        switchCustom.setChecked(prefs.isCustomEnabled());
        switchModern.setChecked(prefs.isModernEnabled());
        switchDialog.setChecked(prefs.isDialogEnabled());
        switchNotify.setChecked(prefs.isNotifyEnabled());
        switchTrace.setChecked(prefs.isLifecycleTraceEnabled());

        // 中心圆球：点击弹跳 + 轻提示（弧度外观见 bg_sphere.xml）
        View sphere = findViewById(R.id.viewSphere);
        sphere.setOnClickListener(v -> {
            // 120ms 放大 + 160ms 回缩：短促的"按下去弹回来"节奏，反馈明显又不打断
            // 操作（时长为手调经验值，1.15 倍放大幅度肉眼可感又不夸张）
            v.animate().scaleX(1.15f).scaleY(1.15f).setDuration(120)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(160).start())
                    .start();
            tip("圆球反馈：设置已就绪", CustomToast.Type.SUCCESS);
        });

        // 回填样式单选：default 分支涵盖 MODERN 与一切未知/脏值，
        // 与 ToastCenter.resolveStyle 的兜底策略（解析失败落 MODERN）保持一致
        switch (prefs.getToastStyle()) {
            case "NATIVE":
                radioGroup.check(R.id.radioNative);
                break;
            case "CUSTOM":
                radioGroup.check(R.id.radioCustom);
                break;
            default:
                radioGroup.check(R.id.radioModern);
                break;
        }

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String style;
            String label;
            if (checkedId == R.id.radioNative) {
                style = ToastStyle.NATIVE.name();
                label = "原生";
            } else if (checkedId == R.id.radioCustom) {
                style = ToastStyle.CUSTOM.name();
                label = "自定义";
            } else {
                style = ToastStyle.MODERN.name();
                label = "现代化";
            }
            prefs.setToastStyle(style);
            tip("默认提示样式已切换为【" + label + "】", CustomToast.Type.SUCCESS);
        });

        switchNative.setOnCheckedChangeListener((b, checked) ->
                saveAndFeedback(AppPreferences.keyNative(), checked, "原生 Toast"));
        switchCustom.setOnCheckedChangeListener((b, checked) ->
                saveAndFeedback(AppPreferences.keyCustom(), checked, "自定义 Toast"));
        switchModern.setOnCheckedChangeListener((b, checked) ->
                saveAndFeedback(AppPreferences.keyModern(), checked, "现代化 Snackbar"));
        switchDialog.setOnCheckedChangeListener((b, checked) ->
                saveAndFeedback(AppPreferences.keyDialog(), checked, "模态对话框"));
        switchNotify.setOnCheckedChangeListener((b, checked) ->
                saveAndFeedback(AppPreferences.keyNotify(), checked, "系统通知"));
        switchTrace.setOnCheckedChangeListener((b, checked) ->
                saveAndFeedback(AppPreferences.keyLifecycleTrace(), checked, "生命周期日志"));
    }

    /**
     * 六个开关共用的"写入 + 反馈"路径。
     * 为什么 key 由 AppPreferences.keyXxx() 提供而不手写字符串：读写两端
     * 引用同一常量工厂，杜绝拼写不一致导致的静默失效；为什么关闭时用
     * WARNING 类型：语义上提醒"该类提示此后将完全静默"。
     */
    private void saveAndFeedback(String key, boolean checked, String label) {
        prefs.setEnabled(key, checked);
        String state = checked ? "已开启" : "已关闭（完全静默）";
        ToastCenter.show(this, label + " " + state, checked ? CustomToast.Type.SUCCESS : CustomToast.Type.WARNING);
    }

    // ---------- 下拉菜单 ----------

    @Override
    protected Page page() {
        return Page.SETTINGS;
    }

    /** 菜单 id 取 1~4 的小整数：仅在本页菜单内匹配用，不会与 android.R.id 资源 id 冲突 */
    @Override
    protected List<NavDestination> destinations() {
        return Arrays.asList(
                new NavDestination(1, "返回本页", SettingsActivity.class,
                        "当前就在设置页，无需重复跳转", CustomToast.Type.WARNING),
                new NavDestination(2, "提示实验室", ToastLabActivity.class,
                        "从【设置】跳到【提示实验室】—— 验证刚才的偏好是否生效", CustomToast.Type.INFO),
                new NavDestination(3, "生命周期演示", LifecycleDemoActivity.class,
                        "从【设置】跳到【生命周期演示】—— 日志开关立即生效", CustomToast.Type.INFO),
                new NavDestination(4, "回到首页", MainActivity.class,
                        "从【设置】跳到【首页】—— 返回难题清单", CustomToast.Type.SUCCESS)
        );
    }
}
