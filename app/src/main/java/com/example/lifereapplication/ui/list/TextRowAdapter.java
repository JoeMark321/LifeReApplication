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
 * <p>ViewHolder 只持有行 View 与文本引用，复用时零分配；
 * 文本在 bind 时生成（演示用），真实业务应来自数据源。</p>
 */
public class TextRowAdapter extends RecyclerView.Adapter<TextRowAdapter.RowHolder> {

    private final List<String> items = new ArrayList<>();
    private final OnRowClickListener listener;

    public interface OnRowClickListener {
        void onRowClick(int position, String text);
    }

    public TextRowAdapter(OnRowClickListener listener) {
        this.listener = listener;
    }

    /** 生成 count 条模拟数据 */
    public void submitCount(int count) {
        items.clear();
        for (int i = 0; i < count; i++) {
            items.add(String.format(Locale.CHINA, "条目 #%03d —— 滑动后退出再进入，会精确回到当前位置", i + 1));
        }
        notifyDataSetChanged();
    }

    public int itemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_text, parent, false);
        return new RowHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RowHolder holder, int position) {
        String text = items.get(position);
        holder.text.setText(text);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRowClick(holder.getBindingAdapterPosition(), text);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RowHolder extends RecyclerView.ViewHolder {
        final TextView text;

        RowHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.textRow);
        }
    }

    /** 供 Activity 提示用的上下文快捷方法 */
    static Context contextOf(RecyclerView rv) {
        return rv.getContext();
    }
}
