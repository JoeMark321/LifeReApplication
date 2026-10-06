# ViewPager2 联动、Toast 管理系统与项目配置优化文档

> **架构**：MVVM（全项目统一）｜ **语言**：Java ｜ **minSdk 30 / targetSdk 37**
> 分支 `feature/ui-optimization-lifecycle` ｜ 本文档对应最新提交

---

## 1. 兼容性处理

### 1.1 刘海屏 / 全面屏适配（多 SDK 版本，已实测修复）

**问题现象**：Android 15+ 设备上内容顶进状态栏，标题被状态栏图标遮挡。

**根因**：targetSdk 35+ 时系统**强制 edge-to-edge**（状态栏透明、内容全屏延伸），
旧式"内容从状态栏下开始"的默认行为不复存在，必须手动处理 WindowInsets。

**跨版本自动实现方案**（两层配合，无版本分支代码）：

| 层 | 实现 | 覆盖版本 |
|---|---|---|
| 主题层 | [themes.xml](../app/src/main/res/values/themes.xml)：`statusBarColor=brand_primary` + `windowLayoutInDisplayCutoutMode=shortEdges` + `windowBackground=bg_page` | Android 11~14（着色）；15+（刘海模式与底色） |
| 代码层 | [BaseMenuActivity.applyEdgeToEdgeInsets()](../app/src/main/java/com/example/lifereapplication/ui/nav/BaseMenuActivity.java)：监听 `android.R.id.content` 的 WindowInsets，Toolbar 顶部内边距 = 状态栏+刘海高度（蓝色自然延伸进状态栏），内容底部内边距 = 手势条高度 | 全版本（15+ 必需，14- 自动兼容 insets=0） |

**公共父主题**：日/夜主题共用 `Theme.LifeRe.Core`（values 定义，values-night 只做
`Base.Theme.LifeReApplication parent=Theme.LifeRe.Core` 覆盖），避免两处维护。

**Toolbar 必须可长高（挖孔屏关键坑）**：所有布局的 Toolbar 写
`layout_height=wrap_content` + `minHeight=?attr/actionBarSize`。
若写死 `?attr/actionBarSize`，在挖孔屏上 padding=152px 后标题只剩 ~56px
会被纵向裁切（MIUI 挖孔机实测）；wrap_content 让 Toolbar 被 padding 撑高，
标题永远有完整的 actionBarSize 空间。

**实测**（模拟器 API 36 + 真机 Android 17 挖孔屏截屏验证）：状态栏图标渲染在
品牌蓝背景上，标题"首页"完整可见，列表尾部不被手势条遮挡。

> 注意：横屏刘海缺口在左右两侧（cutout top=0），纵向布局无需处理侧向 insets；
> 如需完美适配侧边刘海，可扩展监听 `displayCutout` 的 left/right。

### 1.2 Arrays.asList 构造适配器数据（Chapter 用法）

[ChapterPagerAdapter](../app/src/main/java/com/example/lifereapplication/ui/chapter/ChapterPagerAdapter.java) 演示了经典的 `ExampleItem` 章节写法：

```java
public static final List<ExampleItem> CHAPTERS = Arrays.asList(
        new ExampleItem("第一章 · 线性列表", "100 条长列表，滚动位置精确记忆"),
        new ExampleItem("第二章 · 双列网格", "网格布局下的状态保持"),
        new ExampleItem("第三章 · 三列网格", "宽屏信息密度与边界钳制"),
        new ExampleItem("第四章 · 混合复习", "回到线性列表，验证前面章节状态")
);
```

**注意**：`Arrays.asList` 返回**定长列表**，只可 set 不可 add/remove；章节数据固定不变，正好匹配。若需要增删，外层包一层 `new ArrayList<>(Arrays.asList(...))`。

### 1.3 ViewPager2 与 Fragment 联动

```
ChapterActivity
 ├─ TabLayout  ◄── TabLayoutMediator（双向联动：点tab翻页/翻页选中tab）
 └─ ViewPager2 ── ChapterPagerAdapter (FragmentStateAdapter)
                      └─ createFragment(pos) → ListFragment.newInstance(listId, span)
```

**FragmentStateAdapter 工作原理**（对应需求第 5 项"详细讲解"）：

| 阶段 | 内部行为 | 效果 |
|---|---|---|
| 页面可见 | Fragment 完整生命周期，View 已创建 | 正常交互 |
| 相邻页（offscreenPageLimit=1 内） | 保留 View，仅停止交互 | 快速滑回零成本 |
| 超出窗口 | `onDestroyView` 但保留 `FragmentState`（savedState） | 滑回时视图重建、滚动位置等状态自动恢复 |
| 更远页面 | removeFragment，仅留 Bundle | 内存占用有界 |

