package com.example.lifereapplication;

import static org.junit.Assert.assertEquals;

import com.example.lifereapplication.util.toast.ToastThrottle;

import org.junit.Test;

/**
 * ToastThrottle 防抖节流单元测试：
 * 覆盖 首次放行 / 窗口内静默 / 三次上限 / 窗口重置。
 */
public class ToastThrottleTest {

    @Test
    public void firstClick_passes() {
        ToastThrottle throttle = new ToastThrottle();
        assertEquals(ToastThrottle.Result.PASS, throttle.record(1000L));
    }

    @Test
    public void secondClick_withinWindow_suppressed() {
        ToastThrottle throttle = new ToastThrottle();
        throttle.record(1000L);
        assertEquals(ToastThrottle.Result.SUPPRESSED, throttle.record(1500L));
    }

    @Test
    public void thirdClick_withinWindow_suppressed() {
        ToastThrottle throttle = new ToastThrottle();
        throttle.record(1000L);
        throttle.record(1200L);
        assertEquals(ToastThrottle.Result.SUPPRESSED, throttle.record(1400L));
    }

    @Test
    public void fourthClick_withinWindow_overLimit() {
        ToastThrottle throttle = new ToastThrottle();
        throttle.record(1000L);
        throttle.record(1100L);
        throttle.record(1200L);
        assertEquals(ToastThrottle.Result.OVER_LIMIT, throttle.record(1300L));
    }

    @Test
    public void windowReset_allowsAgain() {
        ToastThrottle throttle = new ToastThrottle();
        throttle.record(1000L);
        throttle.record(1100L);
        throttle.record(1200L);
        throttle.record(1300L); // OVER_LIMIT
        // 3 秒后新窗口：重新放行
        assertEquals(ToastThrottle.Result.PASS, throttle.record(1000L + 3000L));
    }

    @Test
    public void boundary_exactlyWindowEnd_resets() {
        ToastThrottle throttle = new ToastThrottle();
        throttle.record(1000L);
        // now - last == window → 属于新窗口
        assertEquals(ToastThrottle.Result.PASS, throttle.record(1000L + ToastThrottle.WINDOW_MS));
    }

    @Test
    public void reset_clearsCounters() {
        ToastThrottle throttle = new ToastThrottle();
        throttle.record(1000L);
        throttle.record(1100L);
        throttle.reset();
        assertEquals(0, throttle.clicksInWindow());
    }
}
