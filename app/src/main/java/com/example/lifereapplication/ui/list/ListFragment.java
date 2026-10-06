package com.example.lifereapplication.ui.list;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lifereapplication.R;
import com.example.lifereapplication.util.ScrollStateKeeper;
import com.example.lifereapplication.util.toast.CustomToast;
import com.example.lifereapplication.util.toast.ToastCenter;

/**
 * 长列表演示 Fragment：线性 / 网格两种布局 + 滚动位置精确记忆。
 *
 * <p><b>状态保持时序（对应文档第 3 节流程图）：</b></p>
 * <pre>
 * 滑动 ──► onPause：findFirstVisibleItemPosition+offset ──► ScrollStateKeeper.save
 *            （内存 LruCache 立即可用，SP 异步落盘防进程被杀）
 * 重新进入 ──► onViewCreated：restore ──► clamp(数据量, viewport)
 *            ──► post { scrollToPositionWithOffset } ──► 精确复位 &lt; 200ms
 * </pre>
 *
 * <p><b>边界处理：</b></p>
 * <ul>
 *     <li>空列表：{@code clamp()} 返回 null，直接放弃恢复；</li>
 *     <li>数据变少：position 钳制到 itemCount-1，不越界；</li>
 *     <li>列表项尺寸变化 / 横竖屏切换：偏移超过 viewport 时归零，
 *     退化为「滚到该位置」，视觉上依然正确；</li>
 *     <li>内存紧张：SP 落盘态兜底，重建后从磁盘恢复。</li>
 * </ul>
 *
 * <p><b>横竖屏适配：</b>span 由 Activity 按屏幕方向传入（竖屏 1 列 / 2 列，
 * 横屏 2 列 / 3 列），Fragment 不感知方向，职责单一。</p>
 */
public class ListFragment extends Fragment {

    private static final String ARG_LIST_ID = "arg_list_id";
    private static final String ARG_SPAN = "arg_span";
    private static final String ARG_RESTORE_MODE = "arg_restore_mode";
    private static final int ROW_COUNT = 100;

    /**
     * 恢复模式（演示一 / 演示二的刻意差异，v4 最终定义）：
     * <ul>
     *     <li>{@link #MODE_EXACT}（列表演演一）：像素级精确恢复——该在哪就在哪，
     *     保存/恢复 child.getTop() 原始像素（含负值半截），不跳上/下一条、不显示完整；</li>
     *     <li>{@link #MODE_ITEM_HEAD}（列表演演二）：条目头对齐——偏移清零，
     *     恢复时该条目完完整整贴顶显示，列表尾自然钳制到底。</li>
     * </ul>
     */
    public static final int MODE_EXACT = 0;
    public static final int MODE_ITEM_HEAD = 1;

    private TextRowAdapter adapter;
    private String listId;
    private int span;
    private int restoreMode;
    private RecyclerView recyclerView;

    /** 工厂方法：参数必须走 arguments（进程被杀恢复后不丢） */
    public static ListFragment newInstance(String listId, int span) {
        return newInstance(listId, span, MODE_EXACT);
    }

    public static ListFragment newInstance(String listId, int span, int restoreMode) {
        ListFragment fragment = new ListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LIST_ID, listId);
        args.putInt(ARG_SPAN, span);
        args.putInt(ARG_RESTORE_MODE, restoreMode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        listId = args != null ? args.getString(ARG_LIST_ID, "demo.default") : "demo.default";
        span = args != null ? args.getInt(ARG_SPAN, 1) : 1;
        restoreMode = args != null ? args.getInt(ARG_RESTORE_MODE, MODE_EXACT) : MODE_EXACT;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerList);

        adapter = new TextRowAdapter((position, text) -> {
            if (getActivity() instanceof ListDemoActivity) {
                ToastCenter.show(getActivity(),
                        "点击了第 " + (position + 1) + " 条：" + text.substring(0, 8),
                        CustomToast.Type.INFO);
            }
        });
        adapter.submitCount(ROW_COUNT);

        RecyclerView.LayoutManager layoutManager;
        if (span > 1) {
            // 明确列数（旧调用方式）
            layoutManager = new GridLayoutManager(requireContext(), span);
        } else if (span == 0) {
            // 自适应列数：按最小列宽 170dp 动态计算（宽屏自动加列，窄屏自动减列）
            layoutManager = createAdaptiveGridLayout();
        } else {
            layoutManager = new LinearLayoutManager(requireContext());
        }

