package com.zhuangfei.timetable.utils

import android.content.Context

/**
 * 尺寸工具类
 */
object ScreenUtils {
    /**
     * 获取屏幕的高度Px
     * @param context
     * @return
     */
    fun getHeightInPx(context: Context): Int {
        return context.resources.displayMetrics.heightPixels
    }

    /**
     * 获取屏幕的宽度Px
     * @param context
     * @return
     */
    @JvmStatic
    fun getWidthInPx(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }

    /**
     * 获取屏幕的高度Dp
     * @param context
     * @return
     */
    fun getHeightInDp(context: Context): Int {
        val height = context.resources.displayMetrics.heightPixels.toFloat()
        return px2dip(context, height)
    }

    /**
     * 获取屏幕的宽度Dp
     * @param context
     * @return
     */
    fun getWidthInDp(context: Context): Int {
        val width = context.resources.displayMetrics.widthPixels.toFloat()
        return px2dip(context, width)
    }

    /**
     * dp转换为px
     * @param context 上下文
     * @param dpValue dp
     * @return px
     */
    @JvmStatic
    fun dip2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * px转换为dp
     * @param context 上下文
     * @param pxValue px
     * @return dp
     */
    fun px2dip(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }
}