package com.mobiroo.n.sourcenextcorporation.agent.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Compress;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.action.SettingsButler;
import com.mobiroo.n.sourcenextcorporation.agent.R;

import org.json.JSONObject;

import java.io.File;
import java.util.Map;

/**
 * Created by omarseyal on 2/26/14.
 */
public class DebugFileUtils {

    public static void sendEmail(Context c) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");

        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{Constants.FEEDBACK_EMAIL_ADDRESS});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Agent Feedback");
        intent.putExtra(Intent.EXTRA_TEXT, String.format("Version: %s\nOS: %s\nMake: %s/%s\nModel: %s\n\n\n",
                Utils.getPackageVersion(c), Build.VERSION.RELEASE, Build.BRAND, Build.MANUFACTURER, Build.MODEL));

        Uri uri = DebugFileUtils.generateDebugLog(c);
        if (uri != null) {
            intent.putExtra(Intent.EXTRA_STREAM, uri);
        }
        try {
            c.startActivity(Intent.createChooser(intent, c.getString(R.string.menu_chooser_mail)));
        } catch (Exception e) {
            Logger.d("DebugFileUtils: Error starting send email intent: " + e.toString());
        }
    }

    private static void logPreferences(String label, SharedPreferences prefs) {
        StringBuilder sb = new StringBuilder();
        sb.append("SharedPreferences dump of ");
        sb.append(label);
        sb.append(": \n");

        Map<String, ?> keys = prefs.getAll();

        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            sb.append("---->");
            sb.append(entry.getKey());
            sb.append(": ");
            sb.append(entry.getValue() != null ? entry.getValue().toString() : "");
            sb.append("\n");
        }

        sb.append("End of SharedPreferences dump");
        Logger.d(sb.toString(), false);
    }

    private static void logDatabase(Context context) {
        SQLiteDatabase db = TaskDatabaseHelper.getInstance(context).getReadableDatabase();
        JSONObject dbDump = TaskDatabaseHelper.generateDatabaseBackup(db);
        Logger.d("DB dump: \n" + dbDump.toString() + "\n");
    }

    public static void clearDebugLog(Context context) {
        try {
            getLogFile().delete();
            Toast.makeText(context, "Debug file cleared.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Logger.d("Could not clear log file.");
            Logger.e(e.toString());
        }
    }

    private static File getLogFile() {
        File root = Environment.getExternalStorageDirectory();
        File container = new File(root.getPath() + "/" + Logger.DIR_NAME + "/");
        return new File(container, Logger.FILE_NAME);
    }

    private static File getZipFile() {
        File root = Environment.getExternalStorageDirectory();
        File container = new File(root.getPath() + "/" + Logger.DIR_NAME + "/");
        return new File(container, Logger.FILE_NAME.replace(".", "_") + ".zip");
    }

    public static void viewDebugLog(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri debugLog = generateDebugLog(activity, true);
        if (debugLog == null) {
            return;
        }
        intent.setDataAndType(debugLog, "text/plain");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(intent);
    }

    public static Uri generateDebugLog(Context context) {
        return generateDebugLog(context, false);
    }

    protected static Uri generateDebugLog(Context context, boolean forceNoZip) {

        logPreferences("AppPrefs", PreferenceManager
                .getDefaultSharedPreferences(context));

        logPreferences(ActivityRecognitionHelper.PREFS_NAME,
                context.getSharedPreferences(ActivityRecognitionHelper.PREFS_NAME, Context.MODE_PRIVATE));
        logPreferences(Constants.PREFS_NAME, context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE));
        logPreferences(AgentPreferences.OTHER_PREFS_FILE, context.getSharedPreferences(AgentPreferences.OTHER_PREFS_FILE, Context.MODE_PRIVATE));
        logPreferences("SettingsButler", context.getSharedPreferences(SettingsButler.PREFS_FILE, Context.MODE_PRIVATE));

        logDatabase(context);

        Logger.d("Agent version: " + Utils.getPackageVersion(context));
        Logger.d("Android version: " + String.format("%s -- Make: %s/%s -- Model: %s",
                Build.VERSION.RELEASE, Build.BRAND, Build.MANUFACTURER, Build.MODEL));


        // generate debug file
        try {
            File log = getLogFile();
            if (!log.exists() || !log.canRead()) {
                Toast.makeText(context,
                        "Log does not exist or cannot be read",
                        Toast.LENGTH_LONG).show();
                return null;
            } else {
                Logger.d("Attaching " + log, false);
                if (forceNoZip || !PrefsHelper.getPrefBool(context, Constants.PREF_ZIP_DEBUG_FILE, true)) {
//                    return Uri.parse("file://" + log);
                    return getFileProviderUri(context, log);
                }

                File[] filesToCompress = {log};
                File zipFile = getZipFile();
                Compress cp = new Compress(filesToCompress, zipFile);
                cp.zip();
//                return Uri.parse("file://" + zipFile);
                return getFileProviderUri(context, zipFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static Uri getFileProviderUri(Context context, File file) {
        return FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
    }
}
