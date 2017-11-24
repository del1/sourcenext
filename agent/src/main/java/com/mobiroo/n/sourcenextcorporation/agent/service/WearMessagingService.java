package com.mobiroo.n.sourcenextcorporation.agent.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.R;

/**
 * Created by krohnjw on 6/27/2014.
 */
public class WearMessagingService extends Service implements MessageApi.MessageListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    public static final String START_ACTIVITY_PATH = "/start-activity";
    public static final String STOP_ACTIVITY_PATH  = "/stop-activity";
    public static final String ACTIVITY_STOPPED_PATH = "/activity-stopped";

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_BODY = "extra_body";
    public static final String EXTRA_ICON = "extra_icon";
    public static final String EXTRA_NOTIFICATION = "extra_notification";

    private static final int ICON_DRIVE = 1;
    private static final int ICON_BATTERY = 2;
    private static final int ICON_MEETING = 3;
    private static final int ICON_SLEEP = 4;
    private static final int ICON_PARKING = 5;

    public static final String EXTRA_ACTION = "pending_action";

    public static final int START_ACTIVITY = 1;
    public static final int STOP_ACTIVITY = 2;
    private int mAction;

    private GoogleApiClient mGoogleApiClient;

    private byte[] mPayload;
    private String mPath;

    private int getIdFromResource(int resId) {

        switch(resId) {
            case R.drawable.ic_battery_agent_color:
                return ICON_BATTERY;
            case R.drawable.ic_drive_agent_color:
                return ICON_DRIVE;
            case R.drawable.ic_meeting_agent_color:
                return ICON_MEETING;
            case R.drawable.ic_sleep_agent_color:
                return ICON_SLEEP;
            case R.drawable.ic_parking_agent_color:
                return ICON_PARKING;
        }
        return 0;
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int args) {
        Logger.d("Starting service to send message to wear with action " + intent.getAction());
        mAction = intent.getIntExtra(EXTRA_ACTION, STOP_ACTIVITY);
        final int icon = getIdFromResource(intent.getIntExtra(EXTRA_ICON, 0));
        final String title = intent.hasExtra(EXTRA_TITLE) ? intent.getStringExtra(EXTRA_TITLE) : "Agent";
        final String body = intent.hasExtra(EXTRA_BODY) ? intent.getStringExtra(EXTRA_BODY) : "Body";

        String payload = icon + "##" + title + "##" + body;
        Logger.d("Sending payload" + payload);

        mPayload = payload.getBytes();
        mPath = (mAction == START_ACTIVITY) ? START_ACTIVITY_PATH : STOP_ACTIVITY_PATH;

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onDestroy();

    }

    @Override
    public void onConnected(Bundle bundle) {
        Logger.d("Client connected");
        Wearable.MessageApi.addListener(mGoogleApiClient, this);

        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(
                new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                        for (final Node node : getConnectedNodesResult.getNodes()) {
                            Logger.d("Sending message to node " + node.getId());
                            Wearable.MessageApi.sendMessage(
                                    mGoogleApiClient, node.getId(), mPath, mPayload)
                                    .setResultCallback(getSendMessageResultCallback());
                        }
                    }
                });

    }

    private ResultCallback<MessageApi.SendMessageResult> getSendMessageResultCallback() {
        return new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                if (!sendMessageResult.getStatus().isSuccess()) {
                    Logger.d("Failed to connect to Google Api Client with status "
                            + sendMessageResult.getStatus());
                } else {
                    Logger.d("Got result " + sendMessageResult.getStatus());
                }
            }
        };
    }

    @Override
    public void onConnectionSuspended(int i) {
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Logger.d("Failed to connect to Google client API");
    }
}
