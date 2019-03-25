package com.dujiajun.courseblock;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class CourseDBHelper extends SQLiteOpenHelper {

    public static final String CREATE_COURSE_TABLE = "CREATE TABLE course(" +
            "id integer primary key autoincrement," +
            "cno integer not null," +
            "cname text not null," +
            "start_week integer," +
            "end_week integer," +
            "teacher text," +
            "location text," +
            "day_in_week integer," +
            "class_in_day integer)";

    private Context mContext;
    public CourseDBHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_COURSE_TABLE);
        Toast.makeText(mContext, "CREATED DATABASE", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
