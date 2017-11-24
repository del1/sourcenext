package com.mobiroo.n.sourcenextcorporation.agent.util;

import android.content.Context;
import android.os.Environment;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.AppSpecific;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;

public class AgentPrefDumper {
    private static final String AGENT_DELIM = "<<ENDAGENT>>";
    private static final String ITEM_DELIM = "::::";

    public static void dumpAgentPrefs(Context context) {
        clearPrefsFile();
        if (!saveAgentPrefs(context)) {return;}

        // generate debug file
        try {
            File prefsFile = getPrefsFile();
            BufferedWriter output = new BufferedWriter(new FileWriter(prefsFile));
            for (Agent agent:AgentFactory.getAllAgents(context)) {
                output.write(agent.getGuid() + ITEM_DELIM);
                output.write(agent.getRawPreferences());
                output.write(AGENT_DELIM);
            }
            output.close();
        } catch (Exception e) {
            Logger.d("AgentPrefDumper: Error writing prefs file: " + e.toString());
            e.printStackTrace();
        }
    }

    public static void restoreAgentPrefs(Context context) {
        if (!haveSavedAgentPrefs(context)) {
            setSaveAgentPrefs(context, false);
            return;
        }
        setSaveAgentPrefs(context, true);

        try {
            Scanner scanner = new Scanner(getPrefsFile());
            scanner.useDelimiter(AGENT_DELIM);
            while (scanner.hasNext()) {
                String agentLine = scanner.next();
                String[] agentLineArr = agentLine.split(ITEM_DELIM);
                if (agentLineArr.length != 2) {
                    Logger.d("AgentPrefDumper: Bad agent line; skipping: " + agentLine);
                    continue;
                }
                Logger.d("AgentPrefDumper: Restored: " + agentLineArr[0]);
                TaskDatabaseHelper.syncAgentPrefsToDb(context, agentLineArr[0], agentLineArr[1]);
            }
            scanner.close();
        } catch (Exception e) {
            Logger.d("AgentPrefDumper: Error restoring agent prefs: " + e.toString());
            return;
        }

    }

    public static void setSaveAgentPrefs(Context context, boolean val) {
        PrefsHelper.setPrefBool(context, Constants.PREF_PRESERVE_SETTINGS_AGENT_UNINSTALL, val);
    }
    public static boolean saveAgentPrefs(Context context) {
        return PrefsHelper.getPrefBool(context, Constants.PREF_PRESERVE_SETTINGS_AGENT_UNINSTALL, false);
    }
    public static boolean haveSavedAgentPrefs(Context context) {
        File prefsFile = getPrefsFile();
        return (prefsFile.exists() && prefsFile.canRead());
    }


    private static File getPrefsFile() {
        File root = Environment.getExternalStorageDirectory();
        File container = new File(root.getPath() + "/" + Logger.DIR_NAME + "/");
        return new File(container, AppSpecific.AGENT_PREFS_DUMP_FILE_NAME);
    }

    private static void clearPrefsFile() {
        try {
            getPrefsFile().delete();
        } catch (Exception e) {
            Logger.d("AgentPrefDumper: Could not clear log file: " + e.toString());
        }
    }

}