ViewPager2 本质是封装 RecyclerView：复用回收机制、支持 `DiffUtil`、支持竖向翻页、RTL、Modifiable collection —— 这正是它取代 ViewPager1 的原因。

---

## 2. 项目结构与配置优化

### 2.1 引入 viewpager2

```toml
# gradle/libs.versions.toml
viewpager2 = "1.1.0"
viewpager2 = { group = "androidx.viewpager2", name = "viewpager2", version.ref = "viewpager2" }
```

```gradle
// app/build.gradle
implementation libs.viewpager2
```

### 2.2 项目结构（当前全貌）

```
com.example.lifereapplication/
├── data/
│   ├── local/          # Room：AppDatabase / dao/ / entity/
│   ├── memory/         # 仅内存：LifecycleEventLog（有界队列）
│   ├── model/          # 领域模型 Problem
│   ├── prefs/          # SharedPreferences：AppPreferences
│   └── repository/     # 唯一数据入口（含 LruCache 分级）
├── ui/
│   ├── chapter/        # ViewPager2 章节联动（新增）
│   ├── detail/         # 详情页
│   ├── dialog/         # 对话框风格 Activity
│   ├── lifecycle/      # 生命周期演示
│   ├── list/           # Fragment 长列表 + 状态保持（新增）
│   ├── main/           # 首页 + BannerAdapter
│   ├── nav/            # BaseMenuActivity / NavDestination
│   ├── settings/       # 提示权限设置
│   ├── toast/          # 提示实验室
│   ├── widget/         # LifecycleFlowView 自定义控件
│   └── (按功能分包，职责单一)
└── util/
    ├── toast/          # 六件套（见第 3 节）
    ├── ScrollStateKeeper.java
    ├── DialogHelper.java
    └── NotificationHelper.java
```

### 2.3 颜色系统现代化

[colors.xml](../app/src/main/res/values/colors.xml) 采用语义化分层：

```
品牌色 brand_primary / brand_primary_dark / brand_accent / brand_warn
表面   bg_page / bg_card / bg_chip / divider
文本   text_primary / text_secondary / text_hint
功能   snackbar_bg / stage_*（生命周期）/ level_*（难度）
```

所有布局只引用语义色，不出现裸 `#RRGGBB`；深色模式只需覆盖同名资源。

### 2.4 尺寸适配

新增 [dimens.xml](../app/src/main/res/values/dimens.xml)：**4dp 网格间距体系**（space_xs~xl）、**圆角体系**（radius_sm/md/lg/pill）、**字号体系**（text_caption~headline）。横幅高度、徽标尺寸等控件尺寸也收编为 dimen，全局一致、可一次调整。

### 2.5 滚动条样式

```xml
<item name="android:scrollbarThumbVertical">@drawable/scrollbar_thumb</item>
<item name="android:scrollbarSize">@dimen/scrollbar_size</item>
<item name="android:scrollbarDefaultDelayBeforeFade">800</item>
<item name="android:scrollbarFadeDuration">300</item>
```

[scrollbar_thumb.xml](../app/src/main/res/drawable/scrollbar_thumb.xml)：半透明品牌色胶囊圆角（radius 99dp），全项目列表统一；`DefaultDelayBeforeFade=800ms` 保证交互过程中滚动条保持可见（对应需求第 3 项）。

---

## 3. 右上角菜单功能实现

| 需求 | 实现 |
|---|---|
| 保存逻辑 | 菜单跳转时 `AppPreferences.setLastPage(目标Activity类名)` 持久化（[BaseMenuActivity](../app/src/main/java/com/example/lifereapplication/ui/nav/BaseMenuActivity.java)） |
| 返回时保存上一界面状态 | 上次访问页展示在首页"关于本项目"提示中；列表滚动状态由 ScrollStateKeeper 双缓存保留 |
| 菜单打开不影响主界面 | Toolbar 溢出菜单是独立弹出窗口（`popupTheme=Light`），打开期间主界面不获得焦点也不销毁，关闭后原样恢复 |
| 滚动条保持显示 | 主题级 `scrollbarDefaultDelayBeforeFade=800ms` + 菜单打开不触发列表重绘，滚动条状态无感知保留 |

---

## 4. Toast 提示系统优化（MVVM 架构）

### 4.1 六件套结构

```
ToastDispatcher（View↔VM 桥：节流裁决 → 入队）
    │
    ├─ ToastThrottle（纯逻辑 VM：2s 窗口 / 3 次上限）
    └─ ToastQueue（状态 VM：LiveData 队列头 + 串行计时）
              │ observe
         ToastQueueHost（View 宿主：BaseMenuActivity 生命周期内渲染）
              │
         ToastCenter → NATIVE / CUSTOM / MODERN（样式路由 + 权限管理）
```

