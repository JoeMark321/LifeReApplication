package com.example.lifereapplication.ui.list;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lifereapplication.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 通用文本条目适配器（列表/网格共用，支持横竖屏数量自适应）。
 *
 * <p><b>为什么列表和网格能共用一个 Adapter：</b>RecyclerView 的布局形态
 * 完全由 LayoutManager 决定（线性 / GridLayoutManager），Adapter 只负责
 * "给定 position 产出一个条目视图"，与几列无关——换布局不必换 Adapter，
 * 横竖屏 / 列数切换零成本。</p>
 *
 * <p>ViewHolder 只持有行 View 与文本引用，复用时零分配；
 * 文本在 bind 时生成（演示用），真实业务应来自数据源。
 * 数据整批一次性生成、无局部更新诉求，直接 notifyDataSetChanged 即可，
 * 无需引入 DiffUtil。</p>
 */
public class TextRowAdapter extends RecyclerView.Adapter<TextRowAdapter.RowHolder> {

    // 声明为 final：Adapter 生命周期内引用不变，避免中途被替换导致复用状态错乱
    private final List<String> items = new ArrayList<>();
    private final OnRowClickListener listener;

    /** 行点击回调：position 用于提示定位，text 用于提示展示内容 */
    public interface OnRowClickListener {
        void onRowClick(int position, String text);
    }

    public TextRowAdapter(OnRowClickListener listener) {
        this.listener = listener;
    }

    /**
     * 生成 count 条模拟数据。
     * 为什么用 notifyDataSetChanged 而不是 DiffUtil：演示数据整批重建、
     * 无增量更新与动画诉求，全量刷新最简单直接。
     */
    public void submitCount(int count) {
        items.clear();
        for (int i = 0; i < count; i++) {
            items.add(String.format(Locale.CHINA, "条目 #%03d —— 滑动后退出再进入，会精确回到当前位置", i + 1));
        }
        notifyDataSetChanged();
    }

    /** 供外部（Fragment）读取条目数做 clamp 越界保护，不必暴露整个数据集 */
    public int itemCount() {
        return items.size();
    }

    /**
     * 只在复用池没有空闲视图时才会被调用。
     * inflate 传 attachToParent=false：挂载时机交给 RecyclerView，
     * 它会在自己的布局流程里 addView，提前挂上反而测量错乱。
     */
    @NonNull
    @Override
    public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_text, parent, false);
        return new RowHolder(view);
    }

    /**
     * 复用的核心步骤：每个滚入屏幕的 position 都会走这里刷新内容，
     * 所以这里只做"赋值"不做创建性操作，保证复用零分配。
     */
    @Override
    public void onBindViewHolder(@NonNull RowHolder holder, int position) {
        String text = items.get(position);
        holder.text.setText(text);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                // 点击发生在 bind 之后，位置可能因数据增删而漂移：
                // 实时取 getBindingAdapterPosition() 而非闭包里过期的 position
                listener.onRowClick(holder.getBindingAdapterPosition(), text);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /** 只缓存子 View 引用：findViewById 仅在创建 Holder 时执行一次，复用期零查找 */
    static class RowHolder extends RecyclerView.ViewHolder {
        final TextView text;

        RowHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.textRow);
        }
    }

    /** 包内可见的工具方法：从 RecyclerView 反查 Context，省去为取上下文单独传参 */
    static Context contextOf(RecyclerView rv) {
        return rv.getContext();
    }
}
