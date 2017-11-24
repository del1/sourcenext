package com.mobiroo.n.sourcenextcorporation.agent.service;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.activity.MainActivity;
import com.mobiroo.n.sourcenextcorporation.agent.billing.IabClient;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;

import java.util.HashMap;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * helper methods.
 */
public class ExpireTrialIntentService extends IntentService {

    private static final String EXTRA_SHOW_NOTIFICATION = "show_notification";

    public ExpireTrialIntentService() {
        super("ExpireTrialIntentService");
    }

    public static Intent getStartIntent(Context context, boolean show_notification) {
        Intent i = new Intent(context, ExpireTrialIntentService.class);
        i.putExtra(EXTRA_SHOW_NOTIFICATION, show_notification);
        return i;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            if (IabClient.checkLocalUnlock(this)) {
                return;
            }

            List<Agent> agents = AgentFactory.getAllAgents(this);
            HashMap<String, Agent> agentsHash = new HashMap<String, Agent>(agents.size());

            for (Agent agent : agents) {
                agentsHash.put(agent.getGuid(), agent);
                agent.uninstall(this, false);
            }
            Utils.checkReceivers(this);
            IabClient.endTrial(this);
            if (intent != null && intent.hasExtra(EXTRA_SHOW_NOTIFICATION)) {
                try {
                    if (intent.getExtras().getBoolean(EXTRA_SHOW_NOTIFICATION, false)) {
                        if (!PrefsHelper.getPrefBool(this, "has_shown_expired", false)) {
                            PrefsHelper.setPrefBool(this, "has_shown_expired", true);
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
                            builder.setSmallIcon(R.drawable.ic_agent_inverse);
                            Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
                            builder.setLargeIcon(largeIcon);
                            builder.setContentIntent(PendingIntent.getActivity(
                                    this,
                                    0,
                                    new Intent(this, MainActivity.class),
                                    PendingIntent.FLAG_ONE_SHOT
                            ));
                            builder.setWhen(System.currentTimeMillis());
                            builder.setContentTitle(getString(R.string.trial_expired_ticker));
                            builder.setTicker(getString(R.string.trial_expired_ticker));
                            String message = getString(R.string.trial_expired_message);
                            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
                            builder.setContentText(message);
                            NotificationManagerCompat manager = NotificationManagerCompat.from(this);
                            manager.notify(1, builder.build());
                        }

                    }
                } catch (Exception e) { }
            }
        }
    }

}
