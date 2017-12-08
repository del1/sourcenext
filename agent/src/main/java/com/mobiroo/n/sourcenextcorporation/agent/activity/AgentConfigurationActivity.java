package com.mobiroo.n.sourcenextcorporation.agent.activity;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.widget.CustomViewPager;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentInfoFragment;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.ConfigureAgentFragment;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.FeedFragment;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification.AgentNotificationInterface;
import com.mobiroo.n.sourcenextcorporation.agent.receiver.AgentStatusChangedReceiver;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentStateListener;
import com.mobiroo.n.sourcenextcorporation.agent.util.Usage;
import com.mobiroo.n.sourcenextcorporation.agent.util.tasks.AgentTasksHelper;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification.AgentFirstStartingNotification;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;
import com.mobiroo.n.sourcenextcorporation.agent.util.tasks.AgentTaskCollection;

import java.util.ArrayList;
import java.util.List;

/***
 * AgentConfigurationActivity is the main user interface by which a user
 * interacts with an agent It contains three modes -- install, setup, and
 * configure. install is for uninstalled, setup is used once just after install,
 * and configure is used every subsequent time (until uninstall reverts).
 * 
 * @author omarseyal
 * 
 */
public class AgentConfigurationActivity extends FragmentActivity {
	// elements for all user interfaces
	protected LinearLayout mConfigMenu;
	protected TextView mMenuBarText;
	protected ImageView mMenuIcon;
	protected Switch mOnOffSwitch;


	protected LinearLayout mContainer;

	protected CustomViewPager mViewPager;

	protected String mAgentGuid;
    protected String mFromNotif;
    protected String mFromWidget;
    protected int mTriggerType;

    protected AgentTaskCollection mTaskCollection;

	protected ArrayList<AgentStateListener> mAgentStateListenerList;

	protected enum InstallLoc {
		ON_OFF_BUTTON, STATUS_BAR, UNKNOWN, FIRST_STARTING_NOTIFICATION
	}

	/*
	 * SECTION -- instantiation / onCreate / onPause / etc. methods
	 */
	public AgentConfigurationActivity() {
        super();
        mTaskCollection = new AgentTaskCollection();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
        mTaskCollection.cancelTasks();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_agent_config, menu);

		MenuItem switchItem = menu.findItem(R.id.switch_item);

        if(switchItem != null) {
    		mOnOffSwitch = (Switch) switchItem.getActionView();
        }

		setActionBarUIFromState(AgentFactory.getAgentFromGuid(
                AgentConfigurationActivity.this, mAgentGuid));

		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();

        final ActionBar actionBar = getActionBar();

		mAgentGuid = intent.getStringExtra(MainActivity.EXTRA_AGENT_GUID);
        mFromNotif = intent.getStringExtra(MainActivity.EXTRA_FROM_NOTIF);
        mFromWidget = intent.getStringExtra(MainActivity.EXTRA_FROM_WIDGET);
        mTriggerType = intent.getIntExtra(MainActivity.EXTRA_TRIGGER_TYPE, Constants.TRIGGER_TYPE_UNKNOWN);

		Agent agent = AgentFactory.getAgentFromGuid(
				AgentConfigurationActivity.this, mAgentGuid);

        if(agent == null) {
			finish();
			return;
		}

		Usage.logEvent(this, Usage.Events.OPENED_AGENT,
                false, new Usage.EventProperty(Usage.Properties.AGENT_NAME,
                        mAgentGuid));


		setContentView(R.layout.layout_agent_configuration);

		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setHomeButtonEnabled(true);
		actionBar.setTitle(agent.getName());

		mContainer = (LinearLayout) findViewById(R.id.agent_configuration_container);

		mConfigMenu = (LinearLayout) View.inflate(this,
				R.layout.layout_agent_configuration_status, null);
		mMenuBarText = (TextView) mConfigMenu.findViewById(R.id.agent_configuration_status_text);
		mMenuIcon = (ImageView) mConfigMenu
				.findViewById(R.id.agent_configuration_status_icon);

