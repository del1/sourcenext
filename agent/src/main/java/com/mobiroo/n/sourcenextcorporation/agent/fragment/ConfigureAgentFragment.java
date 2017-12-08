package com.mobiroo.n.sourcenextcorporation.agent.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.item.MeetingAgent;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.animation.FadeAnimation;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentUIElement;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentStateListener;
import com.mobiroo.n.sourcenextcorporation.agent.util.Usage;
import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.activity.AgentConfigurationActivity;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.tasks.AgentTaskCollection;

import java.util.List;

@SuppressWarnings("rawtypes")
public class ConfigureAgentFragment extends Fragment {
	public static final String AGENT_GUID = "agent_guid";

	protected String mAgentGuid;

	SparseArray<SettingSaver> mActivityResultLookup;
	LinearLayout mBlocker;
	TextView mBlockerText;
	View mContent;
	LinearLayout mAdvancedConfigItems;
	LinearLayout mMoreSettingsButtonContainer;

	AgentStateListener mAgentStateListener;

	protected AgentTaskCollection mTaskCollection;
	protected boolean mMoreShown;

	public void addAgentStateListener(AgentStateListener agentStateListener) {
		mAgentStateListener = agentStateListener;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (outState != null) {
			outState.putBoolean("mMoreShown", mMoreShown);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mTaskCollection.cancelTasks();
	}

	public class ConfigureAgentFragmentWrapper implements
			AgentConfigurationProvider {

		@Override
		public Activity getActivity() {
			return ConfigureAgentFragment.this.getActivity();
		}

		public void updateSetting(String prefName, String prefVal,
								  Runnable onPostRunnable) {
			new UpdateSettingTask(getActivity(), prefName, prefVal,
					onPostRunnable).execute();
		}

		public void updateSettingSilent(String prefName, String prefVal) {
			new UpdateSettingTask(getActivity(), null, prefName, prefVal)
					.execute();
		}

		public void updateSetting(String agentName, String prefName, String prefVal) {
			Logger.i("prop data for " + agentName);
			new UpdateSettingTask(getActivity(), null, agentName, prefName, prefVal)
					.execute();
		}

		public void updateSetting(String prefName, String prefVal) {
			if(isAdded()) {
				ProgressDialog dialog = ProgressDialog.show(this.getActivity(), "",
						getString(R.string.agent_updating_settings), true);

				dialog.setIndeterminateDrawable(
						getResources().getDrawable(R.drawable.agent_animation_agent));

				new UpdateSettingTask(getActivity(), dialog, prefName, prefVal).execute();
			}
		}

		@Override
		public void startUpdateSettingActivityForResult(Intent intent,
														int resultCode) {
			ConfigureAgentFragment.this.startActivityForResult(intent,
					resultCode);
		}

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mTaskCollection = new AgentTaskCollection();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		Bundle args = getArguments();
		mAgentGuid = args.getString(AGENT_GUID);

		if (savedInstanceState != null) {
			mMoreShown = savedInstanceState.getBoolean("mMoreShown", false);
		} else {
			mMoreShown = false;
		}

		AgentConfigurationActivity activity = (AgentConfigurationActivity) getActivity();
		activity.addAgentStateListener(new ConfigurationFragmentAgentListener());

		Agent agent = AgentFactory.getAgentFromGuid(
				getActivity(), mAgentGuid);

		//getting all the views dynamically in mConent variable...
		mContent = inflater.inflate(R.layout.fragment_agent_config, null);

		//generating the setting in the else block...
		if(Utils.isMarshmallowOrUp() && isMeetingAgent()) {
			checkPermissions(inflater, mContent);
		} else {
			generateSettings(inflater, mContent);
		}

		// one time ui actions below
		mAdvancedConfigItems = ((LinearLayout) mContent
				.findViewById(R.id.advanced_configuration_items_wrapper));
		mMoreSettingsButtonContainer = (LinearLayout) mContent
				.findViewById(R.id.more_settings_button_container);

		//on clicking the button the settings/configuration tab gets maximized down...
		mContent.findViewById(R.id.more_settings_text).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						mMoreShown = true;
						Usage.logEvent(getActivity(), Usage.Events.MORE_SETTINGS_CLICKED, true,
								new Usage.EventProperty(Usage.Properties.AGENT_NAME, mAgentGuid));
						FadeAnimation.crossfade(mAdvancedConfigItems,
								mMoreSettingsButtonContainer, 1000);
					}
				});

		mContent.findViewById(R.id.reset_text).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {

						new AlertDialog.Builder(getActivity())
								.setIcon(R.drawable.ic_launcher)
								.setTitle(R.string.agent_uninstall_confirm)
								.setMessage(R.string.agent_confirm_reset_hal)
								.setPositiveButton(R.string.yes,
										new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog,
																int which) {
												ProgressDialog pd = ProgressDialog.show(getActivity(), "",
														getString(R.string.agent_updating_settings), true);
												pd.setIndeterminateDrawable(getResources().getDrawable(
														R.drawable.agent_animation_agent));
												new ResetSettingsTask(getActivity(), pd)
														.execute();
											}
										})
								.setNegativeButton(R.string.no, null)
								.show();

					}
				});

		//more settings button is clicked and more settings are shown...
		if (mMoreShown) {
			mAdvancedConfigItems.setVisibility(View.VISIBLE);
			mMoreSettingsButtonContainer.setVisibility(View.GONE);
		}


		//if the agent is disabled or uninstalled the setting cannot be changed...
		mBlocker = (LinearLayout) View.inflate(getActivity(),
				R.layout.fragment_agent_config_blocker, null);
		mBlockerText = (TextView) mBlocker.findViewById(R.id.blocker_text);
		((FrameLayout) mContent.findViewById(R.id.config_frame))
				.addView(mBlocker);

		//the agent is enabled...
		if (agent.isInstalled()) {
			if (agent.isActive() && (!agent.isPaused())) {
				mBlocker.setVisibility(View.VISIBLE);
				mBlockerText.setText(R.string.agent_config_blocker_started);

			} else {
				mBlocker.setVisibility(View.GONE);
			}
		} else {
			mBlocker.setVisibility(View.VISIBLE);
			mBlockerText.setText(R.string.agent_config_blocker_disabled);
		}

		return mContent;
	}

	private boolean isMeetingAgent() {
		return mAgentGuid != null && mAgentGuid.equals(MeetingAgent.HARDCODED_GUID);
	}

	private void checkPermissions(LayoutInflater inflater, View contentView) {

		List<String> requiredPermissions = Utils.getRequiredPermissions(getActivity(), new String[] {
				Manifest.permission.READ_CALENDAR, Manifest.permission.GET_ACCOUNTS
		});

		if (requiredPermissions.isEmpty()) {
			generateSettings(inflater, contentView);
		} else {
			Utils.requestPermissions(getActivity(),
					requiredPermissions.toArray(new String[requiredPermissions.size()]),
					Constants.PERMISSIONS_REQUEST_MEETING);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
										   @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		switch (requestCode) {
			case Constants.PERMISSIONS_REQUEST_MEETING:
				generateSettings(LayoutInflater.from(getActivity()), mContent);
				break;
		}
	}

	// for repeated ui settings
	protected void generateSettings(LayoutInflater inflater, View contentView) {

		final Agent agent = AgentFactory.getAgentFromGuid(getActivity(), mAgentGuid);

		//elements array contains the list of setting elements...
		AgentUIElement elements[] = agent.getSettings(new ConfigureAgentFragmentWrapper());

		boolean advanced = false;
		LinearLayout configItems;
		final LinearLayout advancedConfigItems;
		final LinearLayout advancedConfigItemsContainer;

		configItems = ((LinearLayout) contentView
				.findViewById(R.id.configuration_items));
		// if present, blow away old config items
		configItems.removeAllViews();

		advancedConfigItems = ((LinearLayout) contentView
				.findViewById(R.id.advanced_configuration_items));
		// if present, blow away old advanced config items
		advancedConfigItems.removeAllViews();


		advancedConfigItemsContainer = ((LinearLayout) contentView
				.findViewById(R.id.advanced_configuration_items_wrapper));

		if (elements != null) {
			for (AgentUIElement element : elements) {
				if (element == null) {
					advanced = true;
				} else {
					if (advanced) {
						advancedConfigItems.addView(element.getView(getActivity()));
					}
					//if the element are not null then else block is executed...
					else {
						configItems.addView(element.getView(getActivity()));
					}
				}
			}
		}

		advancedConfigItemsContainer.setVisibility(View.GONE);

		if (!advanced) {
			// no advanced items; hide more settings button
			FrameLayout moreSettings = ((FrameLayout) contentView.findViewById(R.id.more_more_settings_frame));
			moreSettings.setVisibility(View.GONE);
		}

		mActivityResultLookup = new SparseArray<SettingSaver>();

		for (int i = 0; i < elements.length; i++) {
			if ((elements[i] != null)
					&& (elements[i].getActivityResultKey() != 0)) {
				mActivityResultLookup.put(
						elements[i].getActivityResultKey(),
						(SettingSaver) elements[i]);
			}
		}

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		int requestCodeLowerBits = requestCode & 32767;

		if (mActivityResultLookup != null) {
			SettingSaver ss = mActivityResultLookup.get(requestCodeLowerBits);
			if (ss != null) {
				if (resultCode == Activity.RESULT_OK) {
					ss.saveSetting(data);
				}
			}
		}
	}

	protected class ResetSettingsTask extends AsyncTask<Void, Agent, Agent> {
		protected Context mContext;
		protected ProgressDialog mDialog;
		protected Runnable mOnPostRunnable;

		public ResetSettingsTask(Context context, ProgressDialog dialog) {
			mContext = context;
			mOnPostRunnable = null;
			mDialog = dialog;
		}

		@Override
		protected Agent doInBackground(Void... nothing) {

			Agent agent = AgentFactory.getAgentFromGuid(mContext, mAgentGuid);

			if (agent == null) {
				return null;
			}

			try {
				agent.resetSettingsToDefault(mContext, true);

				if (mDialog != null) {
					Thread.sleep(400); // so people can see dialog
				}

			} catch (Exception e) {
				Logger.e("error resetting preferences for " + mAgentGuid + ": "
						+ e.toString());
				e.printStackTrace();
			}


			return agent;
		}

		@Override
		protected void onPreExecute() {
			mTaskCollection.addTask(this);
		}

		@Override
		protected void onPostExecute(Agent result) {

			// This shows a toast and has to run on main. Moved from
			// doInBackground as a result.
			Logger.d("Async ResetTask completed.");
			generateSettings(getActivity().getLayoutInflater(), mContent);

			if (this.isCancelled()) {
				return;
			}

			mTaskCollection.completeTask(this);

			mMoreShown = false;
			mMoreSettingsButtonContainer.setAlpha(1f);
			mMoreSettingsButtonContainer.setVisibility(View.VISIBLE);


			if (mDialog != null) {
				mDialog.dismiss();
			}
			if (mOnPostRunnable != null) {
				mOnPostRunnable.run();
			}
		}
	}

	protected class UpdateSettingTask extends AsyncTask<Void, Agent, Agent> {
		protected Context mContext;
		protected ProgressDialog mDialog;
		protected String mPrefName;
		protected String mPrefVal;
		protected Runnable mOnPostRunnable;
		protected String mUpdateAgentGuid;

		public UpdateSettingTask(Context context, ProgressDialog dialog, String agentGuid,
								 String prefName, String prefVal) {
			mUpdateAgentGuid = agentGuid;
			mContext = context;
			mOnPostRunnable = null;
			mDialog = dialog;
			mPrefName = prefName;
			mPrefVal = prefVal;
		}

		public UpdateSettingTask(Context context, ProgressDialog dialog,
								 String prefName, String prefVal) {
			mUpdateAgentGuid = mAgentGuid;
			mContext = context;
			mOnPostRunnable = null;
			mDialog = dialog;
			mPrefName = prefName;
			mPrefVal = prefVal;
		}

		public UpdateSettingTask(Context context, String prefName,
								 String prefVal, Runnable onPostRunnable) {
			mUpdateAgentGuid = mAgentGuid;
			mContext = context;
			mOnPostRunnable = onPostRunnable;
			mDialog = null;
			mPrefName = prefName;
			mPrefVal = prefVal;
		}

		@Override
		protected Agent doInBackground(Void... nothing) {

			Agent agent = AgentFactory.getAgentFromGuid(mContext, mUpdateAgentGuid);

			if (agent == null) {
				return null;
			}

			try {

				String prefValAdjusted;
				if ((mPrefVal == null) || (mPrefVal.trim().equals(""))) {
					prefValAdjusted = "empty";
				} else {
					prefValAdjusted = mPrefVal.substring(0,
							Math.min(32, mPrefVal.length()));
				}

				// fixes github issue #692
				// don't want to track individual Meeting Account names
				String prefNameAdjusted = mPrefName;
				if (prefNameAdjusted.startsWith(AgentPreferences.MEETING_ACCOUNTS)) {
					prefNameAdjusted = AgentPreferences.MEETING_ACCOUNTS;
				}

				Usage.logEvent(
						mContext,
						Usage.Events.CHANGE_AGENT_SETTINGS,
						true,
						new Usage.EventProperty(Usage.Properties.AGENT_NAME,
								mAgentGuid),
						new Usage.EventProperty(
								Usage.Properties.SETTING_NAME, prefNameAdjusted),
						new Usage.EventProperty(
								Usage.Properties.SETTING_VALUE, prefValAdjusted));

				if (mDialog != null) {
					Thread.sleep(400); // so people can see dialog
				}

				agent.updatePreference(mPrefName, mPrefVal);
			} catch (Exception e) {
				Logger.e("error updating preference for " + mAgentGuid + ": " + e.toString());
				e.printStackTrace();
			}
			return agent;
		}

		@Override
		protected void onPreExecute() {
			mTaskCollection.addTask(this);
		}

		@Override
		protected void onPostExecute(Agent result) {
			if (this.isCancelled()) {
				return;
			}

			Logger.d("Async UpdateSetting finished for " + mPrefName + " to " + mPrefVal);

			if (mDialog != null) {
				mDialog.dismiss();
			}
			if (mOnPostRunnable != null) {
				mOnPostRunnable.run();
			}

			mTaskCollection.completeTask(this);
		}
	}

	public class ConfigurationFragmentAgentListener implements
			AgentStateListener {

		@Override
		public void agentEnabled() {
			if (mBlocker == null)
				return;

			mBlocker.setVisibility(View.GONE);
		}

		@Override
		public void agentDisabled() {
			if (mBlocker == null)
				return;

			mBlocker.setVisibility(View.VISIBLE);
			mBlockerText.setText(R.string.agent_config_blocker_disabled);


			mBlockerText.setOnClickListener(null);
			mBlockerText.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					Agent agent = AgentFactory.getAgentFromGuid(getActivity(), mAgentGuid);
					agent.install(getActivity(), false, false);
				}
			});

			mAdvancedConfigItems.setVisibility(View.GONE);
			mMoreSettingsButtonContainer.setVisibility(View.VISIBLE);
			mMoreSettingsButtonContainer.setAlpha(1f);

			generateSettings(getActivity().getLayoutInflater(), mContent);
		}

		@Override
		public void agentPaused() {
			if (mBlocker == null)
				return;

			mBlocker.setVisibility(View.GONE);
		}

		@Override
		public void agentStarted() {
			if (mBlocker == null)
				return;

			mBlocker.setVisibility(View.VISIBLE);
			mBlockerText.setText(R.string.agent_config_blocker_started);

			mBlockerText.setOnClickListener(null);
			mBlockerText.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					Agent agent = AgentFactory.getAgentFromGuid(getActivity(), mAgentGuid);
					agent.pause(getActivity());
				}
			});
		}

		@Override
		public void agentFinished() {
			return;
		}

	}
}
