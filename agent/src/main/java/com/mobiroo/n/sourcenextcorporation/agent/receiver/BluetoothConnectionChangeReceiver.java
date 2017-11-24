package com.mobiroo.n.sourcenextcorporation.agent.receiver;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.item.DbAgent;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.TaskDatabaseHelper;

public class BluetoothConnectionChangeReceiver extends BroadcastReceiver {
    public static final String BLUETOOTH_CONNECTED = "android.bluetooth.device.action.ACL_CONNECTED";
    public static final String BLUETOOTH_DISCONNECTED = "android.bluetooth.device.action.ACL_DISCONNECTED";

    @Override
    public void onReceive(Context context, Intent intent) {
        BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (device == null) {
            Logger.i("BluetoothConnectionChangeReceiver: device not found.");
            return;
        }

        String action = intent.getAction();
        if (! (action.equals(BLUETOOTH_CONNECTED) || action.equals(BLUETOOTH_DISCONNECTED))) {
            return;
        }
        boolean enableDisable = action.equals(BLUETOOTH_CONNECTED);


        String mac = null;

        try {
            mac = device.getAddress();
            Logger.i("Bluetooth received; mac = " + mac + " enableDisable: " + String.valueOf(enableDisable));
        } catch (Exception e) {
            Logger.i("BluetoothConnectionChangeReceiver: failed to get device mac.  Exception caught.");
            Logger.i(e.getMessage());
            return;
        }

        SQLiteDatabase db = TaskDatabaseHelper.getInstance(context).getReadableDatabase();
        Cursor c = TaskDatabaseHelper.getEnabledTriggersOfType(db, Constants.TRIGGER_TYPE_BLUETOOTH);
        if (! c.moveToFirst()) {c.close(); return;}

        do {
            int triggerId = c.getInt(c.getColumnIndex(TaskDatabaseHelper.FIELD_ID));
            String agentGuid = c.getString(c.getColumnIndex(TaskDatabaseHelper.FIELD_GUID));
            String matchMac = c.getString(c.getColumnIndex(TaskDatabaseHelper.FIELD_KEY_1));

            if ((matchMac == null) || (! mac.equals(matchMac))) {
                Logger.i("BluetoothConnectionChangeReceiver: mac did not match.");
                continue;
            }

            if (! AgentFactory.getAgentFromGuid(context, agentGuid).isInstalled()) {
                Logger.i("BluetoothConnectionChangeReceiver: Ignoring " + matchMac + " as " + agentGuid + " is not installed");
                continue;
            }


            if (enableDisable) {
                Logger.i("Bluetooth Connection trigger: fired for trigger id: " + String.valueOf(triggerId));
                DbAgent.setActive(context, agentGuid, Constants.TRIGGER_TYPE_BLUETOOTH);
            } else {
                Logger.i("Bluetooth Disconnection trigger: fired for trigger id: " + String.valueOf(triggerId));
                DbAgent.setInactive(context, agentGuid, Constants.TRIGGER_TYPE_BLUETOOTH);
            }


        } while (c.moveToNext());

        c.close();
    }

}
