package com.smabo.dany_kun.circularlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.graphics.RectF;
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
    private static final int DEFAULT_RADIUS_PX = 100;

    private int circleGravity = Gravity.LEFT | Gravity.TOP;
    private PointF centerPosition = new PointF();
    private int start = 0;
    private int end = 0;
    private int radius = 1;
    private int incrementalAngle;
    private boolean excludeLast = false;

    private View centerView;
    private CircleCenterType circleCenterType = CircleCenterType.NONE;

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
            radius = a.getDimensionPixelSize(R.styleable.CircularLayout_circle_radius, DEFAULT_RADIUS_PX);
            centerId = a.getResourceId(R.styleable.CircularLayout_circle_centerRes, -1);
            circleGravity = a.getInt(R.styleable.CircularLayout_circle_gravity, -1);
            circleCenterTypeInt = a.getInteger(R.styleable.CircularLayout_circle_centerType, 0);
            excludeLast = a.getBoolean(R.styleable.CircularLayout_circle_exclude_last, false);
        } finally {
            a.recycle();
        }

        defineCenterType(circleCenterTypeInt);

        //Inflate and set the circle center view from constructor
        if (circleCenterType == CircleCenterType.RESOURCE)
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
                circleCenterType = CircleCenterType.FIRST;
                break;
            case 2:
                circleCenterType = CircleCenterType.RESOURCE;
                break;
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        //Hold a reference to the first view and define as the circle center
        if (circleCenterType == CircleCenterType.FIRST)
            centerView = getChildAt(0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int childCount = getChildCount();
        int goneViewCount = getChildGoneViewCount();
        int measuredViewCount = childCount - goneViewCount;

        //No child to measure, return the view as a point
        //TODO include case of fixed dimensions
        if (measuredViewCount == 0) {
            setMeasuredDimension(0, 0);
            return;
        }

        incrementalAngle = (end - start) / (measuredViewCount - excludedPointsNbr());

        RectF viewRect = computeViewRect(incrementalAngle, childCount, widthMeasureSpec, heightMeasureSpec);

        int desiredWidth = (int) viewRect.width();
        int desiredHeight = (int) viewRect.height();
        int layoutWidth = Utils.getDimension(widthMeasureSpec, desiredWidth);
        int layoutHeight = Utils.getDimension(heightMeasureSpec, desiredHeight);

        setMeasuredDimension(layoutWidth, layoutHeight);

        resetCenterPointPositionRelativeToView(viewRect, layoutWidth, layoutHeight);
    }

    private void resetCenterPointPositionRelativeToView(RectF viewRect, int layoutWidth, int layoutHeight) {
        centerPosition.x = getCenterRelativePositionX(viewRect.left, layoutWidth, (int) viewRect.width());
        centerPosition.y = getCenterRelativePositionY(viewRect.top, layoutHeight, (int) viewRect.height());
    }

    private RectF computeViewRect(int incrementalAngle, int childCount, int widthMeasureSpec, int heightMeasureSpec) {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;

        int k = 0;
        for (int j = 0; j < childCount; j++) {
            View child = getChildAt(j);

            if (child.getVisibility() != GONE) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
                int childHeight = child.getMeasuredHeight();
                int childWidth = child.getMeasuredWidth();
                double distanceToCenterXProjection;
                double distanceToCenterYProjection;

                if (!isCenterView(child)) {
                    float angleRadian = (float) Math.toRadians(start + k * incrementalAngle);
                    k++;
                    distanceToCenterXProjection = radius * Math.cos(angleRadian);
                    distanceToCenterYProjection = radius * Math.sin(angleRadian);
                } else {
                    distanceToCenterXProjection = 0;
                    distanceToCenterYProjection = 0;
                }

                CircularLayout.LayoutParams circularLayoutParams = (LayoutParams) child.getLayoutParams();
                double childOffsetX = getHorizontalChildOffset(circularLayoutParams);
                double childOffsetY = getVerticalChildOffset(circularLayoutParams);

                //Getting the actual frame for the container
                minX = (float) Math.min(minX, distanceToCenterXProjection - childOffsetX * childWidth);
                minY = (float) Math.min(minY, distanceToCenterYProjection - childOffsetY * childHeight);
                maxX = (float) Math.max(maxX, distanceToCenterXProjection + (1 - childOffsetX) * childWidth);
                maxY = (float) Math.max(maxY, distanceToCenterYProjection + (1 - childOffsetY) * childHeight);
            }
        }
        return new RectF(minX, minY, maxX, maxY);
    }

    private double getVerticalChildOffset(LayoutParams circularLayoutParams) {
        int verticalGravity = circularLayoutParams.childLayoutGravity & Gravity.VERTICAL_GRAVITY_MASK;
        switch (verticalGravity) {
            case Gravity.TOP:
                return 0;
            case Gravity.BOTTOM:
                return 1;
        }
        return 0.5;
    }

    private double getHorizontalChildOffset(LayoutParams circularLayoutParams) {
        int horizontalGravity = circularLayoutParams.childLayoutGravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        switch (horizontalGravity) {
            case Gravity.LEFT:
                return 1;
            case Gravity.RIGHT:
                return 0;
        }
        return 0.5;
    }

    private int excludedPointsNbr() {
        return (centerView == null ? 0 : 1) + (excludeLast ? -1 : 0);
    }

    private boolean isCenterView(View child) {
        return child != null && centerView != null && child.equals(centerView);
    }

    private float getCenterRelativePositionY(float minY, int layoutHeight, int desiredHeight) {
        switch (circleGravity & Gravity.VERTICAL_GRAVITY_MASK) {
            case Gravity.CENTER_VERTICAL:
                return minY - (layoutHeight / 2 - desiredHeight / 2);
            case Gravity.BOTTOM:
                return minY - (layoutHeight - desiredHeight);
        }
        return minY;
    }

    private float getCenterRelativePositionX(float minX, int layoutWidth, int desiredWidth) {
        switch (circleGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.CENTER_HORIZONTAL:
                return minX - (layoutWidth / 2 - desiredWidth / 2);
            case Gravity.RIGHT:
                return minX - (layoutWidth - desiredWidth);
        }
        return minX;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int childCount = getChildCount();

        int k = 0;
        for (int j = 0; j < childCount; j++) {
            View child = getChildAt(j);

            if (child.getVisibility() != GONE) {
                int childPositionX = 0;
                int childPositionY = 0;
                if (!isCenterView(child)) {
                    double angleRadian = Math.toRadians(start + k * incrementalAngle);
                    childPositionX = (int) (radius * Math.cos(angleRadian));
                    childPositionY = (int) (radius * Math.sin(angleRadian));
                    k++;
                }

                CircularLayout.LayoutParams circularLayoutParams = (LayoutParams) child.getLayoutParams();
                double childOffsetX = getHorizontalChildOffset(circularLayoutParams);
                double childOffsetY = getVerticalChildOffset(circularLayoutParams);

                childPositionX -= childOffsetX * child.getMeasuredWidth() + centerPosition.x;
                childPositionY -= childOffsetY * child.getMeasuredHeight() + centerPosition.y;

                child.layout(childPositionX, childPositionY, childPositionX + child.getMeasuredWidth(), childPositionY + child.getMeasuredHeight());
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

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new CircularLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof CircularLayout.LayoutParams;
    }

    public void setRadiusPxSize(int radius) {
        this.radius = radius;
    }

    public void setStartAgnle(int angleStart) {
        this.start = angleStart;
    }

    public void setEndAngle(int angleEnd) {
        this.end = angleEnd;
    }

    public void setCenterType(CircleCenterType circleCenterType) {
        this.circleCenterType = circleCenterType;
        if (circleCenterType == CircleCenterType.FIRST)
            centerView = getChildAt(0);
        else if (circleCenterType == CircleCenterType.NONE)
            centerView = null;
    }

    public enum CircleCenterType {
        NONE, FIRST, RESOURCE
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {

        private int childLayoutGravity;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.CircularLayout);
            childLayoutGravity = a.getInt(R.styleable.CircularLayout_circle_gravity, Gravity.CENTER);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams p) {
            super(p);
        }

        public LayoutParams(LayoutParams source) {
            super(source);
            childLayoutGravity = source.childLayoutGravity;
        }
    }

}
