package com.smabo.dany_kun.circularlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by dany on 11/04/15.
 */
public class CircularLayout extends ViewGroup {

    private static final String TAG = CircularLayout.class.getCanonicalName();

    private int circleGravity = Gravity.LEFT | Gravity.TOP;
    private float offsetCenterX;
    private float offsetCenterY;
    private int start = 0;
    private int end = 0;
    private int radius = 1;
    private int incrementalAngle;

    @Nullable
    private View centerView;

    private enum CircleCenter {
        NONE, FIRST, RESOURCE
    }

    private CircleCenter circleCenter = CircleCenter.NONE;

    public CircularLayout(Context context) {
        super(context);
        init(context, null);
    }

    public CircularLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CircularLayout, 0, 0);

        int circleCenterTypeInt = 0;
        int centerId = -1;

        try {
            start = a.getInteger(R.styleable.CircularLayout_circle_start, 0);
            end = a.getInteger(R.styleable.CircularLayout_circle_end, 360);
            radius = a.getDimensionPixelSize(R.styleable.CircularLayout_circle_radius, 100);
            centerId = a.getResourceId(R.styleable.CircularLayout_circle_centerRes, -1);
            circleGravity = a.getInt(R.styleable.CircularLayout_circle_gravity, -1);
            circleCenterTypeInt = a.getInteger(R.styleable.CircularLayout_circle_centerType, 0);
        } finally {
            a.recycle();
        }

        defineCenterType(circleCenterTypeInt);

        //Inflate and set the circle center view from constructor
        if (circleCenter == CircleCenter.RESOURCE)
            inflateCenter(context, centerId);
    }

    private void inflateCenter(Context context, int centerId) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        centerView = layoutInflater.inflate(centerId, this, false);

        if (centerView == null) {
            throw new IllegalArgumentException("The resource set for the center is not valid. Is circle_centerRes attributes set?");
        }
        addView(centerView, 0);
    }

    private void defineCenterType(int circleCenterTypeInt) {
        switch (circleCenterTypeInt) {
            case 1:
                circleCenter = CircleCenter.FIRST;
                break;
            case 2:
                circleCenter = CircleCenter.RESOURCE;
                break;
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        //Hold a reference to the first view and define as the circle center
        if (circleCenter == CircleCenter.FIRST)
            centerView = getChildAt(0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;

        int childCount = getChildCount();
        int goneViewCount = getChildGoneViewCount();
        int measuredViewCount = childCount - goneViewCount;

        //No child to measure, return an empty view
        //TODO include case of fixed dimensions
        if (measuredViewCount == 0) {
            setMeasuredDimension(0, 0);
            return;
        }

        incrementalAngle = (end - start) / (measuredViewCount - excludedPointsNbr());

        int k = 0;
        for (int j = 0; j < childCount; j++) {
            View child = getChildAt(j);

            if (child.getVisibility() != GONE) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
                int childHeight = child.getMeasuredHeight();
                int childWidth = child.getMeasuredWidth();
                double childRelativeCenterX;
                double childRelativeCenterY;

                if (!isCenterView(child)) {
                    float angleRadian = (float) Math.toRadians(start + k * incrementalAngle);
                    k++;
                    childRelativeCenterX = radius * Math.cos(angleRadian);
                    childRelativeCenterY = radius * Math.sin(angleRadian);
                } else {
                    childRelativeCenterX = 0;
                    childRelativeCenterY = 0;
                }

                //Getting the actual frame for the container
                minX = (float) Math.min(minX, childRelativeCenterX - childWidth / 2);
                minY = (float) Math.min(minY, childRelativeCenterY - childHeight / 2);
                maxX = (float) Math.max(maxX, childRelativeCenterX + childWidth / 2);
                maxY = (float) Math.max(maxY, childRelativeCenterY + childHeight / 2);
            }
        }

        offsetCenterX = minX;
        offsetCenterY = minY;

        int desiredWidth = (int) (maxX - minX);
        int desiredHeight = (int) (maxY - minY);

        int layoutWidth = Utils.getDimension(widthMeasureSpec, desiredWidth);
        int layoutHeight = Utils.getDimension(heightMeasureSpec, desiredHeight);

        setMeasuredDimension(layoutWidth, layoutHeight);

        repositionWithGravity(layoutWidth, layoutHeight, desiredWidth, desiredHeight);

    }

    private int excludedPointsNbr() {
        return centerView == null ? 0 : 1;
    }

    private boolean isCenterView(View child) {
        return child != null && centerView != null && child.equals(centerView);
    }

    private void repositionWithGravity(int layoutWidth, int layoutHeight, int desiredWidth, int desiredHeight) {
        repositionX(layoutWidth, desiredWidth);
        repositionY(layoutHeight, desiredHeight);
    }

    private void repositionX(int layoutWidth, int desiredWidth) {
        switch (circleGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.CENTER_HORIZONTAL:
                offsetCenterX -= layoutWidth / 2 - desiredWidth / 2;
                break;
            case Gravity.RIGHT:
                offsetCenterX -= layoutWidth - desiredWidth;
                break;
        }
    }

    private void repositionY(int layoutHeight, int desiredHeight) {
        switch (circleGravity & Gravity.VERTICAL_GRAVITY_MASK) {
            case Gravity.CENTER_VERTICAL:
                offsetCenterY -= layoutHeight / 2 - desiredHeight / 2;
                break;
            case Gravity.BOTTOM:
                offsetCenterY -= layoutHeight - desiredHeight;
                break;
        }
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int childCount = getChildCount();

        int k = 0;
        for (int j = 0; j < childCount; j++) {
            View child = getChildAt(j);

            if (child.getVisibility() != GONE) {
                if (isCenterView(child)) {
                    int childPositionX = (int) (-child.getMeasuredWidth() / 2 - offsetCenterX);
                    int childPositionY = (int) (-child.getMeasuredHeight() / 2 - offsetCenterY);
                    child.layout(childPositionX, childPositionY, childPositionX + child.getMeasuredWidth(), childPositionY + child.getMeasuredHeight());

                } else {
                    double angleRadian = Math.toRadians(start + k * incrementalAngle);
                    int childPositionX = (int) (radius * Math.cos(angleRadian) - child.getMeasuredWidth() / 2 - offsetCenterX);
                    int childPositionY = (int) (radius * Math.sin(angleRadian) - child.getMeasuredHeight() / 2 - offsetCenterY);

                    child.layout(childPositionX, childPositionY, childPositionX + child.getMeasuredWidth(), childPositionY + child.getMeasuredHeight());

                    k++;
                }
            }
        }
    }

    private int getChildGoneViewCount() {
        int result = 0;
        int i = getChildCount();

        for (int j = 0; j < i; j++) {
            result = result + getChildAt(j).getVisibility() == GONE ? 0 : 1;
        }
        return result;
    }
}
