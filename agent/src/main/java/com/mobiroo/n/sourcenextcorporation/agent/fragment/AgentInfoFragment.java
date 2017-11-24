package com.mobiroo.n.sourcenextcorporation.agent.fragment;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.activity.AgentConfigurationActivity;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.item.BatteryAgent;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.animation.FadeAnimation;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentStateListener;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;
import com.mobiroo.n.sourcenextcorporation.agent.util.Usage;
import com.mobiroo.n.sourcenextcorporation.agent.util.Usage.Events;
import com.mobiroo.n.sourcenextcorporation.agent.util.tasks.AgentTaskCollection;
import com.mobiroo.n.sourcenextcorporation.agent.util.tasks.AgentTasksHelper;

import java.util.ArrayList;
import java.util.Iterator;

public class AgentInfoFragment extends Fragment {

    public static final String AGENT_GUID = "agent_guid";

    private static final int REQUEST_WRITE_SETTINGS = 1;

    protected boolean mMoreShown;
	protected String mAgentGuid;
	
	protected TextView mNextActivates;
	protected TextView mStartButton;
	protected TextView mStartButtonBottom;

    protected AgentTaskCollection mTaskCollection = new AgentTaskCollection();

	@Override
	public void onSaveInstanceState (Bundle outState) {
		if(outState != null) {
			outState.putBoolean("mMoreShown", mMoreShown);
		}			
	}

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTaskCollection.cancelTasks();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTaskCollection = new AgentTaskCollection();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
    	
    	if(savedInstanceState != null) {
    		mMoreShown = savedInstanceState.getBoolean("mMoreShown", false);
    	} else {
    		mMoreShown = false;
    	}
    	
    	Bundle args = getArguments();
    	mAgentGuid = args.getString(AGENT_GUID);
    	Agent agent = AgentFactory.getAgentFromGuid(getActivity(), mAgentGuid);

    	View triggerView = inflater.inflate(R.layout.list_item_config_description, container, false);

        // generate permissions block (or hide permissions block if none are present)
        TextView descriptionView = (TextView) triggerView.findViewById(R.id.longDescription);
        descriptionView.setText(agent.getLongDescription());
        
        mNextActivates = (TextView) triggerView.findViewById(R.id.nextActivates);
    	mNextActivates.setText(agent.getEnabledStatusMessage());

        if (agent.isInstalled() && (!agent.isActive()) && (!agent.isPaused())) {
            triggerView.findViewById(R.id.nextActivatesContainer).setVisibility(View.VISIBLE);
        } else {
            triggerView.findViewById(R.id.nextActivatesContainer).setVisibility(View.GONE);
        }
        
        Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "Roboto-Light.ttf");  
        descriptionView.setTypeface(font);          
        
        // generate permissions block (or hide permissions block if none are present)
        LinearLayout triggers = (LinearLayout) triggerView.findViewById(R.id.triggers);
        for(int i=0; i<agent.getTriggerArray().length; i++) {
        	triggers.addView(agent.getTriggerArray()[i].getView(getActivity()));
        }
        
        final LinearLayout moreInfo = (LinearLayout) triggerView.findViewById(R.id.more_info_content_container);
        moreInfo.setVisibility(View.GONE);
        
        final LinearLayout moreInfoButtonContainer = (LinearLayout) triggerView.findViewById(R.id.more_info_button_container);
        TextView moreInfoButton = (TextView) triggerView.findViewById(R.id.more_info_text);
        moreInfoButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mMoreShown = true;
				Usage.logEvent(getActivity(), Events.MORE_INFO_CLICKED, true,
                        new Usage.EventProperty(Usage.Properties.AGENT_NAME, mAgentGuid));
				FadeAnimation.crossfade(moreInfo, moreInfoButtonContainer, 1000);
			}
              });

        mStartButton = (TextView) triggerView.findViewById(R.id.manual_start_text);
        mStartButtonBottom = (TextView) triggerView.findViewById(R.id.manual_start_text_bottom);

        if(!agent.isStartable()) {
            mStartButton.setVisibility(View.GONE);
            mStartButtonBottom.setVisibility(View.GONE);
        } else {
            ArrayList<TextView> startButtons = new ArrayList<TextView>();
            startButtons.add(mStartButton);
            startButtons.add(mStartButtonBottom);
            Iterator<TextView> buttonIterator = startButtons.iterator();
            while (buttonIterator.hasNext()) {
                TextView curButton = (TextView) buttonIterator.next();
                curButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Agent agent = AgentFactory.getAgentFromGuid(getActivity(), mAgentGuid);
                        PrefsHelper.setPrefBool(getActivity(),
                                AgentPreferences.AGENT_STARTED_FROM_CONFIG + mAgentGuid, true);
                        AgentTasksHelper.manualActivateDeactivate(getActivity(), mAgentGuid, !agent.isActive(), agent.isPaused(), mTaskCollection);
                    }
                });
                if (agent.isInstalled() && !agent.isActive()) {
                    curButton.setVisibility(View.VISIBLE);
                }
            }
        }

        if(mMoreShown) {
        	moreInfo.setVisibility(View.VISIBLE);
        	moreInfoButtonContainer.setVisibility(View.GONE);
        }
    	
    	View contentView = inflater.inflate(R.layout.fragment_agent_info, null);
    	((LinearLayout) contentView.findViewById(R.id.configuration_items)).addView(triggerView);
    	
    	AgentConfigurationActivity activity = (AgentConfigurationActivity) getActivity();
    	activity.addAgentStateListener(new InfoFragmentAgentListener());

        // Check modify settings permission for battery agent
		if (agent.getGuid().equals(BatteryAgent.HARDCODED_GUID)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			checkModifySettingsPermission();
		}
		return contentView;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkModifySettingsPermission() {
        // Check for the permissions to modify system settings
        if (!Settings.System.canWrite(getActivity())) {
            showSettingsPermissionInfoDialog();
        }
    }

    @SuppressLint("NewApi")
    private void showSettingsPermissionInfoDialog() {
        new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.modify_system_settings_info))
                .setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface anInterface, int i) {
                        startModifySettingsIntent();
                        anInterface.dismiss();
                    }
                })
                .setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface anInterface, int i) {
                        anInterface.dismiss();
                    }
                })
                .show();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void startModifySettingsIntent() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + getActivity().getPackageName()));

        if(intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_WRITE_SETTINGS);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

//        switch (requestCode) {
//            case REQUEST_WRITE_SETTINGS:
//                if (resultCode != Activity.RESULT_OK) {
//                    getActivity().finish();
//                }
//        }
    }

    public class InfoFragmentAgentListener implements AgentStateListener {

		@Override
		public void agentEnabled() {
			mNextActivates.setVisibility(View.VISIBLE);
			mStartButton.setVisibility(View.VISIBLE);
			mStartButtonBottom.setVisibility(View.VISIBLE);
		}

		@Override
		public void agentDisabled() {
			mNextActivates.setVisibility(View.GONE);
			mStartButton.setVisibility(View.INVISIBLE);
			mStartButtonBottom.setVisibility(View.INVISIBLE);
		}

		@Override
		public void agentPaused() {
			mStartButton.setVisibility(View.VISIBLE);
			mStartButtonBottom.setVisibility(View.VISIBLE);
		}

		@Override
		public void agentStarted() {
			mStartButton.setVisibility(View.INVISIBLE);
			mStartButtonBottom.setVisibility(View.INVISIBLE);
		}
		
		@Override
		public void agentFinished() {
			mStartButton.setVisibility(View.VISIBLE);
			mStartButtonBottom.setVisibility(View.VISIBLE);
		}

	}
}
