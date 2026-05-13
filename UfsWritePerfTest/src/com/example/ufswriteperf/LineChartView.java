package com.example.ufswriteperf;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LineChartView extends View {

    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<DataPoint> points = new ArrayList<DataPoint>();

    public LineChartView(Context context) {
        super(context);
        init();
    }

    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        axisPaint.setColor(Color.GRAY);
        axisPaint.setStrokeWidth(3f);

        linePaint.setColor(Color.parseColor("#1E88E5"));
        linePaint.setStrokeWidth(4f);

        textPaint.setColor(Color.DKGRAY);
        textPaint.setTextSize(28f);
    }

    public void setPoints(List<DataPoint> points) {
        this.points = points;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        int left = 90;
        int right = 40;
        int top = 40;
        int bottom = 90;

        canvas.drawColor(Color.WHITE);
        canvas.drawLine(left, h - bottom, w - right, h - bottom, axisPaint);
        canvas.drawLine(left, top, left, h - bottom, axisPaint);

        if (points == null || points.size() < 2) {
            canvas.drawText("暂无数据", left + 20, top + 40, textPaint);
            return;
        }

        long maxMs = Math.max(1L, points.get(points.size() - 1).elapsedMs);
        float maxBytes = points.get(points.size() - 1).bytes;
        float plotW = (w - right - left);
        float plotH = (h - bottom - top);

        float lastX = -1f;
        float lastY = -1f;
        for (DataPoint point : points) {
            float x = left + ((float) point.elapsedMs / (float) maxMs) * plotW;
            float y = h - bottom - (point.bytes / maxBytes) * plotH;

            if (lastX >= 0) {
                canvas.drawLine(lastX, lastY, x, y, linePaint);
            }
            lastX = x;
            lastY = y;
        }

        canvas.drawText("X: 累积耗时 (ms)", left, h - 20, textPaint);
        canvas.drawText("Y: 累积写入数据量 (Bytes)", left + 10, top + 30, textPaint);
        canvas.drawText(String.format(Locale.US, "末点: %d bytes / %d ms",
                points.get(points.size() - 1).bytes,
                points.get(points.size() - 1).elapsedMs), left + 10, top + 65, textPaint);
    }

    public static class DataPoint {
        public final int bytes;
        public final long elapsedMs;

        public DataPoint(int bytes, long elapsedMs) {
            this.bytes = bytes;
            this.elapsedMs = elapsedMs;
        }
    }
}
