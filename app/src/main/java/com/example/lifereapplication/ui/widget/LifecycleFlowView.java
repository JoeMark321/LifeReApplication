package com.example.lifereapplication.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.lifereapplication.R;

/**
 * 生命周期流程图（自定义 View）。
 *
 * <p><b>为什么用自定义 View：</b>七个回调 + 三种生命周期范围 + 跳转路径，
 * 用静态图片既不能高亮当前状态，也无法跟随主题/深色模式；用一堆 TextView
 * 又难以画出箭头与连线。自定义 View 可以按“当前阶段”实时重绘，
 * 把抽象的生命周期时序变成一眼能看懂的图形。</p>
 *
 * <p>绘制策略（无嵌套布局、无 RecyclerView，只有 3 行 Canvas 绘制）：</p>
 * <ol>
 *     <li>按固定行数布局 chips，行内用箭头连接；</li>
 *     <li>当前阶段高亮为主色，已走过阶段为浅绿，未走到为灰色；</li>
 *     <li>行与行之间画向下箭头，表示流程方向；</li>
 *     <li>只在 {@link #setCurrentStage(String)} 变化时 invalidate()，
 *         不主动请求布局，避免滚动时额外测量开销。</li>
 * </ol>
 */
public class LifecycleFlowView extends View {

    /**
     * 三行排布：一行最多 3 个 chip——再多单枚宽度会被压缩到不可读；
     * 分组同时暗合语义（前台三连 / 后台与回返 / 终结）。
     * 数组顺序即全局序号，"已走过"的着色按该顺序跨行累计。
     */
    private static final String[][] ROWS = {
            {"onCreate", "onStart", "onResume"},
            {"onPause", "onStop", "onRestart"},
            {"onDestroy"}
    };

    /** 首帧默认高亮 onCreate：视图被宿主驱动前先呈现有意义的初始态 */
    private static final String CURRENT = "onCreate";

    // 三支画笔都开抗锯齿：圆角矩形与文字在关闭抗锯齿时边缘会有明显毛边
    private final Paint chipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // 小号 chip 文字（12sp）居中 + 伪粗体，保证可读性
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final float density;    // 屏幕密度：尺寸一律按 dp 设计、运行时换算成 px
    private final float chipHeight; // 40dp：兼顾文字可读性与触摸热区
    private final float chipGap;    // 10dp：行内 chip 间距（需容纳右向箭头）
    private final float rowGap;     // 22dp：行距（需容纳下向箭头）

    private String currentStage = CURRENT;

    /** 链式收敛到三参构造：代码创建与 XML 解析（attrs）共用同一初始化路径 */
    public LifecycleFlowView(Context context) {
        this(context, null);
    }

    public LifecycleFlowView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LifecycleFlowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // 箭头颜色只需解析一次；chip/文字颜色随状态变化，留到绘制时解析
        density = getResources().getDisplayMetrics().density;
        chipHeight = 40 * density;
        chipGap = 10 * density;
        rowGap = 22 * density;

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(12 * density);
        textPaint.setFakeBoldText(true);

