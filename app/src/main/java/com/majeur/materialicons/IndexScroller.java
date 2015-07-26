package com.majeur.materialicons;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

public class IndexScroller {

    private float mIndexbarWidth;
    private float mIndexbarMargin;
    private float mPreviewPadding;
    private float mDensity;
    private float mScaledDensity;
    private float mAlphaRate;
    private int mState = STATE_HIDDEN;
    private int mListViewWidth;
    private int mListViewHeight;
    private int mCurrentSection = -1;
    private boolean mIsIndexing = false;
    private RecyclerView recyclerView = null;
    public List<String> mSections = new ArrayList<>();
    public List<Integer> mSectionPosition = new ArrayList<>();
    private RectF mIndexbarRect;

    private Handler mHandler = new Handler();

    private static final int STATE_HIDDEN = 0;
    private static final int STATE_SHOWING = 1;
    private static final int STATE_SHOWN = 2;
    private static final int STATE_HIDING = 3;

    private Paint mIndexBarPaint = new Paint();
    private Paint mPreviewPaint = new Paint();
    private Paint mPreviewTextPaint = new Paint();
    private Paint mIndexPaint = new Paint();
    private RectF mPreviewRect = new RectF();

    public IndexScroller(Context context, RecyclerView lv) {
        mDensity = context.getResources().getDisplayMetrics().density;
        mScaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        recyclerView = lv;

        mIndexbarWidth = 20 * mDensity;
        mIndexbarMargin = 10 * mDensity;
        mPreviewPadding = 5 * mDensity;
    }

    public void draw(Canvas canvas) {
        if (mState == STATE_HIDDEN)
            return;

        // mAlphaRate determines the rate of opacity
        mIndexBarPaint.setColor(Color.BLACK);
        mIndexBarPaint.setAlpha((int) (64 * mAlphaRate));
        mIndexBarPaint.setAntiAlias(true);
        canvas.drawRoundRect(mIndexbarRect, 5 * mDensity, 5 * mDensity, mIndexBarPaint);

        if (mSections != null && mSections.size() > 0) {
            // Preview is shown when mCurrentSection is set
            if (mCurrentSection >= 0) {
                mPreviewPaint.setColor(Color.BLACK);
                mPreviewPaint.setAlpha(96);
                mPreviewPaint.setAntiAlias(true);
                mPreviewPaint.setShadowLayer(3, 0, 0, Color.argb(64, 0, 0, 0));

                mPreviewTextPaint.setColor(Color.WHITE);
                mPreviewTextPaint.setAntiAlias(true);
                mPreviewTextPaint.setTextSize(50 * mScaledDensity);

                float previewTextWidth = mPreviewTextPaint.measureText(mSections.get(mCurrentSection));
                float previewSize = 2 * mPreviewPadding + mPreviewTextPaint.descent() - mPreviewTextPaint.ascent();
                mPreviewRect.set((mListViewWidth - previewSize) / 2
                        , (mListViewHeight - previewSize) / 2
                        , (mListViewWidth - previewSize) / 2 + previewSize
                        , (mListViewHeight - previewSize) / 2 + previewSize);

                canvas.drawRoundRect(mPreviewRect, 5 * mDensity, 5 * mDensity, mPreviewPaint);
                canvas.drawText(mSections.get(mCurrentSection), mPreviewRect.left + (previewSize - previewTextWidth) / 2 - 1
                        , mPreviewRect.top + mPreviewPadding - mPreviewTextPaint.ascent() + 1, mPreviewTextPaint);
            }

            mIndexPaint.setColor(Color.WHITE);
            mIndexPaint.setAlpha((int) (255 * mAlphaRate));
            mIndexPaint.setAntiAlias(true);
            mIndexPaint.setTextSize(12 * mScaledDensity);

            float sectionHeight = (mIndexbarRect.height() - 2 * mIndexbarMargin) / mSections.size();
            float paddingTop = (sectionHeight - (mIndexPaint.descent() - mIndexPaint.ascent())) / 2;
            for (int i = 0; i < mSections.size(); i++) {
                float paddingLeft = (mIndexbarWidth - mIndexPaint.measureText(mSections.get(i))) / 2;
                canvas.drawText(mSections.get(i), mIndexbarRect.left + paddingLeft
                        , mIndexbarRect.top + mIndexbarMargin + sectionHeight * i + paddingTop - mIndexPaint.ascent(), mIndexPaint);
            }
        }
    }


    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:

