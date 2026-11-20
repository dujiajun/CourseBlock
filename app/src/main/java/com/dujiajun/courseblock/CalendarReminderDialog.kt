package com.dujiajun.courseblock

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import com.dujiajun.courseblock.calendar.CalendarReminderTimes
import com.dujiajun.courseblock.constant.PreferenceKey

internal class CalendarReminderDialog : DialogFragment() {
    private lateinit var options: LinearLayout
    private lateinit var customInput: EditText
    private val selected = mutableSetOf<Int>()
    private val displayed = mutableSetOf<Int>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val savedTimes = CalendarReminderTimes.parse(preferences.getString(PreferenceKey.CALENDAR_REMINDER_MINUTES, null))
        selected.addAll(savedInstanceState?.getIntArray(STATE_SELECTED)?.toSet() ?: savedTimes)
        displayed.addAll(CalendarReminderTimes.presets)
        displayed.addAll(savedInstanceState?.getIntArray(STATE_DISPLAYED)?.toSet() ?: selected)

        val content = layoutInflater.inflate(R.layout.dialog_calendar_reminders, null)
        options = content.findViewById(R.id.calendar_reminder_options)
        customInput = content.findViewById(R.id.calendar_reminder_custom)
        customInput.hint = getString(
            R.string.calendar_reminder_custom_hint,
            CalendarReminderTimes.customRange.first,
            CalendarReminderTimes.customRange.last,
        )
        customInput.setText(savedInstanceState?.getString(STATE_CUSTOM))
        renderOptions()
        content.findViewById<Button>(R.id.calendar_reminder_add_custom).setOnClickListener { addCustomTime() }
        customInput.setOnEditorActionListener { _, action, _ ->
            if (action != EditorInfo.IME_ACTION_DONE) false
            else {
                addCustomTime()
                true
            }
        }

        return AlertDialog.Builder(context)
            .setTitle(R.string.calendar_reminder_time)
            .setView(content)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok, null)
            .create()
    }

    override fun onStart() {
        super.onStart()
        val alert = requireDialog() as AlertDialog
        alert.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (customInput.text.isNotBlank() && !addCustomTime()) return@setOnClickListener
            val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val previous = CalendarReminderTimes.parse(
                preferences.getString(PreferenceKey.CALENDAR_REMINDER_MINUTES, null),
            )
            val edit = preferences.edit()
                .putString(PreferenceKey.CALENDAR_REMINDER_MINUTES, CalendarReminderTimes.encode(selected))
                .putBoolean(PreferenceKey.CALENDAR_REMINDERS_PENDING, true)
            if (!preferences.contains(PreferenceKey.CALENDAR_LEGACY_REMINDER_MINUTES)) {
                edit.putString(PreferenceKey.CALENDAR_LEGACY_REMINDER_MINUTES, CalendarReminderTimes.encode(previous))
            }
            edit.apply()
            parentFragmentManager.setFragmentResult(RESULT_KEY, Bundle())
            dismiss()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntArray(STATE_SELECTED, selected.toIntArray())
        outState.putIntArray(STATE_DISPLAYED, displayed.toIntArray())
        if (::customInput.isInitialized) outState.putString(STATE_CUSTOM, customInput.text.toString())
    }

    private fun renderOptions() {
        val context = options.context
        options.removeAllViews()
        displayed.sorted().forEach { minutes ->
            options.addView(AppCompatCheckBox(context).apply {
                text = if (minutes == CalendarReminderTimes.AT_START) getString(R.string.calendar_reminder_at_start)
                else getString(R.string.calendar_minutes_before, minutes)
                minHeight = (48 * resources.displayMetrics.density).toInt()
                isChecked = minutes in selected
                setOnCheckedChangeListener { button, checked ->
                    if (!checked) selected.remove(minutes)
                    else if (selected.size < CalendarReminderTimes.MAX_COUNT) selected.add(minutes)
                    else {
                        button.isChecked = false
                        Toast.makeText(context, limitMessage(), Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    private fun addCustomTime(): Boolean {
        val minutes = customInput.text.toString().trim().toIntOrNull()
        if (minutes == null || minutes !in CalendarReminderTimes.customRange) {
            customInput.error = getString(
                R.string.calendar_reminder_custom_range,
                CalendarReminderTimes.customRange.first,
                CalendarReminderTimes.customRange.last,
            )
            return false
        }
        if (minutes !in selected && selected.size >= CalendarReminderTimes.MAX_COUNT) {
            customInput.error = limitMessage()
            return false
        }
        selected.add(minutes)
        displayed.add(minutes)
        customInput.error = null
        customInput.setText("")
        renderOptions()
        return true
    }

    private fun limitMessage() = getString(R.string.calendar_reminder_limit, CalendarReminderTimes.MAX_COUNT)

    companion object {
        const val RESULT_KEY = "calendar_reminder_times_changed"
        private const val TAG = "calendar_reminder_dialog"
        private const val STATE_SELECTED = "selected_times"
        private const val STATE_DISPLAYED = "displayed_times"
        private const val STATE_CUSTOM = "custom_minutes"

        fun show(manager: FragmentManager) {
            if (!manager.isStateSaved && manager.findFragmentByTag(TAG) == null) {
                CalendarReminderDialog().showNow(manager, TAG)
            }
        }

        fun summary(context: Context, times: Set<Int>): String {
            val before = times.filter { it != CalendarReminderTimes.AT_START }.sorted()
            return listOfNotNull(
                context.getString(R.string.calendar_reminder_at_start).takeIf { CalendarReminderTimes.AT_START in times },
                before.takeIf { it.isNotEmpty() }?.let {
                    val minutes = it.joinToString(context.getString(R.string.calendar_reminder_list_separator))
                    context.getString(R.string.calendar_minutes_before_list, minutes)
                },
            ).joinToString(" · ").ifEmpty { context.getString(R.string.calendar_reminder_off) }
        }
    }
}
