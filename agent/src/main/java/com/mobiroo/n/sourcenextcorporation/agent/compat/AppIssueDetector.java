package com.mobiroo.n.sourcenextcorporation.agent.compat;

import android.app.AlarmManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.activity.MainActivity;
import com.mobiroo.n.sourcenextcorporation.agent.util.ActivityRecognitionHelper;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;
import com.mobiroo.n.sourcenextcorporation.agent.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AppIssueDetector {

	public static AppIssue[] getIssues(Context context,
			PackageManager pm,
			HashMap<String, String> filter) {
		List<ApplicationInfo> apps;
		ArrayList<AppIssue> issues = new ArrayList<AppIssue>();

		// initialize it once -- don't keep getting it.
		apps = pm.getInstalledApplications(PackageManager.GET_ACTIVITIES
				| PackageManager.GET_PROVIDERS);

		HashMap<String, AppIssue> appIssues = getAppIssues(context);
		Logger.i("AppIssueDetector: apps=" + apps.size() +
				", issues=" + appIssues.size() +
				", filter_size=" + (filter == null ? "zero(null)" : filter.size()));
		for (ApplicationInfo app : apps) {
			if (appIssues.containsKey(app.packageName)) {
				Logger.i("AppIssueDetector: Issue found: " + app.packageName);
				issues.add(appIssues.get(app.packageName));
			}
		}

		if (filter == null) {
			Logger.i("AppIssueDetector: Returning unfiltered list of "
					+ issues.size() + " issues.");
			return issues.toArray(new AppIssue[issues.size()]);
		}

		ArrayList<AppIssue> selectedIssues = new ArrayList<AppIssue>();

		for (int i = 0; i < issues.size(); i++) {
			if (!filter.containsKey(issues.get(i).getId())) {
				selectedIssues.add(issues.get(i));
			}
		}

		Logger.i("AppIssueDetector: Returning filtered list of "
				+ selectedIssues.size() + " issues.");

		return selectedIssues.toArray(new AppIssue[selectedIssues.size()]);

	}

	public static AppIssue[] getAllIssues(Context context) {
		return getIssues(context, context.getPackageManager(), new String[0]);
	}

	public static AppIssue[] getIssues(Context context) {
		return getIssues(context, context.getPackageManager(), PrefsHelper.getPrefStringArray(context, AgentPreferences.ISSUES_DISMISSED, "", AgentPreferences.STRING_SPLIT));
	}

	public static AppIssue[] getIssues(Context context, PackageManager pm, String[] filter) {
		HashMap<String, String> issueMap = new HashMap<String, String>();
		for (String id : filter) {
			issueMap.put(id, id);
		}
		return getIssues(context, pm, issueMap);
	}

	public static AppIssue[] getIssues(Context context, PackageManager pm) {
		return getIssues(context, pm, new HashMap<String, String>());
	}
	
	
	private static HashMap<String, AppIssue> getAppIssues(Context context) {
		HashMap<String, AppIssue> issues_all = new HashMap<String, AppIssue>();

		issues_all.put("com.jb.gosms", new AppIssue("gosmstextblock", "com.jb.gosms", AppIssue.AppIssueSeverity.ERROR, R.string.app_issue_goprosms_title, R.string.app_issue_goprosms_description, R.string.app_issue_goprosms_link));
		/*issues_all.put("com.textra", new AppIssue("textra", "com.textra", AppIssueSeverity.ERROR, R.string.app_issue_textra_title, R.string.app_issue_textra_description, -1));*/

		// if AD has never run within 15 minutes of install, create an issue
		long wat = PrefsHelper.getPrefLong(context, MainActivity.WELCOME_AGENTS_INSTALLED_AT, 0);
		if ((wat != 0) && ((System.currentTimeMillis() - wat) > AlarmManager.INTERVAL_FIFTEEN_MINUTES) && 
				(ActivityRecognitionHelper.getLastDetectionUpdate(context) == ActivityRecognitionHelper.UNKNOWN_LAST_UPDATE) &&
				ActivityRecognitionHelper.shouldStartActivityRecognition(context)) {
			issues_all.put("com.mobiroo.n.sourcenextcorporation.agent", new AppIssue("testagent", "com.mobiroo.n.sourcenextcorporation.agent", AppIssue.AppIssueSeverity.WARNING, R.string.app_issue_ad_install_title, R.string.app_issue_ad_install_description, -1));
		}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int mode = Settings.Secure.LOCATION_MODE_HIGH_ACCURACY;
            try { mode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE); }
            catch (Exception e) { Logger.e("Could not get location mode: " + e, e); }

            if ((mode == Settings.Secure.LOCATION_MODE_OFF) || (mode == Settings.Secure.LOCATION_MODE_SENSORS_ONLY)) {
                issues_all.put("com.mobiroo.n.sourcenextcorporation.agent", new AppIssue("testagent", "com.mobiroo.n.sourcenextcorporation.agent", AppIssue.AppIssueSeverity.WARNING, R.string.app_issue_location_setting_title, R.string.app_issue_location_setting_description, -1));
            }
        }

		return issues_all;
	}
}
