package com.example.lifereapplication.ui.main;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lifereapplication.R;
import com.example.lifereapplication.data.model.Problem;
import com.example.lifereapplication.ui.widget.LifecycleFlowView;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页适配器。
 *
 * <p>混合两种 ViewType：</p>
 * <ul>
 *     <li>{@link #TYPE_HEADER}：引导区（使用说明 + 生命周期流程图 + 快捷入口），
 *         作为第 0 项参与复用，滚出屏幕后其子 View 同样会被回收；</li>
 *     <li>{@link #TYPE_PROBLEM}：难题卡片，按 stage 显示 1、2、3… 序号。</li>
 * </ul>
 *
 * <p><b>性能要点：</b>onBindViewHolder 内不做任何 IO 与对象分配以外的耗时操作；
 * 点击动画用 View 属性动画（GPU 合成）；Header 中的流程图独立 invalidate，
 * 不会触发整页重绑。</p>
 */
public class ProblemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_PROBLEM = 1;

    private static final String[] STAGE_HINTS = {
            "onCreate：Activity 被创建，只做一次性初始化（找 View、恢复状态、建 ViewModel）。",
            "onStart：界面即将可见但还不能交互，适合刷新轻量数据。",
            "onResume：获得焦点、可交互，适合启动动画、占用独占资源。",
            "onPause：失去焦点但部分可见，只做轻量保存，耗时操作会拖慢跳转。",
            "onStop：完全不可见，释放大对象、反注册监听、写数据库都放这里。",
            "onRestart：从停止态回到前台，先于 onStart 调用。",
            "onDestroy：最终清理，把持有的引用置空，避免内存泄漏。"
    };

    private final List<Problem> problems = new ArrayList<>();
    private final List<Problem> featured = new ArrayList<>();
    private final OnItemClickListener listener;
    private final OnHeaderActionListener headerListener;
    private final OnBannerClickListener bannerListener;

    private HeaderHolder headerHolder;

    public interface OnItemClickListener {
        void onItemClick(Problem problem);
    }

    public interface OnHeaderActionListener {
        void onQuickActionClick(String tag);
    }

    /** 横幅点击：由 Activity 实现跳转 */
    public interface OnBannerClickListener {
        void onBannerClick(Problem problem);
    }

    public ProblemAdapter(OnItemClickListener listener,
                          OnHeaderActionListener headerListener,
                          OnBannerClickListener bannerListener) {
        this.listener = listener;
        this.headerListener = headerListener;
        this.bannerListener = bannerListener;
        setHasStableIds(true);
    }

    public void setProblems(List<Problem> list) {
        problems.clear();
        if (list != null) {
            problems.addAll(list);
        }
        notifyDataSetChanged();
    }

    /**
     * 提交横幅数据（featured 主题）。
     * 关键点：横幅 RecyclerView 在头部 item 内部，Activity 的 onCreate 阶段
     * findViewById 拿不到它（还没被创建），因此数据先缓存在适配器，
     * 由 bindHeader 在 HeaderHolder 创建时初始化并填充。
     */
    public void submitFeatured(List<Problem> list) {
        featured.clear();
        if (list != null) {
            featured.addAll(list);
        }
        if (headerHolder != null && headerHolder.bannerAdapter != null) {
            headerHolder.bannerAdapter.submit(featured);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_HEADER : TYPE_PROBLEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_home_header, parent, false);
            HeaderHolder holder = new HeaderHolder(view, bannerListener);
            headerHolder = holder;
            return holder;
        }
        View view = inflater.inflate(R.layout.item_problem, parent, false);
        return new ProblemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderHolder) {
            bindHeader((HeaderHolder) holder);
        } else {
            bindProblem((ProblemHolder) holder, problems.get(position - 1), position);
        }
    }

    private void bindHeader(HeaderHolder holder) {
        holder.cardFirst.setOnClickListener(v -> {
            animateClick(v, () -> headerListener.onQuickActionClick("beginner"));
        });
        holder.cardSecond.setOnClickListener(v -> {
            animateClick(v, () -> headerListener.onQuickActionClick("pitfall"));
        });
        holder.cardThird.setOnClickListener(v -> {
            animateClick(v, () -> headerListener.onQuickActionClick("interview"));
        });
        holder.flowView.setCurrentStage("onCreate");
        holder.flowHint.setText(STAGE_HINTS[0]);

        // 横幅只在 HeaderHolder 首次创建时初始化一次（复用不重建）
        if (holder.bannerAdapter == null) {
            holder.bannerAdapter = new BannerAdapter();
            holder.banner.setAdapter(holder.bannerAdapter);
            holder.banner.setLayoutManager(BannerAdapter.horizontalLayout(
                    holder.itemView.getContext()));
            holder.bannerAdapter.submit(featured);
        }
    }

    private void bindProblem(ProblemHolder holder, Problem problem, int position) {
        holder.stage.setText(String.valueOf(position));
        holder.title.setText(problem.getTitle());
        holder.description.setText(problem.getDescription());
        holder.category.setText(problem.getCategory());
        holder.difficulty.setText(problem.getDifficulty());
        holder.difficulty.setTextColor(difficultyColor(holder.itemView, problem.getDifficulty()));

        holder.itemView.setOnClickListener(v ->
                animateClick(v, () -> listener.onItemClick(problem)));
    }

    private int difficultyColor(View view, String difficulty) {
        int resId;
        if ("入门".equals(difficulty) || "简单".equals(difficulty)) {
            resId = R.color.level_easy;
        } else if ("中等".equals(difficulty)) {
            resId = R.color.level_medium;
        } else {
            resId = R.color.level_hard;
        }
        return ContextCompat.getColor(view.getContext(), resId);
    }

    /** 由 Activity 在自身生命周期回调时驱动，实时高亮流程图 */
    public void updateLifecycleStage(String stage) {
        if (headerHolder == null) {
            return;
        }
        int index = stageIndex(stage);
        if (index < 0) {
            return;
        }
        headerHolder.flowView.setCurrentStage(stage);
        headerHolder.flowHint.setText(STAGE_HINTS[index]);
    }

    private int stageIndex(String stage) {
        String[] all = {"onCreate", "onStart", "onResume", "onPause", "onStop", "onRestart", "onDestroy"};
        for (int i = 0; i < all.length; i++) {
            if (all[i].equals(stage)) {
                return i;
            }
        }
        return -1;
    }

    private void animateClick(View view, Runnable action) {
        view.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(90)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(90)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        if (action != null) {
                                            action.run();
                                        }
                                    }
                                });
                    }
                });
    }

    @Override
    public int getItemCount() {
        return problems.size() + 1; // +1 为头部
    }

    @Override
    public long getItemId(int position) {
        return position == 0 ? -1L : problems.get(position - 1).getId();
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final LifecycleFlowView flowView;
        final TextView flowHint;
        final View cardFirst;
        final View cardSecond;
        final View cardThird;
        final RecyclerView banner;
        BannerAdapter bannerAdapter;

        HeaderHolder(@NonNull View itemView, OnBannerClickListener bannerListener) {
            super(itemView);
            flowView = itemView.findViewById(R.id.lifecycleFlow);
            flowHint = itemView.findViewById(R.id.textFlowHint);
            cardFirst = itemView.findViewById(R.id.cardFirst);
            cardSecond = itemView.findViewById(R.id.cardSecond);
            cardThird = itemView.findViewById(R.id.cardThird);
            banner = itemView.findViewById(R.id.rvBanner);
            // 横幅点击路由给 Activity（BannerAdapter 通过 parent tag 取回调）
            if (banner != null && bannerListener != null) {
                banner.setTag((BannerAdapter.OnBannerClickListener) bannerListener::onBannerClick);
            }
        }
    }

    static class ProblemHolder extends RecyclerView.ViewHolder {
        final TextView stage;
        final TextView title;
        final TextView description;
        final TextView category;
        final TextView difficulty;

        ProblemHolder(@NonNull View itemView) {
            super(itemView);
            stage = itemView.findViewById(R.id.textStage);
            title = itemView.findViewById(R.id.textTitle);
            description = itemView.findViewById(R.id.textDescription);
            category = itemView.findViewById(R.id.textCategory);
            difficulty = itemView.findViewById(R.id.textDifficulty);
        }
    }
}