                // If down event occurs inside index bar region, start indexing
                if (mState != STATE_HIDDEN && contains(ev.getX(), ev.getY())) {
                    setState(STATE_SHOWN);

                    // It demonstrates that the motion event started from index bar
                    mIsIndexing = true;
                    // Determine which section the point is in, and move the list to that section
                    mCurrentSection = getSectionByPoint(ev.getY());
                    recyclerView.scrollToPosition(mSectionPosition.get(mCurrentSection));
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsIndexing) {
                    // If this event moves inside index bar
                    if (contains(ev.getX(), ev.getY())) {
                        // Determine which section the point is in, and move the list to that section
                        mCurrentSection = getSectionByPoint(ev.getY());
                        recyclerView.scrollToPosition(mSectionPosition.get(mCurrentSection));
                    }
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsIndexing) {
                    mIsIndexing = false;
                    mCurrentSection = -1;
                }
                if (mState == STATE_SHOWN) {
                    setState(STATE_HIDING);
                }
                break;
        }
        return false;
    }

    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        mListViewWidth = w;
        mListViewHeight = h;
        mIndexbarRect = new RectF(w - mIndexbarMargin - mIndexbarWidth
                , mIndexbarMargin
                , w - mIndexbarMargin
                , h - mIndexbarMargin);
    }

    public void show() {
        if (mState == STATE_HIDDEN)
            setState(STATE_SHOWING);
        else if (mState == STATE_HIDING)
            setState(STATE_HIDING);
    }

    public void hide() {
        if (mState == STATE_SHOWN)
            setState(STATE_HIDING);
    }

    private void setState(int state) {
        if (state < STATE_HIDDEN || state > STATE_HIDING)
            return;

        mState = state;
        switch (mState) {
            case STATE_HIDDEN:
                // Cancel any fade effect
                mHandler.removeCallbacks(mFadeRunnable);
                break;
            case STATE_SHOWING:
                // Start to fade in
                mAlphaRate = 0;
                fade(0);
                break;
            case STATE_SHOWN:
                // Cancel any fade effect
                mHandler.removeCallbacks(mFadeRunnable);
                break;
            case STATE_HIDING:
                // Start to fade out after three seconds
                mAlphaRate = 1;
                fade(3000);
                break;
        }
    }

    public boolean contains(float x, float y) {
        // Determine if the point is in index bar region, which includes the right margin of the bar
        return (x >= mIndexbarRect.left && y >= mIndexbarRect.top && y <= mIndexbarRect.top + mIndexbarRect.height());
    }

    private int getSectionByPoint(float y) {
        if (mSections == null || mSections.size() == 0)
            return 0;
        if (y < mIndexbarRect.top + mIndexbarMargin)
            return 0;
        if (y >= mIndexbarRect.top + mIndexbarRect.height() - mIndexbarMargin)
            return mSections.size() - 1;
        return (int) ((y - mIndexbarRect.top - mIndexbarMargin) / ((mIndexbarRect.height() - 2 * mIndexbarMargin) / mSections.size()));
    }

    private void fade(long delay) {
        mHandler.postDelayed(mFadeRunnable, delay);
    }

    private Runnable mFadeRunnable = new Runnable() {
        @Override
        public void run() {
            switch (mState) {
                case STATE_SHOWING:
                    // Fade in effect
                    mAlphaRate += (1 - mAlphaRate) * 0.2;
                    if (mAlphaRate > 0.9) {
                        mAlphaRate = 1;
                        setState(STATE_SHOWN);
                    }

                    recyclerView.invalidate();
                    fade(10);
                    break;
                case STATE_SHOWN:
                    // If no action, hide automatically
                    setState(STATE_HIDING);
                    break;
                case STATE_HIDING:
                    // Fade out effect
                    mAlphaRate -= mAlphaRate * 0.2;
                    if (mAlphaRate < 0.1) {
                        mAlphaRate = 0;
                        setState(STATE_HIDDEN);
                    }

                    recyclerView.invalidate();
                    fade(10);
                    break;
            }
        }
    };

    public void notifyChanges(List<String> sectionNames, List<Integer> sectionPosition) {

        // Pre-calculate and pass your section header and position
        mSections = sectionNames;
        mSectionPosition = sectionPosition;

    }
}