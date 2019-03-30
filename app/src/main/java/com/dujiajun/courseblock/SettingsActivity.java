package com.dujiajun.courseblock;

import android.os.Bundle;
import android.widget.Toast;

import java.util.Calendar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

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
        //private ListPreference curWeekListPreference;
        private ListPreference curTermListPreference;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_settings, rootKey);

            //curWeekListPreference = (ListPreference) findPreference("cur_week");
            curYearListPreference = (ListPreference) findPreference("cur_year");
            curTermListPreference = (ListPreference) findPreference("cur_term");

            int curRealYear = Calendar.getInstance().get(Calendar.YEAR);
            //Log.e("CourseBlock", String.valueOf(curRealYear));
            String[] years = new String[4];
            String[] year_values = new String[4];
            for (int i = 0; i < 4; i++) {
                year_values[i] = String.valueOf(i + curRealYear - 4 + 1);
                years[i] = (String.valueOf(i + curRealYear - 4 + 1) + "-" + String.valueOf(i + curRealYear - 4 + 2));
            }
            curYearListPreference.setEntries(years);
            curYearListPreference.setEntryValues(year_values);
            //curYearListPreference.setValueIndex(0);
            curYearListPreference.setDefaultValue(year_values[0]);
            curYearListPreference.setSummary(curYearListPreference.getEntry());

            /*String[] weeks = new String[20];
            for (int i = 0; i < 20; i++) {
                weeks[i] = (String.valueOf(i + 1));
            }
            curWeekListPreference.setEntries(weeks);
            curWeekListPreference.setEntryValues(weeks);
            //curWeekListPreference.setValueIndex(0);
            curWeekListPreference.setDefaultValue(weeks[0]);
            curWeekListPreference.setSummary(curWeekListPreference.getEntry());*/

            //curTermListPreference.setValueIndex(0);
            curTermListPreference.setDefaultValue(getResources().getStringArray(R.array.pref_term_entries)[0]);
            curTermListPreference.setSummary(curTermListPreference.getEntry());

            curYearListPreference.setOnPreferenceChangeListener(this);
            curTermListPreference.setOnPreferenceChangeListener(this);
            //curWeekListPreference.setOnPreferenceChangeListener(this);
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
                    curYearListPreference.setSummary(curYearListPreference.getEntries()
                            [curYearListPreference.findIndexOfValue((String) newValue)]);
                    break;
                case "cur_term":
                    curTermListPreference.setSummary(curTermListPreference.getEntries()
                            [curTermListPreference.findIndexOfValue((String) newValue)]);
                    break;
                case "cur_week":
                    //curWeekListPreference.setSummary((String) newValue);
                    break;
                case "show_weekend":
                    Toast.makeText(getActivity(), R.string.change_take_effect, Toast.LENGTH_SHORT).show();
                    break;
                case "show_not_cur_week":
                    Toast.makeText(getActivity(), R.string.change_take_effect, Toast.LENGTH_SHORT).show();
                    break;
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