### 4.2 三大能力

1. **防重复**：2 秒窗口内重复点击 → `SUPPRESSED` 完全静默，只弹第一次；
2. **消息队列**：每次只展示一条，展示完（2000/3500ms 计时）自动弹下一条，永不互相遮罩；队列上限 8 条，超出挤掉最旧；
3. **三次上限**：窗口内第 4 次起 → `OVER_LIMIT` → 弹模态对话框"提示太频繁？"，一键跳转【设置】管理提示类型（若用户关闭了对话框权限则尊重设置静默）。

### 4.3 MVVM 分工说明

| 层 | 类 | 职责 |
|---|---|---|
| Model/State | `ToastQueue`（持 LiveData 状态）、`ToastThrottle`（纯逻辑） | 持有状态与规则，不碰 View |
| ViewModel 桥 | `ToastDispatcher` | 裁决 + 入队，不渲染 |
| View | `ToastQueueHost` + `ToastCenter` + 三种 Toast 实现 | 观察 LiveData 渲染，生命周期由 Activity 感知 |

LiveData 生命周期语义带来的免费收益：页面 STOPPED 时队列渲染自动暂停，回到前台补播，不会出现"页面已销毁还弹 Toast"。

### 4.4 使用示例

```java
// 业务代码统一换成这一行（其余能力自动获得）：
ToastDispatcher.dispatch(activity, "保存成功", CustomToast.Type.SUCCESS, false);
```

---

## 5. Adapter + ViewPager2 + Fragment 整合：完整代码

### 5.1 FragmentStateAdapter

见 [ChapterPagerAdapter.java](../app/src/main/java/com/example/lifereapplication/ui/chapter/ChapterPagerAdapter.java)（要点：`createFragment` 必须每次返回**新** Fragment 实例；复用 `ListFragment.newInstance(listId, span)`，每章独立 listId 使滚动状态互不串扰；稳定 id 用 position）。

### 5.2 Activity 装配

```java
viewPager.setAdapter(new ChapterPagerAdapter(this));
new TabLayoutMediator(tabLayout, viewPager, true,
        (tab, position) -> tab.setText(pagerAdapter.pageTitle(position))
).attach();

viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
    @Override public void onPageSelected(int position) {
        ToastDispatcher.dispatch(ChapterActivity.this,
                "第 " + (position + 1) + " 章", CustomToast.Type.INFO, false);
    }
});
```

### 5.3 最佳实践

- `createFragment()` **禁止**缓存 Fragment 实例复用（会导致恢复状态错乱）；
- 页间数据用 Fragment arguments 传递，进程被杀恢复不丢；
- 需要 notifyDataSetChanged 时同步实现 `getItemId/containsItem` 提供稳定 id；
- 嵌套横滑列表时调用 `viewPager.setDirectionToRotateForRTL` 无关，重点在子 RecyclerView 的 `nestedScrollingEnabled=false` 或让系统方向裁决（本项目横幅即后者）。

---

## 6. 测试与验收

**构建**：`BUILD SUCCESSFUL`（assembleDebug + testDebugUnitTest）
**单元测试 16 项全部通过**：

| 套件 | 数量 | 覆盖 |
|---|---|---|
| ScrollStateKeeperTest | 8 | 状态钳制边界 + 1/3 位置 5% 误差验收 |
| ToastThrottleTest（新增） | 7 | 首次放行 / 2~3 次静默 / 第 4 次 OVER_LIMIT / 窗口重置 / 边界值 / reset |
| ExampleUnitTest | 1 | 模板 |

**手动验收路径**：
1. 刘海屏设备：首页内容延伸进刘海，标题不被遮挡；
2. 首页 ⋮ → 章节学习：4 个 Tab 与页面双向联动，翻页提示走队列不刷屏；
3. 快速连点任意菜单 4 次：第 4 次弹"提示太频繁"对话框 → 去设置；
4. 任意列表滚动中打开右上角菜单：滚动条与位置均保持，关闭菜单原样恢复；
5. "关于本项目"提示可见上次访问页面记录。

---

## 7. 参考文档

- [ViewPager2 官方指南](https://developer.android.com/develop/ui/views/screens/pager-2) ｜ [Fragment 间滑动](https://developer.android.com/guide/fragments/manage)
- [刘海屏适配](https://developer.android.com/develop/ui/views/layout/display-cutout)
- [Material Components - TabLayoutMediator](https://github.com/material-components/material-components-android)
- 本项目相关文档：`docs/横滑列表与Fragment状态保持技术文档.md`、`docs/界面全面优化实现文档_v2.md`
