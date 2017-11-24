package com.mobiroo.n.sourcenextcorporation.agent.util;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.widget.Toast;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.item.ActivityDetectorInterface;
import com.mobiroo.n.sourcenextcorporation.agent.item.MeetingAgent;
import com.mobiroo.n.sourcenextcorporation.agent.receiver.BatteryReceiver;
import com.mobiroo.n.sourcenextcorporation.agent.receiver.BluetoothConnectionChangeReceiver;
import com.mobiroo.n.sourcenextcorporation.agent.receiver.DrivingActivityReceiver;
import com.mobiroo.n.sourcenextcorporation.agent.activity.MainActivity;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.item.BatteryAgent;
import com.mobiroo.n.sourcenextcorporation.agent.item.DriveAgent;
import com.mobiroo.n.sourcenextcorporation.agent.item.ParkingAgent;
import com.mobiroo.n.sourcenextcorporation.agent.item.SleepAgent;
import com.mobiroo.n.sourcenextcorporation.agent.receiver.AlarmReceiver;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class Utils {
    /**
     * Encodes a string so that it is suitable to be passed in to the parser after being read
     *
     * @param data
     * @return
     */
    public static String encodeData(String data) {
        String out = data;
        out = out.replace(":", "&#58");
        out = out.replace(";", "&#59");
        return out;
    }

    public static final int SILENCE_TYPE_SILENT = AudioManager.RINGER_MODE_SILENT;
    public static final int SILENCE_TYPE_VIBRATE = AudioManager.RINGER_MODE_VIBRATE;

    public static int convertBooleanToRingerType(boolean b) {
        return (b) ? SILENCE_TYPE_SILENT : SILENCE_TYPE_VIBRATE;
    }

    /**
     * Decodes a potentially encoded string after being parsed so that it is suitable for use
     *
     * @param data
     * @return
     */
    public static String decodeData(String data) {
        String out = data;
        out = out.replace("&#58", ":");
        out = out.replace("&#59", ";");
        return out;
    }

    public static String encodeURL(String url) {
        return url.replace("://", "_//");
    }

    public static String decodeURL(String url) {
        return url.replace("_//", "://");
    }


    /**
     * Tries to detect the presence of the su binary or a SuperUser APK
     *
     * @return
     */
    public static boolean isRootPresent() {
        Logger.d("Checking root");
        try {

            File file = new File("/system/app/Superuser.apk");
            File file2 = new File("/system/app/SuperSU.apk");
            if (file.exists() || file2.exists()) {
                Logger.d("Superuser APK found");
                return true;
            }
        } catch (Exception e) {
            Logger.e(Constants.TAG, "Exception thrown checking for SU APK", e);
        }

        try {
            String binaryName = "su";
            String[] places = {"/sbin/", "/system/bin/", "/system/xbin/", "/data/local/xbin/", "/data/local/bin/", "/system/sd/xbin/"};
            for (String where : places) {
                File file = new File(where + binaryName);
                if (file.exists()) {
                    Logger.d(binaryName + " was found here: " + where);
                    return true;
                }
            }
        } catch (Exception e) {
            Logger.e(Constants.TAG, "Exception locating su binary", e);
        }

        return false;
    }

    /**
     * Runs a specific command using su
     *
     * @param commands command to run
     * @throws IOException
     */
    public static String runCommandAsRoot(String[] commands) throws IOException, InterruptedException {
        String results = "";
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream STDIN = new DataOutputStream(process.getOutputStream());
            BufferedReader STDOUT = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader STDERR = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            // Write all commands
            for (int i = 0; i < commands.length; i++) {
                Logger.d("Adding command " + commands[i]);
                STDIN.writeBytes(commands[i] + "\n");
                STDIN.flush();
            }
            STDIN.writeBytes("exit\n");
            STDIN.flush();
            Logger.d("Waiting");
            process.waitFor();
            Logger.d("Done");
            if (process.exitValue() == 255) {
                Logger.d("su exited with 255");
                return null; // su denied
            }

            StringBuilder output = new StringBuilder();
            while (STDOUT.ready()) {
                String read = STDOUT.readLine();
                output.append(read);
                Logger.d("Output:" + read);
            }

            while (STDERR.ready()) {
                String read = STDERR.readLine();
                output.append(read);
                Logger.d("Error:" + read);
            }
            results = output.toString();
            process.destroy();
        } catch (IOException e) {
            throw e;
        } catch (InterruptedException e) {
            throw e;
        }
        return results;
    }

    /**
     * @return Will request WRITE_SECURE_SETTINGS permission using root and package manager.  Blocking
     * @throws IOException
     * @throws InterruptedException
     */
    public static String requestWriteSecureSettings() throws IOException, InterruptedException {
        if (Build.VERSION.SDK_INT >= 16) {
            return Utils.runCommandAsRoot(new String[]{"pm grant com.jwsoft.nfcactionlauncher android.permission.WRITE_SECURE_SETTINGS"});
        } else {
            // Must move app to /system and alert the user to reboot.
            File container = new File("/data/app/");

            String fileName = "com.jwsoft.nfcactionlauncher.apk";
            String fileBase = "com.jwsoft.nfcactionlauncher";
            File file;
            for (int i = 0; i < 10; i++) {
                file = new File(container, fileBase + "-" + i + ".apk");
                if (file.exists()) {
                    fileName = fileBase + "-" + i + ".apk";
                }
            }


            String[] commands = {
                    "busybox mount -o remount,rw /system",
                    "cat /data/app/" + fileName + " > /system/app/com.jwsoft.nfcactionlauncher.apk",
                    "chmod 644 /system/app/com.jwsoft.nfcactionlauncher.apk",
                    "mount -o remount,ro /system"
            };

            String results = Utils.runCommandAsRoot(commands);
            Logger.d("Results = " + results);
            return "REBOOT:" + results;
        }

    }


    public static int tryParseInt(String[] args, int position, int defaultValue) {
        return tryParseInt(args, position, defaultValue, "");
    }

    public static int tryParseInt(String[] args, int position, int defaultValue, String exceptionMessage) {
        int value = defaultValue;
        String message = (exceptionMessage.isEmpty()) ? "Exception parsing arg at position " + position : exceptionMessage;
        if (args.length > position) {
            try {
                value = Integer.parseInt(args[position]);
            } catch (Exception e) {
                Logger.e(Constants.TAG, message, e);
                value = defaultValue;
            }
        }
        return value;
    }

    public static String tryParseString(String[] args, int position, String defaultValue) {
        return tryParseString(args, position, defaultValue, "");
    }

    public static String tryParseString(String[] args, int position, String defaultValue, String exceptionMessage) {
        String value = defaultValue;
        String message = (exceptionMessage.isEmpty()) ? "Exception parsing arg at position " + position : exceptionMessage;
        if (args.length > position) {
            try {
                value = args[position];
            } catch (Exception e) {
                Logger.e(Constants.TAG, message, e);
                value = defaultValue;
            }
        }
        return value;
    }

    public static String tryParseEncodedString(String[] args, int position, String defaultValue) {
        return tryParseEncodedString(args, position, defaultValue, "");
    }

    public static String tryParseEncodedString(String[] args, int position, String defaultValue, String exceptionMessage) {
        String value = defaultValue;
        String message = (exceptionMessage.isEmpty()) ? "Exception parsing arg at position " + position : exceptionMessage;
        if (args.length > position) {
            try {
                value = args[position];
                value = Utils.decodeData(value);
            } catch (Exception e) {
                Logger.e(Constants.TAG, message, e);
                value = defaultValue;
            }
        }
        return value;
    }

    public static boolean tryParseBoolean(String[] args, int position, boolean defaultValue) {
        return tryParseBoolean(args, position, defaultValue, "");
    }

    public static boolean tryParseBoolean(String[] args, int position, boolean defaultValue, String exceptionMessage) {
        boolean value = defaultValue;
        String message = (exceptionMessage.isEmpty()) ? "Exception parsing arg at position " + position : exceptionMessage;
        if (args.length > position) {
            try {
                value = Boolean.parseBoolean(args[position]);
            } catch (Exception e) {
                Logger.e(Constants.TAG, message, e);
                value = defaultValue;
            }
        }
        return value;
    }

    public static boolean hasWriteSecureSettings(Context context) {
        PackageManager pm = context.getPackageManager();
        boolean hasPermission = false;
        try {
            hasPermission = (pm.checkPermission(permission.WRITE_SECURE_SETTINGS, context.getPackageName()) == PackageManager.PERMISSION_GRANTED);
            Logger.d("HasPermission? = " + hasPermission);
        } catch (Exception e) {
            Logger.e(Constants.TAG, "Exception checking for WRITE_SECURE_SETTINGS", e);
        }
        return hasPermission;
    }

    public static String scrubURI(String uri, String id) {
        String cleanUri = uri.replace(" || _id", ""); // Some custom roms are returning invalid URIs
        cleanUri = uri.replace(id + "/" + id, id);  // Some CM roms are returning the URI with an ID in it already
        return cleanUri;
    }

    public static void checkReceivers(Context context) {
        Cursor c;

        c = TaskDatabaseHelper.getEnabledTriggersOfType(TaskDatabaseHelper.getInstance(context).getReadableDatabase(), Constants.TRIGGER_TYPE_BLUETOOTH);
        modifyReceiver(context, BluetoothConnectionChangeReceiver.class, c.moveToFirst());
        c.close();


        // we active battery receiver if either sleep or battery agent is installed
        // why sleep?  because sleep can start up on charger plugged in
        Agent batteryAgent = AgentFactory.getAgentFromGuid(context, BatteryAgent.HARDCODED_GUID);
        Agent sleepAgent = AgentFactory.getAgentFromGuid(context, SleepAgent.HARDCODED_GUID);
        boolean batteryRecvEnabled = (batteryAgent.isInstalled() || sleepAgent.isInstalled());

        modifyReceiver(context, BatteryReceiver.class, batteryRecvEnabled);
        if (batteryRecvEnabled) {
            BatteryAgent.resetAlarms(context);
        } else {
            BatteryAgent.cancelAlarms(context);
        }

        AlarmReceiver.cancelAllAlarms(context);
        AlarmReceiver.setAllAlarms(context);


        HashMap<String, String> receiverResults = new HashMap<String, String>();
        boolean needsDriveAD = false;
        for (Agent agent : AgentFactory.getAllAgents(context)) {
            if (agent instanceof ActivityDetectorInterface) {
                ActivityDetectorInterface adi = (ActivityDetectorInterface) agent;
                boolean needsAD = adi.needsActivityDetection();
                modifyReceiver(context, adi.getActivityReceiverClass(), needsAD);
                if (DriveAgent.HARDCODED_GUID.equals(agent.getGuid()) || ParkingAgent.HARDCODED_GUID.equals(agent.getGuid())) {
                    needsDriveAD = (needsDriveAD || needsAD);
                }
                receiverResults.put(agent.getGuid(), adi.needsActivityDetection() ? "on" : "off");
            }
        }

        modifyReceiver(context, DrivingActivityReceiver.class, needsDriveAD);

        StringBuilder sb = new StringBuilder();
        for (String key : receiverResults.keySet()) {
            sb.append(key);
            sb.append(" recv = ");
            sb.append(receiverResults.get(key));
            sb.append(", ");
        }
        sb.append(" dar = " + needsDriveAD);
        Logger.d(sb.toString());
    }


    public static void modifyReceiver(Context context, Class<?> receiver, boolean enable) {
        try {
            context.getPackageManager().setComponentEnabledSetting(
                    new ComponentName(context, receiver),
                    enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
            );
        } catch (Exception e) {
            // Stale receiver here or now removed, database loaded receiver will cause a crash.
            Logger.e("Exception modifying receiver " + receiver.getSimpleName() + ": " + e, e);
        }
    }


    public static String getForwardTimeFromArgs(Context context, String[] args) {
        String hour = "";
        try {
            hour = (args[1] == null) ? "" : args[1];
        } catch (Exception e) {
        }

        return hour + " " + context.getString(R.string.layoutAlarmForwardText);
    }

    public static String getTimeFromArgs(String[] args) {
        String hour = "";
        try {
            hour = (args[1] == null) ? "" : args[1];
        } catch (Exception e) {
        }
        String minute = "";
        try {
            minute = (args[2] == null) ? "" : args[2];
        } catch (Exception e) {
        }

        try {
            if (Integer.parseInt(minute) < 10) {
                minute = "0" + minute;
            }
        } catch (Exception e) { /* fail silently */ }

        return hour + ":" + minute;
    }


    public static boolean isHandlerPresentForIntent(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return (resolveInfo.size() > 0) ? true : false;
    }


    public static boolean isPackageInstalled(Context context, String packageName) {

        PackageManager packageManager = context.getPackageManager();
        try {
            Intent i = packageManager.getLaunchIntentForPackage(packageName);
            return (i == null) ? false : true;
        } catch (Exception e) {
            Logger.e("Exception querying for " + packageName);
        }

        return false;

    }

    public static long getLastUpdateTime(Context context) {
        long lut;
        try {
            lut = context
                    .getPackageManager()
                    .getPackageInfo("com.mobiroo.n.sourcenextcorporation.agent", 0).lastUpdateTime;
        } catch (NameNotFoundException e) {
            Logger.d("getInstalledAt called but package not found.");
            lut = 0;
        }
        return lut;
    }

    public static void createToast(Context context, String toast_text) {
        Toast toast = Toast.makeText(context, toast_text, Toast.LENGTH_LONG);
        toast.show();
    }

    public static byte[] serializeToByteArray(Object o) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        try {
            ObjectOutputStream ou = new ObjectOutputStream(bo);
            ou.writeObject(o);
            ou.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bo.toByteArray();
    }

    public static Object deserializeFromByteArray(byte[] bytes) {
        Object o = null;

        ByteArrayInputStream bi = new ByteArrayInputStream(bytes);
        try {
            ObjectInputStream oi = new ObjectInputStream(bi);
            o = oi.readObject();
            oi.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return o;
    }

    public static String serializeToBase64String(Object o) {
        return Base64.encodeToString(serializeToByteArray(o), Base64.DEFAULT);
    }

    public static Object deserializeFromBase64String(String s) {
        return deserializeFromByteArray(Base64.decode(s, Base64.DEFAULT));
    }


    public static boolean getBoolPref(HashMap<String, String> prefs, String prefName, boolean defaultValue) {
        String val = prefs.get(prefName);
        if (val == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(val);
    }

    public static String createTimeOfDayString(int hour, int minute) {
        return String.format(Locale.US, "%02d:%02d", hour, minute);
    }

    public static int[] createTimeOfDayFromString(String timeOfDayString) {
        String[] hourAndMinute_s = timeOfDayString.split(":");
        int[] hourAndMinute_i = new int[2];

        hourAndMinute_i[0] = Integer.parseInt(hourAndMinute_s[0]);
        hourAndMinute_i[1] = Integer.parseInt(hourAndMinute_s[1]);

        return hourAndMinute_i;
    }


    public static Calendar getCalendarInstanceFromTimeString(String timeString) {
        int[] time = createTimeOfDayFromString(timeString);
        int hour = time[0];
        int minute = time[1];

        Calendar then = Calendar.getInstance();   // getInstance() will use device Timezone & Locale
        then.set(Calendar.HOUR_OF_DAY, hour);
        then.set(Calendar.MINUTE, minute);
        then.set(Calendar.SECOND, 0);
        then.set(Calendar.MILLISECOND, 0);

        return then;
    }

    public static long getMillisFromTimeString(String timeString) {
        return getCalendarInstanceFromTimeString(timeString).getTimeInMillis();
    }

    // times should be millis from Utils.getMillisFromTimeString() on settings time, e.g. 23:00
    public static boolean isWithinUserTime(Context context, long startMillis, long endMillis) {
        return Utils.isWithinUserTime(context, startMillis, endMillis, System.currentTimeMillis());
    }

    public static boolean isWithinUserTime(Context context, long startMillis, long endMillis, long nowMillis) {
        boolean during = false;

        if (startMillis > endMillis) {
            during = ((nowMillis >= startMillis) || (nowMillis < endMillis));
        } else {
            during = ((nowMillis >= startMillis) && (nowMillis < endMillis));
        }

        Logger.d(String.format("start/end/now = during: %s/%s/%s = %s",
                String.valueOf(startMillis), String.valueOf(endMillis),
                String.valueOf(nowMillis), String.valueOf(during)));

        return during;
    }


    public static String getTriggerName(Context context, int triggerType) {
        switch (triggerType) {
            case Constants.TRIGGER_ALWAYS_ACTIVE:
                return context.getResources().getString(R.string.trigger_always_active);
            case Constants.TRIGGER_TYPE_MANUAL:
                return context.getResources().getString(R.string.trigger_manual);
            case Constants.TRIGGER_TYPE_BATTERY:
                return context.getResources().getString(R.string.trigger_battery);
            case Constants.TRIGGER_TYPE_SMS:
                return context.getResources().getString(R.string.trigger_sms);
            case Constants.TRIGGER_TYPE_TIME:
                return context.getResources().getString(R.string.trigger_time);
            case Constants.TRIGGER_TYPE_BLUETOOTH:
                return context.getResources().getString(R.string.trigger_bluetooth);
            case Constants.TRIGGER_TYPE_WIFI:
                return context.getResources().getString(R.string.trigger_wifi);
            case Constants.TRIGGER_TYPE_NFC:
                return context.getResources().getString(R.string.trigger_nfc);
            case Constants.TRIGGER_TYPE_PHONE_CALL:
                return context.getResources().getString(R.string.trigger_phone_call);
            case Constants.TRIGGER_TYPE_MISSED_CALL:
                return context.getResources().getString(R.string.trigger_missed_call);
            case Constants.TRIGGER_TYPE_PARKING:
                return context.getResources().getString(R.string.trigger_parking);
            case Constants.TRIGGER_TYPE_DRIVING:
                return context.getResources().getString(R.string.trigger_driving);
            case Constants.TRIGGER_TYPE_BOOT:
                return "Boot";
            default:
                break;
        }

        return context.getResources().getString(R.string.trigger_unknown);
    }

    public static HashSet<String> getHashFromString(String str, String delim) {
        HashSet<String> hash = new HashSet<String>();
        String[] elements = str.split(delim);
        Collections.addAll(hash, elements);
        return hash;
    }

    public static String getStringFromHash(HashSet<String> hash, String delim) {
        Iterator<String> i = hash.iterator();
        StringBuilder sb = new StringBuilder();
        while (i.hasNext()) {
            sb.append(i.next());
            if (i.hasNext()) sb.append(delim);
        }
        return sb.toString();
    }

    public static boolean isPhonePluggedIn(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.getApplicationContext().registerReceiver(null, ifilter);
        return isPhonePluggedIn(batteryStatus);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static boolean isPhonePluggedIn(Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
            return true;
        }
        if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
            return false;
        }

        int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean usbCharge = (chargePlug == BatteryManager.BATTERY_PLUGGED_USB);
        boolean acCharge = (chargePlug == BatteryManager.BATTERY_PLUGGED_AC);

        boolean wirelessCharge = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            wirelessCharge = (chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS);
        }

        return (usbCharge || acCharge || wirelessCharge);
    }

    public static boolean isScreenOn(Context context) {
        return ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).isScreenOn();
    }

    public static int calculateIntegerBrightnessFromPercent(int brightnessPercent) {
        Logger.d("Brightness Percent = " + brightnessPercent);

        int iBrightnessLevel = 1;

        if (Build.MODEL.startsWith("LG-")) {
            Logger.d("Found LG device");
            int LG_MINIMUM = 110;
            double LG_STEP = 1.45;

            iBrightnessLevel = (int) (LG_MINIMUM + Math.round(brightnessPercent * LG_STEP));
            if (iBrightnessLevel < 128) {
                iBrightnessLevel = 128;
            }
        } else {
            iBrightnessLevel = Math.round(brightnessPercent * 255 / 100);
            if (iBrightnessLevel <= 0) {
                iBrightnessLevel = 1;
            }
        }

        if (iBrightnessLevel > 255)
            iBrightnessLevel = 255;

        Logger.d("Calculated integer brightness level: " + iBrightnessLevel);
        return iBrightnessLevel;
    }

    public static final int QUERY_MAX_VOLUME = -1;

    public static void playNotificationSound(Context context) {
        playNotificationSound(context, 0);
    }

    public static void playNotificationSound(Context context, long delayMillis) {
        playNotificationSound(context, delayMillis, QUERY_MAX_VOLUME);
    }

    public static void playNotificationSound(Context context, long delayMillis, int volume) {

        if (!Utils.canChangeDoNotDisturb(context)) {
            postNotification(context, new String[] {
                    Constants.PERMISSION_DO_NOT_DISTURB
            });
            return;
        }

        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);


        int stream = AudioManager.STREAM_NOTIFICATION;
        int prevVolume = am.getStreamVolume(stream);
        int prevRingerMode = am.getRingerMode();

        PriorityMode oldPriorityMode = getCurrentPriorityMode(context);

        try {
            if (delayMillis > 0) {
                Thread.sleep(delayMillis);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if ((oldPriorityMode == PriorityMode.PRIORITY) || (oldPriorityMode == PriorityMode.NONE)) {
                    am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                }
            }

            int max = am.getStreamMaxVolume(stream);
            int newVolume = ((volume == QUERY_MAX_VOLUME) || (volume > max)) ? max : volume;
            Logger.d("Utils: Setting volume temporarily to play notif: " + newVolume + "; max=" + max);
            am.setStreamVolume(stream, newVolume, 0);


            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            Uri actual = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
            if (actual != null) {
                Ringtone r = RingtoneManager.getRingtone(context, notification);
                r.setStreamType(stream);
                r.play();


                int timesLooped = 0;
                while (true) {
                    Thread.sleep(200);
                    timesLooped += 1;
                    if (!r.isPlaying()) {
                        break;
                    }
                    if (timesLooped >= 20) {
                        Logger.d("Utils: ringtone timesLooped >= 20");
                        r.stop();
                        break;
                    }
                }
            } else {
                // User has no notification tone set - Vibrate the device
                Logger.d("Utils: notification URI is null, vibrating");
                Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(700);
            }


            Logger.d("Utils: played notification");
        } catch (Exception e) {
            Logger.d("Utils: could not play notification: " + e.getMessage());
        }

        am.setStreamVolume(stream, prevVolume, 0);
        am.setRingerMode(prevRingerMode);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if ((oldPriorityMode == PriorityMode.PRIORITY) || (oldPriorityMode == PriorityMode.NONE)) {
                am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            }
        }
    }


    public static String getPackageVersion(Context c) {
        try {
            PackageInfo packageInfo = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return String.format("%s (%s)", packageInfo.versionName, packageInfo.versionCode);
        } catch (Exception e) {
            return null;
        }
    }

    public static int getPackageVersionInt(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static boolean isAirplaneModeOn(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return Settings.System.getInt(context.getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        } else {
            return Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        }
    }


    public static String getPhoneNumber(Context context) {
        TelephonyManager tMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tMgr.getLine1Number();
    }

    public static boolean isOnPhoneCall(Context context) {
        TelephonyManager tMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return (tMgr.getCallState() != TelephonyManager.CALL_STATE_IDLE);
    }

    public static String getCurrentSsid(Context context) {
        return getCurrentSsid((WifiManager) context.getSystemService(Context.WIFI_SERVICE));
    }

    public static String getCurrentSsid(WifiManager wifiManager) {
        if (!wifiManager.isWifiEnabled()) {
            return "";
        }

        String ssid = getSsid(wifiManager);
        if ("0x".equals(ssid)) {
            // Returned invalid SSID - try one more time to get a good SSID (seems like a race condition)
            ssid = getSsid(wifiManager);
        }

        Logger.d("Currently connected SSID is: " + ssid);

        return "0x".equals(ssid) ? "" : ssid;
    }

    private static String getSsid(WifiManager m) {
        String ssid = "";
        try {
            ssid = m.getConnectionInfo().getSSID();
        } catch (Exception e) {
            Logger.e("Exception getting SSID: " + e);
        }
        return (ssid != null) ? ssid : "";
    }


    public static enum PriorityMode {ALL, PRIORITY, NONE, UNKNOWN}

    ;

    @SuppressLint("NewApi")
    public static PriorityMode getCurrentPriorityMode(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return PriorityMode.UNKNOWN;
        }
        try {
            switch (Settings.Global.getInt(context.getContentResolver(), "zen_mode")) {
                case 0:
                    return PriorityMode.ALL;
                case 1:
                    return PriorityMode.PRIORITY;
                case 2:
                    return PriorityMode.NONE;
            }
        } catch (Exception e) {
            Logger.d("Error getting priority mode: " + e.toString());
        }
        return PriorityMode.UNKNOWN;
    }

    public static long getAppFirstInstallTime(Context context) {
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.firstInstallTime;
        } catch (NameNotFoundException e) {
            //should never happen
            return 0;
        }
    }

    private static Calendar getLastInstalledDate() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, 2015);
        c.set(Calendar.MONTH, 8);
        c.set(Calendar.DAY_OF_MONTH, 22);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        return c;
    }

    public static boolean shouldGrandfather(Context context) {
        Calendar cInstalled = Calendar.getInstance();
        cInstalled.setTime(new Date(getAppFirstInstallTime(context)));
        //Logger.d("IAB: Dates: " + new SimpleDateFormat("yyyy-MM-dd hh:mm").format(cInstalled.getTime()) + " < " + new SimpleDateFormat("yyyy-MM-dd hh:mm").format(getLastInstalledDate().getTime()));
        return (cInstalled.before(getLastInstalledDate()));
    }

    /**
     * Checks whether the {@code permission} is granted or not

     * @param context The context
     * @param permission Permission to checked for access
     * @return True if permission is granted, false otherwise
     */
    public static boolean isPermissionGranted(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Returns from a given set of permissions which are to be requested from the user
     *
     * @param context The context
     * @param permissions The array of permissions to be checked
     * @return List of permissions which needs to be requested from the user
     */
    public static List<String> getRequiredPermissions(Context context, @NonNull String[] permissions) {
        List<String> requiredPermissions = new ArrayList<>();

        for(String permission : permissions) {
            if(ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(permission);
            }
        }

        return requiredPermissions;
    }

    /**
     * Request user for the given permissions
     *
     * @param activity The activity which will handle the permission result callback
     * @param permission The permissions to be requested
     * @param requestCode The request code
     */
    public static void requestPermissions(Activity activity, String[] permission, int requestCode) {
        ActivityCompat.requestPermissions(activity, permission, requestCode);
    }

    public static void requestPermission(Activity activity, String permission, int requestCode) {
        requestPermissions(activity, new String[] {permission}, requestCode);
    }

    /**
     * Checks whether we have all permissions required for the {@code agent} and requests the missing
     * permissions.
     *
     * @param activity Activity to receive the permission request callback
     * @param agentGuid ID of an agent
     * @return True if all permissions are granted, false otherwise
     */
    public static boolean hasAllPermissions(Activity activity, String agentGuid) {
        List<String> requiredPermissions;

        switch (agentGuid) {
            case MeetingAgent.HARDCODED_GUID:
                requiredPermissions = getRequiredPermissions(activity, Constants.PERMISSIONS_MEETING);
                if(!requiredPermissions.isEmpty()) {
                    requestPermissions(activity,
                            requiredPermissions.toArray(new String[requiredPermissions.size()]),
                            Constants.PERMISSIONS_REQUEST_MEETING);

                    return false;
                } else {
                    return true;
                }

            case ParkingAgent.HARDCODED_GUID:
                requiredPermissions = getRequiredPermissions(activity, new String[] {
                        permission.ACCESS_FINE_LOCATION
                });

                if(!requiredPermissions.isEmpty()) {
                    requestPermissions(activity,
                            requiredPermissions.toArray(new String[requiredPermissions.size()]),
                            Constants.PERMISSIONS_REQUEST_LOCATION);

                    return false;
                } else {
                    return true;
                }
        }

        return true;
    }

    /**
     * @return True if android API version is 23 or above, false otherwise
     */
    public static boolean isMarshmallowOrUp() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    @SuppressLint("NewApi")
    public static boolean canChangeDoNotDisturb(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        return !(Utils.isMarshmallowOrUp() && !notificationManager.isNotificationPolicyAccessGranted());
    }

    public static void postNotification(Context context, @NonNull String[] permissions) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle(context.getString(R.string.notification_title));
        builder.setContentText(context.getString(R.string.notification_message));
        builder.setTicker(context.getString(R.string.notification_title));
        builder.setContentIntent(getNotificationIntent(context, permissions));
        builder.setSmallIcon(R.drawable.ic_agent_inverse);
        builder.setAutoCancel(true);

        Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
        builder.setLargeIcon(largeIcon);

        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(
                context.getString(R.string.notification_message)));

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(Constants.NOTIFICATION_ID, builder.build());
    }

    @SuppressLint("InlinedApi")
    private static PendingIntent getNotificationIntent(Context context, String[] permissions) {
        Intent intent;
        switch (permissions[0]) {
            case Constants.PERMISSION_DO_NOT_DISTURB:
                intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                break;
            case Constants.PERMISSION_WRITE_SYSTEM_SETTINGS:
                intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                break;
            default:
                intent = new Intent(context, MainActivity.class);
                intent.putExtra(Constants.EXTRAS_PERMISSIONS, permissions);
                break;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
