package com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.R;

import com.mobiroo.n.sourcenextcorporation.agent.service.WearMessagingService;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.activity.MainActivity;


public class NotificationFactory {
    public static final int ID_NOTIF_MAIN = 100;
    public static final int ID_NOTIF_STATUS = 200;
    public static final int ID_NOTIF_WIFI_LEARNING = 300;

    public static final String STATUS_NOTIFICATION = "StatusNotification";

    public static interface CustomDispatch {
        public void dispatchNotification();
    }

    public static void dismissMain(Context context, String agentGuid) {
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(context);

        notificationManager.cancel(agentGuid, ID_NOTIF_MAIN);
    }


    public static void dismissWithTag(Context context, String tag, int agentId) {
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(context);
        notificationManager.cancel(tag, agentId);
    }

    public static Notification buildWearNotification(Context context, AgentNotification agentNotification) {
        return buildNotification(context, agentNotification, false, null);
    }

    public static Notification buildNotification(Context context, AgentNotification agentNotification) {
        return buildNotification(context, agentNotification, false, null);
    }

    public static Notification buildNotification(Context context, AgentNotification agentNotification, boolean extend, PendingIntent wearPendingIntent) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context);

        NotificationCompat.WearableExtender extender = new NotificationCompat.WearableExtender();

        // ----------------
        //   build basics
        // ----------------
        builder.setSmallIcon(agentNotification.getAgentWhiteIcon());
        builder.setContentTitle(agentNotification.getTitle());
        builder.setContentText(agentNotification.getMessage());
        builder.setTicker(agentNotification.getTicker());
        builder.setWhen(System.currentTimeMillis());

        Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
        builder.setLargeIcon(largeIcon);

        // ----------------------------------
        //   build large-style notification
        // ----------------------------------
        if(agentNotification.getDetails() != null) {
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

            /*for(String line : agentNotification.getDetails().getNotificationLineItems()) {
                inboxStyle.addLine(line);
            }*/
            inboxStyle.setBigContentTitle(agentNotification.getTitle());

            builder.setStyle(inboxStyle);
        } else {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(agentNotification.getMessage()));
        }

        // if actions exist, add actions to notification
        for (AgentNotificationAction action : agentNotification.getActions()) {
            builder.addAction(action.getIconId(), action.getActionName(), action.getIntent());
            if (extend) extender.addAction(new NotificationCompat.Action(action.getIconId(), action.getActionName(), action.getIntent()));
        }

        // -----------------
        //   build intents
        // -----------------

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);

        // Adds the Intent that starts the Activity to the top of the stack

        if(agentNotification.getStackedIntent() != null) {
            stackBuilder.addNextIntent(agentNotification.getStackedIntent());
        } else {
            Intent resultIntent = new Intent(context,
                    MainActivity.class);
            stackBuilder.addNextIntent(resultIntent);
        }

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        if (extend) {
            //extender.setDisplayIntent(wearPendingIntent);
            builder.extend(extender);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (((agentNotification instanceof AgentStartingNotification) ||
                    (agentNotification instanceof AgentFirstStartingNotification) ||
                    (agentNotification instanceof AgentDelayNotification))) {
                builder.setPriority(Notification.PRIORITY_MAX);
            }
        }

        return builder.setAutoCancel(extend || (!agentNotification.isOngoing())).setOngoing(!extend && agentNotification.isOngoing()).build();
    }

    public static void notify(Context context, AgentNotification agentNotification) {
        if (agentNotification instanceof CustomDispatch) {
            Logger.d("isCustom");
            ((CustomDispatch) agentNotification).dispatchNotification();
            return;
        }


        Notification notification =  buildNotification(context, agentNotification, false, null);
        //Notificaiton wear = buildWearNotification(conteification);

        if (agentNotification.isOngoing()) { notification.flags |= Notification.FLAG_ONGOING_EVENT; }
/*
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(context);*/

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        SharedPreferences p = context.getSharedPreferences(Constants.PREFS_NAME, 0);
        int n = p.getInt(Constants.PREF_NOTIFICATIONS, Constants.PREF_VAL_NOTIFICATIONS_ONE);

        switch(n) {
            case Constants.PREF_VAL_NOTIFICATIONS_ONE:
                notificationManager.cancel(agentNotification.getNotifTag(), agentNotification.getNotifId());
                notificationManager.notify(agentNotification.getNotifTag(), agentNotification.getNotifId(), notification);

                Intent wear_activity = new Intent(context, WearMessagingService.class);

                wear_activity.setAction(WearMessagingService.START_ACTIVITY_PATH);
                wear_activity.putExtra(WearMessagingService.EXTRA_ACTION, WearMessagingService.START_ACTIVITY);
                wear_activity.putExtra(WearMessagingService.EXTRA_ICON, agentNotification.getAgent().getColorIconId());
                wear_activity.putExtra(WearMessagingService.EXTRA_TITLE, agentNotification.getTitle());
                String message = "";
                if (agentNotification.getDetails() != null) {

                    StringBuilder b = new StringBuilder();
                   /* for (String line : agentNotification.getDetails().getNotificationLineItems()) {
                        b.append(line + "\n");
                    }*/
                    message = b.toString();
                } else {
                    message = agentNotification.getMessage();
                }
                Logger.d(String.format("Info %s, %s, %s", agentNotification.getAgent().getColorIconId(), agentNotification.getTitle(), message));

                wear_activity.putExtra(WearMessagingService.EXTRA_BODY, message);

                // Disable wear messaging service as we aren't using it currently
                //context.startService(wear_activity);
                break;
            case Constants.PREF_VAL_NOTIFICATIONS_NONE:
                return;
        }

    }



}