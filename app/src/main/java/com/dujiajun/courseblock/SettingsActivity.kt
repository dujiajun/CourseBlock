package com.dujiajun.courseblock

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.DropDownPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.dujiajun.courseblock.calendar.AcademicTerm
import com.dujiajun.courseblock.constant.PreferenceKey
import com.dujiajun.courseblock.helper.APKVersionInfoUtils.getVersionName
import com.dujiajun.courseblock.helper.CourseManager
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Request.Builder
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

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
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_settings, rootKey)
            val manager = CourseManager.getInstance(requireContext().applicationContext)
            manager.updateStatus()
            curYearListPreference = findPreference(PreferenceKey.CURRENT_YEAR)!!
            curYearListPreference.isPersistent = false
            val selected = manager.term
            val newestYear = maxOf(selected.year, manager.suggestedTerm.year)
            val years = (newestYear downTo newestYear - 6).toList()
            curYearListPreference.entries = years.map { "$it–${it + 1}" }.toTypedArray()
            curYearListPreference.entryValues = years.map(Int::toString).toTypedArray()
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
            curTermListPreference.isPersistent = false
            curTermListPreference.entryValues = arrayOf("1", "2", "3")
            updateTermPreferences(selected)
            curTermListPreference.onPreferenceChangeListener = this
            showTimePreference.onPreferenceChangeListener = this
            showWeekendPreference.onPreferenceChangeListener = this
            showNotCurWeekPreference.onPreferenceChangeListener = this
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
            findPreference<Preference>("follow_current_term")!!.setOnPreferenceClickListener {
                manager.updateStatus()
                manager.selectTerm(null)
                updateTermPreferences(manager.term)
                true
            }
            checkNewVersion()
        }

        private fun updateTermPreferences(term: AcademicTerm) {
            curYearListPreference.value = term.year.toString()
            curYearListPreference.summary = "${term.year}–${term.year + 1}"
            curTermListPreference.value = term.number.toString()
            curTermListPreference.summary = resources.getStringArray(R.array.pref_term_entries)[term.number - 1]
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
                PreferenceKey.CURRENT_YEAR, PreferenceKey.CURRENT_TERM -> {
                    val manager = CourseManager.getInstance(requireContext().applicationContext)
                    manager.updateStatus()
                    val current = manager.term
                    val value = (newValue as String).toInt()
                    val choice = if (preference.key == PreferenceKey.CURRENT_YEAR) {
                        AcademicTerm(value, current.number)
                    } else {
                        AcademicTerm(current.year, value)
                    }
                    if (choice.number == 3 && manager.school.isGraduate) {
                        Toast.makeText(activity, R.string.graduate_summer_unavailable, Toast.LENGTH_LONG).show()
                        return false
                    }
                    manager.selectTerm(choice)
                    updateTermPreferences(choice)
                }

                PreferenceKey.STATUS -> {
                    statusPreference.summary =
                        statusPreference.entries[statusPreference.findIndexOfValue(newValue as String)]
                    view?.post {
                        if (isAdded) {
                            val manager = CourseManager.getInstance(requireContext().applicationContext)
                            manager.updateStatus()
                            updateTermPreferences(manager.term)
                        }
                    }
                }

                PreferenceKey.SHOW_WEEKEND, PreferenceKey.SHOW_COURSE_TIME, PreferenceKey.SHOW_NOT_CUR_WEEK,
                PreferenceKey.USE_CHI_ICON ->
                    Toast.makeText(activity, R.string.change_take_effect, Toast.LENGTH_SHORT).show()

                else -> {}
            }
            return true
        }

    }
}