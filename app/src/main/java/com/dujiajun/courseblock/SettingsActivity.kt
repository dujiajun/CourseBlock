package com.dujiajun.courseblock

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.DatePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.DropDownPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import com.dujiajun.courseblock.constant.PreferenceKey
import com.dujiajun.courseblock.helper.APKVersionInfoUtils.getVersionName
import com.dujiajun.courseblock.helper.CourseManager
import com.dujiajun.courseblock.helper.WeekManager
import com.dujiajun.courseblock.helper.WeekManager.Companion.getInstance
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Request.Builder
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar
import java.util.Locale

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, SettingsFragment())
            .commit()
    }

    class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {
        private lateinit var curYearListPreference: ListPreference
        private lateinit var curTermListPreference: ListPreference
        private lateinit var statusPreference: DropDownPreference
        private lateinit var weekManager: WeekManager
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_settings, rootKey)
            val preferences = PreferenceManager.getDefaultSharedPreferences(
                requireActivity()
            )
            weekManager = getInstance(requireContext())
            curYearListPreference = findPreference(PreferenceKey.CURRENT_YEAR)!!
            val curRealMonth = Calendar.getInstance()[Calendar.MONTH] + 1
            var curRealYear = Calendar.getInstance()[Calendar.YEAR]
            if (curRealMonth < 6) {
                curRealYear-- // 6月前为上一学年，6月底学期结束，开放下一学年课表
            }
            val years = arrayOfNulls<String>(SHOW_YEARS)
            val yearValues = arrayOfNulls<String>(SHOW_YEARS)
            var prefValues =
                preferences.getString(PreferenceKey.CURRENT_YEAR, CourseManager.defaultYear)
            var summary: String? = ""
            for (i in 0 until SHOW_YEARS) {
                yearValues[i] = (i + curRealYear - SHOW_YEARS + 1).toString()
                years[i] =
                    (i + curRealYear - SHOW_YEARS + 1).toString() + "-" + (i + curRealYear - SHOW_YEARS + 2)
                if (prefValues == yearValues[i]) summary = years[i]
            }
            curYearListPreference.entries = years
            curYearListPreference.entryValues = yearValues
            curYearListPreference.summary = summary
            curYearListPreference.onPreferenceChangeListener = this
            statusPreference = findPreference(PreferenceKey.STATUS)!!
            statusPreference.summary = statusPreference.entry
            statusPreference.onPreferenceChangeListener = this
            val showNotCurWeekPreference =
                findPreference<SwitchPreference>(PreferenceKey.SHOW_NOT_CUR_WEEK)!!
            val showWeekendPreference =
                findPreference<SwitchPreference>(PreferenceKey.SHOW_WEEKEND)!!
            val showTimePreference =
                findPreference<SwitchPreference>(PreferenceKey.SHOW_COURSE_TIME)!!
            curTermListPreference = findPreference(PreferenceKey.CURRENT_TERM)!!
            prefValues =
                preferences.getString(PreferenceKey.CURRENT_TERM, CourseManager.defaultTerm)
            val terms = arrayOf("1", "2", "3")
            for (i in terms.indices) {
                if (prefValues == terms[i]) summary =
                    resources.getStringArray(R.array.pref_term_entries)[i]
            }
            curTermListPreference.entryValues = terms
            curTermListPreference.summary = summary
            curTermListPreference.onPreferenceChangeListener = this
            showTimePreference.onPreferenceChangeListener = this
            showWeekendPreference.onPreferenceChangeListener = this
            showNotCurWeekPreference.onPreferenceChangeListener = this
            val firstDayPreference = findPreference<Preference>(PreferenceKey.FIRST_MONDAY)!!
            val lastDayPreference = findPreference<Preference>(PreferenceKey.LAST_SUNDAY)!!
            val calendar = Calendar.getInstance(Locale.CHINA)
            firstDayPreference.onPreferenceClickListener =
                Preference.OnPreferenceClickListener { preference: Preference ->
                    val dialog = DatePickerDialog(
                        requireContext(),
                        { _: DatePicker?, year: Int, month: Int, dayOfMonth: Int ->
                            if (calendar[Calendar.DAY_OF_WEEK] != Calendar.MONDAY) {
                                Toast.makeText(
                                    activity,
                                    R.string.change_to_monday,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            weekManager.setFirstDay(year, month, dayOfMonth)
                            preference.summary = weekManager.showFirstDate
                            calendar.time = weekManager.firstDate
                        },
                        calendar[Calendar.YEAR],
                        calendar[Calendar.MONTH],
                        calendar[Calendar.DAY_OF_MONTH]
                    )
                    dialog.datePicker.firstDayOfWeek = Calendar.MONDAY
                    dialog.show()
                    true
                }
            lastDayPreference.onPreferenceClickListener =
                Preference.OnPreferenceClickListener { preference: Preference ->
                    val dialog = DatePickerDialog(
                        requireContext(),
                        { _: DatePicker?, year: Int, month: Int, dayOfMonth: Int ->
                            if (calendar[Calendar.DAY_OF_WEEK] != Calendar.SUNDAY) {
                                Toast.makeText(
                                    activity,
                                    R.string.change_to_sunday,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            weekManager.setLastDay(year, month, dayOfMonth)
                            preference.summary = weekManager.showLastDate
                            calendar.time = weekManager.lastDate
                        },
                        calendar[Calendar.YEAR],
                        calendar[Calendar.MONTH],
                        calendar[Calendar.DAY_OF_MONTH]
                    )
                    dialog.datePicker.firstDayOfWeek = Calendar.SUNDAY
                    dialog.show()
                    true
                }
            firstDayPreference.summary = weekManager.showFirstDate
            lastDayPreference.summary = weekManager.showLastDate
            val homepagePreference = findPreference<Preference>("homepage")!!
            homepagePreference.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    openUrl("https://github.com/dujiajun/CourseBlock/")
                    false
                }
            val feedbackPreference = findPreference<Preference>("feedback")!!
            feedbackPreference.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    openUrl("https://github.com/dujiajun/CourseBlock/issues")
                    false
                }
            checkNewVersion()
        }

        private fun checkNewVersion() {
            val client = OkHttpClient()
            val request: Request = Builder()
                .url("https://api.github.com/repos/dujiajun/CourseBlock/releases/latest")
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {}

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body!!.string()
                    try {
                        val json = JSONObject(body)
                        val tag = json.getString("tag_name")
                        val asset = json.getJSONArray("assets").getJSONObject(0)
                        val downloadUrl = asset.getString("browser_download_url")
                        val curVersion = 'v'.toString() + getVersionName(
                            requireActivity()
                        )
                        if (curVersion < tag) {
                            activity?.runOnUiThread {
                                val homepagePreference = findPreference<Preference>("homepage")!!
                                homepagePreference.summary = "有新版本，点击下载"
                                homepagePreference.onPreferenceClickListener =
                                    Preference.OnPreferenceClickListener {
                                        openUrl(downloadUrl)
                                        false
                                    }
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            })
        }

        private fun openUrl(url: String) {
            val intent = Intent()
            intent.action = "android.intent.action.VIEW"
            val contentUrl = Uri.parse(url)
            intent.data = contentUrl
            startActivity(intent)
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            return true
        }

        override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
            when (preference.key) {
                PreferenceKey.CURRENT_YEAR -> {
                    if ((newValue as String).toInt() < 2018 && curTermListPreference.entryValues != null && curTermListPreference.entryValues[2] == newValue) {
                        Toast.makeText(activity, R.string.summer_term, Toast.LENGTH_SHORT).show()
                        return false
                    }
                    curYearListPreference.summary =
                        curYearListPreference.entries[curYearListPreference.findIndexOfValue(
                            newValue
                        )]
                }

                PreferenceKey.CURRENT_TERM -> {
                    if (curYearListPreference.value != null && curYearListPreference.value.toInt() < 2018 && curTermListPreference.entryValues[2] == newValue) {
                        Toast.makeText(activity, R.string.summer_term, Toast.LENGTH_SHORT).show()
                        return false
                    }
                    curTermListPreference.summary =
                        curTermListPreference.entries[curTermListPreference.findIndexOfValue(
                            newValue as String
                        )]
                }

                PreferenceKey.STATUS -> {
                    statusPreference.summary =
                        statusPreference.entries[statusPreference.findIndexOfValue(newValue as String)]
                    Toast.makeText(activity, R.string.change_take_effect, Toast.LENGTH_SHORT).show()
                }

                PreferenceKey.SHOW_WEEKEND, PreferenceKey.SHOW_COURSE_TIME, PreferenceKey.SHOW_NOT_CUR_WEEK,
                PreferenceKey.USE_CHI_ICON, PreferenceKey.FIRST_MONDAY, PreferenceKey.LAST_SUNDAY ->
                    Toast.makeText(activity, R.string.change_take_effect, Toast.LENGTH_SHORT).show()

                else -> {}
            }
            return true
        }

        companion object {
            private const val SHOW_YEARS = 7
        }
    }
}