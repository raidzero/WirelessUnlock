package com.raidzero.wirelessunlock.activities;

import android.content.*;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import com.raidzero.wirelessunlock.*;
import com.raidzero.wirelessunlock.adapters.DeviceListAdapter;
import com.raidzero.wirelessunlock.global.AppDevice;
import com.raidzero.wirelessunlock.global.AppHelper;
import com.raidzero.wirelessunlock.global.Common;
import com.raidzero.wirelessunlock.global.DeviceListLoader;

import java.util.ArrayList;

/**
 * Created by raidzero on 5/7/14 7:51 PM
 */
public class MainActivity extends ActionBarActivity {

    private static final String tag = "WirelessUnlock/MainActivity";

    public static AppHelper appHelper;

    private MessageReceiver messageReceiver;

    // views
    private static TextView lockStatusView = null;
    private static ListView trustedBluetoothList = null;
    private static ListView trustedWifiList = null;

    // lists
    ArrayList<AppDevice> trustedBluetoothDevices = new ArrayList<AppDevice>();
    ArrayList<AppDevice> trustedWifiNetworks = new ArrayList<AppDevice>();

    // adapters
    DeviceListAdapter btAdapter;
    DeviceListAdapter wifiAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        appHelper = (AppHelper) getApplicationContext();

        lockStatusView = (TextView) findViewById(R.id.textView_lockStatus);

        trustedBluetoothList = (ListView) findViewById(R.id.list_trusted_bluetooth_devices);
        trustedWifiList = (ListView) findViewById(R.id.list_trusted_wifi_devices);

        loadDevices();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_add_bluetooth:
                addBluetoothDevice();
                return true;
            case R.id.action_add_wifi:
                addWifiNetwork();
                return true;
            case R.id.action_log:
                openLog();
                return true;
            case R.id.action_settings:
                openSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        btAdapter = new DeviceListAdapter(this, trustedBluetoothDevices, true);
        wifiAdapter = new DeviceListAdapter(this, trustedWifiNetworks, true);

        loadDevices();

        trustedBluetoothList.setAdapter(btAdapter);
        trustedWifiList.setAdapter(wifiAdapter);

        trustedBluetoothList.setOnItemClickListener(bluetoothClickListener);
        trustedWifiList.setOnItemClickListener(wifiClickListener);

        // register message receiver if not already
        if (messageReceiver == null) {
            messageReceiver = new MessageReceiver();
        }
        IntentFilter ifilter = new IntentFilter(Common.messageIntent);
        registerReceiver(messageReceiver, ifilter);

        appHelper.processChanges();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // unregister message receiver
        if (messageReceiver != null) {
            unregisterReceiver(messageReceiver);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        writeDeviceFile();
    }

    private void writeDeviceFile() {
        ArrayList<AppDevice> allTrustedDevices = new ArrayList<AppDevice>();
        allTrustedDevices.addAll(trustedBluetoothDevices);
        allTrustedDevices.addAll(trustedWifiNetworks);

        try {
            DeviceListLoader.writeDeviceList(allTrustedDevices, openFileOutput(Common.deviceFile, Context.MODE_PRIVATE));
        } catch (Exception e) {
            // nothing
        }

        appHelper.processChanges();
    }

    private void loadDevices() {
        try {
            trustedBluetoothDevices = DeviceListLoader.loadDeviceList(
                    AppDevice.DeviceType.BLUETOOTH, openFileInput(Common.deviceFile));
            trustedWifiNetworks = DeviceListLoader.loadDeviceList(
                    AppDevice.DeviceType.WIFI, openFileInput(Common.deviceFile));

            Log.d(tag, String.format("Got %d BT devices", trustedBluetoothDevices.size()));
            Log.d(tag, String.format("Got %d Wifi devices", trustedWifiNetworks.size()));
        } catch (Exception e) {
            // nothing
        }

        Log.d(tag, "loadDevices() done");
    }

    public class MessageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(tag, "MessageReceiver!");

            String lockStatus = intent.getStringExtra("message");

            Log.d(tag, "lockStatus: " + lockStatus);

            if (lockStatus != null) {
                lockStatusView.setText(lockStatus);
            }
        }
    }

    private void openLog() {
        Intent i = new Intent(this, LogActivity.class);
        startActivity(i);
    }

    private void openSettings() {
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
    }

    private void addWifiNetwork() {
        Intent i = new Intent(this, AddWifiActivity.class);
        startActivityForResult(i, Common.addDeviceRequestCode);
    }

    private void addBluetoothDevice() {
        Intent i = new Intent(this, AddBluetoothActivity.class);
        startActivityForResult(i, Common.addDeviceRequestCode);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case Common.addDeviceRequestCode:
                    AppDevice d = (AppDevice) data.getExtras().getParcelable("device");
                    Log.d(tag, "Received deviceName: " + d.getName());

                    if (d.getType() == AppDevice.DeviceType.BLUETOOTH) {
                        if (!trustedBluetoothDevices.contains(d)) {
                            trustedBluetoothDevices.add(d);
                            btAdapter.notifyDataSetChanged();
                        }
                    }

                    if (d.getType() == AppDevice.DeviceType.WIFI ) {
                        if (!trustedWifiNetworks.contains(d)) {
                            trustedWifiNetworks.add(d);
                            wifiAdapter.notifyDataSetChanged();
                        }
                    }

                    writeDeviceFile();
                    //appHelper.processChanges();
                    break;
            }
        }
    }

    // click listeners
    AdapterView.OnItemClickListener wifiClickListener = new AdapterView.OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            Log.d(tag, "clickListener fired on " + position);
            AppDevice d = trustedWifiNetworks.get(position);

            Intent i = new Intent(getApplicationContext(), DeviceSettingsActivity.class);
            i.putExtra("device", d);
            startActivity(i);
        }
    };

    AdapterView.OnItemClickListener bluetoothClickListener = new AdapterView.OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            Log.d(tag, "clickListener fired on " + position);
            AppDevice d = trustedBluetoothDevices.get(position);

            Intent i = new Intent(getApplicationContext(), DeviceSettingsActivity.class);
            i.putExtra("device", d);
            startActivity(i);
        }
    };

}
