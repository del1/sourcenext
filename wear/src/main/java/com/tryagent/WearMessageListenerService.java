package com.tryagent;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by krohnjw on 6/27/2014.
 */
public class WearMessageListenerService extends WearableListenerService {
    private static final String START_ACTIVITY_PATH = "/start-activity";



    @Override
    public void onMessageReceived(MessageEvent event) {
        Log.d("Agent", "Message received with action " + event.getPath());
        if (event.getPath().equals(START_ACTIVITY_PATH)) {
            byte[] argBytes = event.getData();
            String data = new String(argBytes);
            Log.d("Agent", "received " + data);


            String[] args = data.split("##");

            Log.d("Agent", "Starting WearDisplayActivity");
            Intent startIntent = new Intent(this, WearDisplayActivity.class);
            startIntent.putExtra(WearDisplayActivity.EXTRA_BODY, args[2]);
            startIntent.putExtra(WearDisplayActivity.EXTRA_TITLE, args[1]);
            startIntent.putExtra(WearDisplayActivity.EXTRA_ICON, Integer.parseInt(args[0]));

            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
        }
    }
}
