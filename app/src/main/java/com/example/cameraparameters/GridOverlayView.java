package com.example.cameraparameters;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class GridOverlayView extends View {

    private Paint paint = new Paint();

    public GridOverlayView(Context context) {
        super(context);
        init();
    }

    public GridOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GridOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setColor(0x80FFFFFF); // Color blanco con algo de transparencia
        paint.setStrokeWidth(2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Dibujar líneas verticales
        canvas.drawLine(width / 3, 0, width / 3, height, paint);
        canvas.drawLine(2 * width / 3, 0, 2 * width / 3, height, paint);

        // Dibujar líneas horizontales
        canvas.drawLine(0, height / 3, width, height / 3, paint);
        canvas.drawLine(0, 2 * height / 3, width, 2 * height / 3, paint);
    }
}