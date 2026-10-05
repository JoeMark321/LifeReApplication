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

    private static final String[][] ROWS = {
            {"onCreate", "onStart", "onResume"},
            {"onPause", "onStop", "onRestart"},
            {"onDestroy"}
    };

    private static final String CURRENT = "onCreate";

    private final Paint chipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final float density;
    private final float chipHeight;
    private final float chipGap;
    private final float rowGap;

    private String currentStage = CURRENT;

    public LifecycleFlowView(Context context) {
        this(context, null);
    }

    public LifecycleFlowView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LifecycleFlowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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

    /** 设置当前阶段，重绘高亮 */
    public void setCurrentStage(String stage) {
        if (stage != null && !stage.equals(currentStage)) {
            currentStage = stage;
            invalidate();
        }
    }

    public String getCurrentStage() {
        return currentStage;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = (int) (ROWS.length * chipHeight + (ROWS.length - 1) * rowGap)
                + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(width, height);
    }

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

        // 垂直居中绘制文本
        float textY = top + chipHeight / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(stage, left + width / 2f, textY, textPaint);
    }

    private void drawArrow(Canvas canvas, float startX, float centerY, float length) {
        float height = 6 * density;
        Path path = new Path();
        path.moveTo(startX, centerY - height);
        path.lineTo(startX + length, centerY);
        path.lineTo(startX, centerY + height);
        path.close();
        canvas.drawPath(path, arrowPaint);
    }

    private void drawDownArrow(Canvas canvas, float centerX, float startY, float length) {
        float width = 6 * density;
        Path path = new Path();
        path.moveTo(centerX - width, startY);
        path.lineTo(centerX + width, startY);
        path.lineTo(centerX, startY + length);
        path.close();
        canvas.drawPath(path, arrowPaint);
    }

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

    private int color(int resId) {
        return getContext().getColor(resId);
    }
}
