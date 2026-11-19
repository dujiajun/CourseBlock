package com.dujiajun.courseblock

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.preference.DropDownPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.dujiajun.courseblock.calendar.AndroidCalendar
import com.dujiajun.courseblock.constant.PreferenceKey
import com.dujiajun.courseblock.helper.APKVersionInfoUtils.getVersionName
import com.dujiajun.courseblock.helper.CourseManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        private lateinit var removeCalendarPreference: Preference
        private var calendarRemovalDialog: AlertDialog? = null

        private val calendarPermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                if (AndroidCalendar.hasPermission(requireContext())) confirmCalendarRemoval()
                else view?.let { content ->
                    Snackbar.make(content, R.string.calendar_remove_no_permission, Snackbar.LENGTH_LONG)
                        .setAction(R.string.calendar_open_settings) {
                            val uri = Uri.fromParts("package", requireContext().packageName, null)
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri))
                        }
                        .show()
                }
            }

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
            setupCalendarPreferences()
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

        private fun setupCalendarPreferences() {
            val reminderPreference = findPreference<ListPreference>(PreferenceKey.CALENDAR_REMINDER_MINUTES)!!
            reminderPreference.entries = AndroidCalendar.reminderTimes
                .map { getString(R.string.calendar_minutes_before, it) }.toTypedArray()
            reminderPreference.entryValues = AndroidCalendar.reminderTimes.map(Int::toString).toTypedArray()
            reminderPreference.summaryProvider = Preference.SummaryProvider<ListPreference> { preference ->
                val selected = preference.entry
                    ?: getString(R.string.calendar_minutes_before, AndroidCalendar.DEFAULT_REMINDER_MINUTES)
                "$selected\n${getString(R.string.calendar_reminder_help)}"
            }

            removeCalendarPreference = findPreference("calendar_remove_term")!!
            removeCalendarPreference.setOnPreferenceClickListener {
                if (AndroidCalendar.hasPermission(requireContext())) confirmCalendarRemoval()
                else calendarPermissions.launch(AndroidCalendar.permissions)
                true
            }
        }

        private fun confirmCalendarRemoval() {
            if (calendarRemovalDialog?.isShowing == true) return
            val context = requireContext()
            val manager = CourseManager.getInstance(context.applicationContext)
            manager.updateStatus()
            val termKey = manager.termKey
            val label = TermPickerDialog.label(context, manager.term)
            calendarRemovalDialog = AlertDialog.Builder(context)
                .setTitle(R.string.calendar_remove_term)
                .setMessage(getString(R.string.calendar_remove_confirm, label))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.calendar_remove) { _, _ -> removeCalendarTerm(termKey) }
                .show()
        }

        override fun onDestroyView() {
            calendarRemovalDialog?.dismiss()
            calendarRemovalDialog = null
            super.onDestroyView()
        }

        private fun removeCalendarTerm(termKey: String) {
            val application = requireContext().applicationContext
            val previousCalendar = preferenceManager.sharedPreferences
                ?.getLong(PreferenceKey.LEGACY_CALENDAR_ID, -1L)?.takeIf { it >= 0 }
            removeCalendarPreference.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val count = withContext(Dispatchers.IO) {
                        AndroidCalendar(application.contentResolver, application.packageName)
                            .removeTerm(termKey, previousCalendar)
                    }
                    val message = if (count == 0) getString(R.string.calendar_remove_empty)
                    else resources.getQuantityString(R.plurals.calendar_removed, count, count)
                    showCalendarMessage(message)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    showCalendarMessage(getString(R.string.calendar_remove_failed))
                } finally {
                    removeCalendarPreference.isEnabled = true
                }
            }
        }

        private fun showCalendarMessage(message: String) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }

        private fun updateTermSummary() {
            val manager = CourseManager.getInstance(requireContext().applicationContext)
            manager.updateStatus()
            val label = TermPickerDialog.label(requireContext(), manager.term)
            removeCalendarPreference.summary = label
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