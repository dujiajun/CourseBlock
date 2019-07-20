package com.dujiajun.courseblock;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import java.util.Calendar;

public class SettingsActivity extends AppCompatActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

        private ListPreference curYearListPreference;
        private ListPreference curTermListPreference;
        private SwitchPreference showNotCurWeekPreference;
        private SwitchPreference showWeekendPreference;
        private SwitchPreference showTimePreference;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_settings, rootKey);


            curYearListPreference = (ListPreference) findPreference("cur_year");
            curTermListPreference = (ListPreference) findPreference("cur_term");

            showNotCurWeekPreference = (SwitchPreference) findPreference("show_not_cur_week");
            showWeekendPreference = (SwitchPreference) findPreference("show_weekend");
            showTimePreference = (SwitchPreference) findPreference("show_course_time");

            int curRealYear = Calendar.getInstance().get(Calendar.YEAR);

            String[] years = new String[4];
            String[] year_values = new String[4];
            for (int i = 0; i < 4; i++) {
                year_values[i] = String.valueOf(i + curRealYear - 4 + 1);
                years[i] = ((i + curRealYear - 4 + 1) + "-" + (i + curRealYear - 4 + 2));
            }
            curYearListPreference.setEntries(years);
            curYearListPreference.setEntryValues(year_values);

            curYearListPreference.setDefaultValue(year_values[0]);
            curYearListPreference.setSummary(curYearListPreference.getEntry());

            curTermListPreference.setDefaultValue(getResources().getStringArray(R.array.pref_term_entries)[0]);
            curTermListPreference.setSummary(curTermListPreference.getEntry());

            curYearListPreference.setOnPreferenceChangeListener(this);
            curTermListPreference.setOnPreferenceChangeListener(this);

            showTimePreference.setOnPreferenceChangeListener(this);
            showWeekendPreference.setOnPreferenceChangeListener(this);
            showNotCurWeekPreference.setOnPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            switch (preference.getKey()) {
                default:
                    break;
            }
            return true;
        }


        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            switch (preference.getKey()) {
                case "cur_year":
                    if (Integer.valueOf((String) newValue) < 2018
                            && curTermListPreference.getEntryValues() != null
                            && curTermListPreference.getEntryValues()[2].equals(newValue)) {
                        Toast.makeText(getActivity(), R.string.summer_term, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    curYearListPreference.setSummary(curYearListPreference.getEntries()
                            [curYearListPreference.findIndexOfValue((String) newValue)]);
                    break;
                case "cur_term":

                    if (curYearListPreference.getValue() != null
                            && Integer.valueOf(curYearListPreference.getValue()) < 2018
                            && curTermListPreference.getEntryValues()[2].equals(newValue)) {
                        Toast.makeText(getActivity(), R.string.summer_term, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    curTermListPreference.setSummary(curTermListPreference.getEntries()
                            [curTermListPreference.findIndexOfValue((String) newValue)]);
                    break;
                case "show_weekend":
                case "show_not_cur_week":
                case "show_course_time":
                    Toast.makeText(getActivity(), R.string.change_take_effect, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
            return true;
        }
    }
}
