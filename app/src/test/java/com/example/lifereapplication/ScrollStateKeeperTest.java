package com.example.lifereapplication;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import com.example.lifereapplication.util.ScrollStateKeeper;

import org.junit.Test;

/**
 * ScrollStateKeeper 纯逻辑（边界钳制）单元测试。
 * 覆盖：正常恢复 / 空列表 / 数据变少 / 负值 / 超大偏移 / null 输入。
 */
public class ScrollStateKeeperTest {

    @Test
    public void clamp_normalState_keptAsIs() {
        int[] result = ScrollStateKeeper.clamp(new int[]{33, 40}, 100, 2000);
        assertArrayEquals(new int[]{33, 40}, result);
    }

    @Test
    public void clamp_emptyList_returnsNull() {
        assertNull(ScrollStateKeeper.clamp(new int[]{10, 0}, 0, 2000));
    }

    @Test
    public void clamp_nullState_returnsNull() {
        assertNull(ScrollStateKeeper.clamp(null, 100, 2000));
    }

    @Test
    public void clamp_dataShrunk_positionClampedToLastItem() {
        // 曾滑到第 80 条，数据被删到只剩 10 条 → 钳制到第 10 条（index 9）
        int[] result = ScrollStateKeeper.clamp(new int[]{80, 120}, 10, 2000);
        assertArrayEquals(new int[]{9, 120}, result);
    }

    @Test
    public void clamp_negativePosition_fixedToZero() {
        int[] result = ScrollStateKeeper.clamp(new int[]{-5, 0}, 10, 2000);
        assertArrayEquals(new int[]{0, 0}, result);
    }

    @Test
    public void clamp_negativeOffset_fixedToZero() {
        int[] result = ScrollStateKeeper.clamp(new int[]{3, -40}, 10, 2000);
        assertArrayEquals(new int[]{3, 0}, result);
    }

    @Test
    public void clamp_offsetLargerThanViewport_resetToZero() {
        // 横竖屏切换后列表项尺寸变化：偏移超 viewport → 归零退化
        int[] result = ScrollStateKeeper.clamp(new int[]{50, 9999}, 100, 2000);
        assertArrayEquals(new int[]{50, 0}, result);
    }

    @Test
    public void clamp_restoreAtExactOneThird_withinFivePercent() {
        // 需求验收：滑到约 1/3 处（100 条 → index 33），恢复误差 ≤ 5%（≤5 条）
        int saved = 100 / 3;
        int[] result = ScrollStateKeeper.clamp(new int[]{saved, 40}, 100, 2000);
        int restored = result[0];
        int error = Math.abs(restored - saved);
        org.junit.Assert.assertTrue("误差条数 " + error + " 超过 5%",
                error <= (int) Math.ceil(100 * 0.05));
    }
}
