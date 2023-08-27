package com.dujiajun.courseblock.helper

import android.content.Context
import android.content.pm.PackageManager

/**
 * 获取当前APK的版本号和版本名
 */
object APKVersionInfoUtils {
    /**
     * 获取当前apk的版本号
     *
     * @param mContext
     * @return
     */
    fun getVersionCode(mContext: Context): Int {
        var versionCode = 0
        try {
            //获取软件版本号，对应AndroidManifest.xml下android:versionCode
            versionCode = mContext.packageManager.getPackageInfo(mContext.packageName, 0).versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return versionCode
    }

    /**
     * 获取当前apk的版本名
     *
     * @param context 上下文
     * @return
     */
    @JvmStatic
    fun getVersionName(context: Context): String {
        var versionName = ""
        try {
            //获取软件版本号，对应AndroidManifest.xml下android:versionName
            versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return versionName
    }
}