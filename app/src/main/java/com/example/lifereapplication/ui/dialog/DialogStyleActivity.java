package com.example.lifereapplication.ui.dialog;

import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;

import com.example.lifereapplication.R;
import com.example.lifereapplication.util.toast.CustomToast;

/**
 * 对话框风格 Activity（半透明主题）。
 *
 * <p>宿主只执行 onPause 不会 onStop 的活教材：本页用
 * {@code windowIsTranslucent + 透明背景} 声明，系统判定宿主仍部分可见。</p>
 *
 * <p>为什么刻意不继承 {@code BaseMenuActivity}：本页要伪装成"浮在宿主上的
 * 对话框"，不应有 Toolbar/下拉菜单/insets 适配，保持最简继承链才能不破坏
 * 对话框观感，也不往生命周期实验里混入额外回调干扰。</p>
 */
public class DialogStyleActivity extends androidx.appcompat.app.AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_style);

        Button btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> {
            // 先提示后 finish：Toast 属于应用层窗口，不随 Activity 销毁而取消，
            // 用提示预告"宿主即将 onResume"，把因果讲在关闭动作之前
            CustomToast.show(this, "关闭对话框风格页：宿主即将 onResume", CustomToast.Type.INFO);
            finish();
            // 手动指定淡入淡出：与全项目统一的页面切换观感一致，
            // "淡出"也更贴合对话框消失的语义
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }
}
