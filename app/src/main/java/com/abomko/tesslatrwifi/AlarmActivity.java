package com.abomko.tesslatrwifi;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;

public class AlarmActivity extends AppCompatPreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new AlarmListFragment()).commit();
        this.setupActionBar();
    }
    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
//            actionBar.
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_alarms, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        } else if (id == R.id.menu_alarms) {

        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || AlarmListFragment.class.getName().equals(fragmentName);
    }

    public static class AlarmListFragment extends PreferenceFragment
    {
        private SharedPreferences prefs;

        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.alarms);
            PreferenceScreen screen = getPreferenceScreen();
            CheckBoxPreference checkBoxPref = new CheckBoxPreference(this.getActivity());
            checkBoxPref.setTitle("title");
            checkBoxPref.setSummary("summary");
            checkBoxPref.setChecked(true);

            int position = getActivity().getIntent().getIntExtra("EXTRA_POSITION", 0);
            prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

            int count = prefs.getInt("device_" + position + "_alarm_count", 0);
            if (count > 0) {
                for (int i = 0; i<count; i++) {
                    //show alarms
                }
            }


            screen.addPreference(checkBoxPref);
            //category.addPreference(checkBoxPref);

        }
    }
}
