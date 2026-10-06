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
     * 恢复模式（历史演进）：
     * v2 曾提供 MODE_EXACT（负偏移半截还原）与 MODE_ITEM_HEAD（偏移清零）；
     * v3 按需求统一为「条目头对齐」——保存时偏移恒为 0、首条半截则记下一条，
     * 恢复时条目完完整整贴顶，列表尾由 RV 自然钳制到底。
     * 两个常量保留用于 API 兼容，当前行为一致。
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

        // ---- 状态恢复（首帧定位，全程无抖动）----
        // ① 先在 LayoutManager 上设置 pending anchor（scrollToPositionWithOffset
        //    允许在 attach 前调用），首次布局即消费，不会出现"先画顶部再跳"的抖动；
        // ② 再用 OnPreDrawListener 在首帧绘制前做一次像素级校正——消除
        //    item margin/padding 锚点残差。
        // 保存语义 v3：offset 恒为 0 → 恢复时条目完完整整贴顶显示；
        // 条目在列表尾贴不了顶时由 RV 自然钳制到底。
        int[] state = ScrollStateKeeper.restore(requireContext(), listId);
        int[] safe = ScrollStateKeeper.clamp(state, ROW_COUNT, viewportHeight());
        int pendingPosition = -1;
        if (safe != null) {
            pendingPosition = safe[0];
            if (layoutManager instanceof LinearLayoutManager) {
                ((LinearLayoutManager) layoutManager)
                        .scrollToPositionWithOffset(pendingPosition, 0);
            }
        }

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        if (pendingPosition >= 0) {
            installFirstFrameCorrection(pendingPosition);
            if (getActivity() != null) {
                ToastCenter.show(getActivity(),
                        "已定位到第 " + (pendingPosition + 1) + " 条（条目完整显示）",
                        CustomToast.Type.SUCCESS);
            }
        }
    }

    /**
     * 首帧绘制前的像素级校正：
     * scrollToPositionWithOffset 的锚点不感知 item margin 与 RV paddingTop，
     * 恢复后目标条目可能残留几 px~20px 的偏差；在 PreDraw（首帧绘制前）把
     * 目标条目的 child.top 校正到 paddingTop（完整贴顶），用户零感知。
     */
    private void installFirstFrameCorrection(final int position) {
        final RecyclerView rv = recyclerView;
        if (rv == null) {
            return;
        }
        final int targetTop = rv.getPaddingTop();
        rv.getViewTreeObserver().addOnPreDrawListener(
                new android.view.ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        rv.getViewTreeObserver().removeOnPreDrawListener(this);
                        if (rv.getLayoutManager() instanceof LinearLayoutManager) {
                            android.view.View child =
                                    ((LinearLayoutManager) rv.getLayoutManager())
                                            .findViewByPosition(position);
                            if (child != null) {
                                int delta = targetTop - child.getTop();
                                if (delta != 0) {
                                    rv.scrollBy(0, delta);
                                }
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
        // 需求定义的保存语义（v3）：偏移量恒为 0，条目完完整整显示。
        // ① 首条只剩下半截（top<0）→ 改记它下面第一条完整可见的条目；
        // ② 首条完整可见 → 直接记它。
        // 两种情况保存的 offset 都是 0 → 返回时该条目完整贴顶显示；
        // 若条目贴近列表尾、贴不了顶，RecyclerView 会自然钳制到底（"到底了就到底"），
        // 不会把尾部拉出空白。
        View firstChild = recyclerView.getChildAt(0);
        if (firstChild != null && firstChild.getTop() < 0) {
            position += 1;
        }
        ScrollStateKeeper.save(requireContext(), listId, position, 0);
    }

    @Override
    public void onDestroyView() {
        recyclerView = null; // 防 Fragment 视图泄漏
        super.onDestroyView();
    }
}
