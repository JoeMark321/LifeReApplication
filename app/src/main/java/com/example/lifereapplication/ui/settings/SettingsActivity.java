package com.example.lifereapplication.ui.settings;

import android.os.Bundle;
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

        switchNative.setChecked(prefs.isNativeEnabled());
        switchCustom.setChecked(prefs.isCustomEnabled());
        switchModern.setChecked(prefs.isModernEnabled());
        switchDialog.setChecked(prefs.isDialogEnabled());
        switchNotify.setChecked(prefs.isNotifyEnabled());
        switchTrace.setChecked(prefs.isLifecycleTraceEnabled());

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
