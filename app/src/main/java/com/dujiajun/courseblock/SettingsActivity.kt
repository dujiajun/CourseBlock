package com.dujiajun.courseblock

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
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
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.container, SettingsFragment()).commit()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {
        private lateinit var termPreference: Preference
        private lateinit var statusPreference: DropDownPreference

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_settings, rootKey)
            termPreference = findPreference("study_term")!!
            termPreference.setOnPreferenceClickListener {
                TermPickerDialog.show(parentFragmentManager)
                true
            }
            parentFragmentManager.setFragmentResultListener(TermPickerDialog.RESULT_KEY, this) { _, _ ->
                updateTermSummary()
            }
            statusPreference = findPreference(PreferenceKey.STATUS)!!
            statusPreference.summary = statusPreference.entry
            statusPreference.onPreferenceChangeListener = this
            updateTermSummary()
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

        private fun updateTermSummary() {
            val manager = CourseManager.getInstance(requireContext().applicationContext)
            manager.updateStatus()
            val label = TermPickerDialog.label(requireContext(), manager.term)
            termPreference.summary = if (manager.selectedTerm == null) {
                getString(R.string.term_automatic_summary, label)
            } else label
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
                PreferenceKey.STATUS -> {
                    statusPreference.summary =
                        statusPreference.entries[statusPreference.findIndexOfValue(newValue as String)]
                    view?.post { if (isAdded) updateTermSummary() }
                }

                else -> {}
            }
            return true
        }

    }
}