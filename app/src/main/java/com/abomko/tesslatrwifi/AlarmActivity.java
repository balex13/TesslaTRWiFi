package com.abomko.tesslatrwifi;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.Arrays;

public class AlarmActivity extends AppCompatPreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new AlarmItemFragment()).commit();
        this.setupActionBar();
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            preference.setSummary(value.toString());
            return true;
        }
    };

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);

            int position = getIntent().getIntExtra("EXTRA_POSITION", 0);
            int itemNumber = getIntent().getIntExtra("EXTRA_ALARM_NUMBER", 0);

            if (position > 0 && itemNumber >0) {
                String titleKey = "device_" + position + "_alarm_" + itemNumber + "title";
                actionBar.setTitle(
                    PreferenceManager.getDefaultSharedPreferences(this).getString(titleKey, "")
                );
            }
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || AlarmItemFragment.class.getName().equals(fragmentName);
    }

    public static class AlarmItemFragment extends PreferenceFragment
    {
        private SharedPreferences prefs;

        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.alarm_item);
            PreferenceScreen screen = getPreferenceScreen();

            int position = getActivity().getIntent().getIntExtra("EXTRA_POSITION", 0);
            int itemNumber = getActivity().getIntent().getIntExtra("EXTRA_ALARM_NUMBER", -1);
            prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

            int count = prefs.getInt("device_" + position + "_alarm_count", 0);

            if (itemNumber == -1) {
                itemNumber = count;
                count++;
                prefs.edit().putInt("device_" + position + "_alarm_count", count).apply();
            }

            String titleKey = "device_" + position + "_alarm_" + itemNumber + "title";
            String temperatureKey = "device_" + position + "_alarm_" + itemNumber + "temperature";
            String alarmTypeKey = "device_" + position + "_alarm_" + itemNumber + "type";

            String title = prefs.getString(titleKey, "");
            String temperature = prefs.getString(temperatureKey, "0");
            String alarmType = prefs.getString(alarmTypeKey, "1");

            EditTextPreference titlePreference = new EditTextPreference(this.getActivity());
            titlePreference.setTitle("Title");
            titlePreference.setSummary(title);
            titlePreference.setKey(titleKey);

            EditTextPreference temperaturePreference = new EditTextPreference(this.getActivity());
            temperaturePreference.setTitle("Temperature");
            temperaturePreference.setSummary(temperature);
            temperaturePreference.setKey(temperatureKey);
            TextView etpTextView = (TextView) temperaturePreference.getEditText();
            etpTextView.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL + InputType.TYPE_NUMBER_FLAG_SIGNED);

            ListPreference alarmTypePreference = new ListPreference(this.getActivity());
            alarmTypePreference.setTitle("Alarm type");
            alarmTypePreference.setKey(alarmTypeKey);
            CharSequence[] entries = { "Cooling", "Heating" };
            CharSequence[] entryValues = {"1" , "2"};
            alarmTypePreference.setSummary(entries[Arrays.asList(entryValues).indexOf(alarmType)]);
            alarmTypePreference.setEntries(entries);
            alarmTypePreference.setDefaultValue("1");
            alarmTypePreference.setEntryValues(entryValues);

            screen.addPreference(titlePreference);
            screen.addPreference(temperaturePreference);
            screen.addPreference(alarmTypePreference);

            findPreference(titleKey).setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
            findPreference(temperatureKey).setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        }

        private void updateData() {

        }


    }
}
