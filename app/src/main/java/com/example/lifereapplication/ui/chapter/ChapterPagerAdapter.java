package com.example.lifereapplication.ui.chapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.lifereapplication.ui.list.ListFragment;

import java.util.List;

/**
 * 章节适配器：ViewPager2 联动 Fragment 的标准实现。
 *
 * <p><b>为什么继承 {@link FragmentStateAdapter} 而不是 RecyclerView.Adapter：</b>
 * FragmentStateAdapter 内部把每个 Fragment 包成 StatefulAdapter 条目塞进
 * ViewPager2 的内部 RecyclerView：</p>
 * <ul>
 *     <li>不可见页（超出 offscreenPageLimit）→ {@code onDestroyView} 但保留
 *     {@code FragmentState}（含 our 滚动位置等 savedState），滑回时视图重建、
 *     状态自动恢复；</li>
 *     <li>彻底不可见（更远）→ removeFragment，只留 Bundle，内存占用有界；</li>
 *     <li>这正是「状态保持」需求在 ViewPager2 里的官方答案，与我们的
 *     ScrollStateKeeper 双缓存互补。</li>
 * </ul>
 *
 * <p><b>Arrays.asList 的使用方式（第 2 项需求）：</b>章节数据源用
 * {@code Arrays.asList(new ExampleItem(...), ...)} 一步构造固定列表，
 * 由本适配器按 index 提供标题与 Fragment。</p>
 */
public class ChapterPagerAdapter extends FragmentStateAdapter {

    /** 章节数据源：Arrays.asList 一步构造（演示需求的 Chapter 适配器用法） */
    public static final List<ExampleItem> CHAPTERS = java.util.Arrays.asList(
            new ExampleItem("第一章 · 线性列表", "100 条长列表，滚动位置精确记忆"),
            new ExampleItem("第二章 · 双列网格", "网格布局下的状态保持"),
            new ExampleItem("第三章 · 三列网格", "宽屏信息密度与边界钳制"),
            new ExampleItem("第四章 · 混合复习", "回到线性列表，验证前面章节状态")
    );

    /** 每章对应的列表标识（ScrollStateKeeper 的 key）与 span；0 = 按屏宽自适应列数 */
    private static final String[] LIST_IDS = {
            "chapter.1", "chapter.2", "chapter.3", "chapter.4"
    };
    private static final int[] SPANS = {1, 0, 0, 1};

    public ChapterPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // 复用 ListFragment：每章是独立的 listId，滚动状态互不串扰
        return ListFragment.newInstance(LIST_IDS[position], SPANS[position]);
    }

    @Override
    public int getItemCount() {
        return CHAPTERS.size();
    }

    /** 供 TabLayout 联动取标题 */
    public String pageTitle(int position) {
        return CHAPTERS.get(position).getTitle().split(" · ")[0];
    }

    @Override
    public long getItemId(int position) {
        return position; // 章节固定不变，稳定 id
    }

    @Override
    public boolean containsItem(long itemId) {
        return itemId >= 0 && itemId < CHAPTERS.size();
    }
}