        // ---- 状态恢复（两步确定性定位，杜绝逐轮漂移）----
        // 第 1 步：pending anchor 统一用 offset=0（该值在所有 LayoutManager/ROM 下
        //          定位结果确定：条目头贴着内容区顶），在 setAdapter 前调用，首帧即生效；
        // 第 2 步：PreDraw（首帧绘制前）用 scrollBy 精确补齐 (savedTop - 0) 的像素差——
        //          scrollBy 是纯像素滚动、零误差，最终 child.top == savedTop 逐像素一致。
        // 旧实现把负偏移直接塞进 pending anchor，部分 ROM/网格下锚点解析不精确，
        // 且 PreDraw 监听可能在目标 child 尚未布局时被移除 → 校正失效 → 每轮漂移。
        int[] state = ScrollStateKeeper.restore(requireContext(), listId);
        int[] safe = ScrollStateKeeper.clamp(state, ROW_COUNT, viewportHeight());
        int pendingPosition = -1;
        int correctionDelta = 0;
        if (safe != null) {
            pendingPosition = safe[0];
            correctionDelta = restoreMode == MODE_EXACT ? safe[1] : recyclerView.getPaddingTop();
            if (layoutManager instanceof LinearLayoutManager) {
                ((LinearLayoutManager) layoutManager)
                        .scrollToPositionWithOffset(pendingPosition, 0);
            }
        }

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        if (pendingPosition >= 0) {
            installFirstFrameCorrection(pendingPosition, correctionDelta);
            if (getActivity() != null) {
                ToastCenter.show(getActivity(),
                        restoreMode == MODE_EXACT
                                ? "已精确回到第 " + (pendingPosition + 1) + " 条（该在哪就在哪）"
                                : "已定位到第 " + (pendingPosition + 1) + " 条（条目头对齐）",
                        CustomToast.Type.SUCCESS);
            }
        }
    }

    /**
     * 首帧绘制前的像素级校正（scrollBy 纯像素滚动，零误差）：
     * EXACT      → delta = 保存的 child.getTop() 原值（可为负，半截）；
     * ITEM_HEAD  → delta = paddingTop（条目完整贴顶）。
     * 监听直到目标 child 完成布局才移除并校正，杜绝"校正被跳过→逐轮漂移"。
     * 条目在列表尾时 delta 会被 scrollBy 自然钳制到底（到底了就到底）。
     */
    private void installFirstFrameCorrection(final int position, final int delta) {
        final RecyclerView rv = recyclerView;
        if (rv == null) {
            return;
        }
        rv.getViewTreeObserver().addOnPreDrawListener(
                new android.view.ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        if (rv.getLayoutManager() instanceof LinearLayoutManager) {
                            android.view.View child =
                                    ((LinearLayoutManager) rv.getLayoutManager())
                                            .findViewByPosition(position);
                            if (child == null) {
                                return true; // 目标条目尚未布局：保持监听，下帧再试
                            }
                            rv.getViewTreeObserver().removeOnPreDrawListener(this);
                            if (delta != 0) {
                                rv.scrollBy(0, delta);
                            }
                        }
                        return true;
                    }
                });
    }

    /** 自适应网格：列数随容器宽度变化（含横竖屏/折叠屏），最小列宽 170dp，1~4 列 */
    private GridLayoutManager createAdaptiveGridLayout() {
        final GridLayoutManager glm = new GridLayoutManager(requireContext(), 2);
        final int minColWidth = (int) (170 * getResources().getDisplayMetrics().density);
        recyclerView.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, ob, od) -> {
            int width = r - l;
            if (width <= 0) {
                return;
            }
            int cols = Math.max(1, Math.min(4, width / minColWidth));
            if (cols != glm.getSpanCount()) {
                glm.setSpanCount(cols);
            }
        });
        return glm;
    }

    private int viewportHeight() {
        return recyclerView.getHeight() > 0
                ? recyclerView.getHeight()
                : requireActivity().getResources().getDisplayMetrics().heightPixels;
    }

    /**
     * onPause 是保存的黄金时机：保证被调用（比 onStop/onDestroy 更可靠），
     * 且系统约束该回调必须轻量 —— apply() 异步写满足要求。
     */
    @Override
    public void onPause() {
        saveScrollState();
        super.onPause();
    }

    private void saveScrollState() {
        if (recyclerView == null || adapter == null || adapter.itemCount() == 0) {
            return; // 空列表无需保存
        }
        RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();
        int position;
        if (lm instanceof GridLayoutManager) {
            position = ((GridLayoutManager) lm).findFirstVisibleItemPosition();
        } else if (lm instanceof LinearLayoutManager) {
            position = ((LinearLayoutManager) lm).findFirstVisibleItemPosition();
        } else {
            return;
        }
        if (position < 0) {
            return;
        }
        // 精确保存语义（v4）：记录第一条可见项的位置 + 它的真实像素 top（可为负，
        // 代表半截滚出屏幕）。恢复时 EXACT 模式原样还原这一像素位——该在哪就在哪，
        // 不跳上/下一条、不显示完整；ITEM_HEAD 模式在恢复侧自行清零。
        View firstChild = recyclerView.getChildAt(0);
        int pixelOffset = firstChild == null ? 0 : firstChild.getTop();
        ScrollStateKeeper.save(requireContext(), listId, position, pixelOffset);
    }

    @Override
    public void onDestroyView() {
        recyclerView = null; // 防 Fragment 视图泄漏
        super.onDestroyView();
    }
}
