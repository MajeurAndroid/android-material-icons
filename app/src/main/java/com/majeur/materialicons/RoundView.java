package com.majeur.materialicons;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.View;
import android.widget.GridView;

public class RoundView extends View {

    private Paint paint;
    private Paint checkedPaint;
    private boolean checked = false;

    public RoundView(Context context) {
        super(context);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        checkedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        checkedPaint.setColor(Color.parseColor("#FF808080"));

        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90, getResources().getDisplayMetrics());
        GridView.LayoutParams layoutParams = new GridView.LayoutParams(size, size);
        setLayoutParams(layoutParams);
    }

    public void setRoundColor(int color) {
        paint.setColor(color);
        invalidate();
    }

    public int getRoundColor() {
        return paint.getColor();
    }


    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int height = getHeight();
        int width = getWidth();

        if (checked)
            canvas.drawCircle(width / 2, height / 2, (int) (width * 0.410), checkedPaint);

        canvas.drawCircle(width / 2, height / 2, (int) (width * 0.375), paint);
    }
}
