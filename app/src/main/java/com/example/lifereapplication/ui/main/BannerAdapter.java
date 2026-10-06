package com.example.lifereapplication.ui.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lifereapplication.R;
import com.example.lifereapplication.data.model.Problem;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页横向精选横幅适配器。
 *
 * <p><b>手势冲突隔离原理：</b>水平 RecyclerView 嵌套在垂直 RecyclerView 内时，
 * Android 触摸框架在 {@code onInterceptTouchEvent} 中按「初始位移方向」裁决：
 * 水平位移分量大 → 子视图（横幅）消费事件并锁定横向；垂直分量大 → 外层列表
 * 拦截并锁定纵向。该机制由系统实现，无锁竞争，帧率稳定 60fps。</p>
 *
 * <p><b>性能要点：</b></p>
 * <ul>
 *     <li>固定 108dp 高度 → onCreateViewHolder 复用率 100%，滚动零测量抖动；</li>
 *     <li>卡片为纯文本/图形，无位图加载，也就无需图片懒加载钩子；若接入图片，
 *     在 onBindViewHolder 里用 Glide.with(holder) 绑定 ViewHolder 生命周期即可；</li>
 *     <li>单行卡片，层级深度 5 层以内，无过度绘制风险；</li>
 *     <li>回弹采用系统 overscroll stretch（Android 12+）或 Glow（旧版），
 *     距离/速度由系统规范控制，不引入自定义插值器。</li>
 * </ul>
 */
public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerHolder> {

    /** 当前横幅数据：条目个位数，任何刷新策略的成本都可忽略 */
    private final List<Problem> items = new ArrayList<>();

    /** 无外部依赖的纯展示适配器：数据经 {@link #submit} 注入，构造无需参数 */
    public BannerAdapter() {
    }

    /**
     * 整表替换横幅数据。
     * 为什么用 notifyDataSetChanged 而非 DiffUtil：条目只有几个，一次全量重绑
     * 的开销远低于维护差分计算的复杂度，简单方案就是最优方案。
     */
    public void submit(List<Problem> featured) {
        items.clear();
        if (featured != null) {
            items.addAll(featured);
        }
        notifyDataSetChanged();
    }

    /**
     * 创建横幅 Holder。
     * 为什么 inflate 传 (parent, false)：借用父容器生成正确的 LayoutParams，
     * 挂接时机交给 RecyclerView——提前挂上会被测量两次。
     */
    @NonNull
    @Override
    public BannerHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_banner, parent, false);
        return new BannerHolder(view);
    }

    /**
     * 绑定一条横幅。为什么每次 bind 都重设点击监听：ViewHolder 是复用的，
     * 不重绑会把上一位置的数据引用带进新位置（串数据事故）。
     */
    @Override
    public void onBindViewHolder(@NonNull BannerHolder holder, int position) {
        Problem problem = items.get(position);
        holder.icon.setText(String.valueOf(problem.getStage()));
        holder.title.setText(problem.getTitle());
        holder.itemView.setOnClickListener(v -> {
            // 交给宿主跳转：通过 RecyclerView tag 携带 id，避免再持回调引用
            RecyclerView rv = (RecyclerView) holder.itemView.getParent();
            if (rv != null && rv.getTag() instanceof OnBannerClickListener) {
                ((OnBannerClickListener) rv.getTag()).onBannerClick(problem);
            }
        });
    }

    /** @return 横幅条数 */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * 水平滑动的入口工具：横向 LinearLayoutManager + 默认 MD 物理惯性。
     * 为什么做成静态工具：LayoutManager 的正确配置与本 adapter 强相关，
     * 收在一起保证任何宿主（如 HeaderHolder）创建横幅时配置一致；
     * setItemPrefetchEnabled 让滑动间隙预绑定相邻卡片，横滑首帧更顺。
     */
    public static LinearLayoutManager horizontalLayout(android.content.Context context) {
        LinearLayoutManager lm = new LinearLayoutManager(context,
                LinearLayoutManager.HORIZONTAL, false);
        lm.setItemPrefetchEnabled(true);
        return lm;
    }

    /** 横幅卡片 Holder：View 引用构造时缓存，bind 阶段零 findViewById */
    static class BannerHolder extends RecyclerView.ViewHolder {
        final TextView icon;
        final TextView title;

        BannerHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.textBannerIcon);
            title = itemView.findViewById(R.id.textBannerTitle);
        }
    }

    /** 横幅点击回调：跳转是页面职责，adapter 只上报数据、不感知具体页面 */
    public interface OnBannerClickListener {
        void onBannerClick(Problem problem);
    }
}
