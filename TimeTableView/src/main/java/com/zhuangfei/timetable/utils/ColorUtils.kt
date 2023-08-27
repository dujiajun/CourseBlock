package com.zhuangfei.timetable.utils

/**
 * Created by Liu ZhuangFei on 2018/7/25.
 */
object ColorUtils {
    /**
     * 合成指定颜色、指定不透明度的颜色，
     * 0:完全透明，1：不透明
     * @param color
     * @param alpha 0:完全透明，1：不透明
     * @return
     */
    @JvmStatic
    fun alphaColor(color: Int, alpha: Float): Int {
        val a = 255.coerceAtMost(0.coerceAtLeast((alpha * 255).toInt())) shl 24
        val rgb = 0x00ffffff and color
        return a + rgb
    }
}