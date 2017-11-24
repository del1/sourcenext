package com.mobiroo.n.sourcenextcorporation.agent.compat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;

public class AppIssueDialogFactory {

	public static AlertDialog getPlainDialogForIssue(final AppIssue issue,
			final Context context) {
		// 1. Instantiate an AlertDialog.Builder with its constructor
		AlertDialog.Builder builder = new AlertDialog.Builder(context);

		LayoutInflater inflater = LayoutInflater.from(context);
		View view = inflater.inflate(R.layout.issue_dialog, null);

		// 2. Chain together various setter methods to set the dialog
		// characteristics
		((TextView) view.findViewById(R.id.title))
				.setText(R.string.app_issue_dialog_message);
		((TextView) view.findViewById(R.id.text)).setText(issue
				.getExplanation());

		if (issue.getLink() != -1) {
			((TextView) view.findViewById(R.id.link)).setText(issue.getLink());
			((TextView) view.findViewById(R.id.link))
					.setMovementMethod(LinkMovementMethod.getInstance());
			Linkify.addLinks(((TextView) view.findViewById(R.id.link)),
					Linkify.WEB_URLS);
		} else {
			view.findViewById(R.id.link).setVisibility(View.GONE);
		}

		builder.setView(view);

		// Add the buttons
		builder.setPositiveButton(R.string.app_issue_ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// User clicked OK button
					}
				});
		// 3. Get the AlertDialog from create()
		return builder.create();
	}

	public static AlertDialog getKeepSaveDialogForIssue(final AppIssue issue,
			final Context context, final Runnable positiveCallBack) {
		// 1. Instantiate an AlertDialog.Builder with its constructor
		AlertDialog.Builder builder = new AlertDialog.Builder(context);

		LayoutInflater inflater = LayoutInflater.from(context);
		View view = inflater.inflate(R.layout.issue_dialog, null);

		// 2. Chain together various setter methods to set the dialog
		// characteristics
		((TextView) view.findViewById(R.id.title))
				.setText(R.string.app_issue_dialog_message);
		((TextView) view.findViewById(R.id.text)).setText(issue.mExplanation);
		if (issue.getLink() != -1) {
			((TextView) view.findViewById(R.id.link)).setText(issue.getLink());
			((TextView) view.findViewById(R.id.link))
					.setMovementMethod(LinkMovementMethod.getInstance());
			Linkify.addLinks(((TextView) view.findViewById(R.id.link)),
					Linkify.WEB_URLS);
		} else {
			view.findViewById(R.id.link).setVisibility(View.GONE);
		}

		builder.setView(view);

		// Add the buttons
		builder.setPositiveButton(R.string.app_issue_keep,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// User clicked OK button
					}
				});
		builder.setNegativeButton(R.string.app_issue_dismiss,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// User cancelled the dialog
						PrefsHelper.getPrefString(context,
								AgentPreferences.ISSUES_DISMISSED, "");
						PrefsHelper.appendPrefStringArray(context,
								AgentPreferences.ISSUES_DISMISSED, "",
								issue.getId(), AgentPreferences.STRING_SPLIT);
						positiveCallBack.run();
					}
				});

		// 3. Get the AlertDialog from create()
		return builder.create();
	}
}
