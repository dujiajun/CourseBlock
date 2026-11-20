package com.dujiajun.courseblock

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.dujiajun.courseblock.calendar.AndroidCalendar
import com.dujiajun.courseblock.calendar.CalendarImport
import com.dujiajun.courseblock.calendar.CalendarReminderTimes
import com.dujiajun.courseblock.calendar.importRemainingCourses
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
import java.time.Instant

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
        private lateinit var addCalendarPreference: Preference
        private lateinit var removeCalendarPreference: Preference
        private lateinit var reminderPreference: Preference
        private lateinit var retryRemindersPreference: Preference
        private var calendarRemovalDialog: AlertDialog? = null

        private val addCalendarPermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                if (AndroidCalendar.hasPermission(requireContext())) addCalendarCourses()
                else showCalendarPermissionDenied(R.string.calendar_permission_denied)
            }

        private val removeCalendarPermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                if (AndroidCalendar.hasPermission(requireContext())) confirmCalendarRemoval()
                else showCalendarPermissionDenied(R.string.calendar_remove_no_permission)
            }

        private val updateReminderPermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                if (AndroidCalendar.hasPermission(requireContext())) updateImportedReminders()
                else showCalendarPermissionDenied(R.string.calendar_reminder_pending)
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
            reminderPreference = findPreference(PreferenceKey.CALENDAR_REMINDER_MINUTES)!!
            retryRemindersPreference = findPreference("calendar_retry_reminders")!!
            showPendingReminders()
            retryRemindersPreference.setOnPreferenceClickListener {
                if (AndroidCalendar.hasPermission(requireContext())) updateImportedReminders()
                else updateReminderPermissions.launch(AndroidCalendar.permissions)
                true
            }
            fun updateReminderSummary() {
                val stored = preferenceManager.sharedPreferences
                    ?.getString(PreferenceKey.CALENDAR_REMINDER_MINUTES, null)
                reminderPreference.summary = CalendarReminderDialog.summary(requireContext(), CalendarReminderTimes.parse(stored))
            }
            updateReminderSummary()
            reminderPreference.setOnPreferenceClickListener {
                CalendarReminderDialog.show(parentFragmentManager)
                true
            }
            parentFragmentManager.setFragmentResultListener(CalendarReminderDialog.RESULT_KEY, this) { _, _ ->
                updateReminderSummary()
                updateImportedReminders()
            }

            addCalendarPreference = findPreference("calendar_add")!!
            addCalendarPreference.setOnPreferenceClickListener {
                addCalendarCourses()
                true
            }

            removeCalendarPreference = findPreference("calendar_remove_all")!!
            removeCalendarPreference.setOnPreferenceClickListener {
                if (AndroidCalendar.hasPermission(requireContext())) confirmCalendarRemoval()
                else removeCalendarPermissions.launch(AndroidCalendar.permissions)
                true
            }
        }

        private fun showPendingReminders() {
            retryRemindersPreference.isVisible = preferenceManager.sharedPreferences
                ?.getBoolean(PreferenceKey.CALENDAR_REMINDERS_PENDING, false) == true
        }

        private fun updateImportedReminders() {
            val application = requireContext().applicationContext
            val preferences = requireNotNull(preferenceManager.sharedPreferences)
            if (!AndroidCalendar.hasPermission(application)) {
                showPendingReminders()
                showCalendarMessage(getString(R.string.calendar_reminder_pending))
                return
            }

            val minutes = CalendarReminderTimes.parse(preferences.getString(PreferenceKey.CALENDAR_REMINDER_MINUTES, null))
            val legacy = CalendarReminderTimes.parse(preferences.getString(PreferenceKey.CALENDAR_LEGACY_REMINDER_MINUTES, null))
            val previousCalendar = preferences.getLong(PreferenceKey.LEGACY_CALENDAR_ID, -1L).takeIf { it >= 0 }
            setCalendarActionsEnabled(false)
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val count = withContext(Dispatchers.IO) {
                        AndroidCalendar(application.contentResolver, application.packageName)
                            .updateImportedReminders(Instant.now(), previousCalendar, legacy, minutes)
                    }
                    preferences.edit().remove(PreferenceKey.CALENDAR_REMINDERS_PENDING).apply()
                    val message = if (count == 0) getString(R.string.calendar_reminder_saved)
                    else resources.getQuantityString(R.plurals.calendar_reminders_updated, count, count)
                    showCalendarMessage(message)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.w("CourseBlockReminders", "Couldn't update calendar reminders", error)
                    showCalendarMessage(getString(R.string.calendar_reminder_pending))
                } finally {
                    showPendingReminders()
                    setCalendarActionsEnabled(true)
                }
            }
        }

        private fun addCalendarCourses() {
            val context = requireContext()
            val manager = CourseManager.getInstance(context.applicationContext)
            manager.updateStatus()
            setCalendarActionsEnabled(false)
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    when (val result = importRemainingCourses(context, manager)) {
                        is CalendarImport.Message -> showCalendarMessage(result.text)
                        CalendarImport.PermissionRequired -> addCalendarPermissions.launch(AndroidCalendar.permissions)
                    }
                } finally {
                    setCalendarActionsEnabled(true)
                }
            }
        }

        private fun confirmCalendarRemoval() {
            if (calendarRemovalDialog?.isShowing == true) return
            calendarRemovalDialog = AlertDialog.Builder(requireContext())
                .setTitle(R.string.calendar_remove_all)
                .setMessage(R.string.calendar_remove_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.calendar_remove) { _, _ -> removeCalendarCourses() }
                .show()
        }

        override fun onDestroyView() {
            calendarRemovalDialog?.dismiss()
            calendarRemovalDialog = null
            super.onDestroyView()
        }

        private fun removeCalendarCourses() {
            val application = requireContext().applicationContext
            val previousCalendar = preferenceManager.sharedPreferences
                ?.getLong(PreferenceKey.LEGACY_CALENDAR_ID, -1L)?.takeIf { it >= 0 }
            setCalendarActionsEnabled(false)
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val count = withContext(Dispatchers.IO) {
                        AndroidCalendar(application.contentResolver, application.packageName)
                            .removeImportedCourses(previousCalendar)
                    }
                    val message = if (count == 0) getString(R.string.calendar_remove_empty)
                    else resources.getQuantityString(R.plurals.calendar_removed, count, count)
                    showCalendarMessage(message)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    showCalendarMessage(getString(R.string.calendar_remove_failed))
                } finally {
                    setCalendarActionsEnabled(true)
                }
            }
        }

        private fun setCalendarActionsEnabled(enabled: Boolean) {
            addCalendarPreference.isEnabled = enabled
            removeCalendarPreference.isEnabled = enabled
            reminderPreference.isEnabled = enabled
            retryRemindersPreference.isEnabled = enabled
        }

        private fun showCalendarPermissionDenied(message: Int) {
            val content = view ?: return
            Snackbar.make(content, message, Snackbar.LENGTH_LONG)
                .setAction(R.string.calendar_open_settings) {
                    val uri = Uri.fromParts("package", requireContext().packageName, null)
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri))
                }
                .show()
        }

        private fun showCalendarMessage(message: String) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
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