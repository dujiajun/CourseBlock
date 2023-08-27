package com.dujiajun.courseblock.helper

import android.os.Handler
import com.dujiajun.courseblock.model.Course
import okhttp3.Callback
import okhttp3.OkHttpClient


abstract class CourseDownloader {
    @JvmField
    protected val client: OkHttpClient = OkHttpClient.Builder().cookieJar(WebViewCookieHandler()).build()

    @JvmField
    var loginUrl: String = ""

    @JvmField
    protected var courseUrl: String = ""

    @JvmField
    protected var courses: List<Course> = ArrayList()

    abstract fun getCourses(year: String, term: String, handler: Handler)
    protected abstract fun parseFrom(json: String): List<Course>
    protected abstract fun download(year: String, term: String, callback: Callback)

    companion object {
        const val DOWNLOADED = 0
        const val FAILED = 1
        const val UNLOGIN = 2
    }
}