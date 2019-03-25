package com.dujiajun.courseblock;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class CourseTableView extends LinearLayout {

    private LinearLayout layoutTitle;
    private LinearLayout layoutLeft;
    public CourseTableView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.view_coursetable,this);

        layoutTitle = findViewById(R.id.layout_title_day);
        layoutLeft = findViewById(R.id.layout_left_class);

        //ArrayList<TextView> listTitleDay = new ArrayList<>();
        //ArrayList<TextView> listLeftClass = new ArrayList<>();
        String[] strTitleDay = getResources().getStringArray(R.array.days_in_week);

        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT
                , ViewGroup.LayoutParams.WRAP_CONTENT
                ,1);
        params.gravity = Gravity.CENTER;

        for (String s:strTitleDay) {
            TextView textView = new TextView(context);
            textView.setText(s);
            //textView.setTextSize(18);
            textView.setGravity(Gravity.CENTER);
            textView.setLayoutParams(params);
            layoutTitle.addView(textView);
        }

        for (int i = 1; i <= 13; i++) {
            TextView textView = new TextView(context);
            textView.setText(String.valueOf(i));
            //textView.setTextSize(18);
            textView.setGravity(Gravity.CENTER);
            textView.setLayoutParams(params);
            layoutLeft.addView(textView);
        }
    }


}
