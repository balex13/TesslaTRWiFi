package com.abomko.tesslatrwifi;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncStatusObserver;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.abomko.tesslatrwifi.accounts.GenericAccountService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private NotificationManager mNotificationManager;
    private ArrayList<String> listData = new ArrayList<String>();
    private ArrayList<Map<String, String>> data = new ArrayList<>();
    private SharedPreferences prefs;
    private ListView mainList;
    private MyListAdaper myListAdaper;
    private Object mSyncObserverHandle;
    private Menu mOptionsMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mainList = (ListView) findViewById(R.id.main_list);

        updateListContent();
        prefs.registerOnSharedPreferenceChangeListener(this);
        myListAdaper = new MyListAdaper(this, R.layout.list_item, listData, data);
        mainList.setAdapter(myListAdaper);
        mainList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this, "List item was clicked at " + position, Toast.LENGTH_SHORT).show();
            }
        });
        mainList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this, "List item was long clicked at " + position, Toast.LENGTH_SHORT).show();

                Intent intentAlarm = new Intent(view.getContext(), AlarmActivity.class);
                intentAlarm.putExtra("EXTRA_POSITION", position);
                startActivityForResult(intentAlarm, 0);

                return true;
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        final Context context = this.getApplicationContext();
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();

                SyncUtils.CreateSyncAccount(MainActivity.this);
                SyncUtils.TriggerRefresh();

                //mNotificationManager.notify(1, createNotification(true));
                Toast.makeText(context, "Current temperature is: " + prefs.getString("temperature", "--"), Toast.LENGTH_SHORT).show();
            }
        });
        mNotificationManager = (NotificationManager) this.getSystemService(Context
                .NOTIFICATION_SERVICE);

        SyncUtils.CreateSyncAccount(this);
    }

    private void updateListContent() {
        int deviceCount = prefs.getInt("device_count", 0);
        for(int i = 1; i <= deviceCount; i++) {
            Map<String, String> itemData = new HashMap<String, String>();
            itemData.put("main_label", prefs.getString("device_label_" + i, "Device" + i));
            String temperature = prefs.getString("temperature_" + i, "00");
            String temperatureSwitch = prefs.getString("temperature_" + i + "_switch", "00");
            boolean isPlus = Float.parseFloat(temperature) >= 0;
            itemData.put("temperature", (isPlus?"+":"-") + " " + temperature + " °C");
            isPlus = Float.parseFloat(temperatureSwitch) >= 0;
            itemData.put("temperature_switch", (isPlus?"+":"-") + " " + temperatureSwitch + " °C");

            if (data.size()<i) {
                data.add(itemData);
            } else {
                data.set(i - 1, itemData);
            }
            if (listData.size()<i) {
                listData.add(i - 1, "Device" + i);
            }
        }
        if (myListAdaper != null) {
            myListAdaper.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        getMenuInflater().inflate(R.menu.menu_refresh, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                SettingsActivity sa = new SettingsActivity();
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            // If the user clicks the "Refresh" button.
            case R.id.menu_refresh:
                SyncUtils.TriggerRefresh();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
    /**
     * Creates a new notification depending on the argument.
     *
     * @param makeHeadsUpNotification A boolean value to indicating whether a notification will be
     *                                created as a heads-up notification or not.
     *                                <ul>
     *                                <li>true : Creates a heads-up notification.</li>
     *                                <li>false : Creates a non-heads-up notification.</li>
     *                                </ul>
     *
     * @return A Notification instance.
     */
    private Notification createNotification(boolean makeHeadsUpNotification) {
        Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
//                .setPriority(Notification.PRIORITY_DEFAULT)
//                .setCategory(Notification.CATEGORY_MESSAGE)
                .setContentTitle("Sample Notification")
                .setContentText("This is a normal notification.");
        if (makeHeadsUpNotification) {
            Intent push = new Intent();
            push.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            push.setClass(this, MainActivity.class);

            PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 0,
                    push, PendingIntent.FLAG_CANCEL_CURRENT);
            notificationBuilder
                    .setContentText("Heads-Up Notification on Android L or above.")
                    .setFullScreenIntent(fullScreenPendingIntent, true);
        }
        return notificationBuilder.getNotification();// build();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateListContent();
    }


    private class MyListAdaper extends ArrayAdapter<String> {
        private int layout;
        private List<String> mObjects;
        private ArrayList<Map<String, String>> trObjects;
        private MyListAdaper(Context context, int resource, List<String> objects, ArrayList<Map<String, String>> trObjects) {
            super(context, resource, objects);
            mObjects = objects;
            this.trObjects = trObjects;
            layout = resource;
        }

        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder mainViewholder = null;
            if(convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(layout, parent, false);
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.main_label = (TextView) convertView.findViewById(R.id.main_label);
                viewHolder.temperature = (TextView) convertView.findViewById(R.id.temperature);
                viewHolder.temperature_switch = (TextView) convertView.findViewById(R.id.temperature_switch);
                viewHolder.main_label.setText(trObjects.get(position).get("main_label"));
                viewHolder.temperature.setText(trObjects.get(position).get("temperature"));
                viewHolder.temperature_switch.setText(trObjects.get(position).get("temperature_switch"));
                convertView.setTag(viewHolder);
            } else {
                mainViewholder = (ViewHolder) convertView.getTag();
                mainViewholder.main_label.setText(trObjects.get(position).get("main_label"));
                mainViewholder.temperature.setText(trObjects.get(position).get("temperature"));
                mainViewholder.temperature.setText(trObjects.get(position).get("temperature_switch"));
            }
//            mainViewholder.button.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    Toast.makeText(getContext(), "Button was clicked for list item " + position, Toast.LENGTH_SHORT).show();
//                }
//            });
//            mainViewholder.title.setText(getItem(position));

            return convertView;
        }
    }
    public class ViewHolder {
        TextView main_label;
        TextView temperature;
        TextView temperature_switch;
    }


    @Override
    public void onResume() {
        super.onResume();
        mSyncStatusObserver.onStatusChanged(0);

        // Watch for sync state changes
        final int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING |
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
        mSyncObserverHandle = ContentResolver.addStatusChangeListener(mask, mSyncStatusObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSyncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
        }
    }

    /**
     * Set the state of the Refresh button. If a sync is active, turn on the ProgressBar widget.
     * Otherwise, turn it off.
     *
     * @param refreshing True if an active sync is occuring, false otherwise
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void setRefreshActionButtonState(boolean refreshing) {
        if (mOptionsMenu == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            return;
        }

        final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
        if (refreshItem != null) {
            if (refreshing) {
                refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
            } else {
                refreshItem.setActionView(null);
            }
        }
    }

    private SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        /** Callback invoked with the sync adapter status changes. */
        @Override
        public void onStatusChanged(int which) {
            MainActivity.this.runOnUiThread(new Runnable() {
                /**
                 * The SyncAdapter runs on a background thread. To update the UI, onStatusChanged()
                 * runs on the UI thread.
                 */
                @Override
                public void run() {
                    // Create a handle to the account that was created by
                    // SyncService.CreateSyncAccount(). This will be used to query the system to
                    // see how the sync status has changed.
                    Account account = GenericAccountService.GetAccount(SyncUtils.ACCOUNT_TYPE);
                    if (account == null) {
                        // GetAccount() returned an invalid value. This shouldn't happen, but
                        // we'll set the status to "not refreshing".
                        setRefreshActionButtonState(false);
                        return;
                    }

                    // Test the ContentResolver to see if the sync adapter is active or pending.
                    // Set the state of the refresh button accordingly.
                    boolean syncActive = ContentResolver.isSyncActive(
                            account, SyncUtils.CONTENT_AUTHORITY);
                    boolean syncPending = ContentResolver.isSyncPending(
                            account, SyncUtils.CONTENT_AUTHORITY);
                    setRefreshActionButtonState(syncActive || syncPending);
                }
            });
        }
    };

}
