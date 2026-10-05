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
 */
public class DialogStyleActivity extends androidx.appcompat.app.AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_style);

        Button btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> {
            CustomToast.show(this, "关闭对话框风格页：宿主即将 onResume", CustomToast.Type.INFO);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }
}
