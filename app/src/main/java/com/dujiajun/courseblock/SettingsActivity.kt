package com.dujiajun.courseblock;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.DropDownPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.dujiajun.courseblock.constant.PreferenceKey;
import com.dujiajun.courseblock.helper.APKVersionInfoUtils;
import com.dujiajun.courseblock.helper.CourseManager;
import com.dujiajun.courseblock.helper.WeekManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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

        private static final int SHOW_YEARS = 7;
        private ListPreference curYearListPreference;
        private ListPreference curTermListPreference;
        private DropDownPreference statusPreference;
        private WeekManager weekManager;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_settings, rootKey);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            weekManager = WeekManager.getInstance(getContext());
            curYearListPreference = findPreference(PreferenceKey.CURRENT_YEAR);

            int curRealMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
            int curRealYear = Calendar.getInstance().get(Calendar.YEAR);
            if (curRealMonth < 6) {
                curRealYear--; // 6月前为上一学年，6月底学期结束，开放下一学年课表
            }

            String[] years = new String[SHOW_YEARS];
            String[] year_values = new String[SHOW_YEARS];
            String pref_values = preferences.getString(PreferenceKey.CURRENT_YEAR, CourseManager.getDefaultYear());
            String summary = "";
            for (int i = 0; i < SHOW_YEARS; i++) {
                year_values[i] = String.valueOf(i + curRealYear - SHOW_YEARS + 1);
                years[i] = ((i + curRealYear - SHOW_YEARS + 1) + "-" + (i + curRealYear - SHOW_YEARS + 2));
                if (pref_values.equals(year_values[i]))
                    summary = years[i];
            }
            curYearListPreference.setEntries(years);
            curYearListPreference.setEntryValues(year_values);


            curYearListPreference.setSummary(summary);
            curYearListPreference.setOnPreferenceChangeListener(this);


            statusPreference = findPreference(PreferenceKey.STATUS);
            statusPreference.setSummary(statusPreference.getEntry());
            statusPreference.setOnPreferenceChangeListener(this);

            SwitchPreference showNotCurWeekPreference = findPreference(PreferenceKey.SHOW_NOT_CUR_WEEK);
            SwitchPreference showWeekendPreference = findPreference(PreferenceKey.SHOW_WEEKEND);
            SwitchPreference showTimePreference = findPreference(PreferenceKey.SHOW_COURSE_TIME);

            curTermListPreference = findPreference(PreferenceKey.CURRENT_TERM);
            pref_values = preferences.getString(PreferenceKey.CURRENT_TERM, CourseManager.getDefaultTerm());
            String[] terms = new String[]{"1", "2", "3"};
            for (int i = 0; i < terms.length; i++) {
                if (pref_values.equals(terms[i]))
                    summary = getResources().getStringArray(R.array.pref_term_entries)[i];
            }
            curTermListPreference.setEntryValues(terms);
            curTermListPreference.setSummary(summary);


            curTermListPreference.setOnPreferenceChangeListener(this);

            showTimePreference.setOnPreferenceChangeListener(this);
            showWeekendPreference.setOnPreferenceChangeListener(this);
            showNotCurWeekPreference.setOnPreferenceChangeListener(this);

            Preference firstDayPreference = findPreference(PreferenceKey.FIRST_MONDAY);
            Preference lastDayPreference = findPreference(PreferenceKey.LAST_SUNDAY);
            Calendar calendar = Calendar.getInstance(Locale.CHINA);
            firstDayPreference.setOnPreferenceClickListener(preference -> {
                DatePickerDialog dialog = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
                    if (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                        Toast.makeText(getActivity(), R.string.change_to_monday, Toast.LENGTH_SHORT).show();
                    }
                    weekManager.setFirstDay(year, month, dayOfMonth);
                    preference.setSummary(weekManager.getShowFirstDate());
                    calendar.setTime(weekManager.getFirstDate());
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                dialog.getDatePicker().setFirstDayOfWeek(Calendar.MONDAY);
                dialog.show();
                return true;
            });
            lastDayPreference.setOnPreferenceClickListener(preference -> {
                DatePickerDialog dialog = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
                    if (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                        Toast.makeText(getActivity(), R.string.change_to_sunday, Toast.LENGTH_SHORT).show();
                    }
                    weekManager.setLastDay(year, month, dayOfMonth);
                    preference.setSummary(weekManager.getShowLastDate());
                    calendar.setTime(weekManager.getLastDate());
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                dialog.getDatePicker().setFirstDayOfWeek(Calendar.SUNDAY);
                dialog.show();
                return true;
            });
            firstDayPreference.setSummary(weekManager.getShowFirstDate());
            lastDayPreference.setSummary(weekManager.getShowLastDate());

            Preference homepagePreference = findPreference("homepage");
            homepagePreference.setOnPreferenceClickListener(preference -> {
                openUrl("https://github.com/dujiajun/CourseBlock/");
                return false;
            });

            Preference feedbackPreference = findPreference("feedback");
            feedbackPreference.setOnPreferenceClickListener(preference -> {
                openUrl("https://github.com/dujiajun/CourseBlock/issues");
                return false;
            });

            checkNewVersion();
        }

        private void checkNewVersion() {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://api.github.com/repos/dujiajun/CourseBlock/releases/latest")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {

                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String body = response.body().string();
                    try {
                        JSONObject json = new JSONObject(body);
                        String tag = json.getString("tag_name");
                        JSONObject asset = json.getJSONArray("assets").getJSONObject(0);
                        String download_url = asset.getString("browser_download_url");
                        String cur_version = 'v' + APKVersionInfoUtils.getVersionName(getActivity());
                        if (cur_version.compareTo(tag) < 0) {
                            getActivity().runOnUiThread(() -> {
                                Preference homepagePreference = findPreference("homepage");
                                homepagePreference.setSummary("有新版本，点击下载");
                                homepagePreference.setOnPreferenceClickListener(preference -> {
                                    openUrl(download_url);
                                    return false;
                                });
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });
        }

        private void openUrl(String url) {
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            Uri content_url = Uri.parse(url);
            intent.setData(content_url);
            startActivity(intent);
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            return true;
        }


        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            switch (preference.getKey()) {
                case PreferenceKey.CURRENT_YEAR -> {
                    if (Integer.parseInt((String) newValue) < 2018
                            && curTermListPreference.getEntryValues() != null
                            && curTermListPreference.getEntryValues()[2].equals(newValue)) {
                        Toast.makeText(getActivity(), R.string.summer_term, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    curYearListPreference.setSummary(curYearListPreference.getEntries()
                            [curYearListPreference.findIndexOfValue((String) newValue)]);
                }
                case PreferenceKey.CURRENT_TERM -> {
                    if (curYearListPreference.getValue() != null
                            && Integer.parseInt(curYearListPreference.getValue()) < 2018
                            && curTermListPreference.getEntryValues()[2].equals(newValue)) {
                        Toast.makeText(getActivity(), R.string.summer_term, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    curTermListPreference.setSummary(curTermListPreference.getEntries()
                            [curTermListPreference.findIndexOfValue((String) newValue)]);
                }
                case PreferenceKey.STATUS ->
                        statusPreference.setSummary(statusPreference.getEntries()[statusPreference.findIndexOfValue((String) newValue)]);
                case PreferenceKey.SHOW_WEEKEND, PreferenceKey.SHOW_COURSE_TIME, PreferenceKey.SHOW_NOT_CUR_WEEK, "use_chi_icon", PreferenceKey.FIRST_MONDAY, PreferenceKey.LAST_SUNDAY ->
                        Toast.makeText(getActivity(), R.string.change_take_effect, Toast.LENGTH_SHORT).show();
                default -> {
                }
            }
            return true;
        }
    }
}