        arrowPaint.setStyle(Paint.Style.FILL);
        arrowPaint.setColor(getContext().getColor(R.color.stage_arrow));
    }

    /**
     * 设置当前阶段，重绘高亮。
     * 为什么先比较再 invalidate：生命周期回调可能快速连续触发
     * （onResume→onPause），值未变时跳过重绘，省掉无效帧。
     */
    public void setCurrentStage(String stage) {
        if (stage != null && !stage.equals(currentStage)) {
            currentStage = stage;
            invalidate();
        }
    }

    /** 供宿主查询当前高亮阶段（调试或状态回显用） */
    public String getCurrentStage() {
        return currentStage;
    }

    /**
     * 宽度服从父级给定（铺满可用宽），高度按行数与行距自算——
     * 布局 XML 无需写死高度，行数增减时视图自适应。
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = (int) (ROWS.length * chipHeight + (ROWS.length - 1) * rowGap)
                + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(width, height);
    }

    /**
     * 全量重绘。为什么不做脏矩形/分层：chip 总共 7 枚，整图重绘的
     * 开销可以忽略，简单实现就是对的实现。
     * 每行 chip 等宽 =（可用宽 - 行内箭头空间）/ chip 数，
     * 行与行 chip 数不同也能保持对齐的网格感。
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left = getPaddingLeft();
        float top = getPaddingTop();
        float availableWidth = getWidth() - getPaddingLeft() - getPaddingRight();

        int currentIndex = indexOf(currentStage);

        for (int row = 0; row < ROWS.length; row++) {
            String[] chips = ROWS[row];
            int chipCount = chips.length;
            float arrowSpace = chipGap * (chipCount - 1);
            float chipWidth = (availableWidth - arrowSpace) / chipCount;

            for (int i = 0; i < chipCount; i++) {
                String stage = chips[i];
                int stageIndex = indexOf(stage);
                float chipLeft = left + i * (chipWidth + chipGap);
                float chipTop = top + row * (chipHeight + rowGap);

                drawChip(canvas, chipLeft, chipTop, chipWidth, stage, stageIndex == currentIndex,
                        stageIndex < currentIndex);

                // 行内箭头
                if (i < chipCount - 1) {
                    drawArrow(canvas,
                            chipLeft + chipWidth + 2 * density,
                            chipTop + chipHeight / 2f,
                            chipGap - 4 * density);
                }
            }

            // 行间向下箭头（最后一行不画）
            if (row < ROWS.length - 1) {
                float centerX = left + availableWidth / 2f;
                float arrowTop = top + row * (chipHeight + rowGap) + chipHeight + 4 * density;
                drawDownArrow(canvas, centerX, arrowTop, rowGap - 8 * density);
            }
        }
    }

    /**
     * 绘制一枚阶段 chip：当前/已走过/未走到三态分别取主题色，
     * 12dp 圆角与整体卡片风格保持一致。
     */
    private void drawChip(Canvas canvas, float left, float top, float width,
                          String stage, boolean isCurrent, boolean isPassed) {
        float right = left + width;
        float bottom = top + chipHeight;
        float radius = 12 * density;

        if (isCurrent) {
            chipPaint.setColor(color(R.color.stage_current));
            textPaint.setColor(color(R.color.stage_current_text));
        } else if (isPassed) {
            chipPaint.setColor(color(R.color.stage_passed));
            textPaint.setColor(color(R.color.stage_passed_text));
        } else {
            chipPaint.setColor(color(R.color.stage_idle));
            textPaint.setColor(color(R.color.stage_idle_text));
        }

        canvas.drawRoundRect(left, top, right, bottom, radius, radius, chipPaint);

        // 垂直居中绘制文本：以字形边界中点回推基线，能抵消字体上下留白的
        // 不对称；直接拿 chipHeight/2 当基线会让文字整体偏上
        float textY = top + chipHeight / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(stage, left + width / 2f, textY, textPaint);
    }

    /** 行内右向箭头：实心三角直接用 Path 画，省去 drawable 资源与状态维护 */
    private void drawArrow(Canvas canvas, float startX, float centerY, float length) {
        float height = 6 * density; // 6dp 箭头高度：与 chip 文字体量匹配，过大喧宾夺主
        Path path = new Path();
        path.moveTo(startX, centerY - height);
        path.lineTo(startX + length, centerY);
        path.lineTo(startX, centerY + height);
        path.close();
        canvas.drawPath(path, arrowPaint);
    }

    /** 行间下向箭头：表达"onResume 之后流程换行进入 onPause"的接续关系 */
    private void drawDownArrow(Canvas canvas, float centerX, float startY, float length) {
        float width = 6 * density;
        Path path = new Path();
        path.moveTo(centerX - width, startY);
        path.lineTo(centerX + width, startY);
        path.lineTo(centerX, startY + length);
        path.close();
        canvas.drawPath(path, arrowPaint);
    }

    /**
     * 阶段名 → 全局序号（按 ROWS 排布跨行累计，决定"已走过"着色的先后）。
     * 返回 -1 表示图中不存在该阶段，绘制时自然退化为全部"未走到"灰态。
     */
    private int indexOf(String stage) {
        int index = 0;
        for (String[] row : ROWS) {
            for (String chip : row) {
                if (chip.equals(stage)) {
                    return index;
                }
                index++;
            }
        }
        return -1;
    }

    /** 按当前 Context 的主题解析颜色，保证与宿主页面的配色一致 */
    private int color(int resId) {
        return getContext().getColor(resId);
    }
}
