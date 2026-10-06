package com.example.lifereapplication.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 用户偏好设置（SharedPreferences 持久化）。
 *
 * <p>存放的是“少量、键值型、需要跨启动保留”的设置，例如提示样式偏好与
 * 各类提示的总开关（提示权限管理）。内容少、读写频率低，用
 * SharedPreferences 比引入数据库更轻。</p>
 *
 * <p><b>为什么所有写入都用 {@code apply()} 而非 {@code commit()}：</b>偏好变更
 * 都发生在用户点击的瞬间，commit 同步落盘会阻塞主线程；apply 内存立即生效、
 * 异步落盘，符合交互场景的轻量要求。</p>
 *
 * <p><b>为什么统一放一个文件：</b>键总量不过十几条，分文件只增加管理成本；
 * 单文件也让"设置页按 key 动态写开关"这类通用逻辑可以直接读写。</p>
 */
public class AppPreferences {

    private static final String FILE_NAME = "lifere_prefs";

    // 全部 key 集中定义为常量：调用处不手写字符串，避免打错且可全局搜索
    private static final String KEY_TOAST_STYLE = "toast_style";
    private static final String KEY_NATIVE_ENABLED = "toast_native_enabled";
    private static final String KEY_CUSTOM_ENABLED = "toast_custom_enabled";
    private static final String KEY_MODERN_ENABLED = "toast_modern_enabled";
    private static final String KEY_DIALOG_ENABLED = "dialog_enabled";
    private static final String KEY_NOTIFY_ENABLED = "notify_enabled";
    private static final String KEY_LIFECYCLE_TRACE_ENABLED = "lifecycle_trace_enabled";
    private static final String KEY_LAST_PAGE = "last_page";
    private static final String KEY_LAST_CHAPTER = "last_chapter";
    private static final String KEY_LIST_DEMO_TAB = "list_demo_tab";

    private final SharedPreferences sp;

    /**
     * 构造时一次取好 SP 引用，后续读写不再重复向系统查询。
     * 为什么用 ApplicationContext：SP 是进程级共享单例，缓存它的引用若绑着
     * Activity 上下文会造成泄漏；本类的生命周期与进程一致。
     */
    public AppPreferences(Context context) {
        this.sp = context.getApplicationContext()
                .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 提示样式偏好，默认“现代化”（Snackbar）。
     * 为什么默认 MODERN：应用主打现代提示方式，新用户第一次就该看到主打的
     * Snackbar 效果，而不是回退到老式 Toast。
     */
    public String getToastStyle() {
        return sp.getString(KEY_TOAST_STYLE, "MODERN");
    }

    /** 保存提示样式偏好（apply 异步落盘，理由见类注释） */
    public void setToastStyle(String style) {
        sp.edit().putString(KEY_TOAST_STYLE, style).apply();
    }

    // ---- 各类提示的总开关，默认全部开启 ----
    // 为什么默认 true：这是提示演示应用，任何一类默认关闭都会让用户误以为功能缺失；
    // 关闭属于用户在设置页主动 opt-out 的选择，而非默认弃用
    public boolean isNativeEnabled() {
        return sp.getBoolean(KEY_NATIVE_ENABLED, true);
    }

    public boolean isCustomEnabled() {
        return sp.getBoolean(KEY_CUSTOM_ENABLED, true);
    }

    public boolean isModernEnabled() {
        return sp.getBoolean(KEY_MODERN_ENABLED, true);
    }

    public boolean isDialogEnabled() {
        return sp.getBoolean(KEY_DIALOG_ENABLED, true);
    }

    public boolean isNotifyEnabled() {
        return sp.getBoolean(KEY_NOTIFY_ENABLED, true);
    }

    public boolean isLifecycleTraceEnabled() {
        return sp.getBoolean(KEY_LIFECYCLE_TRACE_ENABLED, true);
    }

    /** 右上角菜单跳转时记忆的目标页（Activity 类名），用于"返回时提示上一界面" */
    public String getLastPage() {
        return sp.getString(KEY_LAST_PAGE, null);
    }

    /** 记录跳转目标页（与 getLastPage 配对：跳转前写入，返回时读出用于提示"来自上一界面"） */
    public void setLastPage(String pageClassName) {
        sp.edit().putString(KEY_LAST_PAGE, pageClassName).apply();
    }

    /**
     * 章节学习：上次停留的章节页码（ViewPager2 position）。
     * 默认 -1 表示"从未进入过"，调用方据此跳过恢复而不是跳到第 0 页。
     */
    public int getLastChapter() {
        return sp.getInt(KEY_LAST_CHAPTER, -1);
    }

    /** 与 getLastChapter 配对：翻页时写入，下次进入章节页时恢复 */
    public void setLastChapter(int position) {
        sp.edit().putInt(KEY_LAST_CHAPTER, position).apply();
    }

    /** 列表演演：上次选中的布局 tab（tag 字符串） */
    public String getListDemoTab() {
        return sp.getString(KEY_LIST_DEMO_TAB, null);
    }

    /** 与 getListDemoTab 配对：切换 tab 时写入，下次进入时恢复上次选择 */
    public void setListDemoTab(String tag) {
        sp.edit().putString(KEY_LIST_DEMO_TAB, tag).apply();
    }

    /** 通用 tab 记忆（按页面 key 隔离，列表演演三等新页面使用） */
    public String getListTab(String pageKey) {
        return sp.getString("list_tab." + pageKey, null);
    }

    /** 与 getListTab 配对；key 加 pageKey 前缀隔离，各页面的 tab 记忆互不干扰 */
    public void setListTab(String pageKey, String tag) {
        sp.edit().putString("list_tab." + pageKey, tag).apply();
    }

    /**
     * 按 key 通用写入开关。
     * 为什么需要泛化入口：设置页按 key 列表动态生成开关行，手里只有字符串 key；
     * 若逐项写专用 setter，设置页就得依赖全部具体方法，耦合更重。
     */
    public void setEnabled(String key, boolean value) {
        sp.edit().putBoolean(key, value).apply();
    }

    // ---- key 的静态暴露：设置页 / 提示分发处需要 key 字符串来定位某一开关，
    // 统一从这里取，保证读写两端永远用同一个 key ----
    public static String keyNative() {
        return KEY_NATIVE_ENABLED;
    }

    public static String keyCustom() {
        return KEY_CUSTOM_ENABLED;
    }

    public static String keyModern() {
        return KEY_MODERN_ENABLED;
    }

    public static String keyDialog() {
        return KEY_DIALOG_ENABLED;
    }

    public static String keyNotify() {
        return KEY_NOTIFY_ENABLED;
    }

    public static String keyLifecycleTrace() {
        return KEY_LIFECYCLE_TRACE_ENABLED;
    }
}
