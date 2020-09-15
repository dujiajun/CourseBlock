package com.dujiajun.courseblock;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class CourseDBHelper extends SQLiteOpenHelper {

    private static final String CREATE_COURSE_TABLE = "CREATE TABLE course(" +
            "id integer primary key autoincrement," +
            "name text not null," +
            "room text," +
            "teacher text," +
            "start integer," +
            "step integer," +
            "day integer," +
            "weeklist text," +
            "course_id text," +
            "note text," +
            "from_server integer default 1)";
    private static final String DB_NAME = "course.db";

    public CourseDBHelper(@Nullable Context context) {
        super(context, DB_NAME, null, 3);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_COURSE_TABLE);
        //Toast.makeText(mContext, "CREATED DATABASE", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 1:
                db.execSQL("ALTER TABLE course ADD COLUMN course_id text");
                db.execSQL("ALTER TABLE course ADD COLUMN note text");
            case 2:
                db.execSQL("ALTER TABLE course ADD COLUMN from_server integer default 1");
        }
    }
}