		mConfigMenu.findViewById(R.id.agent_configuration_status_back_button).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						statusClicked();
					}
				});

		mContainer.addView(mConfigMenu, 0);

		mViewPager = (CustomViewPager) findViewById(R.id.agent_configuration_pager);

		mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageScrollStateChanged(int arg0) { }

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) { }

			@Override
			public void onPageSelected(int arg0) {
				String fragmentName = AgentConfigurationActivity.this.getFragments()[arg0].title;
			}
		});

		mViewPager.setOffscreenPageLimit(4);
		mViewPager.setAdapter(getPagerAdapter());

        // tabs should be added before ui state set, because creating tabs
        // causes onTabSelected to be fired.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        for (int i = 0; i < getFragments().length; i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(getFragments()[i].title)
                            .setTabListener(mTabListener));
        }

		setMainUIFromState(agent);

        if (AgentFirstStartingNotification.NAME.equals(mFromNotif)
                && (agent instanceof AgentNotificationInterface)) {

            // for first started, show info by default
            setCurrentItem(getDefaultDisabledPosition());

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(((AgentNotificationInterface) agent).getNotifFirstStartDialogDescription())
                    .setTitle(agent.getName())
                    .setPositiveButton(R.string.agent_info_in, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            AgentTasksHelper.showStartingNotification(AgentConfigurationActivity.this, mAgentGuid, mTriggerType, mTaskCollection);
                        }
                    }).setNeutralButton(R.string.agent_info_this_time, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    AgentTasksHelper.manualActivateDeactivate(AgentConfigurationActivity.this, mAgentGuid, false, true, mTaskCollection);
                }
            })
                    .setNegativeButton(R.string.agent_info_no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            installUninstallAgent(false, InstallLoc.FIRST_STARTING_NOTIFICATION);
                        }
                    })
                    .create()
                    .show();
        } else {
            setPositionFromState(agent);
        }


        mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

	}

	public void toggleBar() {
		if (mContainer.findViewById(R.id.agent_configuration_status_container) != null) {
			mContainer.removeView(mConfigMenu);
		} else {
			mContainer.addView(mConfigMenu, 0);
		}
	}

    protected void setCurrentItem(int page) {
        ActionBar ab = getActionBar();
        ab.selectTab(ab.getTabAt(page));
    }

	protected void setActionBarUIFromState(Agent agent) {
        if (mOnOffSwitch == null) {
            Logger.d("Possible error: no onOff switch.");
            return;
        }

        mOnOffSwitch.setOnCheckedChangeListener(null);

		if (agent != null && agent.isInstalled()) {
			if (agent.isActive()) {
				mOnOffSwitch.setChecked(true);
				mOnOffSwitch.setEnabled(false);
			} else {
				mOnOffSwitch.setChecked(true);
				mOnOffSwitch.setEnabled(true);
			}
		} else {
			mOnOffSwitch.setChecked(false);
			mOnOffSwitch.setEnabled(true);
		}

        mOnOffSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                mOnOffSwitch.setOnCheckedChangeListener(null);
                mOnOffSwitch.setChecked(!isChecked);
                mOnOffSwitch.setOnCheckedChangeListener(this);
                installUninstallAgent(isChecked, InstallLoc.ON_OFF_BUTTON);
            }

        });
	}


	protected void setUIFromState(Agent agent) {
		setMainUIFromState(agent);
        setActionBarUIFromState(agent);
	}

    protected void setPositionFromState(Agent agent) {
        if(agent.isInstalled()) {
            setCurrentItem(getDefaultEnabledPosition());
        } else {
            setCurrentItem(getDefaultDisabledPosition());
        }
    }
	
	protected void setMainUIFromState(Agent agent) {
		if (agent.isInstalled()) {
			if (agent.isActive()) {
				mMenuBarText.setTextColor(getResources().getColor(
						R.color.status_started));
				mMenuBarText.setText(agent.getStartedStatusMessage());
				mConfigMenu
						.setBackgroundResource(R.drawable.agent_status_bar_started_clickable);
				mMenuIcon.setImageResource((agent.needsUIPause() && (agent.getTriggeredBy() != Constants.TRIGGER_TYPE_MANUAL))
                        ? R.drawable.ic_pause_light : R.drawable.ic_stop_light);
				mMenuIcon.setVisibility(View.VISIBLE);
				if (mContainer.findViewById(R.id.agent_configuration_status_container) == null) {
					mContainer.addView(mConfigMenu, 0);
				}
            } else if (agent.isPaused() && (agent.needsUIPause())) {
                mMenuBarText.setTextColor(getResources().getColor(
                        R.color.status_paused));
                mMenuBarText.setText(agent.getPausedStatusMessage());
                mConfigMenu
                        .setBackgroundResource(R.drawable.agent_status_bar_paused_clickable);
                mMenuIcon.setImageResource(R.drawable.ic_play_light);
                mMenuIcon.setVisibility(View.VISIBLE);
                if (mContainer.findViewById(R.id.agent_configuration_status_container) == null) {
                    mContainer.addView(mConfigMenu, 0);
                }

            } else {
				mMenuBarText.setText(agent.getEnabledStatusMessage());
				mMenuBarText.setTextColor(getResources().getColor(
						R.color.status_enabled));
				mConfigMenu
						.setBackgroundResource(R.drawable.agent_status_bar_enabled_clickable);
				mMenuIcon.setVisibility(View.GONE);
				if (mContainer.findViewById(R.id.agent_configuration_status_container) != null) {
					mContainer.removeView(mConfigMenu);
				}
			}
		} else {
			mMenuBarText.setText(R.string.agent_status_bar_disabled);
			mMenuBarText.setTextColor(getResources().getColor(
					R.color.status_disabled));
			mConfigMenu
					.setBackgroundResource(R.drawable.agent_status_bar_disabled_clickable);
			mMenuIcon.setVisibility(View.GONE);
			if (mContainer.findViewById(R.id.agent_configuration_status_container) == null) {
				mContainer.addView(mConfigMenu, 0);
			}

		}
	}

	@Override
	public void onBackPressed() {
		finish();
	}

	@Override
	public void onResume() {
		super.onResume();
		registerReceiver(mAgentChangedReceiver,
				AgentStatusChangedReceiver.getFilterAllActions());
	}

	private AgentStatusChangedReceiver mAgentChangedReceiver = new AgentStatusChangedReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Agent agent = AgentFactory.getAgentFromGuid(
					AgentConfigurationActivity.this,
					AgentConfigurationActivity.this.mAgentGuid);

            Logger.i("status change detected.");


            if (mAgentStateListenerList != null) {

				if (!agent.isInstalled()) {
					Logger.d("status change: disabled.");
					for (AgentStateListener asl : mAgentStateListenerList)
						asl.agentDisabled();
				} else if (agent.isActive()) {
					Logger.d("status change: started.");
					for (AgentStateListener asl : mAgentStateListenerList)
						asl.agentStarted();
				} else if (agent.isPaused()) {
					Logger.d("status change: paused.");
					for (AgentStateListener asl : mAgentStateListenerList)
						asl.agentPaused();
				} else {
					Logger.d("status change: enabled.");
					for (AgentStateListener asl : mAgentStateListenerList)
						asl.agentEnabled();
                    AgentTasksHelper.checkReceiversAsync(AgentConfigurationActivity.this, mTaskCollection);
				}
			}
			
			setUIFromState(agent);

		}

	};

	@Override
	public void onPause() {
		super.onPause();

        try {
            unregisterReceiver(mAgentChangedReceiver);
        } catch (Exception e) {
            Logger.d("AgentConfigurationActivity: could not unregister receiver:" + e.toString());
        }
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Logger.d("AgentConfigurationActivity received result.");
	}

	protected void statusClicked() {
		Agent agent = AgentFactory.getAgentFromGuid(getApplicationContext(),
				mAgentGuid);
		if (agent.isInstalled()) {
			if (agent.isActive()) {
                AgentTasksHelper.manualActivateDeactivate(this, mAgentGuid, false, true, mTaskCollection);
			} else if (agent.isPaused()) {
                AgentTasksHelper.manualActivateDeactivate(this, mAgentGuid, true, true, mTaskCollection);
			} else {
				// do nothing. user flow is confusing. on/of makes more sense.
				// installUninstallAgent(false);
			}
		} else {
			// do nothing, user flow is confusing. on/off makes more sense
			// installUninstallAgent(true, InstallLoc.STATUS_BAR);
		}
	}

	/*
	 * SECTION -- Pager UI
	 */

	protected AgentConfigurationPagerAdapter mAgentConfigurePagerAdapter;

	protected FragmentStatePagerAdapter getPagerAdapter() {
		if (mAgentConfigurePagerAdapter == null)
			mAgentConfigurePagerAdapter = new AgentConfigurationPagerAdapter(
					getSupportFragmentManager());
		return mAgentConfigurePagerAdapter;
	}

	FragmentHolder[] mViewFrags = null;

	protected int getDefaultEnabledPosition() {
		boolean opened = PrefsHelper.getPrefBool(this,
				AgentPreferences.AGENT_OPENED + mAgentGuid, false);
		Logger.i(AgentPreferences.AGENT_OPENED + mAgentGuid + " : " + opened);
		if (opened) {
			boolean startedFromConfig = PrefsHelper.getPrefBool(this,
					AgentPreferences.AGENT_STARTED_FROM_CONFIG + mAgentGuid, false);
			if (startedFromConfig) {
				PrefsHelper.setPrefBool(this,
						AgentPreferences.AGENT_STARTED_FROM_CONFIG + mAgentGuid, false);
				return mViewPager.getCurrentItem();
			} else {
				return 1;
			}
		} else {
			PrefsHelper.setPrefBool(this, AgentPreferences.AGENT_OPENED
					+ mAgentGuid, true);
			return getDefaultDisabledPosition();
		}
	}

	protected int getDefaultDisabledPosition() {
		return 0;
	}

	protected void populateViewFrags() {
		mViewFrags = new FragmentHolder[2];

		Bundle args1 = new Bundle();
		args1.putString(FeedFragment.AGENT_GUID, mAgentGuid);

		mViewFrags[1] = new FragmentHolder();
		mViewFrags[1].fragment = new ConfigureAgentFragment();

		mViewFrags[1].fragment.setArguments(args1);
		mViewFrags[1].title = getResources().getString(
				R.string.agent_menu_config);

		Bundle args2 = new Bundle();
		args2.putString(FeedFragment.AGENT_GUID, mAgentGuid);

		AgentInfoFragment aif = new AgentInfoFragment();

		mViewFrags[0] = new FragmentHolder();
		mViewFrags[0].fragment = aif;
		mViewFrags[0].fragment.setArguments(args2);
		mViewFrags[0].title = getResources().getString(
				R.string.agent_menu_description);
	}

	protected FragmentHolder[] getFragments() {
		if (mViewFrags == null) {
			populateViewFrags();
		}

		return mViewFrags;
	}

	public class FragmentHolder {
		public Fragment fragment;
		public String title;
	}

	public class AgentConfigurationPagerAdapter extends
			FragmentStatePagerAdapter {
		public AgentConfigurationPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {			
			return AgentConfigurationActivity.this.getFragments()[position].fragment;
		}

		@Override
		public int getCount() {
			return AgentConfigurationActivity.this.getFragments().length;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return AgentConfigurationActivity.this.getFragments()[position].title;
		}
	}

    ActionBar.TabListener mTabListener = new ActionBar.TabListener() {
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            mViewPager.setCurrentItem(tab.getPosition());
        }

        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            mViewPager.setCurrentItem(tab.getPosition());
        }

        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
            // probably ignore this event
        }
    };

	/*
	 * SECTION -- Action Methods
	 */

	private void installUninstallAgent(boolean install, final InstallLoc installLoc) {
		if (!install) {
			Agent agent = AgentFactory.getAgentFromGuid(this, mAgentGuid);

			int message = agent.settingsHaveChanged() ? R.string.agent_uninstall_confirm_warning
					: R.string.agent_uninstall_confirm_warning_nochange;
			if (PrefsHelper.getPrefBool(AgentConfigurationActivity.this, Constants.PREF_PRESERVE_SETTINGS_AGENT_UNINSTALL, false)) {
				message = R.string.agent_uninstall_confirm_warning_nochange;
			}

			new AlertDialog.Builder(this)
					.setIcon(R.drawable.ic_launcher)
					.setTitle(R.string.agent_uninstall_confirm)
					.setMessage(message)
					.setPositiveButton(R.string.yes,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									Usage.logEvent(AgentConfigurationActivity.this, convertInstallLocToUsageEvent(false, installLoc), false, new Usage.EventProperty(Usage.Properties.AGENT_NAME, mAgentGuid));
                                    AgentTasksHelper.installUninstall(AgentConfigurationActivity.this, mAgentGuid, false, mTaskCollection);

								}
							})
					.setNegativeButton(R.string.no, null)
                    .show();
		} else {
			Agent agent = AgentFactory.getAgentFromGuid(this, mAgentGuid);
			if (!agent.isInstalled()) {
				if (installLoc != InstallLoc.UNKNOWN) {
					Usage.logEvent(this, convertInstallLocToUsageEvent(true, installLoc), false, new Usage.EventProperty(Usage.Properties.AGENT_NAME, mAgentGuid));
				}
                AgentTasksHelper.installUninstall(this, mAgentGuid, true, mTaskCollection);
			}
		}
	}




    private Usage.Events convertInstallLocToUsageEvent(boolean install, InstallLoc installLoc) {
        switch (installLoc)  {
            case ON_OFF_BUTTON:
                return install ? Usage.Events.INSTALL_AGENT_WITH_ON_OFF_BUTTON : Usage.Events.UNINSTALL_AGENT_WITH_ON_OFF_BUTTON;
            case FIRST_STARTING_NOTIFICATION:
                return install ? Usage.Events.UNDEFINED : Usage.Events.UNINSTALL_AGENT_WITH_FIRST_STARTING_NOTIFICATION;
            case STATUS_BAR:
                return install ? Usage.Events.INSTALL_AGENT_WITH_STATUS_BAR : Usage.Events.UNDEFINED;
        }
        return Usage.Events.UNDEFINED;
    }



	public void addAgentStateListener(AgentStateListener agentStateListener) {
		if (mAgentStateListenerList == null)
			mAgentStateListenerList = new ArrayList<AgentStateListener>();

		mAgentStateListenerList.add(agentStateListener);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
			@NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case Constants.PERMISSIONS_REQUEST_READ_PHONE_STATE:
            case Constants.PERMISSIONS_REQUEST_SMS:
            case Constants.PERMISSIONS_REQUEST_VOICE_RESPONSE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0) {

                    for(int grantResult : grantResults) {
                        if(grantResult != PackageManager.PERMISSION_GRANTED) {
                            // permission denied
                            sendPermissionResultBroadcast(false);
                            return;
                        }
                    }

                    // All permissions granted
                    sendPermissionResultBroadcast(true);
                } else {
                    sendPermissionResultBroadcast(false);
                }

                break;
        }

		// Forward permission request result to the fragments
		List<Fragment> fragments = getSupportFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
			}
		}
	}

    private void sendPermissionResultBroadcast(boolean isPermissionGranted) {
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(new Intent(Constants.ACTION_PERMISSION_RESULT)
                        .putExtra("is_permissions_granted", isPermissionGranted));
    }
}
