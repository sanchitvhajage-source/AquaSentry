package com.example.floodalert;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

public class FloodLevelView extends View {

    private Paint groundPaint, stickFigurePaint, carPaint, waterPaint, textPaint, scalePaint;
    private float waterLevelFeet = 0f; // The current water level to draw
    private ValueAnimator animator;

    // Constants for drawing
    private static final float MAX_DISPLAY_FEET = 5f;

    public FloodLevelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    private void initPaints() {
        groundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        groundPaint.setColor(Color.parseColor("#30363D"));
        groundPaint.setStyle(Paint.Style.FILL);

        stickFigurePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        stickFigurePaint.setColor(Color.parseColor("#E6EDF3"));
        stickFigurePaint.setStrokeWidth(8f);
        stickFigurePaint.setStrokeCap(Paint.Cap.ROUND);

        carPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        carPaint.setColor(Color.parseColor("#7D8590"));
        carPaint.setStyle(Paint.Style.FILL);

        waterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        waterPaint.setColor(Color.parseColor("#388BFD"));
        waterPaint.setAlpha(150); // Make it slightly transparent
        waterPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#E6EDF3"));
        textPaint.setTextSize(40f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        scalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scalePaint.setColor(Color.parseColor("#7D8590"));
        scalePaint.setTextSize(24f);
        scalePaint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // --- Draw Ground ---
        float groundHeight = height * 0.9f;
        canvas.drawRect(0, groundHeight, width, height, groundPaint);

        // --- Draw Stick Figure ---
        float figureX = width * 0.25f;
        float figureBaseY = groundHeight - 1;
        canvas.drawCircle(figureX, figureBaseY - 140, 30, stickFigurePaint); // Head
        canvas.drawLine(figureX, figureBaseY - 110, figureX, figureBaseY - 50, stickFigurePaint); // Body
        canvas.drawLine(figureX, figureBaseY, figureX - 30, figureBaseY - 50, stickFigurePaint); // Left Leg
        canvas.drawLine(figureX, figureBaseY, figureX + 30, figureBaseY - 50, stickFigurePaint); // Right Leg
        canvas.drawLine(figureX - 25, figureBaseY - 90, figureX + 25, figureBaseY - 90, stickFigurePaint); // Arms

        // --- Draw Car ---
        float carY = groundHeight - 40;
        canvas.drawRect(width * 0.5f, carY, width * 0.85f, groundHeight, carPaint);
        canvas.drawRect(width * 0.55f, carY - 40, width * 0.8f, carY, carPaint);
        canvas.drawCircle(width * 0.58f, groundHeight, 15, stickFigurePaint);
        canvas.drawCircle(width * 0.77f, groundHeight, 15, stickFigurePaint);

        // --- Draw Water Level ---
        float effectiveGroundHeight = groundHeight * 0.8f;
        float waterHeightPx = (waterLevelFeet / MAX_DISPLAY_FEET) * effectiveGroundHeight;
        if (waterHeightPx > 0) {
            canvas.drawRect(0, groundHeight - waterHeightPx, width, groundHeight, waterPaint);
        }

        // --- Draw Scale ---
        float scaleX = width * 0.9f;
        for (int i = 1; i <= 4; i++) {
            float lineY = groundHeight - (i / MAX_DISPLAY_FEET) * effectiveGroundHeight;
            canvas.drawLine(scaleX, lineY, scaleX + 20, lineY, scalePaint);
            canvas.drawText(i + " ft", scaleX + 30, lineY + 8, scalePaint);
        }

        // --- Draw Text Display ---
        String waterLevelText = String.format("Current Risk: %.1f ft", waterLevelFeet);
        canvas.drawText(waterLevelText, width / 2f, height * 0.15f, textPaint);
    }

    /**
     * The main method to update the flood level. Animates the water rising.
     * @param newLevelInFeet The new flood level to display.
     */
    public void setFloodLevel(float newLevelInFeet) {
        if (animator != null) {
            animator.cancel();
        }

        // Ensure the new level is within the display bounds (0 to 5 feet)
        float clampedLevel = Math.max(0f, Math.min(newLevelInFeet, MAX_DISPLAY_FEET));

        animator = ValueAnimator.ofFloat(this.waterLevelFeet, clampedLevel);
        animator.setDuration(1500); // 1.5 seconds animation
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            this.waterLevelFeet = (float) animation.getAnimatedValue();
            invalidate(); // Redraw the view on each animation frame
        });
        animator.start();
    }
}