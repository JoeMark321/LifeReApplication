package com.example.lifereapplication.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.lifereapplication.R;
import com.example.lifereapplication.ui.main.MainActivity;

/**
 * 系统通知统一封装。
 *
 * <p><b>为什么存在：</b>发一条通知要处理多套版本差异——Android 8.0+ 必须先建
 * {@link NotificationChannel}，否则通知被系统直接丢弃；Android 12+ 的
 * PendingIntent 必须显式声明可变性，否则直接崩溃；Android 13+ 还要
 * {@code POST_NOTIFICATIONS} 运行时权限。把这些样板收口到这里后，调用方
 * 只需一行 {@link #showNotification}，不必关心版本分支。</p>
 *
 * <p><b>架构位置：</b>util 工具层，是提示系统的一种输出通道（与 Toast/
 * Snackbar/Dialog 并列）；发送前必须先经 {@link #canNotify} 检查，且受
 * AppPreferences 的 notify_enabled 应用内开关约束。</p>
 */
public class NotificationHelper {

    /** 渠道 ID：通知与渠道的关联标识；同一 ID 重复创建是幂等更新（不会报错） */
    private static final String CHANNEL_ID = "LifeReApplicationChannel";
    /** 渠道名：展示在系统设置的通知渠道列表里，面向用户，必须是可读名称 */
    private static final String CHANNEL_NAME = "生命周期学习应用";
    /**
     * 自增通知 ID。为什么每次 +1 而不是固定值：用同一 ID 再次 notify 是"更新"
     * 语义，会覆盖前一条；生命周期事件可能短时间连发多条，各给独立 ID 才都可见。
     */
    private static int notificationId = 0;

    /**
     * 是否已具备发通知的全部条件：应用内开关 + 系统权限（Android 13+ 运行时）。
     *
     * <p>为什么两层检查缺一不可：{@code checkSelfPermission} 只覆盖 Android 13+
     * 新增的 POST_NOTIFICATIONS 运行时授权；而用户在系统设置里整体关闭通知、
     * 或低版本系统的状态要靠 {@code areNotificationsEnabled()} 兜底。任何一层
     * 不满足，系统都会<b>静默丢弃</b>通知（不抛异常），所以必须在发送前主动判断。</p>
     */
    public static boolean canNotify(Context context) {
        // 第一层：Android 13+ 运行时权限（低版本未授权该权限时此检查恒通过，无害）
        if (ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        // 第二层：通知总开关（用户可在系统设置里对应用整体关闭）
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /**
     * 发送一条系统通知（点击回到主页，展示后自动消失）。
     * 发通知前必须先 {@link #canNotify}，否则 Android 13+ 会静默丢弃。
     *
     * <p>为什么"先建渠道"放在每次发送路径里而不做一次性初始化：
     * {@code createNotificationChannel} 对已存在的渠道 ID 是幂等的（更新配置而非
     * 报错），放在发送前能保证首次调用也一定成功，还省去维护"渠道是否已创建"
     * 的额外状态。</p>
     */
    public static void showNotification(Context context, String title, String message) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Android 8.0+ 强制要求渠道：不建渠道的通知会被系统直接丢弃，所以必须按版本分支
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    // IMPORTANCE_DEFAULT：有声响、状态栏有图标但不弹悬浮横幅，
                    // 对学习提醒这类低打扰场景刚好
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("生命周期学习应用的通知渠道");
            manager.createNotificationChannel(channel);
        }

        // 点击通知回到主页：给通知一个明确的落地页，而不是点了没反应
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                // FLAG_IMMUTABLE：Android 12+ 必须显式声明可变性（不声明直接崩溃），
                // 选不可变更安全——Intent 内容不允许被通知系统外部修改；
                // FLAG_UPDATE_CURRENT：重复发通知时复用并更新同一个 PendingIntent 而非报错
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 用 NotificationCompat：统一新旧系统的构建 API，低版本自动忽略不支持的特性
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                // smallIcon 是必填项：不设置时 build 后 notify 会直接抛 IllegalArgumentException
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                // priority 是 8.0 以下老设备的优先级（8.0+ 由渠道 IMPORTANCE 接管），
                // 两处都设置才能保证全版本行为一致
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                // 点击后自动消失：否则用户点完通知还留在栏里要手动划掉
                .setAutoCancel(true);

        // 自增 ID：保证每条通知独立展示、互不覆盖（原因见字段注释）
        manager.notify(notificationId++, builder.build());
    }
}
