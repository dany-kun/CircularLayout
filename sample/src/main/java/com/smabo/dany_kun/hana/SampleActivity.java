package com.smabo.dany_kun.hana;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.smabo.dany_kun.circularlayout.CircularLayout;


public class SampleActivity extends ActionBarActivity {

    public static final int CHILD_NBR = 10;
    private CircularLayout circularLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        circularLayout = (CircularLayout) findViewById(R.id.circularlayout);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int j = 0; j < CHILD_NBR; j++) {
            TextView textView = (TextView) inflater.inflate(R.layout.child, circularLayout, false);
            textView.setText(j + "");
            circularLayout.addView(textView);
        }

        findViewById(R.id.submit_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCircularLayoutPropertiesSet();
            }
        });

    }

    private void onCircularLayoutPropertiesSet() {
        int radius = getEditTextIntValue(R.id.radius_view);
        circularLayout.setRadiusPxSize(radius);

        int angleStart = getEditTextIntValue(R.id.angle_start);
        circularLayout.setStartAgnle(angleStart);

        int angleEnd = getEditTextIntValue(R.id.angle_end);
        circularLayout.setEndAngle(angleEnd);

        CheckBox checkBox = (CheckBox) findViewById(R.id.firstview_center);
        circularLayout.setCenterType(checkBox.isChecked() ? CircularLayout.CircleCenterType.FIRST : CircularLayout.CircleCenterType.NONE);

        circularLayout.requestLayout();
    }

    private int getEditTextIntValue(@IdRes int viewId) {
        return Integer.valueOf(((EditText) findViewById(viewId)).getText().toString());
    }

}
