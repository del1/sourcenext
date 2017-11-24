package com.tryagent;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.InsetActivity;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;


public class WearDisplayActivity extends InsetActivity {

    private TextView mTitle;
    private TextView mBody;
    private ImageView mIcon;

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_BODY = "extra_body";
    public static final String EXTRA_ICON = "extra_icon";

    private static final int ICON_DEFAULT = 0;
    private static final int ICON_DRIVE = 1;
    private static final int ICON_BATTERY = 2;
    private static final int ICON_MEETING = 3;
    private static final int ICON_SLEEP = 4;
    private static final int ICON_PARKING = 5;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onReadyForContent() {
        setContentView(isRound() ? R.layout.round_activity_my : R.layout.rect_activity_my);

        final int icon = getResIdForIcon(getIntent().getIntExtra(EXTRA_ICON, ICON_DEFAULT));
        final String title = getIntent().hasExtra(EXTRA_TITLE) ? getIntent().getStringExtra(EXTRA_TITLE) : "Agent";
        final String body = getIntent().hasExtra(EXTRA_BODY) ? getIntent().getStringExtra(EXTRA_BODY) : "Body";

        mTitle = (TextView) findViewById(R.id.title);
        mBody = (TextView) findViewById(R.id.body);
        mIcon = (ImageView) findViewById(R.id.icon);

        mIcon.setImageResource(icon);
        mBody.setText(body);
        mTitle.setText(title);

    }

    private int getResIdForIcon(final int icon) {
        switch (icon) {
            case ICON_BATTERY:
                return R.drawable.ic_battery_agent_color;
            case ICON_DRIVE:
                return R.drawable.ic_drive_agent_color;
            case ICON_MEETING:
                return R.drawable.ic_meeting_agent_color;
            case ICON_PARKING:
                return R.drawable.ic_parking_agent_color;
            case ICON_SLEEP:
                return R.drawable.ic_sleep_agent_color;
            default:
                return R.drawable.ic_launcher;
        }
    }
}