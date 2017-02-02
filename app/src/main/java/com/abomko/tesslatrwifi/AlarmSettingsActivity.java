package com.abomko.tesslatrwifi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

public class AlarmSettingsActivity extends AppCompatPreferenceActivity {
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

    public void openAlarm(int itemNumber) {
        Intent intent = new Intent(this, AlarmActivity.class);
        intent.putExtra("EXTRA_POSITION", getIntent().getIntExtra("EXTRA_POSITION", 0));
        intent.putExtra("EXTRA_ALARM_NUMBER", itemNumber);
        startActivityForResult(intent, 0);
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
            //getFragmentManager().beginTransaction().replace(android.R.id.content, new AlarmItemFragment()).commit();
            openAlarm(-1);
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

            int position = getActivity().getIntent().getIntExtra("EXTRA_POSITION", 0);
            prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

            int count = prefs.getInt("device_" + position + "_alarm_count", 0);
            if (count > 0) {
                for (int i = 0; i<count; i++) {
                    //show alarms
                    String title = prefs.getString("device_" + position + "_alarm_" + i + "title", "");
                    float f = 0;
                    String summary = prefs.getString("device_" + position + "_alarm_" + i + "temperature", "");
                    if (title.isEmpty() && summary.isEmpty() && i == count-1) {
                        prefs.edit().putInt("device_" + position + "_alarm_count", count - 1).apply();
                        continue;
                    }
                    CheckBoxPreference checkBoxPref = new CheckBoxPreference(this.getActivity());
                    checkBoxPref.setTitle(title);
                    checkBoxPref.setSummary(summary);
                    checkBoxPref.setChecked(true);

                    screen.addPreference(checkBoxPref);
                }
            }


            //category.addPreference(checkBoxPref);

        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            final ListView listView = (ListView) view.findViewById(android.R.id.list);
            if (listView != null) {
                registerForContextMenu(listView);
            }
        }

        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
        {
            super.onCreateContextMenu(menu, v, menuInfo);
            MenuInflater inflateLayout = getActivity().getMenuInflater();
            inflateLayout.inflate(R.menu.menu_alarm_context, menu);
        }

        public boolean onContextItemSelected(MenuItem item)
        {
            AdapterView.AdapterContextMenuInfo info =
                    (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            switch (item.getItemId())
            {
                case R.id.menu_alarm_context_edit:
                    AlarmSettingsActivity activity = (AlarmSettingsActivity)getActivity();
                    activity.openAlarm(info.position);
                    break;
                case R.id.menu_alarm_context_delete:
                    makeToast("Delete");
                    deleteItem(info.position);
                    break;
            }
            return super.onOptionsItemSelected(item);
        }

        private void deleteItem(int itemNumber) {
            int position = getActivity().getIntent().getIntExtra("EXTRA_POSITION", 0);
            int count = prefs.getInt("device_" + position + "_alarm_count", 0);

            if (itemNumber <= 0 || itemNumber > count -1) {
                return;
            }

            for (int i=itemNumber;i<count - 1;i++) {
                String titleKeyOld = "device_" + position + "_alarm_" + (i - 1) + "title";
                String temperatureKeyOld = "device_" + position + "_alarm_" + (i - 1) + "temperature";
                String titleKeyNew = "device_" + position + "_alarm_" + i + "title";
                String temperatureKeyNew = "device_" + position + "_alarm_" + i + "temperature";
                prefs.edit().putString(titleKeyOld, prefs.getString(titleKeyNew, "")).apply();
                prefs.edit().putString(temperatureKeyOld, prefs.getString(temperatureKeyNew, "")).apply();
            }

            String titleKey = "device_" + position + "_alarm_" + (count - 1) + "title";
            String temperatureKey = "device_" + position + "_alarm_" + (count - 1) + "temperature";
            prefs.edit().remove(titleKey).apply();
            prefs.edit().remove(temperatureKey).apply();
            prefs.edit().putInt("device_" + position + "_alarm_count", count - 1).apply();
        }

        public void makeToast(String message)
        {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }

    }
}
