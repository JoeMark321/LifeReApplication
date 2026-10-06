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
    private static final int ROW_COUNT = 100;

    private TextRowAdapter adapter;
    private String listId;
    private int span;
    private RecyclerView recyclerView;

    /** 工厂方法：参数必须走 arguments（进程被杀恢复后不丢） */
    public static ListFragment newInstance(String listId, int span) {
        ListFragment fragment = new ListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LIST_ID, listId);
        args.putInt(ARG_SPAN, span);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        listId = args != null ? args.getString(ARG_LIST_ID, "demo.default") : "demo.default";
        span = args != null ? args.getInt(ARG_SPAN, 1) : 1;
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
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        restoreScrollState();
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

    /** 恢复滚动位置：先恢复、等布局完成后再带偏移对齐 */
    private void restoreScrollState() {
        int[] state = ScrollStateKeeper.restore(requireContext(), listId);
        if (state == null) {
            return; // 首次进入：无历史状态，自然从顶部开始
        }
        int[] safe = ScrollStateKeeper.clamp(state, adapter.itemCount(), viewportHeight());
        if (safe == null) {
            return; // 空列表边界
        }
        RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();
        recyclerView.post(() -> {
            // offset 支持负值：负数代表条目部分滚出屏幕（半截显示），
            // scrollToPositionWithOffset 会原样恢复半截效果
            if (lm instanceof LinearLayoutManager) {
                ((LinearLayoutManager) lm).scrollToPositionWithOffset(safe[0], safe[1]);
            }
        });
        if (getActivity() != null) {
            ToastCenter.show(getActivity(),
                    "已恢复滚动位置：第 " + (safe[0] + 1) + " 条", CustomToast.Type.SUCCESS);
        }
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
        int offset;
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
        // 不减 paddingTop：scrollToPositionWithOffset 的坐标系就是相对 RV 顶边，
        // 条目滚出屏幕时 top 为负（半截），恢复时要原样还原这一半
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
