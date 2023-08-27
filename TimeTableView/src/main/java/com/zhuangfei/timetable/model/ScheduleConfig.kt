package com.zhuangfei.timetable.model

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import com.zhuangfei.timetable.TimetableView
import com.zhuangfei.timetable.listener.ISchedule.OnConfigHandleListener

/**
 * 本地配置
 */
class ScheduleConfig(private val context: Context) {
    /**
     * 获取配置处理器
     *
     * @return ISchedule.OnConfigHandleListener
     */
    var onConfigHandleListener: OnConfigHandleListener? = null
        private set
    private var mConfigPreferences: SharedPreferences? = null
    private var mEditor: SharedPreferences.Editor? = null
    private var mConfigMap: MutableMap<String, String>
    private var configName: String? = "default_schedule_config"

    init {
        mConfigMap = HashMap()
    }

    /**
     * 设置本地配置的名称
     *
     * @param name 配置名称
     * @return ScheduleConfig
     */
    fun setConfigName(name: String): ScheduleConfig {
        if (configName == null) return this
        if (mConfigPreferences == null || configName != name) {
            configName = name
            mConfigPreferences = context.getSharedPreferences(configName, Context.MODE_PRIVATE)
            mEditor = mConfigPreferences?.edit()
        }
        return this
    }

    /**
     * 设置配置处理器
     *
     * @param mOnConfigHandleListener 配置处理器
     * @return ScheduleConfig
     */
    fun setOnConfigHandleListener(mOnConfigHandleListener: OnConfigHandleListener?): ScheduleConfig {
        onConfigHandleListener = mOnConfigHandleListener
        return this
    }

    /**
     * 将配置提交到缓存，需要使用commit()将其保存到本地
     *
     * @param key   属性名
     * @param value 属性值
     * @return ScheduleConfig
     */
    fun put(key: String, value: String): ScheduleConfig {
        mConfigMap[key] = value
        return this
    }

    /**
     * 从缓存中取出属性key的值
     *
     * @param key 属性名
     * @return String
     */
    operator fun get(key: String): String? {
        return mConfigMap[key]
    }

    val configMap: Map<String, String>
        /**
         * 获取缓存的属性Map
         *
         * @return
         */
        get() = mConfigMap

    /**
     * 将指定的Map作为缓存
     *
     * @param mConfigMap Map<String></String>, String>
     * @return ScheduleConfig
     */
    fun setConfigMap(mConfigMap: MutableMap<String, String>): ScheduleConfig {
        this.mConfigMap = mConfigMap
        return this
    }

    /**
     * 将缓存中的修改提交到本地
     */
    fun commit() {
        val set: MutableSet<String> = HashSet()
        for ((key, value) in mConfigMap) {
            set.add(key.trim { it <= ' ' } + "=" + value.trim { it <= ' ' })
        }
        val finalSet = mConfigPreferences!!.getStringSet("scheduleconfig_set", HashSet())
        finalSet!!.addAll(set)
        mConfigMap.clear()
        mEditor!!.putStringSet("scheduleconfig_set", finalSet)
        mEditor!!.commit()
    }

    /**
     * 清除缓存和本地属性配置
     */
    fun clear() {
        mConfigMap.clear()
        mEditor!!.clear()
        mEditor!!.commit()
    }

    /**
     * 导出本地配置文件中的数据
     *
     * @return set集合，每个元素都是一个配置，格式：key=value
     */
    fun export(): Set<String>? {
        return mConfigPreferences!!.getStringSet("scheduleconfig_set", HashSet())
    }

    /**
     * 将集合配置导入到本地
     *
     * @param data
     */
    fun load(data: Set<String>) {
        val finalSet = mConfigPreferences!!.getStringSet("scheduleconfig_set", HashSet())
        finalSet!!.addAll(data)
        mConfigMap.clear()
        mEditor!!.putStringSet("scheduleconfig_set", finalSet)
        mEditor!!.commit()
    }

    /**
     * 设置TimetableView的属性，使配置生效
     *
     * @param view TimetableView
     */
    fun use(view: TimetableView?) {
        if (onConfigHandleListener == null) return
        val keySet = mConfigPreferences!!.getStringSet("scheduleconfig_set", HashSet())

        for (key in keySet!!) {
            if (!TextUtils.isEmpty(key) && key.indexOf("=") != -1) {
                val trimmed = key.trim { it <= ' ' }
                val configArray = trimmed.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (configArray.size == 2) {
                    onConfigHandleListener!!.onParseConfig(configArray[0], configArray[1], view)
                }
            }
        }
    }
}