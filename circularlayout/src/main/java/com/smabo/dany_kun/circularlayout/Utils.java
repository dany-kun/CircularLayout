package com.smabo.dany_kun.circularlayout;

import android.util.Log;
import android.view.View;

/**
 * Created by dany on 11/04/15.
 */
public class Utils {

    public static void log(String tag, String message) {
        Log.i(tag, message);
    }

    public static int getDimension(int dimMeasureSpec, int desiredDim) {
        int dimMode = View.MeasureSpec.getMode(dimMeasureSpec);
        int dimSize = View.MeasureSpec.getSize(dimMeasureSpec);

        int dim;
        //Width dimension specified
        if (dimMode == View.MeasureSpec.EXACTLY) {
            //Must be this size
            dim = dimSize;
        }
        //Match_parent or constrained wrap_content
        else if (dimMode == View.MeasureSpec.AT_MOST) {
            dim = Math.min(desiredDim, dimSize);
        } else {
            //Be whatever you want
            dim = desiredDim;
        }
        return dim;
    }
}
