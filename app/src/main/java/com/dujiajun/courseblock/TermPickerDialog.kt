package com.dujiajun.courseblock

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.dujiajun.courseblock.calendar.AcademicCalendar
import com.dujiajun.courseblock.calendar.AcademicTerm
import com.dujiajun.courseblock.helper.CourseManager
import java.time.LocalDate

internal class TermPickerDialog : DialogFragment() {
    private var pendingYear: Int? = null
    private var pendingTerm: Int? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val manager = CourseManager.getInstance(context.applicationContext)
        manager.updateStatus()
        val selected = manager.term
        val isAutomatic = manager.selectedTerm == null
        val today = LocalDate.now(AcademicCalendar.ZONE)
        val newestYear = maxOf(manager.suggestedTerm.year, if (today.monthValue >= 6) today.year else today.year - 1)
        val years = ((newestYear downTo (newestYear - 6)).toList() + selected.year).distinct().sortedDescending()
        val numbers = if (manager.school.isGraduate) listOf(1, 2) else listOf(1, 2, 3)
        val dialog = AlertDialog.Builder(context)
        val form = requireActivity().layoutInflater.inflate(R.layout.dialog_academic_term, null)
        val year = form.findViewById<NumberPicker>(R.id.term_picker_year)
        val number = form.findViewById<NumberPicker>(R.id.term_picker_number)
        val termNames = resources.getStringArray(R.array.pref_term_entries)
        pendingYear = savedInstanceState?.getInt(STATE_YEAR) ?: selected.year
        pendingTerm = savedInstanceState?.getInt(STATE_TERM) ?: selected.number
        bindOptions(year, years.map { getString(R.string.academic_year_label, it, it + 1) }, years.indexOf(pendingYear))
        bindOptions(number, numbers.map { termNames[it - 1] }, numbers.indexOf(pendingTerm))
        year.setOnValueChangedListener { _, _, position -> pendingYear = years[position] }
        number.setOnValueChangedListener { _, _, position -> pendingTerm = numbers[position] }

        if (!isAutomatic) {
            dialog.setNeutralButton(R.string.term_use_automatic) { _, _ -> save(manager, null) }
        }
        return dialog
            .setTitle(R.string.term_picker_title)
            .setView(form)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                save(manager, AcademicTerm(years[year.value], numbers[number.value]))
            }
            .create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pendingYear?.let { outState.putInt(STATE_YEAR, it) }
        pendingTerm?.let { outState.putInt(STATE_TERM, it) }
    }

    private fun bindOptions(picker: NumberPicker, labels: List<String>, selected: Int) {
        picker.minValue = 0
        picker.maxValue = labels.lastIndex
        picker.displayedValues = labels.toTypedArray()
        picker.wrapSelectorWheel = false
        picker.value = selected.coerceAtLeast(0)
    }

    private fun save(manager: CourseManager, term: AcademicTerm?) {
        manager.selectTerm(term)
        parentFragmentManager.setFragmentResult(RESULT_KEY, Bundle())
    }

    companion object {
        const val RESULT_KEY = "academic_term_selected"
        private const val TAG = "academic_term_picker"
        private const val STATE_YEAR = "academic_year"
        private const val STATE_TERM = "academic_term"

        fun show(manager: FragmentManager) {
            if (!manager.isStateSaved && manager.findFragmentByTag(TAG) == null) {
                TermPickerDialog().showNow(manager, TAG)
            }
        }

        fun label(context: Context, term: AcademicTerm): String {
            val names = context.resources.getStringArray(R.array.pref_term_entries)
            return context.getString(R.string.display_term, term.year, term.year + 1, names[term.number - 1])
        }
    }
}
